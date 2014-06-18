package com.example.filesharing;

public class QueueListenTask extends java.util.TimerTask
{

	@Override
	public void run()
	{	 
	  synchronized( FileSharing.listenqueue.queueListen_List)
	  {
	  for(int i=0;i<FileSharing.listenqueue.queueListen_List.size();i++)
		  FileSharing.listenqueue.queueListen_List.get(i).cancel();
	  FileSharing.listenqueue.queueListen_List.clear();
	  }
	  FileSharing.listenqueue.notifyThread();	
	}
}
