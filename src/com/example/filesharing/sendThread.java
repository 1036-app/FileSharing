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
		// System.out.println("进入发送函数");
		try {
			socket = new DatagramSocket();
			socket.setBroadcast(true);	 
			addr=InetAddress.getByName(castIP);	
		} catch (Exception e) 
		{	
			e.printStackTrace();
		}
	}
	
	public void sendPacket(Packet pt,String sub_fileID,int i)
	{
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
	         System.out.println("发送ING "+sub_fileID+"---packet:"+i);
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
			 int too=from+number;
			 if(too<=plist[0].data_blocks+plist[0].coding_blocks) //直接发送number个包
			 {
				 for(int i=0;i<number;i++)
				    sendPacket(plist[from+i] ,plist[from+i].sub_fileID,plist[from+i].seqno);
			 }
			 else
			 {
				for(int j=from;j<plist[0].data_blocks+plist[0].coding_blocks;j++) //不够的话，再从0开始发
					 sendPacket(plist[j] ,plist[j].sub_fileID,plist[j].seqno);
				int off=from+number-(plist[0].data_blocks+plist[0].coding_blocks);
				for(int i=0;i<off;i++)
					sendPacket(plist[i] ,plist[i].sub_fileID,plist[i].seqno);
			 }
	    }		
    }
	
	public void destroy() 
	{	
		socket.close();
	
	}

}
