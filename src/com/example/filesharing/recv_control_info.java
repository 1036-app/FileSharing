package com.example.filesharing;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import android.os.Message;

public class recv_control_info extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public byte[]messages=null;
	public String filename =null;
	public boolean running=true;
	public void init()
	{
		try {
			socket= new DatagramSocket(null); 
			socket.setReuseAddress(true); 
			socket.bind(new InetSocketAddress(FileSharing.control_port)); 
			socket.setBroadcast(true);
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	@Override
	public void run() 
	{
		 init();
	  while(running)
	  {
		 messages=new byte[256];
		 packet = new DatagramPacket(messages, messages.length);
		 try {
			socket.receive(packet);
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		try {
			filename = new String(packet.getData(),0,packet.getLength(),"utf-8");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		} 
		   File f=new File(filename);
		  if (f.exists()&&!packet.getAddress().toString().equals("/"+FileSharing.ipaddress))
		  {
			String mess ="收到删除文件的指令"+filename;
			FileSharing.messageHandle(mess); 
		    Message msg = new Message();
            msg .obj = filename ;
            msg .arg1=2;
            FileSharing.myHandler.sendMessage(msg);	
		  }
	 }
	}
	public void onDestrtoy()
	{
		running=false;
	}
}
