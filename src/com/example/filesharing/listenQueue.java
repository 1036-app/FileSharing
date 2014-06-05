package com.example.filesharing;

import java.io.FileInputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class listenQueue extends Thread
{
	
	public boolean running=true;
	public boolean starting=false;
	public ArrayList<SmallFileData >smallfiles=new ArrayList<SmallFileData >();
	public int total_length=0;
	public String sharedPath="//sdcard//SharedFiles";
	public sendFileFunction sendFunction=new sendFileFunction();
	public void run()
	{
	 while (running)
	 {
	  while(starting)
	  {	
			try {
				sleep(1000);  //��ʱ1��֮����ִ�и��̡߳�
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
	   synchronized(FileSharing.SendFilequeue)
	   {
		  if(FileSharing.SendFilequeue.size()!=0) 
		  {
			String filename=FileSharing.SendFilequeue.peek();
			System.out.println("���еĴ�С��"+FileSharing.SendFilequeue.size());
		    while(filename!=null)
			{
			  String id=Integer.toString(FileSharing.sendFileID);
 			  String file=sharedPath+"//"+filename;
 			  int filelength=0;
 			  FileInputStream fis=null;
 				try {
 					fis = new FileInputStream(file);
 					filelength = fis.available();
 				} catch (Exception e) {
 					e.printStackTrace();
 				}
 			  if(filelength>=FileSharing.maxfilelength)  
 				{ 
 				SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
 			    Date curDate = new Date(System.currentTimeMillis());
 			    String m = formatter.format(curDate);
 			    String mm="�����ļ� "+FileSharing.ipaddress+"-"+id+"��ʱ��: "+m; 
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
 					
 					System.out.println("�ļ��ּ��η��ͣ�"+num);
 					for(int i=0;i<num;i++)
 					{
 			         long start=System.currentTimeMillis();
 					  String sub_id =FileSharing.ipaddress+"-"+id+"--"+i;
 					  FileSharing.sending_subfileid=sub_id;  //��¼���ڷ��͵��ļ����
 					  sendFunction.sendToAll(filename,filelength, sub_id,num);
 					  synchronized(this)
 					   {
 					    try {
						wait(); //�߳�����
					     } catch (InterruptedException e)
					     {	
					     System.out.println(e);
						  e.printStackTrace();
					     }
 					   }
 				long end=System.currentTimeMillis();
 				FileSharing.writeLog("blockSpentTime:"+id+"--"+i+",	"+(end-start)+"ms,	"+"\r\n");
 					} 
 		        }	
 			else	//���С�ļ��ճ�һ�����ļ�
 			{
 				if(FileSharing.SendFilequeue.size()==1)
 				{
 					if(smallfiles.size()==0)
 					{
 					SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
 				    Date curDate = new Date(System.currentTimeMillis());
 				    String m = formatter.format(curDate);
 				    String mm="�����ļ� "+FileSharing.ipaddress+"-"+id+"��ʱ��: "+m; 
 				    FileSharing.messageHandle(mm);
 				    
 				    FileSharing.writeLog("###"+filename+",	");
 				    FileSharing.writeLog(id+",	");
 				    FileSharing.writeLog(filelength+",	");
 				    FileSharing.writeLog(System.currentTimeMillis()+",	");
 				    FileSharing.writeLog(m+",	"+"\r\n");
 				    FileSharing. writeLog("\r\n");
 						String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
 						sendFunction.sendToAll(filename,filelength, sub_id,1);
 					    synchronized(this)
				        {
				         try {
					      wait(); //�߳�����
				           } catch (InterruptedException e)
				          {	
				           System.out.println(e);
					       e.printStackTrace();
				           }
				        }
 				    }
 					else
 					{
 					    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
 					    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id);
 	   					smallfiles.add(sf);
 	   				    total_length=total_length+filelength;
 	   				    sendFunction.sendSmallFiles(smallfiles ,total_length);
 	   				    smallfiles.clear();
 					}
 				}
 				else
 				{
 				    String sub_id =FileSharing.ipaddress+"-"+id+"--"+0;
 				    SmallFileData  sf=new SmallFileData (filename,filelength,sub_id);
 					smallfiles.add(sf);	
 					total_length=total_length+filelength;
 				}
 			}
 			 FileSharing.SendFilequeue.remove();
 			 FileSharing.sendFileID++;  
 		     filename=FileSharing.SendFilequeue.peek();
	       }  //�ڲ�whileѭ������
		    
		    if(smallfiles.size()>0)
		    {
		    	sendFunction.sendSmallFiles(smallfiles,total_length);
	   		    smallfiles.clear();
		    }
		 }		
	    } //end synchronized queue
	 }
	}//���whileѭ������
 }
	public synchronized void notifyThread()
	{
		notify();
	}
	public synchronized void onresume()
	{
		starting=true;
	}
	public synchronized void ondestroy()
	{
		running=false;
	}
	
}
