/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.filesharing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;
import eu.vandertil.jerasure.jni.Jerasure;
import eu.vandertil.jerasure.jni.ReedSolomon;

public class FileSharing extends Activity
{
    /** Called when the activity is first created. */
	test tt=new test();
	public static String ipaddress="";
	public static String bcastaddress="";
	public sendThread sThread=null;
	public recvThread rThread=null;	
	public recvfile refile=null;
	public Button stop=null;
	public TextView information = null;
    int sendFileID=0;
    public static int port=40000;
    public int maxLoss=0;
    public static Handler myHandler=null;
    public static boolean sendFinished=false;
    public HashMap<String ,Integer> Feedpkts=new HashMap<String ,Integer>(); //���������ļ��ţ�������
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData>();//�����˵ķ�������Ϣ
    public HashMap<String,String>sendFiles=new HashMap<String,String>();
    public ArrayList<String>recvFiles=new ArrayList<String>();
    public SendTask sendtask=null;
    public Timer feedtimer=null;
	public Map<String,Timer> feedTimers=new HashMap<String,Timer>();
    public OtherTask othertask=null;
    public Timer othertimer=null;
    String message=null;
	public FileObserver mFileObserver;
	public String sharedPath=null;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
        ipaddress=getIp();
        bcastaddress="255.255.255.255";
        System.out.println("�ҵ�IP��ַ�ǣ�"+ipaddress);
        System.out.println("�ҵĹ㲥��ַ�ǣ�"+bcastaddress);
        rThread=new recvThread(ipaddress,port);
        rThread.start();
        refile=new recvfile();
        refile.start();
        
       
      
        information=(TextView) findViewById(R.id.information);
        information.setMovementMethod(ScrollingMovementMethod.getInstance());
        information.setScrollbarFadingEnabled(false);  
        stop=(Button)findViewById(R.id.stopbutton);
        stop.setOnClickListener(new stopClickListener());
         myHandler = new Handler()
        {
        	public void handleMessage(Message Msg)
        	{
        		String s = (String) Msg.obj;
        		information.append(s+"\n");
        	 }
        };
        //Ҫ�ȴ���һ���ļ���
        sharedPath="//sdcard//SharedFiles";
        File SharedDir=new File(sharedPath);
        if (!SharedDir.exists())
        {
        	SharedDir.mkdirs();
        }
        if(mFileObserver==null ) 
        {
            mFileObserver = new SDCardFileObserver(sharedPath);
            mFileObserver.startWatching(); //��ʼ����
        }
        //��ʱ���±��˵ķ������б�
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,3000,3000); //ÿ3s����һ��
	    
    
    }
   public class SDCardFileObserver extends FileObserver 
    {
       //mask:ָ��Ҫ�������¼����ͣ�Ĭ��ΪFileObserver.ALL_EVENTS
       public SDCardFileObserver(String path, int mask) 
       {
           super(path, mask);
       }

       public SDCardFileObserver(String path) 
       {
           super(path);
       }

       @Override
       public void onEvent(int event, String path) 
       {
           final int action = event & FileObserver.ALL_EVENTS;
           boolean Isrecived=false;
           if(recvFiles.size()!=0)
               Isrecived=recvFiles.contains(path); 
           String s;
           Message m;
           switch (action) 
           {             
           case FileObserver.CREATE:
   			break;
           case FileObserver.MODIFY:  //�ļ����ݱ��޸�ʱ��������ճ���ļ���
               break;
           case FileObserver.CLOSE_WRITE:  //�༭�ļ��󣬹ر�
            s = "----"+path + " CLOSE_WRITE";
       		m = new Message();
       		m.obj = s;
       		myHandler.sendMessage(m);
       	    System.out.println("----"+path + " CLOSE_WRITE");
         	if(Isrecived==false)  //���յĵ��ļ����ٷ���
		        sendToAll(path);
        	   break;
           case FileObserver.MOVED_TO:
              	s = "----"+path + " MOVED_TO";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " MOVED_TO");
      		    sendToAll(path);
              	break;
           case FileObserver.DELETE:
        		s = "----"+path + " DELETE";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " DELETE");
      		    recvFiles.remove(path);  //�ӽ����ļ��б�ɾ��
      		  if(sendFiles.size()>0)
      		  {
      		   Iterator it = sendFiles.keySet().iterator();
     		   while (it.hasNext())
     		   {
     		   String key=null;
     		   key=(String)it.next();
     	       if(sendFiles.get(key).equals(path))  
     			  sendFiles.remove(key);   //�ӷ����ļ��б�ɾ��
     		   }
      		  }
        	   break;

           }
       }
    }

    public class stopClickListener implements OnClickListener 
    {
		public void onClick(View v) 
		{
			onDestroy();	
		}
    }
    public void sendToAll (String filename)
    {
    	Packet[] sendPacket=null; 
        sendPacket=encode(filename,null);
        if(sendPacket!=null)
        {
        sThread=new sendThread(sendPacket,bcastaddress,port,0,sendPacket[0].data_blocks,sendPacket[0].data_blocks);
        sThread.start();    //����������˼���£����͵��ļ�������ַ���˿ڣ���ʼ���Ӱ���ţ���������ţ����Ͱ�������
        message = "���͵İ��ĸ���: "+sendPacket[0].data_blocks;
		messageHandle(message);
		sendFiles.put(sendPacket[0].fileID,sendPacket[0].filename);  //���뵽�����ļ��б�
        }	
    }
    public Packet[] encode(String filename,String fileID)
    {
    	 int filelength;
         int blocklength = 5000;
         int data_blocks;
         int coding_blocks;
         Packet[] plist =null;
         FileInputStream fis;
         
     try {
        	String file=sharedPath+"//"+filename;
 			fis = new FileInputStream(file);
 			filelength = fis.available();
 			System.out.println("�������ļ����� "+filelength);
 		if(filelength>0)
 		 {
 			BufferedInputStream in = new BufferedInputStream(fis);
 			if(filelength % blocklength == 0)
 			{
 				data_blocks = filelength/blocklength;
 			}
 			else
 			{
 				data_blocks = filelength/blocklength+1;
 			}
 			
 			coding_blocks = data_blocks;
 			
 			plist = new Packet[data_blocks+coding_blocks];
 			
 			byte[][] origindata = new byte[data_blocks][blocklength];
 			byte[][] encodedata = new byte[coding_blocks][blocklength];
 			String id=Integer.toString(sendFileID);
 			for(int i=0;i<data_blocks;i++)
 			{
 				byte[] data = new byte[blocklength];
 				in.read(data,0,blocklength);
 				System.arraycopy(data, 0, origindata[i], 0, blocklength);
 				Packet p = new Packet(0, filelength, coding_blocks, data_blocks, i, blocklength);
 				p.data = data;
 				p.seqno = i;
 				p.filename=filename;
 				plist[i] = p;
 				if(fileID==null)
 					p.fileID =ipaddress+"-"+id ;
 				else
 					p.fileID=fileID;
 						
 			}
 			
 	        int w=8;
 	        
 	        int[] matrix = ReedSolomon.reed_sol_vandermonde_coding_matrix(data_blocks, coding_blocks, w);
 			
 	        Jerasure.jerasure_matrix_encode(data_blocks, coding_blocks, w, matrix, origindata , encodedata, blocklength);
 	        
 	        for(int i=data_blocks;i<data_blocks+coding_blocks;i++)
 			{
 				byte[] data = new byte[blocklength];
 				System.arraycopy(encodedata[i-data_blocks], 0, data, 0, blocklength);
 				Packet p = new Packet(0, filelength, coding_blocks, data_blocks, i, blocklength);
 				p.data = data;
 				p.seqno = i;
 				p.filename=filename;
 				plist[i] = p;
 				if(fileID==null)
 					p.fileID =ipaddress+"-"+id;
 				else
 					p.fileID=fileID;
 			}
 	        
 	       
 	      if(fileID==null)
	       {	
 	    	  message = "�������ɵİ��ĸ�����"+plist.length;
 	          messageHandle(message);
	          sendFileID++;
	       }
 		  }
 		  else
 			plist=null;
 		
 		
 		} catch (FileNotFoundException e1) 
 		{
 			System.out.println("û���ҵ����ļ�");
 			e1.printStackTrace();
 		} catch (IOException e) {		
 			e.printStackTrace();
 		}
      
      return plist;
	}


	public void decode(Packet[] plist)
    {
		
		System.out.println("������뺯��  �յ����ĸ�����"+plist.length+" ��ʼ����");
    	Packet p = plist[0];
    	int data_blocks = p.data_blocks;
    	int coding_blocks = p.coding_blocks;
    	int block_length = p.data_length;
    	int file_length = p.file_length;
    	String recvFilename=p.filename;
    	int w=8;
    	byte[][] recvdata = new byte[data_blocks][block_length];
    	byte[][] recvcode = new byte[coding_blocks][block_length];
    	
    	ArrayList<Integer> reverasure = new ArrayList<Integer>();
    	int[] erasure = new int[data_blocks+coding_blocks+1];
    	int len = plist.length;
    	for(int i=0; i<plist.length; i++)
    	{
    		Packet s = plist[i];
    		reverasure.add(s.seqno);
    		if(s.seqno<data_blocks)
    		{
    			recvdata[s.seqno] = s.data;
    		}
    		else
    		{
    			recvcode[s.seqno-data_blocks] = s.data;
    		}	
    	}
    	int j=0;
    	for(int i=0;i<data_blocks+coding_blocks;i++)
    	{
    		if(reverasure.contains(i))
    		{
    			continue;
    		}
    		else
    		{
    			erasure[j]=i;
    			j++;
    		}
    	}
    	erasure[j]=-1;
    	
    	
    	int[] matrix = ReedSolomon.reed_sol_vandermonde_coding_matrix(data_blocks, coding_blocks, w);
    	Jerasure.jerasure_matrix_decode(data_blocks, coding_blocks, w, matrix, false, erasure, recvdata, recvcode, block_length);
        
    	int remain = file_length;
        byte[] writein = new byte[file_length];
        int k=0;
        int offset=0;
    	while(remain > 0)
    	{
    		int process = Math.min(remain, block_length);
    		System.arraycopy(recvdata[k], 0, writein, offset, process);
    		offset = offset + process;
    		remain = remain - process;
    		k++;
    	}
    	message="#####�����ļ�####";
    	messageHandle(message);
    	recvFiles.add(recvFilename);  //��������ļ��б���
    	System.out.println("�����ļ��б�Ĵ�С��"+ recvFiles.size());
    	try {
    		String encodeFile=sharedPath+"//"+recvFilename;
    		BufferedOutputStream bos;
    		bos = new BufferedOutputStream(new FileOutputStream(encodeFile));
			bos.write(writein);
	        bos.close();
		} catch (FileNotFoundException e) 
		{		
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
  
    }
    
    
    public String getIp()
	{  
	    WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
	    if(!wm.isWifiEnabled())                     //���Wifi״̬     
	     wm.setWifiEnabled(true);  
	    WifiInfo wi=wm.getConnectionInfo();        //��ȡ32λ����IP��ַ     
	    int IpAdd=wi.getIpAddress(); 
	    String Ip=intToIp(IpAdd);                 //�����͵�ַת���ɡ�*.*.*.*����ַ  
	    return Ip;    
	}  
	public String intToIp(int IpAdd) 
	{  
	    return (IpAdd & 0xFF ) + "." +  
	    ((IpAdd >> 8 ) & 0xFF) + "." +  
	    ((IpAdd >> 16 ) & 0xFF) + "." +  
	    ( IpAdd >> 24 & 0xFF) ;  
	} 
	
	public void messageHandle(String mess)
	{
		 Message m = new Message();
		 m.obj = mess;
		 myHandler.sendMessage(m);
	}
	
	public class recvfile extends Thread
	{
		public boolean running=true;
		public Packet[] lastPacket=null;
		@Override
		public void run() 
		{
		while(running)
		 {
		    Packet[] recvPacket=null;
			recvPacket=rThread.getPacket();	
			if(recvPacket!=null)
			{		  
			    Packet p=recvPacket[0];
			  if(p.type==0&&recvPacket.length>=p.data_blocks)
			  {
				  message = "�յ��������ݰ�";
				  messageHandle(message);
				  decode(recvPacket);	  //���ý��뺯��
			  }		 
			  if(p.type==1)
			  {
					int v0 = (p.data[0] & 0xff) << 24;//&0xff��byteֵ�޲���ת��int,����Java�Զ�����������,�ᱣ����λ�ķ���λ	
					int v1 = (p.data[1] & 0xff) << 16;
					int v2 = (p.data[2] & 0xff) << 8;
					int v3 = (p.data[3] & 0xff) ;
					int loss= v0 + v1 + v2 + v3;
					message = "��������ʾ��ʧ��������  "+loss;
				     messageHandle(message);
			  if(sendFiles.containsKey(p.fileID)) 
			   {
				message = "**�յ����Ƕ��Լ��ķ�����";
			    messageHandle(message);
				if(Feedpkts.containsKey(p.fileID))
				{
					if(Feedpkts.get(p.fileID)<loss)
					{
					Feedpkts.remove(p.fileID);
					Feedpkts.put(p.fileID, loss);
					}
				}
				else
				{
					Feedpkts.put(p.fileID, loss);
					message = "��ʼ��ʱ��"+p.fileID;
				     messageHandle(message);
					sendtask=new SendTask(p.fileID);
				    feedtimer = new Timer(true);
				    feedTimers.put(p.fileID, feedtimer);
					feedtimer.schedule(sendtask,3000,3000); //3s�ĳ�ʱ
					
				}
			  }
			  else
			  {
				message = "$$�յ����Ǳ��˵ķ�����";
			     messageHandle(message);
				int k=0;
				for(;k<othersFeedpkt.size();k++)
				{
					if(othersFeedpkt.get(k).fileID.equals(p.fileID))
					{
						if(othersFeedpkt.get(k).loss<loss)
						{
							othersFeedpkt.get(k).loss=loss;
							othersFeedpkt.get(k).time=System.currentTimeMillis();
						}
						break;
					}	
				}
				if(othersFeedpkt.size()==k)
				{
					otherFeedData ofd=new otherFeedData(p.fileID,loss,System.currentTimeMillis());
					othersFeedpkt.add(ofd);
				}

			  }
		  }
		}
			try {
			   sleep(10);
			} catch (InterruptedException e)
			{			
				e.printStackTrace();
			}
		 }	
		}
		
		@Override
		public void destroy()
		{
			running=false;	
		}		
	}

	
public class SendTask extends java.util.TimerTask
{
	public String fileID;
	public String fileName;
	SendTask (String fileid)
	{
		this.fileID=fileid;
	}
	 public void run() 
  	   { 
		 if(Feedpkts.containsKey(fileID))
		 {
			 //�ҵ��ļ��ٷ�һ��
			int number=Feedpkts.get(fileID);
	        message = "��ʱ���ٷ� "+number+" ����";
		    messageHandle(message);
		    
		    if(feedTimers.containsKey(fileID))
		    {
		    	feedTimers.get(fileID).cancel();  
		    	feedTimers.remove(fileID);
		    }
		   
		    if(sendFiles.containsKey(fileID))
		    {
		     fileName=sendFiles.get(fileID);
		     Packet[] sPacket=null; 
		     sPacket=encode(fileName,fileID);
		        if(sPacket!=null)
		        {
		        sThread=new sendThread(sPacket,bcastaddress,port,sPacket[0].data_blocks,sPacket.length,number);
				sThread.start();    
				Feedpkts.remove(fileID);      
		        }		    
		    }
		
		 }
  	  }
 }

public class OtherTask extends java.util.TimerTask
{
	
	 public void run() 
  	   { 
		
		 for(int k=0;k<othersFeedpkt.size();k++)
			{
			 if(System.currentTimeMillis()-othersFeedpkt.get(k).time>3000) //3s�����
				{
				 othersFeedpkt.remove(k);
				 k--;
				}
			}
  	  }
   }
  @Override
  protected void onDestroy()
  {
   if(mFileObserver!=null ) 
	 mFileObserver.stopWatching(); //ֹͣ����
   refile.destroy();
   rThread.destroy();
   othertimer.cancel();
   if(sThread!=null)
      sThread.destroy(); 
   super.onDestroy();
 }

}   
