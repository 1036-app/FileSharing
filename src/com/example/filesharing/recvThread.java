package com.example.filesharing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Timer;

import android.os.Message;


public class recvThread extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public boolean running=true;
	public byte[] encodedFrame=null;
	public ArrayList<String> filesID=new ArrayList<String>();
	public ArrayList<Packet[]> lossFiles=new ArrayList<Packet[]>();
	public ArrayList<int[]> pktIDs=new ArrayList<int[]>();  //接收到的对应文件的子包的序号
	public Map<String,Timer> recvTimers=new HashMap<String,Timer>();
	private String localIP="";
	public int port=0;
	public Date nowDate=null;
	public Date curDate=null;
	public Timer recvtimer=null;
	public recvTask recvtask=null;
	public Packet []recvPacket=null;
	public String recvIP=null;
	public String mess=null;
	public String message=null;
	recvThread(String localIP,int port)
	{
		this.localIP="/"+localIP;
		this.port=port;
	}
	public void init()
	{
		try {
			socket = new DatagramSocket(port);
			socket.setBroadcast(true);
			encodedFrame=new byte[FileSharing.blocklength+1000];
		
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
	    packet = new DatagramPacket(encodedFrame, encodedFrame.length);
		try {
		socket.receive(packet);
	    } catch (SocketException e) 
	     {
		  break;
	     }
	   catch (IOException e) 
	    {	
         e.printStackTrace();
	     System.out.println(e.toString());
	    }
		recvIP=packet.getAddress().toString();
		
	  if (!recvIP.equals(localIP)&& packet.getData().length != 0) 
	     {
		   Packet pt=null;
		                //  接收不为空，且不是自己的包 
		   try {  
			    ByteArrayInputStream bais = new ByteArrayInputStream(packet.getData());
			    ObjectInputStream ois = new ObjectInputStream(bais);
			    pt = (Packet)ois.readObject(); 
			    bais.close();  
			    ois.close();  
			  }  
			  catch(Exception e)
			  {    
			    System.out.println(e.toString());
			    e.printStackTrace();
			  } 
		   if(pt.type==1||pt.type==2)
		   {
			   recvPacket=new Packet[1];
	           recvPacket[0]=pt;
	      
	           Message msg = new Message();
	           msg .obj = recvPacket;
	           msg .arg1=1;
	           FileSharing.myHandler.sendMessage(msg);	   	 
		   }
		   else
		   {
			  if(!filesID.contains(pt.sub_fileID))
			  {
				 int offset=0;  //该文件放在列表中的第offset个表项
				 int pktLength=0; //接受的包放在了第pktLength个包
				 if(recvTimers.containsKey(pt.sub_fileID))
				 {
					 recvTimers.get(pt.sub_fileID).cancel();
		    	     recvTimers.remove(pt.sub_fileID);    
				 }
					Random random = new Random();
					long delay=5000+random.nextInt(1000);
					long frequency=5000+random.nextInt(1000);
					
					recvtask=new recvTask(pt.sub_fileID);
					recvtimer = new Timer(true);
					recvTimers.put(pt.sub_fileID, recvtimer);
					recvtimer.schedule(recvtask,delay,frequency);
					   for(offset=0;offset<lossFiles.size();offset++) 
					    {
					        if(pt.sub_fileID.equals(lossFiles.get(offset)[0].sub_fileID))
					        {   
					        	 //已经不是第一次收到该文件
					        	 int a=1;
					        	 for(int k=0;k<pktIDs.get(offset).length;k++)
					        	 {
					        		 if(pt.seqno==pktIDs.get(offset)[k])
									   {  
										   System.out.println("接受到相同的子包的序号 ："+pt.seqno);
										   a=0;	  
									   } 
					        		 if(pktIDs.get(offset)[k]==-1)
					        		 {
					        			 pktLength=k; 
					        			 break;
					        		 }
					        	 	  
					              }
							   if(a==1)
							   {
							   //将新接收到的子包放入列表中
							    lossFiles.get(offset)[pktLength]=pt;  //放入列表中第offect个表项的第pktlength个包
							    pktIDs.get(offset)[pktLength++]=pt.seqno;
							   }
					        	break;
					       }
					    }
					   if(offset==lossFiles.size())
					   {
						    System.out.println("第一次接收该文件");
						    Packet []plist=null;
			                int[] subpacketID =null;	    
							int paks=pt.data_blocks;
							subpacketID=new int[paks];
							plist=new Packet[paks];
							for(int k=0;k<paks;k++)
							{
								subpacketID[k]=-1;
							}
							plist[pktLength]=pt;
							subpacketID[pktLength++]=pt.seqno;
							lossFiles.add(plist);
							pktIDs.add(subpacketID);  
							
					   }
					   
					   if(pktLength==lossFiles.get(offset)[0].data_blocks )
					   {
						   System.out.println("接收完毕，总共接收到的包："+pktLength+"个");
						   System.out.println("lossFiles.get(offset)[0].data_blocks=："+pktLength+"个");
						   filesID.add(lossFiles.get(offset)[0].sub_fileID);
						   //取消接收计时器
						   if(recvTimers.containsKey(lossFiles.get(offset)[0].sub_fileID))
						     { 
								 message ="针对文件： "+pt.sub_fileID+" 的计时器 取消,接收包完毕";
								 FileSharing.messageHandle(message);
						    	 recvTimers.get(pt.sub_fileID).cancel();
						    	 recvTimers.remove(pt.sub_fileID);
						     }
						   recvPacket=lossFiles.get(offset);
						   lossFiles.remove(offset);
						   pktIDs.remove(offset);					
	
						   Message msg = new Message();
				           msg .obj = recvPacket;
				           msg .arg1=1;
				           FileSharing.myHandler.sendMessage(msg);

					   }				   
			   }
			  else
			  {
				 message ="该文件已经完全接受，不再需要其他的包";
				// FileSharing.messageHandle(message);
			  }
	
		    }  
	   }
	  }
	}

	public class recvTask extends java.util.TimerTask
	{
        public String sub_fileID; 
        public Packet[] pkt=null;
        public int[] subSeq=null;
        public int len=0;
		recvTask(String sub_fileID)
		{
		this.sub_fileID=sub_fileID;	
		}
		@Override
		public void run() 
		{
			for(int m=0;m<lossFiles.size();m++)
			{
				if(lossFiles.get(m)[0].sub_fileID.equals(sub_fileID))
				{
					pkt=lossFiles.get(m);
					subSeq=pktIDs.get(m);
					break;
				}
			}
		if(pkt!=null&&subSeq!=null)
		{
			for(int n=0;n<subSeq.length;n++)
			{
				if(subSeq[n]==-1)
        		 {
        			len=n; 
        			break;
        		 }
			}
			int total=pkt[0].data_blocks;
      
          if(len<total)
           {
     		  mess ="超时，收到文件： "+sub_fileID+",包："+len+" 个 。";
     		  FileSharing.messageHandle(mess);
     		  System.out.println("超时，收到文件： "+sub_fileID+",包："+len+" 个 。");
     		  System.out.println("发送反馈包");
			  int lossPkts=total-len ;
			  ArrayList<Integer>losspkts=new  ArrayList<Integer>();
			  losspkts.add(lossPkts);
			  sendFeedBackPackFunction sfb=new sendFeedBackPackFunction(pkt[0].sub_fileID,losspkts,1);
			  sfb.sendFeedBack();
         } 
		}
	  }
	}
	public void destroy() 
	{
		running=false;
		socket.close();
		filesID.clear();
		lossFiles.clear();
		pktIDs.clear();
	    if(recvTimers.size()>0)
		 {
		  Iterator it = recvTimers.keySet().iterator();
		  while (it.hasNext())
		   {
		   String key=null;
		   key=(String)it.next();
		   recvTimers.get(key).cancel();  //防止还有计时器在运行
		   }
		  recvTimers.clear();
		}
	}	
}

