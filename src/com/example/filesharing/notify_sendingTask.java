package com.example.filesharing;

public class notify_sendingTask extends java.util.TimerTask
{	
	public String filename;
	public String sub_fileid;
	public int num;
	notify_sendingTask(String filename,String sub_fileid,int num)
	{
	this.filename=filename;
	this.sub_fileid=sub_fileid;
	this.num=num;
	}
	 public void run() 
	 { 
		 synchronized(FileSharing.subfiles)
		 {
		   if(FileSharing.subfiles.containsKey(sub_fileid))
		   {
			 System.out.println("��ʱû�յ��������������߳�");
			 String message = "��ʱû�յ��������������߳�";
			 FileSharing.messageHandle(message);
			 FileSharing.subfiles.get(sub_fileid).cancel();
			 FileSharing.subfiles.remove(sub_fileid);
			 FileSharing.listenqueue.notifyThread();
			 
		   }
		 }
	
	 }
} 