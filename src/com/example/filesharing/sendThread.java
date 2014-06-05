package com.example.filesharing;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

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
	private int mixfiles=0;
	sendThread(Packet []plist,String castIP,int port,int from,int number,int mixfiles)
	{
		this.plist=plist;
		this.castIP=castIP;
		this.port=port;
		this.from=from;
		this.number=number;
		this.mixfiles=mixfiles;
	}
	public void init()
	{
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
	         messages = baos.toByteArray();   //Packet���г�ȥdata,�����ֶεĴ�С��261���Ҹ��ֽڣ�ʵ��ó��ġ�
	         baos.close();  
	         oos.close(); 
	         packet = new DatagramPacket(messages, messages.length,addr, port);        
	         socket.send(packet);
	         System.out.println("����ING "+sub_fileID+"---packet:"+i);
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
		long start=System.currentTimeMillis();
		 init();
		 if(plist!=null&&plist.length>0)
		 {  
			 if(mixfiles==1)
			 {
				 for(int i=0;i<number;i++)
					 if(plist[i]!=null)
					    sendPacket(plist[i] ,plist[i].sub_fileID,plist[i].seqno); 
			 }
			 else
			 {
			 int too=from+number;
			 if(too<=plist[0].data_blocks+plist[0].coding_blocks) //ֱ�ӷ���number����
			 {
				 for(int i=0;i<number;i++)
				    sendPacket(plist[from+i] ,plist[from+i].sub_fileID,plist[from+i].seqno);
			 }
			 else
			 {
				
				   for(int j=from;j<plist[0].data_blocks+plist[0].coding_blocks;j++) //�����Ļ����ٴ�0��ʼ��
					  sendPacket(plist[j] ,plist[j].sub_fileID,plist[j].seqno);
				   int off=from+number-(plist[0].data_blocks+plist[0].coding_blocks);
				   for(int i=0;i<off;i++)
					  sendPacket(plist[i] ,plist[i].sub_fileID,plist[i].seqno);
			}
			}
	    }	
		 long end=System.currentTimeMillis();
		 String []bb=plist[0].sub_fileID.split("-");
		 FileSharing.writeLog("sendThread:"+bb[1]+"--"+bb[bb.length-1]+",	"+(end-start)+"ms,	"+"\r\n");
    }
	
	public void destroy() 
	{	
		socket.close();
	
	}

}
