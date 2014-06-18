package com.example.filesharing;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Timer;

public class Re_SendTask extends java.util.TimerTask
{
	public String sub_fileID;
	public String fileName;
	public Timer subfiletimer=null;
	sendFile_blockThread sbt=null;
	Re_SendTask (String fileid)
	{
		this.sub_fileID=fileid;
	}
	 public void run() 
  	 { 
		 int i=0;
		 int number=0;
		 ArrayList<Integer> nos=null;
		 String []fileID_break=null;
		 String fileid=null;
		 int type=0;
		 int sub_no=0;
		 if(FileSharing.feedTimers.containsKey(sub_fileID))
  	     {
         System.out.println("准备发送数据，取消计时器 "+sub_fileID);
         FileSharing.feedTimers.get(sub_fileID).cancel();  
         FileSharing.feedTimers.remove(sub_fileID);
  	     } 
	     long start=System.currentTimeMillis();
  
		 int ssize=0;
	  synchronized(FileSharing.Feedpkts)
       {
         for(;i<FileSharing.Feedpkts.size();i++)
         {
      	   if(FileSharing.Feedpkts.get(i).sub_fileID.equals(sub_fileID))
              {
      		     type=FileSharing.Feedpkts.get(i).type;
      		     if(FileSharing.Feedpkts.get(i).type==1)
      		      {
      			   number=FileSharing.Feedpkts.get(i).nos.get(0);
      			   fileID_break=sub_fileID.split("--");  //fileID_break[0]中放的是fileid
                   sub_no=Integer.parseInt(fileID_break[1]); 
                   fileid=fileID_break[0];
      		      }
      		      else
      		      {
      		    	nos=new ArrayList<Integer>();
      		   		nos=FileSharing.Feedpkts.get(i).nos;
      		     	fileid=sub_fileID;
      		      }
      		     ssize=FileSharing.Feedpkts.size();
      	     break;
             }
          } 
         }  //end  synchronized
         if(i!=ssize) 
         {
     	   if(FileSharing.sendFiles.containsKey(fileid))
     	   {
     	     long filelength=0;
     	     int num=0;
		     fileName=FileSharing.sendFiles.get(fileid);
		     String file=fileName;
		     File ff=new File(file);
		     RandomAccessFile raf=null;	
             try {
				raf = new RandomAccessFile(ff,"r");
				filelength=raf.length();
			  } catch (Exception e) 
			  {
				e.printStackTrace();
			  }
		     if(filelength<=FileSharing.maxfilelength)
		    	   num=1;
		     else
		     {
		        if(filelength%FileSharing.maxfilelength==0)
					num=(int)(filelength/FileSharing.maxfilelength);
				else
					num=(int)(filelength/FileSharing.maxfilelength+1);
		     }
		     int length=0;
	 		  if(type==2)
	 		  {
	 			 sbt=new sendFile_blockThread(fileid,nos, num);
	 			 sbt.start();
	 	      }
	 		else if(type==1)
	 		{
	 		byte[] data = new byte[FileSharing.maxfilelength];
	 	    System.out.println("超时，文件丢失的包数目："+number);
	 	    String []fileid_break=sub_fileID.split("-");
	 	    FileSharing.writeLog("Timeout:"+fileid_break[1]+"--"+fileid_break[fileid_break.length-1]+", miss:"+number+","+"sending data packets	"+"\r\n");
	 		 String message=sub_fileID+" 超时，再发 "+number+" 个包";
	 		 FileSharing. messageHandle(message);
		     Packet[] sPacket=null; 
		     boolean isencode=true;
		     synchronized(FileSharing.encodedPacket)
		     {
		       for(int l=0;l<FileSharing.encodedPacket.size();l++)
		       {
		    	 if(FileSharing.encodedPacket.get(l)[0].sub_fileID.equals(sub_fileID))
		    	 {
		    		 sPacket=FileSharing.encodedPacket.get(l);
		    		 isencode=false;
		    		 System.out.println("！！！找到了已经编码的块："+sub_fileID);
		    		 FileSharing.writeLog("找到了已经编码的块："+sub_fileID+"\r\n");
		    		 break;
		    	 }
		       }
		       if(isencode&&sPacket==null)
		       {
		    	   FileInputStream fis=null;
		     	    BufferedInputStream in=null;
		     	   File f=new File(file);
					if(f.exists())
					{
			 	      try {
					fis = new FileInputStream(file);
					filelength = fis.available();
					in = new BufferedInputStream(fis);
					in.skip(FileSharing.maxfilelength*sub_no);
		 			length=in.read(data,0,FileSharing.maxfilelength);
				    } catch (Exception e) 
				    {
					e.printStackTrace();
				    }
					}
		    	 sPacket=FileSharing.fecfunction.encode(data,length,sub_fileID,fileName,num,filelength);
		    	 System.out.println("????重新发送要自己编码");
		    	 FileSharing.writeLog("重新发送要自己编码"+sub_fileID+"\r\n");
		    	 if(FileSharing.encodedPacket.size()==FileSharing.stored_blocks)
		    	      FileSharing.encodedPacket.remove(0);
		    	 FileSharing.encodedPacket.add(sPacket);   
		       }
		     } 
		     synchronized(this)
		     {
		     int Start=FileSharing.nextseq.get(sub_fileID);
		     FileSharing.nextseq.remove(sub_fileID); 
		     System.out.println("本次开始发送的子包序号 "+Start);
		     if(sPacket!=null)
		     {
		    	 int nextStart=Start+number;
				 int total=sPacket[0].data_blocks+sPacket[0].coding_blocks;
				 if(nextStart>=total)
				   {
				       nextStart=nextStart-total;
				       FileSharing.nextseq.put(sub_fileID,nextStart);
				   }
				    else
				   {
				    	FileSharing.nextseq.put(sub_fileID,nextStart);
				   }
				System.out.println("下一次开始发送的子包序号： "+nextStart); 
			long end11=System.currentTimeMillis();
				FileSharing.sThread.inital(sPacket,FileSharing.bcastaddress,FileSharing.port,Start,number,0);
				FileSharing.sThread.sending();  
			long end12=System.currentTimeMillis();
		    FileSharing.writeLog("re-sendingThread-time:  "+sub_fileID+"  "+(end12-end11)+"ms"+"\r\n");
				//发送完冗余包后进行计时
		     }
		   } //end synchronized	
		     synchronized(FileSharing.Feedpkts)
		 	   {
		 		for(int kk=0;kk<FileSharing.Feedpkts.size();kk++)
		 		{
		 		 if(FileSharing.Feedpkts.get(kk).sub_fileID.equals(sub_fileID))
		 		   {
		 			  FileSharing.Feedpkts.remove(kk);
		 			  break;
		 		   }
		 		 }
		 	  }
	 	 } // end if(type==1)
		}  //end if
       } // end if(i!=ssize)
         long end=System.currentTimeMillis();
         FileSharing.writeLog("re-send: "+sub_fileID+",	"+(end-start)+"ms,	"+"\r\n");
  	  } 
 }
