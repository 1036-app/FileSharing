package com.example.filesharing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class sendContInfo 
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public String filename=null;

	public synchronized  void init(String filename)
	{
		this.filename=filename;
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);	 
			addr=InetAddress.getByName(FileSharing.bcastaddress);	
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	public synchronized void sendcontrolInfo()
    {
		byte[] messages=null; 
		try {
			messages=new byte[filename.getBytes("UTF8").length];
			messages=filename.getBytes("UTF8");
		} catch (UnsupportedEncodingException e1) 
		{
			e1.printStackTrace();
		}
	    packet = new DatagramPacket(messages, messages.length,addr, FileSharing .control_port);        
	    try {
			socket.send(packet);
			} catch (IOException e)
			{
				e.printStackTrace();
			}	
	      String mess ="发送删除文件的指令";
		  FileSharing.messageHandle(mess);
	}
	
}

