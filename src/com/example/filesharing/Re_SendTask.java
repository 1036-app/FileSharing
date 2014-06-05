package com.example.filesharing;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

public class Re_SendTask extends java.util.TimerTask
{
	public String sub_fileID;
	public String fileName;
	public notify_sendingTask  subfiletask=null;
	public Timer subfiletimer=null;
	public Packet[] SavedencodedPkts=null; 
	public sendFileFunction sendFunction=new sendFileFunction();
	public fecFuntion fecfunction=new fecFuntion();
	Re_SendTask (String fileid)
	{
		this.sub_fileID=fileid;
	}
	 public void run() 
  	 { 
		 int i=0;
		 int number=0;
		 String sharedPath="//sdcard//SharedFiles";
		 ArrayList<Integer> nos=null;
		 String []aa=null;
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
      			   aa=sub_fileID.split("--");  //aa[0]中放的是fileid
                   sub_no=Integer.parseInt(aa[1]); 
                   fileid=aa[0];
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
         }  
         if(i!=ssize)
         {
     	   if(FileSharing.sendFiles.containsKey(fileid))
		    {
     		 FileInputStream fis=null;
     	     BufferedInputStream in=null;
     	     int filelength=0;
     	     int num=0;
		     fileName=FileSharing.sendFiles.get(fileid);
		     String file=sharedPath+"//"+fileName;
		     try {
					fis = new FileInputStream(file);
					filelength = fis.available();
					in = new BufferedInputStream(fis);
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
		     if(filelength<=FileSharing.maxfilelength)
		    	   num=1;
		     else
		     {
		        if(filelength%FileSharing.maxfilelength==0)
					num=filelength/FileSharing.maxfilelength;
				else
					num=filelength/FileSharing.maxfilelength+1;
		     }
		     int length=0;
	 		 boolean send=true;
	 				if(type==2)
	 				{
	 					for(int mm=0;mm<num;mm++)
	 					{
	 					  byte[] data = new byte[FileSharing.maxfilelength];
	 					  send=true;
	 					  try {
	 				 			 length=in.read(data,0,FileSharing.maxfilelength);
		 						} catch (IOException e) 
		 						{
		 							e.printStackTrace();
		 						}
	 					   for(int nn=0;nn<nos.size();nn++)	
	 					   {
	 						  if(mm==nos.get(nn))
	 						   {
	 						 	send=false;
	 							nos.remove(nn);
	 							nn--;
	 							break;
	 						  }
	 					   }
	 					if(send)
	 					 {
	 						
	 						System.out.println("发送wenjian的快号： "+mm);	
		 		 			System.out.println("##########发送数据长度：  "+length);	
	
		 				String sub_id =sub_fileID+"--"+mm;
		 				Packet[]sendPacket=null;
		 	 			sendPacket=sendFunction.file_blocks(data,length,sub_id,fileName,num,filelength);
		 	 			   if(sendPacket!=null)
		 		           {
		 		            sendThread sThread=new sendThread(sendPacket,FileSharing.bcastaddress,FileSharing.port,0,sendPacket[0].data_blocks,0);
		 		            sThread.start();    
		 		            String message = "发送的包的个数: "+sendPacket[0].data_blocks;
		 		           FileSharing.messageHandle(message);			
		 		           }
	 				  }	
	 				}			
	 			}
	 		else if(type==1)
	 		{
	 		byte[] data = new byte[FileSharing.maxfilelength];
	 	    System.out.println("超时，文件丢失的包数目："+number);
	 	    String []bb=sub_fileID.split("-");
	 	    FileSharing.writeLog("Timeout:"+bb[1]+"--"+bb[bb.length-1]+", miss:"+number+","+"sending data packets	"+"\r\n");
	 		try {
	 			 in.skip(FileSharing.maxfilelength*sub_no);
	 			 length=in.read(data,0,FileSharing.maxfilelength);
				} catch (IOException e) 
				{
				e.printStackTrace();
				}
	 		 String message=sub_fileID+" 超时，再发 "+number+" 个包";
	 		 FileSharing. messageHandle(message);
		     Packet[] sPacket=null; 
		     if(SavedencodedPkts!=null&&SavedencodedPkts[0].sub_fileID.equals(sub_fileID))
		     {	 
		    	 sPacket=SavedencodedPkts;
		     }
		     else
		     {
		    	
		        sPacket=fecfunction.encode(data,length,sub_fileID,fileName,num,filelength);
		        SavedencodedPkts=sPacket;
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
		        sendThread sThread=new sendThread(sPacket,FileSharing.bcastaddress,FileSharing.port,Start,number,0);
				sThread.start();         
	 			//发送完冗余包后进行计时
	 			if(FileSharing.sending_subfileid.equals(sub_fileID))
	 			{
	 			synchronized(FileSharing.subfiles)
	 			{
	 			subfiletask=new notify_sendingTask (fileName,sub_fileID,num);
	 			subfiletimer=new Timer(true);
	 			subfiletimer.schedule(subfiletask, FileSharing.subtime, FileSharing.subtime);
	 			FileSharing.subfiles.put(sub_fileID,subfiletimer);
	 			}
	 		   }
		     }
		    }	
	 	 } 
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
		}  	
       } 
         long end=System.currentTimeMillis();
         String []bb=sub_fileID.split("-");
         FileSharing.writeLog("re-send:"+bb[1]+"--"+bb[bb.length-1]+",	"+(end-start)+"ms,	"+"\r\n");
  	  } 
 }
