package com.example.filesharing;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Stack;

public class requestSyncFunction extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public String storedFiles="";
	public InetAddress addr = null;
    public String files=null;
    requestSyncFunction(String s)
    {
    	files=s;
    }
    requestSyncFunction()
    {
    	files=null;
    }
	public synchronized  void init()
	{
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);	 
			addr=InetAddress.getByName(FileSharing.bcastaddress);	
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
		if(files==null)
		{
		File f=new File(FileSharing.sharedPath);
		Stack <String>  stack= new Stack <String>();
		if(f.exists()&&f.isDirectory())
		  stack.push(f.getAbsolutePath());
		while(!stack.empty())
		{
			String filename=stack.pop();
			File file=new File(filename);
			File ff[]=file.listFiles();
			if(ff!=null)
			for(int i=0;i<ff.length;i++)
				if(ff[i].isDirectory()&& !ff[i].getName().equals(".") && !ff[i].getName() .equals(".."))
					stack.push(ff[i].getAbsolutePath());
				else 
					storedFiles+=ff[i].getAbsolutePath()+"|";
		}
		}
	}
	public  void run()
    {
		init();
		byte[] messages=null; 
		String Filenames=null;
		if(files==null)
			Filenames=storedFiles;
		else
			Filenames=files;
		if(Filenames==null||Filenames=="")
		{
			Filenames="**空";  //自己的共享目录中没有任何文件
		}
		try {
			messages=new byte[Filenames.getBytes("UTF8").length];
			messages=Filenames.getBytes("UTF8");
		} catch (UnsupportedEncodingException e1) 
		{
			e1.printStackTrace();
		}
	    packet = new DatagramPacket(messages, messages.length,addr, FileSharing.sync_port);        
	    try {
			socket.send(packet);
			} catch (IOException e)
			{
				e.printStackTrace();
			}	
	      String mess ="要求同步";
		  FileSharing.messageHandle(mess);	
	  }

}
