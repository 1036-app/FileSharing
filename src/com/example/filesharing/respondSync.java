package com.example.filesharing;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import android.os.Message;


public class respondSync extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public byte[]messages=null;
	public String filenames =null;
	public boolean running=true;
	public void init()
	{
		try {
			socket= new DatagramSocket(null); 
			socket.setReuseAddress(true); 
			socket.bind(new InetSocketAddress(FileSharing.sync_port)); 
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
		 messages=new byte[3072];
		 packet = new DatagramPacket(messages, messages.length);
		 try {
			socket.receive(packet);
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
		try {
			filenames = new String(packet.getData(),0,packet.getLength(),"utf-8");
		} catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
		} 
		  File f=new File(filenames);
		  if (!packet.getAddress().toString().equals("/"+FileSharing.ipaddress))
		  {
			  System.out.println("�յ�����Ϣ"   + filenames );
			if(filenames.startsWith("***")) //�ǶԷ������ģ�Ҫ���ͱ������ļ�
			{
				System.out.println("�Է��������ģ�Ҫ���ͱ����ļ� "+ filenames );
				filenames=filenames.substring(3);
				String [] names=filenames.split("\\|");
				for(int i=0;i<names.length;i++)
				{
					names[i].trim();
					if(names[i]!= null)
					   FileSharing.addToQueue(names[i],true);
				}
			}
			else
			{
		    Message msg = new Message();   //�Է�����Ҫ��ͬ��Ŀ¼
            msg .obj = filenames ;
            msg .arg1=3;
            FileSharing.myHandler.sendMessage(msg);		
			}
		  }
		System.out.println("VVVVVVVVVVVVVVV");
	 }
	}
	public void onDestrtoy()
	{
		running=false;
	}
}
