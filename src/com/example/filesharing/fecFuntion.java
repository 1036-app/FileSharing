package com.example.filesharing;

import java.io.BufferedOutputStream;
import java.io.File;
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
	//public  String sharedPath="//sdcard//SharedFiles";
	
	
	public  synchronized Packet[] encode(byte[] filedata,int subFileLength,String sub_fileID,String filename,int totalsubFiles,long FileLength)
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
            coding_blocks= (int)Math.ceil(data_blocks*20/100)+1;
 			//coding_blocks = data_blocks;
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
 	      String message = plist[0].sub_fileID+" ��������������ĸ�����"+plist.length;
 	      FileSharing.messageHandle(message);
 	      long end =System.currentTimeMillis();
 	      FileSharing.total_encode_timer+=(end-start);
		  return plist;
	}
	
	public  synchronized void decode(Packet[] plist)
    {
		
		System.out.println("������뺯��  �յ����ĸ�����"+plist.length+" ��ʼ����");
    	Packet p = plist[plist.length-1];
    	int data_blocks = p.data_blocks;
    	int coding_blocks = 0;
    	int block_length=FileSharing.blocklength;
    	int file_length = p.subFileLength;  //�ÿ��ļ��ĳ���
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
    	if(coding_blocks ==0)  //����coding_blocks =0�����ؽ��룬ֱ�ӻָ���
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
    	  String message="�����ļ�";
    	  FileSharing.messageHandle(message);
    	  FileSharing.recvFiles.add(plist[0].filename);  //��������ļ��б�
    	  String []directory=plist[0].filename.split("/");
    	  String dire="";
    	  for(int kk=0;kk<directory.length-1;kk++)
    		  dire+="/"+directory[kk];
    	   File ff=new File(dire);
           if(!ff.exists())
           {
        	   ff.mkdirs();
        	   FileSharing.recvFiles.add(dire);
           }
    	   try {
    		String encodeFile=dire+"/"+directory[directory.length-1];
    		BufferedOutputStream bos;
    		bos = new BufferedOutputStream(new FileOutputStream(encodeFile));
			bos.write(writein);
	        bos.close();
		    } catch (Exception e) 
		   {		
			e.printStackTrace();
		   }
    	 String []fileID_break=plist[0].sub_fileID.split("--");
    	 SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
		 Date curDate = new Date(System.currentTimeMillis());
		 String m = formatter.format(curDate);
		 String mm="�����ļ� "+fileID_break[0]+"��ʱ��: "+m; 
		 FileSharing.messageHandle(mm);
		 FileSharing.writeLog(""+"\r\n");
		 FileSharing.writeLog("%%%"+plist[0].filename+",	");
		 FileSharing.writeLog(plist[0].sub_fileID.split("--")[0].split("-")[1]+",	");
		 FileSharing.writeLog(plist[0].fileLength+",	");
		 FileSharing.writeLog(System.currentTimeMillis()+",	");
		 FileSharing.writeLog(m+",	"+"\r\n");
		 //ȡ�����ʱ��
		 synchronized(FileSharing.subfileTimers)
		 {
		   if(FileSharing.subfileTimers.containsKey(fileID_break[0]))
		   {
		   FileSharing.subfileTimers.get(fileID_break[0]).cancel(); 
           FileSharing.subfileTimers.remove(fileID_break[0]);
		   }
		 }
    	}
    	else
    	{
    		//���ļ��ж��
    	send_FbpTask subfileTask=null;
    	Timer subfiletimer=null;
    	String []fileID_break=plist[0].sub_fileID.split("--"); //fileID_break[0]�зŵ���fileid
    	int sub_no=Integer.parseInt(fileID_break[1]);
    	
    	if(FileSharing.subFile_nums.containsKey(fileID_break[0]))
    	{
            //��ǰ���յ������ļ���<10m���Ŀ�
			RecvSubfileData rsfd=new RecvSubfileData(fileID_break[0],plist[0].filename,writein,sub_no);
			FileSharing.RecvSubFiles.add(rsfd);
    		int t=FileSharing.subFile_nums.get(fileID_break[0]);
    		t=t+1;
    		FileSharing.subFile_nums.remove(fileID_break[0]);
    		FileSharing.subFile_nums.put(fileID_break[0], t);
			System.out.println("�յ��Ŀ�ţ�"+sub_no);
            System.out.println("���ļ��ܹ�������"+plist[0].totalsubFiles+" ��ǰ�յ��˿飺"+FileSharing.subFile_nums.get(fileID_break[0]));        
            FileSharing.recv_subfiels_no.get(fileID_break[0]).add(sub_no);
           
            if(FileSharing.subFile_nums.get(fileID_break[0])==plist[0].totalsubFiles)
    		{
            	FileSharing.recv_subfiels_no.remove(fileID_break[0]);
            	 synchronized(FileSharing.subfileTimers)
        		 {
            	  if(FileSharing.subfileTimers.containsKey(fileID_break[0]))
    			   {
            	    FileSharing.subfileTimers.get(fileID_break[0]).cancel();  //ȡ�����ʱ��
            	    FileSharing.subfileTimers.remove(fileID_break[0]);
    			   }
        		 }
                 String message="��ʼ�����ļ�";
                 FileSharing.messageHandle(message);
                 FileSharing.recvFiles.add(plist[0].filename);  //��������ļ��б���
                 String []directory=plist[0].filename.split("/");
           	     String dire="";
           	     for(int kk=0;kk<directory.length-1;kk++)
           		  dire+="/"+directory[kk];
           	     File ff=new File(dire);
                  if(!ff.exists())
                  {
               	   ff.mkdirs();
               	   FileSharing.recvFiles.add(dire);
                  }
             	String encodeFile=dire+"/"+directory[directory.length-1];
                 RandomAccessFile raf=null;	
                   try {
					raf = new RandomAccessFile(encodeFile,"rw");
					raf.setLength(plist[0].fileLength);
				   } catch (Exception e) {
					e.printStackTrace();
				   }
    						
                 for(int nn=0;nn<FileSharing.RecvSubFiles.size();nn++)
    			 {        	
    				if(FileSharing.RecvSubFiles.get(nn).fileID.equals(fileID_break[0]))
    				{
    				   try {  
    			    	    raf.seek(0);   // 1�����Զ�λ              
    			    	    raf.skipBytes(FileSharing.maxfilelength*FileSharing.RecvSubFiles.get(nn).sub_num);//���Ϊ��ֵ���������κ��ֽ�
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
                FileSharing.subFile_nums.remove(fileID_break[0]);
    		    SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
    	        Date curDate = new Date(System.currentTimeMillis());
    	        String m = formatter.format(curDate);
    	        String mm="�����ļ�"+fileID_break[0]+"��ʱ�� : "+m;
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
    		//�յ��˴���10m���ļ����ߵ�һ���յ�<10m���ļ�
    		if(plist[0].fileLength>FileSharing.maxStoredLength)  //�ļ��ĳ��ȴ���10m,ֱ��д��
    		{
    			if(FileSharing.sub_nums.containsKey(fileID_break[0]))
    			{   //��ǰ�Ѿ��յ�������10m���ļ���
    				int bb=FileSharing.sub_nums.get(fileID_break[0]);
    				FileSharing.sub_nums.remove(fileID_break[0]);
    				FileSharing.sub_nums.put(fileID_break[0], bb+1);
    				FileSharing.recv_subfiels_no.get(fileID_break[0]).add(sub_no);      
    			}
    			else
    			{ //��һ���յ�����10m���ļ���
    			  FileSharing.sub_nums.put(fileID_break[0], 1); //����10m�ļ��Ѿ��յ��Ŀ���
    			  ArrayList<Integer>recvsubfiles=new ArrayList<Integer>();
     		      recvsubfiles.add(sub_no);
     		      FileSharing.recv_subfiels_no.put(fileID_break[0], recvsubfiles);
    			}
    			if(!FileSharing.recvFiles.contains(plist[0].filename))
    				FileSharing.recvFiles.add(plist[0].filename);  //��������ļ��б���
    			 String []directory=plist[0].filename.split("/");
    	    	  String dire="";
    	    	  for(int kk=0;kk<directory.length-1;kk++)
    	    		  dire+="/"+directory[kk];
    	    	   File ff=new File(dire);
    	           if(!ff.exists())
    	           {
    	        	   ff.mkdirs();
    	        	   FileSharing.recvFiles.add(dire);
    	           }
    	        String encodeFile=dire+"/"+directory[directory.length-1]; 
                  RandomAccessFile raf=null;
 				  try {
 					raf = new RandomAccessFile(encodeFile,"rw");
 					raf.setLength(plist[0].fileLength);
 					raf.seek(0);   // 1�����Զ�λ              
			    	raf.skipBytes(FileSharing.maxfilelength*sub_no);//���Ϊ��ֵ���������κ��ֽ�
			        raf.write(writein);
 				  } catch (Exception e) 
 				 {
 					e.printStackTrace();
 				 }  
 				 if(FileSharing.sub_nums.get(fileID_break[0])>=plist[0].totalsubFiles)
   			     {
 					System.out.println("�������######## "+fileID_break[0]);
 					 if(FileSharing.subfileTimers.containsKey(fileID_break[0]))
 					 {
					  FileSharing.subfileTimers.get(fileID_break[0]).cancel();
 	    			  FileSharing.subfileTimers.remove(fileID_break[0]);
 					 }
 					FileSharing.recv_subfiels_no.remove(fileID_break[0]) ;
 					FileSharing.sub_nums.remove(fileID_break[0]);
  	                 try {
						raf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
  	               SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
  	 		       Date curDate = new Date(System.currentTimeMillis());
  	 		       String m = formatter.format(curDate);
  	 		       String mm="�����ļ� "+fileID_break[0]+"��ʱ��: "+m;
  	 		       FileSharing.messageHandle(mm);
  	 		       FileSharing. writeLog(""+"\r\n");
  	 		       FileSharing.writeLog("%%%"+plist[0].filename+",	");
  	 		       FileSharing.writeLog(plist[0].sub_fileID.split("--")[0].split("-")[1]+",	");
  	 		       FileSharing.writeLog(plist[0].fileLength+",	");
  	 		       FileSharing.writeLog(System.currentTimeMillis()+",	");
  	 		       FileSharing.writeLog(m+",	"+"\r\n");
   			     }			 
    		}
    		else
    		{  //��һ���յ�С��10m���ļ���
    			 RecvSubfileData rsfd=new RecvSubfileData(fileID_break[0],plist[0].filename,writein,sub_no);
    			 FileSharing.RecvSubFiles.add(rsfd);
    			 FileSharing. subFile_nums.put(fileID_break[0], 1);    
    		     ArrayList<Integer>recvsubfiles=new ArrayList<Integer>();
    		     recvsubfiles.add(sub_no);
    		     FileSharing.recv_subfiels_no.put(fileID_break[0], recvsubfiles);
    		}
    
    	  }
        
    	}
    }
    
}
