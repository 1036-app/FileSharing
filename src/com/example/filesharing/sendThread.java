package com.example.filesharing;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import android.R.integer;

public class sendThread extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	private String castIP="";
	private Packet []plist=null;
	private int port=0;
	private int from=0;
	private int number=0;
	sendThread(Packet []plist,String castIP,int port,int from,int number)
	{
		this.plist=plist;
		this.castIP=castIP;
		this.port=port;
		this.from=from;
		this.number=number;
	}
	public void init()
	{
		 System.out.println("进入发送函数");
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);	 
			addr=InetAddress.getByName(castIP);	
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	
	public void sendPacket(Packet pt,int i)
	{
		FileSharing.totalBlocks++;
		byte[] messages=null;
		try {  
	         ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	         ObjectOutputStream oos = new ObjectOutputStream(baos);  
	         oos.writeObject(pt);
	         messages = baos.toByteArray();   //Packet类中除去data,其它字段的大小是261左右个字节，实验得出的。
	         baos.close();  
	         oos.close(); 
	         packet = new DatagramPacket(messages, messages.length,addr, port);        
	         socket.send(packet);
	         System.out.println("发送ING "+i);
	        }  
		    catch (SocketException e) 
	        {
		      return;
	        }
	        catch(Exception e) 
	        {   
	            e.printStackTrace();  
	        } 
	
	}
	
	
	@Override
	public void run() 
	{ 
		 init();
		 if(plist!=null&&plist.length>0)
		 {  
			 int total_blocks=plist[0].data_blocks+plist[0].coding_blocks;
			 int too=from+number;
			 if(too<=total_blocks)
			 {
				 for(int i=0;i<number;i++)
				    sendPacket(plist[from+i] ,from+i);
			 }
			 else
			 {
				for(int j=from;j<total_blocks;j++)
					 sendPacket(plist[j] ,j);
				int off=from+number-total_blocks;
				for(int i=0;i<off;i++)
					sendPacket(plist[i] ,i);
			 }
	    }		
    }
	
	public void destroy() 
	{	
		socket.close();
	
	}

}
