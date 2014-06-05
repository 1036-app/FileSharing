package com.example.filesharing;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import eu.vandertil.jerasure.jni.Jerasure;
import eu.vandertil.jerasure.jni.ReedSolomon;

public class fecFuntion 
{
	public  String sharedPath="//sdcard//SharedFiles";
	
	public  synchronized Packet[] encode(byte[] filedata,int subFileLength,String sub_fileID,String filename,int totalsubFiles,int FileLength)
    {   
    	 long start=System.currentTimeMillis();
         int data_blocks;
         int coding_blocks;
         Packet[] plist =null;
         int lastlength= FileSharing.blocklength;
          if(subFileLength % FileSharing.blocklength == 0)
			{
				data_blocks = subFileLength/FileSharing.blocklength;
			}
			else
			{
				data_blocks = subFileLength/FileSharing.blocklength+1;
				lastlength= subFileLength%FileSharing.blocklength;
			}
 	
 			coding_blocks = data_blocks;
 			plist = new Packet[coding_blocks+data_blocks];
 			byte[][] origindata = new byte[data_blocks][FileSharing.blocklength];
 			byte[][] encodedata = new byte[coding_blocks][FileSharing.blocklength];
 			
 			for(int i=0;i<data_blocks;i++)
 			{
 				System.arraycopy(filedata, i*FileSharing.blocklength, origindata[i], 0, FileSharing.blocklength);
 				byte[] data = new byte[FileSharing.blocklength];
 				System.arraycopy(origindata[i], 0, data, 0,FileSharing. blocklength);
 				int send_blockLength=0;
 				if(i==data_blocks-1)	
 					send_blockLength=lastlength;	   
 				else
 					send_blockLength=FileSharing.blocklength;
 				Packet p = new Packet(0,subFileLength,coding_blocks, data_blocks, i,send_blockLength,FileLength);
 				p.data = data;
 				p.filename=filename;
 				p.totalsubFiles=totalsubFiles;
 				plist[i] = p;
 				p.sub_fileID=sub_fileID;
 			}
 			
 	        int w=16; 
 	        int[] matrix = ReedSolomon.reed_sol_vandermonde_coding_matrix(data_blocks, coding_blocks, w);
 	        Jerasure.jerasure_matrix_encode(data_blocks, coding_blocks, w, matrix, origindata , encodedata, FileSharing.blocklength);
 	        for(int i=data_blocks;i<data_blocks+coding_blocks;i++)
 			 {
 				byte[] data = new byte[FileSharing.blocklength];
 				System.arraycopy(encodedata[i-data_blocks], 0, data, 0,FileSharing.blocklength);
 				Packet p = new Packet(0,subFileLength,coding_blocks, data_blocks, i, FileSharing.blocklength,FileLength);
 				p.data = data;
 				p.filename=filename;
 				p.totalsubFiles=totalsubFiles;
 				plist[i] = p;
 				p.sub_fileID=sub_fileID;
 			  }	       	
 	      String message = plist[0].sub_fileID+" 编码生成冗余包的个数："+plist.length;
 	      FileSharing.messageHandle(message);
 	      long end =System.currentTimeMillis();
 	      FileSharing.total_encode_timer+=(end-start);
		  return plist;
	}
	
	public  synchronized void decode(Packet[] plist)
    {
		
		System.out.println("进入解码函数  收到包的个数："+plist.length+" 开始解码");
    	Packet p = plist[plist.length-1];
    	int data_blocks = p.data_blocks;
    	int coding_blocks = 0;
    	int block_length=FileSharing.blocklength;
    	int file_length = p.subFileLength;  //该块文件的长度
    	String recvFilename=p.filename;
    	byte[] writein = new byte[file_length];
    	
    	byte[][] recvdata = new byte[data_blocks][block_length];
    	ArrayList<Integer> reverasure = new ArrayList<Integer>();
    	byte[][] recvcode =null;
    	int len = plist.length;
    	boolean isfirst=true;
    	for(int i=0; i<plist.length; i++)
    	{
    		Packet s = plist[i];
    		reverasure.add(s.seqno);
    		if(s.seqno<data_blocks)
    		{
    			recvdata[s.seqno] = s.data;
    		}
    		else
    		{
    			if(isfirst)
    			{
    			 coding_blocks = s.coding_blocks;
    			 recvcode = new byte[coding_blocks][block_length];
    			 isfirst=false;
    			}
    			recvcode[s.seqno-data_blocks] = s.data;
    		}	
    	} 	
    	if(coding_blocks ==0)  //若是coding_blocks =0，不必解码，直接恢复。
    	{	

    		int set=0;
    	    for(int pp=0;pp<plist.length;pp++)
    	    {
    	     System.arraycopy(plist[pp].data, 0, writein, set, plist[pp].data_length);
    	     set=set+plist[pp].data_length;
    	    }
    	}
      else
      {
    	int w=16;
    	int j=0;
    	int[] erasure = new int[data_blocks+coding_blocks+1];
    	for(int i=0;i<data_blocks+coding_blocks;i++)
    	{
    		if(reverasure.contains(i))
    		{
    			continue;
    		}
    		else
    		{
    			erasure[j]=i;
    			j++;
    		}
    	}
    	erasure[j]=-1;
    	
    	
    	int[] matrix = ReedSolomon.reed_sol_vandermonde_coding_matrix(data_blocks, coding_blocks, w);
    	Jerasure.jerasure_matrix_decode(data_blocks, coding_blocks, w, matrix, false, erasure, recvdata, recvcode, block_length);
    	int remain = file_length;
        int k=0;
        int offset=0;
    	while(remain > 0)
    	{
    		int process = Math.min(remain, block_length);
    		System.arraycopy(recvdata[k], 0, writein, offset, process);
    		offset = offset + process;
    		remain = remain - process;
    		k++;
    	} 
      }
    	if(plist[0].totalsubFiles==1)
    	{
    	  String message="生成文件";
    	  FileSharing.messageHandle(message);
    	  FileSharing.recvFiles.add(recvFilename);  //放入接收文件列表
    	  try {
    		String encodeFile=sharedPath+"//"+recvFilename;
    		BufferedOutputStream bos;
    		bos = new BufferedOutputStream(new FileOutputStream(encodeFile));
			bos.write(writein);
	        bos.close();
		    } catch (Exception e) 
		   {		
			e.printStackTrace();
		   }
    	 String []aa=plist[0].sub_fileID.split("--");
    	 SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
		 Date curDate = new Date(System.currentTimeMillis());
		 String m = formatter.format(curDate);
		 String mm="发送文件 "+aa[0]+"的时间: "+m; 
		 FileSharing.messageHandle(mm);
		 FileSharing.writeLog(""+"\r\n");
		 FileSharing.writeLog("%%%"+plist[0].filename+",	");
		 FileSharing.writeLog(plist[0].sub_fileID.split("--")[0].split("-")[1]+",	");
		 FileSharing.writeLog(plist[0].fileLength+",	");
		 FileSharing.writeLog(System.currentTimeMillis()+",	");
		 FileSharing.writeLog(m+",	"+"\r\n");
    	}
    	else
    	{
    		//该文件有多块
    	send_FbpTask subfileTask=null;
    	Timer subfiletimer=null;
    	String []aa=plist[0].sub_fileID.split("--"); //aa[0]中放的是fileid
    	int sub_no=Integer.parseInt(aa[1]);
    	
    	if(FileSharing.subFile_nums.containsKey(aa[0]))
    	{
            //以前就收到过该文件（<10m）的块
			RecvSubfileData rsfd=new RecvSubfileData(aa[0],plist[0].filename,writein,sub_no);
			FileSharing.RecvSubFiles.add(rsfd);
    		int t=FileSharing.subFile_nums.get(aa[0]);
    		t=t+1;
    		FileSharing.subFile_nums.remove(aa[0]);
    		FileSharing.subFile_nums.put(aa[0], t);
			System.out.println("收到的块号："+sub_no);
            System.out.println("该文件总共块数："+plist[0].totalsubFiles+" 当前收到了块："+FileSharing.subFile_nums.get(aa[0]));
            FileSharing.subfileTimers.get(aa[0]).cancel(); //只要有新的数据块接收到，就取消块丢失计时器,再加新的计时器
            FileSharing.subfileTimers.remove(aa[0]);
            FileSharing.recv_subfiels_no.get(aa[0]).add(sub_no);
            subfileTask=new send_FbpTask(aa[0],FileSharing.recv_subfiels_no.get(aa[0]));
            subfiletimer = new Timer(true);
    		FileSharing.subfileTimers.put(aa[0], subfiletimer);
    		subfiletimer.schedule(subfileTask,FileSharing.block_time,FileSharing.block_time);
            
           
            if(FileSharing.subFile_nums.get(aa[0])==plist[0].totalsubFiles)
    		{
            	FileSharing.recv_subfiels_no.remove(aa[0]);
            	FileSharing.subfileTimers.get(aa[0]).cancel();
            	FileSharing.subfileTimers.remove(aa[0]);
               
                 String message="开始生成文件";
                 FileSharing.messageHandle(message);
                 FileSharing.recvFiles.add(recvFilename);  //放入接收文件列表中
                 String encodeFile=sharedPath+"//"+recvFilename;
                 RandomAccessFile raf=null;	
                   try {
					raf = new RandomAccessFile(encodeFile,"rw");
					raf.setLength(plist[0].fileLength);
				} catch (Exception e) {
					e.printStackTrace();
				}
    						
                 for(int nn=0;nn<FileSharing.RecvSubFiles.size();nn++)
    			 {        	
    				if(FileSharing.RecvSubFiles.get(nn).fileID.equals(aa[0]))
    				{
    				   try {  
    			    	    raf.seek(0);   // 1：绝对定位              
    			    	    raf.skipBytes(FileSharing.maxfilelength*FileSharing.RecvSubFiles.get(nn).sub_num);//如果为负值，不跳过任何字节
    			    	    raf.write(FileSharing.RecvSubFiles.get(nn).data);	
    						} catch (Exception e1) 
    						{
    							e1.printStackTrace();
    						}
    				 FileSharing.RecvSubFiles.remove(nn);
    				 nn--;
    				}
    			 }	
                 try {
 					raf.close();
 				} catch (IOException e) 
 				{
 					e.printStackTrace();
 				}  
                FileSharing.subFile_nums.remove(aa[0]);
    		    SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
    	        Date curDate = new Date(System.currentTimeMillis());
    	        String m = formatter.format(curDate);
    	        String mm="接收文件"+aa[0]+"的时间 : "+m;
    	        FileSharing.messageHandle(mm);
    	        FileSharing.writeLog(""+"\r\n");
    	        FileSharing.writeLog("%%%"+plist[0].filename+",	");
    	        FileSharing.writeLog(plist[0].sub_fileID.split("--")[0].split("-")[1]+",	");
    	        FileSharing.writeLog(plist[0].fileLength+",	");
    	        FileSharing.writeLog(System.currentTimeMillis()+",	");
    	        FileSharing.writeLog(m+",	"+"\r\n");
    		} 
    		
    	}  
    	else
    	{
    		//收到了大于10m的文件或者第一次收到<10m的文件
    		if(plist[0].fileLength>1024*10240)  //文件的长度大于10m,直接写出
    		{
    			if(FileSharing.sub_nums.containsKey(aa[0]))
    			{   //以前已经收到过大于10m的文件块
    				int bb=FileSharing.sub_nums.get(aa[0]);
    				FileSharing.sub_nums.remove(aa[0]);
    				FileSharing.sub_nums.put(aa[0], bb+1);
    				FileSharing.subfileTimers.get(aa[0]).cancel();
    				FileSharing.subfileTimers.remove(aa[0]);
    				FileSharing.recv_subfiels_no.get(aa[0]).add(sub_no);      
    			}
    			else
    			{ //第一次收到大于10m的文件块
    			  FileSharing.sub_nums.put(aa[0], 1); //大于10m文件已经收到的块数
    			  ArrayList<Integer>recvsubfiles=new ArrayList<Integer>();
     		      recvsubfiles.add(sub_no);
     		      FileSharing.recv_subfiels_no.put(aa[0], recvsubfiles);
    			}
    			if(!FileSharing.recvFiles.contains(recvFilename))
    				FileSharing.recvFiles.add(recvFilename);  //放入接收文件列表中
    		      String encodeFile=sharedPath+"//"+recvFilename;
                  RandomAccessFile raf=null;
 				  try {
 					raf = new RandomAccessFile(encodeFile,"rw");
 					raf.setLength(plist[0].fileLength);
 					raf.seek(0);   // 1：绝对定位              
			    	raf.skipBytes(FileSharing.maxfilelength*sub_no);//如果为负值，不跳过任何字节
			        raf.write(writein);
 				  } catch (Exception e) 
 				 {
 					e.printStackTrace();
 				 }  
 				 if(FileSharing.sub_nums.get(aa[0])>=plist[0].totalsubFiles)
   			     {
 					System.out.println("接收完毕######## "+aa[0]);
 					FileSharing.recv_subfiels_no.remove(aa[0]) ;
 					FileSharing.sub_nums.remove(aa[0]);
  	                 try {
						raf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
  	               SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
  	 		       Date curDate = new Date(System.currentTimeMillis());
  	 		       String m = formatter.format(curDate);
  	 		       String mm="接收文件 "+aa[0]+"的时间: "+m;
  	 		       FileSharing.messageHandle(mm);
  	 		       FileSharing. writeLog(""+"\r\n");
  	 		       FileSharing.writeLog("%%%"+plist[0].filename+",	");
  	 		       FileSharing.writeLog(plist[0].sub_fileID.split("--")[0].split("-")[1]+",	");
  	 		       FileSharing.writeLog(plist[0].fileLength+",	");
  	 		       FileSharing.writeLog(System.currentTimeMillis()+",	");
  	 		       FileSharing.writeLog(m+",	"+"\r\n");
   			     }
   			    else
   			    {   //大于10m的文件块还没有完全接受
   			       subfileTask=new send_FbpTask(aa[0],FileSharing.recv_subfiels_no.get(aa[0]));
 	    		   subfiletimer = new Timer(true);
 	    		   FileSharing.subfileTimers.put(aa[0], subfiletimer);
 	    		   subfiletimer.schedule(subfileTask,FileSharing.block_time,FileSharing.block_time);
 	    	       System.out.println("开始计时########： "+subfiletimer);
   			    }
 				 
    		}
    		else
    		{  //第一次收到小于10m的文件块
    			 RecvSubfileData rsfd=new RecvSubfileData(aa[0],plist[0].filename,writein,sub_no);
    			 FileSharing.RecvSubFiles.add(rsfd);
    			 FileSharing. subFile_nums.put(aa[0], 1);    
    		     ArrayList<Integer>recvsubfiles=new ArrayList<Integer>();
    		     recvsubfiles.add(sub_no);
    		     FileSharing.recv_subfiels_no.put(aa[0], recvsubfiles);
    		     subfileTask=new send_FbpTask(aa[0],recvsubfiles);
    		     subfiletimer = new Timer(true);
    		     FileSharing.subfileTimers.put(aa[0], subfiletimer);
    		     subfiletimer.schedule(subfileTask,FileSharing.block_time,FileSharing.block_time);
    		     System.out.println("开始计时： "+subfiletimer);
    		}
    
    	  }
        
    	}
    }
    
}
