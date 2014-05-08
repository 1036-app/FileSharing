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
	public ArrayList<int[]> pktIDs=new ArrayList<int[]>();  //���յ��Ķ�Ӧ�ļ����Ӱ������
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
		                //  ���ղ�Ϊ�գ��Ҳ����Լ��İ� 
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
		   mm.obj ="���յ��Ӱ���ID��  "+pt.fileID;
		   FileSharing.myHandler.sendMessage(mm);
		   if(pt.type==1)
		   {
			   Message m = new Message();
			   m.obj ="�������������ߣ�"+packet.getAddress().toString();
			   FileSharing.myHandler.sendMessage(m);
			   recvPacket=new Packet[1];
	           recvPacket[0]=pt;
	           canReturn=true;
		   }
		   else
		   {
			  if(!filesID.contains(pt.fileID))
			  {
				 int offset=0;  //���ļ������б��еĵ�offset������
				 int pktLength=0; //���ܵİ������˵�pktLength����
				 canReturn=false;
					   for(offset=0;offset<lossFiles.size();offset++) 
					    {
					        if(pt.fileID.equals(lossFiles.get(offset)[0].fileID))
					        {   
					        	 //�Ѿ����ǵ�һ���յ����ļ�
					        	 int a=1;
					        	 for(int k=0;k<pktIDs.get(offset).length;k++)
					        	 {
					        		 if(pt.seqno==pktIDs.get(offset)[k])
									   {  
										   System.out.println("���ܵ���ͬ���Ӱ������ ��"+pt.seqno);
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
							   //���½��յ����Ӱ������б���
							    lossFiles.get(offset)[pktLength]=pt;  //�����б��е�offect������ĵ�pktlength����
							    pktIDs.get(offset)[pktLength++]=pt.seqno;
							   }
					        	break;
					       }
					    }
					   if(offset==lossFiles.size())
					   {
						    System.out.println("��һ�ν��ո��ļ�");
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
							//�Ӽ�ʱ��
							Message m = new Message();
							m.obj ="��ʼ��ʱ������ļ� "+pt.fileID;
							FileSharing.myHandler.sendMessage(m);
							recvtask=new recvTask(pt.fileID);
							recvtimer = new Timer(true);
							recvTimers.put(pt.fileID, recvtimer);
							recvtimer.schedule(recvtask,3000,3000); //3s�ĳ�ʱ
					   }
					   
					   if(pktLength==lossFiles.get(offset)[0].data_blocks )
					   {
						   System.out.println("������ϣ��ܹ����յ��İ���"+pktLength);
						   filesID.add(lossFiles.get(offset)[0].fileID);
						   //ȡ�����ռ�ʱ��
						   if(recvTimers.containsKey(lossFiles.get(offset)[0].fileID))
						     { 
							     Message m = new Message();
								 m.obj ="����ļ��� "+pt.fileID+" �ļ�ʱ�� ȡ��,���հ����";
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
				 m.obj ="���ļ��Ѿ���ȫ���ܣ�������Ҫ�����İ�";
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
			 m.obj ="�������Ѿ������˷�����";
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
	    FeedBack.data[0] = (byte)(lossPkts >>> 24);//ȡ���8λ�ŵ�0�±�				 
	    FeedBack.data[1] = (byte)(lossPkts >>> 16);//ȡ�θ�8Ϊ�ŵ�1�±�			 
	    FeedBack.data[2] = (byte)(lossPkts >>> 8); //ȡ�ε�8λ�ŵ�2�±� 
	    FeedBack.data[3] = (byte)(lossPkts );      //ȡ���8λ�ŵ�3�±�
		Packet[]pt=new Packet[1];
		pt[0]=FeedBack;
		Message m = new Message();
		m.obj ="***���ͷ�����";
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
     		  m.obj ="��ʱ����ʱ,���յ��ļ��� "+fileID+",���ĸ�����"+len+" �����ָܻ��ļ�";
     		  FileSharing.myHandler.sendMessage(m);
     	  
        	  //���ͷ�����
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
		 recvTimers.get(key).cancel();  //��ֹ���м�ʱ��������
		 recvTimers.remove(key);
		 }
		}
	}	
}

