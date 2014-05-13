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
	private int to=0;
	private int number=0;
	sendThread(Packet []plist,String castIP,int port,int from,int to,int number)
	{
		this.plist=plist;
		this.castIP=castIP;
		this.port=port;
		this.from=from;
		this.to=to;
		this.number=number;
	}
	public void init()
	{
		 String message ="进入发送函数";
		 FileSharing.messageHandle(message);
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
		byte[] messages=null;
		try {  
	         ByteArrayOutputStream baos = new ByteArrayOutputStream();  
	         ObjectOutputStream oos = new ObjectOutputStream(baos);  
	         oos.writeObject(pt);
	         messages = baos.toByteArray();   //Packet类中除去data,其它字段的大小是261左右个字节，实验得出的。
	         baos.close();  
	         oos.close(); 
	         System.out.println("messages.length "+messages.length);
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
		 
			 if(from>0)  //发送的是冗余包
		     {
			   if(number<to-from)
			   {
				//  选择冗余包发送
				   System.out.println("选择冗余包发送");
			      Object [] values = new Object[number];  
			      Random random = new Random();
			      HashMap<Integer, Integer> hashMap = new HashMap<Integer,Integer>();   
			      while(hashMap.size()<number)   // 生成随机数字并存入HashMap
			      {
			        int num = random.nextInt(to);
			        if(num>=from)
			        {
			    	  hashMap.put(num, 1);
			        }
			   
			      }   
		
			     values=hashMap.keySet().toArray();
			     for(int j=0;j<number;j++)  //hashMap中放的就是不重复的number个随机数
			      {
				  int num=Integer.parseInt(values[j].toString());
				  sendPacket(plist[num], plist[num].seqno); 
			      }
			   }
			   
			   else if(number==to-from)
			   {
				// 冗余包全部发送 ; 
				 for(int k=from;k<to;k++)
				  sendPacket(plist[k], plist[k].seqno); 
			   }
		
		  }
		  
		  else   //发送线程发送数据包或者是反馈包 ，from=0
		  {
			  System.out.println("发送线程发送数据包或者是反馈包 ");
			  int num=0;
			  num=to-from;
			   for(int i=0;i<num;i++)
			     {	  
				 Packet pt=plist[i];
				 sendPacket(pt, i);
			     }
			  
		  }
			
	}		
  }
	
	public void destroy() 
	{	
		socket.close();
	
	}

}
