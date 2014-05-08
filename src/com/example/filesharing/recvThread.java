package com.example.filesharing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;


import android.os.Message;


public class recvThread extends Thread
{
	public DatagramSocket socket;
	public DatagramPacket packet;
	public InetAddress addr = null;
	public boolean running=true;
	public boolean canReturn=false;
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
			encodedFrame=new byte[6000];
		
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
		   Message mm = new Message();
		   mm.obj ="接收到子包的ID：  "+pt.fileID;
		   FileSharing.myHandler.sendMessage(mm);
		   if(pt.type==1)
		   {
			   Message m = new Message();
			   m.obj ="反馈包，发送者："+packet.getAddress().toString();
			   FileSharing.myHandler.sendMessage(m);
			   recvPacket=new Packet[1];
	           recvPacket[0]=pt;
	           canReturn=true;
		   }
		   else
		   {
			  if(!filesID.contains(pt.fileID))
			  {
				 int offset=0;  //该文件放在列表中的第offset个表项
				 int pktLength=0; //接受的包放在了第pktLength个包
				 canReturn=false;
					   for(offset=0;offset<lossFiles.size();offset++) 
					    {
					        if(pt.fileID.equals(lossFiles.get(offset)[0].fileID))
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
							//加计时器
							Message m = new Message();
							m.obj ="开始计时，针对文件 "+pt.fileID;
							FileSharing.myHandler.sendMessage(m);
							recvtask=new recvTask(pt.fileID);
							recvtimer = new Timer(true);
							recvTimers.put(pt.fileID, recvtimer);
							recvtimer.schedule(recvtask,3000,3000); //3s的超时
					   }
					   
					   if(pktLength==lossFiles.get(offset)[0].data_blocks )
					   {
						   System.out.println("接收完毕，总共接收到的包："+pktLength);
						   filesID.add(lossFiles.get(offset)[0].fileID);
						   //取消接收计时器
						   if(recvTimers.containsKey(lossFiles.get(offset)[0].fileID))
						     { 
							     Message m = new Message();
								 m.obj ="针对文件： "+pt.fileID+" 的计时器 取消,接收包完毕";
								 FileSharing.myHandler.sendMessage(m);
						    	 recvTimers.get(pt.fileID).cancel();
						    	 recvTimers.remove(pt.fileID);
						     }
						   recvPacket=lossFiles.get(offset);
						   lossFiles.remove(offset);
						   pktIDs.remove(offset);					
						   
						   canReturn=true;
					   }
						   
			   }
			  else
			  {
				 canReturn=false;
				 Message m = new Message();
				 m.obj ="该文件已经完全接受，不再需要其他的包";
				 FileSharing.myHandler.sendMessage(m);
			  }
	
		    }  
	   }
	  }
	}

	public void sendFeedBack(String id,int lossPkts)
	{
		boolean isSend=true;
		for(int k=0;k<FileSharing.othersFeedpkt.size();k++)
		{
		  if(FileSharing.othersFeedpkt.get(k).fileID.equals(id)&&FileSharing.othersFeedpkt.get(k).loss>=lossPkts)
		  {
			 Message m = new Message();
			 m.obj ="其他人已经发送了反馈包";
			 FileSharing.myHandler.sendMessage(m);
			isSend=false;
			break;
		  }
		}
		if(isSend)
		{
		Packet FeedBack=new Packet(1,0,0,1,0,0);
	    FeedBack.fileID=id;
	    byte[] data = new byte[4];
		FeedBack.data=data;
	    FeedBack.data[0] = (byte)(lossPkts >>> 24);//取最高8位放到0下标				 
	    FeedBack.data[1] = (byte)(lossPkts >>> 16);//取次高8为放到1下标			 
	    FeedBack.data[2] = (byte)(lossPkts >>> 8); //取次低8位放到2下标 
	    FeedBack.data[3] = (byte)(lossPkts );      //取最低8位放到3下标
		Packet[]pt=new Packet[1];
		pt[0]=FeedBack;
		Message m = new Message();
		m.obj ="***发送反馈包";
		FileSharing.myHandler.sendMessage(m);
		sendThread st=new sendThread(pt,FileSharing.bcastaddress,FileSharing.port,0,1,1);
		st.start();	
		}
	}
	
	public class recvTask extends java.util.TimerTask
	{
        public String fileID; 
        public Packet[] pkt=null;
        public int[] subSeq=null;
        public int len=0;
		recvTask(String fileid)
		{
		this.fileID=fileid;	
		}
		@Override
		public void run() 
		{
			for(int m=0;m<lossFiles.size();m++)
			{
				if(lossFiles.get(m)[0].fileID.equals(fileID))
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
        	  Message m = new Message();
     		  m.obj ="计时器到时,接收到文件： "+fileID+",包的个数："+len+" ，不能恢复文件";
     		  FileSharing.myHandler.sendMessage(m);
     	  
        	  //发送反馈包
			  int lossPkts=total-len ;
			  sendFeedBack(fileID,lossPkts) ;		
		
         } 
		}
	  }
	}
	
	public Packet[] getPacket()
	{
		if(canReturn)
		{
		canReturn=false;
		return recvPacket;	
		}
	   else
	   {
		 Packet[] a=null;
		 return a;
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
		 recvTimers.get(key).cancel();  //防止还有计时器在运行
		 recvTimers.remove(key);
		 }
		}
	}	
}

