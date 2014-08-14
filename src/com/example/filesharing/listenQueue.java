package com.example.filesharing;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Queue;
import java.util.Timer;


public class listenQueue extends Thread
{
	
	public boolean running=true;
	public ArrayList<SmallFileData >smallfiles=new ArrayList<SmallFileData >();
	public int small_total_length=0;
	public sendFileFunction sendFunction=new sendFileFunction();
	ArrayList <Timer>queueListen_List=new ArrayList<Timer>();
	public synchronized void run()
	{
	  while (running)
	  {
		 if(FileSharing.SendFilequeue.size()>0)
		 {
			 System.out.println("@@@@进入listenQueue线程  "+System.currentTimeMillis());
			 handleQueue(FileSharing.SendFilequeue);
		 }
		 Timer queueTimer=new Timer(true);
		 QueueListenTask queueTask=new QueueListenTask();
		 queueTimer.schedule(queueTask, FileSharing.sleeptime);
		 queueListen_List.add(queueTimer);
		 try {
			wait();  
		 } catch (InterruptedException e) 
		 {
			e.printStackTrace();
		 }
	  }
	}
	public synchronized void notifyThread()
	{
		notify();
	}
	public synchronized void ondestroy()
	{
	   running=false;
	   synchronized(queueListen_List)
	   {
	    for(int i=0;i<queueListen_List.size();i++)
		    queueListen_List.get(i).cancel();
	     queueListen_List.clear();
      }
	}
	
	public void handleQueue(Queue<String> SendFilequeue)
	{
		 synchronized(SendFilequeue)
		   {
			 if(FileSharing.SendFilequeue.size()>0)
			 {
				String filename=SendFilequeue.peek();
				System.out.println("队列的大小："+SendFilequeue.size());
			    while(filename!=null)
				{
				  String id=Integer.toString(FileSharing.sendFileID);
	 			  String file_id =FileSharing.ipaddress+"-"+id;
	 			  int filelength=0;
	 			  FileInputStream fis=null;
	 			  File f=new File(filename);
	 			  if(f.exists())
	 			  {
	 				try {
	 					fis = new FileInputStream(filename);
	 					filelength = fis.available();
	 				} catch (Exception e) {
	 					e.printStackTrace();
	 				}
	 			  if(filelength>=FileSharing.maxfilelength)  
	 				{ 
	 					System.out.println("文件>100k");
	 				SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
	 			    Date curDate = new Date(System.currentTimeMillis());
	 			    String m = formatter.format(curDate);
	 			    String mm="发送文件 "+FileSharing.ipaddress+"-"+id+"的时间: "+m; 
	 			    FileSharing.messageHandle(mm);
	 			    
	 			   FileSharing.writeLog("###"+filename+",	");
	 			   FileSharing.writeLog(id+",	");
	 			   FileSharing.writeLog(filelength+",	");
	 			   FileSharing.writeLog(System.currentTimeMillis()+",	");
	 			   FileSharing.writeLog(m+",	"+"\r\n");
	 			   FileSharing.writeLog("\r\n");
	 					int num=0;
	 					if(filelength%FileSharing.maxfilelength==0)
	 						num=filelength/FileSharing.maxfilelength;
	 					else
	 						num=filelength/FileSharing.maxfilelength+1;
	 					
	 					System.out.println("文件分几次发送："+num);
	 					for(int i=0;i<num;i++)  //大文件分块发送
	 					{
	 					  long start=0;
	 			          start=System.currentTimeMillis();
	 					  String sub_id =FileSharing.ipaddress+"-"+id+"--"+i;
	 					  sendFunction.sendToAll(filename,filelength, sub_id,num);
	 			          long end=System.currentTimeMillis();
	 				      FileSharing.writeLog("blockSpentTime:"+id+"--"+i+",	"+(end-start)+"ms,	"+"\r\n"); 					 
	 					}
	 		        }	
	 			else if(filelength>0)	//多个小文件凑成一个大文件
	 			{
	 				System.out.println("该文件<100k");
	 				if(SendFilequeue.size()==1)  //当前文件是队列中的最后一个
	 				{
	 					if(smallfiles.size()==0)   //小文件链表为0
	 					{
	 					SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
	 				    Date curDate = new Date(System.currentTimeMillis());
	 				    String m = formatter.format(curDate);
	 				    String mm="发送文件 "+FileSharing.ipaddress+"-"+id+"的时间: "+m; 
	 				    FileSharing.messageHandle(mm);
	 				    
	 				    FileSharing.writeLog("###"+filename+",	");
	 				    FileSharing.writeLog(id+",	");
	 				    FileSharing.writeLog(filelength+",	");
	 				    FileSharing.writeLog(System.currentTimeMillis()+",	");
	 				    FileSharing.writeLog(m+",	"+"\r\n");
	 				    FileSharing. writeLog("\r\n");
	 					String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 					sendFunction.sendToAll(filename,filelength, sub_id,1);
	 				    }
	 					else  //小文件链表不为0
	 					{
	 					    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 					    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id);
	 	   					smallfiles.add(sf);
	 	   			     	small_total_length=small_total_length+filelength;
	 	   				    sendFunction.sendSmallFiles(smallfiles ,small_total_length);
	 	   				    smallfiles.clear();
	 					}
	 				}
	 				else  //队列中还剩下多个文件（>1）
	 				{
	 				    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
	 				    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id);
	 					smallfiles.add(sf);	
	 					small_total_length=small_total_length+filelength;
	 				}
	 			}
	 			 SendFilequeue.remove();
	 			 FileSharing.sendFiles.put(FileSharing.ipaddress+"-"+id, filename);
	 			 FileSharing.sendFileID++;  
	 			 filename=SendFilequeue.peek();
	 			  }
		       }  //内层while循环结束
			    FileSharing.currentLength=0; 
			    FileSharing.file_number=0;
			    if(smallfiles.size()>0) 
			    {
			    	sendFunction.sendSmallFiles(smallfiles,small_total_length);
		   		    smallfiles.clear();
			    }	
			    small_total_length=0;
		   }
		 } //end synchronized queue	
	 }
}
