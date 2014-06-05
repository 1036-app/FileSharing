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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Timer;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.Message;


public class FileSharing extends Activity
{
	
	public static String ipaddress="";
	public static String bcastaddress="";
	public static int port=40000;
	public static final int blocklength=1024;      //ÿ��Ĵ�С1k
	public static final int maxfilelength=102400;    //�ļ����С����Ϊ100k
	public static int sendFileID=0;  //�ļ���id��
	public static String sharedPath="//sdcard//SharedFiles";
	
	public static Handler myHandler=null;
    public static Handler recvHandler= null;
	public sendThread sThread=null;
	public recvThread rThread=null;	
	public test tt=new test();  //���ض�̬��
	public Button stop=null;
	public TextView information = null;
	public ScrollView mScrollView =null;
	public FileObserver mFileObserver;
	public static BufferedWriter bw=null;
	public static long total_encode_timer=0;
	public String message=null;
    public OtherTask othertask=null;
    public Timer othertimer=null;
   
    public static HashMap<String,String>sendFiles=new HashMap<String,String>(); //�����ļ��б�
    public static ArrayList<String>recvFiles=new ArrayList<String>();        //�����ļ��б�
    public static Map<String,Integer>nextseq=new HashMap<String,Integer>(); //next packetID
    public static ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //���������ļ��ţ�������
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//�����˵ķ�������Ϣ
	public static Map<String,Timer> feedTimers=new HashMap<String,Timer>();//�յ�������ʱ��ʼ��ʱ
   
	public static Queue<String> SendFilequeue = new LinkedList<String>();  
	public static Map<String,Timer> subfiles=new HashMap<String,Timer>();
	public static final int subtime=2000; //2s֮��û���յ�������������һ��    
	public static listenQueue listenqueue=null;
	public static String sending_subfileid=null;  //��ǰ���͵��ļ����
	     
	public static ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();//�����յ����ļ���
	public static Map<String,Integer>subFile_nums=new HashMap<String,Integer>(); //�յ���Ӧ�ļ���<10m���Ŀ���
	public static Map<String,Integer>sub_nums=new HashMap<String,Integer>();//����10m���ļ����յ��Ŀ���
	public static Map<String,ArrayList<Integer>> recv_subfiels_no=new HashMap<String,ArrayList<Integer>>(); //�洢�յ��Ķ�Ӧ�ļ��Ŀ��
	public static Map<String,Timer> subfileTimers=new HashMap<String,Timer>();  //ÿ���յ�һ���ļ���ͻ��ʱ
	public static final int block_time=50000;  //�ļ����Ϳ鷴������ʱ��
	
	
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	information=(TextView) findViewById(R.id.information);
        information.setMovementMethod(ScrollingMovementMethod.getInstance());
        information.setScrollbarFadingEnabled(false);  
        stop=(Button)findViewById(R.id.stopbutton);
        stop.setOnClickListener(new stopClickListener());
        mScrollView = (ScrollView)findViewById(R.id.sv_show);
        ipaddress=getIp();
        total_encode_timer=0;
        bcastaddress="192.168.1.255";
        /* WIFI:255.255.255.255
         * Adhoc:192.168.1.255
         */
        information.append("�ҵ�IP��ַ�ǣ�"+ipaddress+"\n");
        information.append("�ҵĹ㲥��ַ�ǣ�"+bcastaddress+"\n");
        rThread=new recvThread(ipaddress,port);
        rThread.start();
        listenqueue=new listenQueue();
        listenqueue.start();
        
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        information.append("�ֻ������߳��ڴ棺"+activityManager.getMemoryClass()+"M\n");	
  	    SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss");
	    Date curDate = new Date(System.currentTimeMillis());
	    String m = formatter.format(curDate);
	    String sPath="//sdcard//Log";
        File SDir=new File(sPath);
        if (!SDir.exists())
        {
        	SDir.mkdirs();
        }
        String logPath=sPath+"//log-"+m+".txt";
  		File f=new File(logPath);		  
    	 try {
			bw=new BufferedWriter(new FileWriter(f));
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	 writeLog("filename"+",	");
    	 writeLog("fileID"+",	");
	     writeLog("fileLength"+",	");
	     writeLog("time"+",	");
         writeLog("formatter-time"+",	"+"\r\n");
        myHandler = new Handler()
        {
        	public void handleMessage(Message Msg)
        	{
        		if(Msg.arg1==0)
        		{
        		  String s = (String) Msg.obj;
        		  information.append(s+"\n");	
        		  if (mScrollView!= null&&information!= null)
                  { 
        			//getMeasuredHeight()��ʵ��View�Ĵ�С������Ļ�޹أ���getHeight�Ĵ�С��ʱ������Ļ�Ĵ�С,��λpx��
        			if(information.getMeasuredHeight()>mScrollView.getHeight())
        			{
                    int offset = information.getMeasuredHeight()-mScrollView.getHeight(); 
                    mScrollView.scrollTo(0,offset+30); //TextView tishi�ĸ߶���30px
        			}
                   
                  }
        		  if(s.equals("�����ļ�"))
        		   Toast.makeText(FileSharing.this,"�����ļ�",Toast.LENGTH_LONG ).show();
        		}  
        		else if(Msg.arg1==1)
        		{
        		    Packet[] recvPacket=(Packet[]) Msg.obj;	  
        		    Packet p=recvPacket[0];
        	        if(p.type==0&&recvPacket.length>=p.data_blocks)
        		    {
        		       information.append( "�յ��������ݰ�\n");	
        		       fecFuntion ff=new fecFuntion();
        		       ff.decode(recvPacket);	  //���ý��뺯��
        		    }		 
        		    if(p.type==1)
        		    {		
        		    	FeedBackData fb=null;	  
        		    	 try {  
        					    ByteArrayInputStream bais = new ByteArrayInputStream(p.data);
        					    ObjectInputStream ois = new ObjectInputStream(bais);
        					    fb = (FeedBackData)ois.readObject(); 
        					    bais.close();  
        					    ois.close();  
        					  }  
        					  catch(Exception e)
        					  {    
        					    System.out.println(e.toString());
        					    e.printStackTrace();
        					  }			   
        			      handleFeedBackPackets(fb);
        		    }
        		  
              } 
        }	       	
     };
        //Ҫ�ȴ���һ���ļ���
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
       
        if(mFileObserver==null ) 
        {
            mFileObserver = new SDCardFileObserver(sharedPath);
            mFileObserver.startWatching(); //��ʼ����
        }
        //��ʱ���±��˵ķ������б�
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,2000,2000); //ÿ3s����һ��
	     
   }
    public static void writeLog(String info)
    {
    	  try {
      		  bw.append(info);
      		  bw.flush();
  		    } catch (Exception e) 
  		    {		
  			e.printStackTrace();
  		    }

    }
    /*
     * �����յ��ķ������������鶪ʧ�������͵����İ���ʧ������
     */
   public void handleFeedBackPackets (FeedBackData fb)
   {	 
	  String fileid=null;
	  if(fb.type==1) 
        {
		  String []aa=fb.sub_fileID.split("--"); 
		  fileid=aa[0];
        }
	  else
		  fileid=fb.sub_fileID;
	 
	  	if(sendFiles.containsKey(fileid)) 
	  	{ 
	  		if(fb.type==1&&subfiles.containsKey(fb.sub_fileID))
	        {
		     synchronized(subfiles)
		      {
		        subfiles.get(fb.sub_fileID).cancel();  //�յ��������󣬾�ȡ�����ʱ��
		        subfiles.remove(fb.sub_fileID);
		      }
	        } 
             synchronized(Feedpkts)
             {     
	           int i=0;
	           for(;i<Feedpkts.size();i++)
	              {
	        	   if(Feedpkts.get(i).sub_fileID.equals(fb.sub_fileID))
	                {		  
	        		     if(Feedpkts.get(i).nos.get(0)<fb.nos.get(0)||Feedpkts.get(i).nos.size()>fb.nos.size())
	                      {
	        			   Feedpkts.get(i).nos=fb.nos;
	                      }
	        	       break;
	                }
	           }
	           if(i==Feedpkts.size())
	           {
	        	 Feedpkts.add(fb);
	        	 information.append("��ʼ��ʱ��"+fb.sub_fileID+"\n");
	        	 System.out.println("��ʼ��ʱ��"+fb.sub_fileID+"\n");
	        	 Re_SendTask sendtask=null;
	        	 Timer feedtimer=null;
			     sendtask=new Re_SendTask(fb.sub_fileID);
			     feedtimer = new Timer(true);
			     feedtimer.schedule(sendtask,1000,1000); //1s�ĳ�ʱ
			     feedTimers.put(fb.sub_fileID, feedtimer);
	           }
            } 
	      }  
           else
           {
            synchronized(othersFeedpkt)
            {
         	int k=0;
        	for(;k<othersFeedpkt.size();k++)
	        {
		        if(othersFeedpkt.get(k).sub_fileID.equals(fb.sub_fileID))
	          	{ 
		        	if(othersFeedpkt.get(k).nos.get(0)<fb.nos.get(0)||othersFeedpkt.get(k).nos.size()>fb.nos.size())
		         	  {
				       othersFeedpkt.get(k).nos=fb.nos;
				       othersFeedpkt.get(k).time=System.currentTimeMillis();
			          }
		 
			      break;
		        }	
	        }
	       if(othersFeedpkt.size()==k)
	        {
	           otherFeedData ofd=new otherFeedData(fb.sub_fileID,fb.nos,System.currentTimeMillis());
		       othersFeedpkt.add(ofd);
	        }
           }
          } 
	   System.out.println("fb.type="+fb.type);
   }
   public class SDCardFileObserver extends FileObserver //��һ���߳�
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
         	{
         		if(!SendFilequeue.contains(path))
         		{
         			synchronized(SendFilequeue)
         			{
         			SendFilequeue.add(path);
         			}
         			listenqueue.onresume();
         		}	
         	}
        	   break;
           case FileObserver.MOVED_TO:
              	s = "----"+path + " MOVED_TO";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " MOVED_TO");
      		  if(Isrecived==false)  //���յĵ��ļ����ٷ���
           	  {
           		if(!SendFilequeue.contains(path))
           		{
           			synchronized(SendFilequeue)
         			{
           			SendFilequeue.add(path);
         			}
           		    listenqueue.onresume();
           		}	
           	   }
              	break;
           case FileObserver.DELETE:
        		s = "----"+path + " DELETE";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " DELETE");
      		    recvFiles.remove(path);  //�ӽ����ļ��б�ɾ��
      		    System.out.println("�����ļ��б�Ĵ�С��"+sendFiles.size());
      		  if(sendFiles.size()>0)
      		  {
      		   Iterator it = sendFiles.keySet().iterator();
     		   while (it.hasNext())
     		   {
     		   String key=null;
     		   key=(String)it.next();
     	       if(sendFiles.get(key).equals(path)) 
     	        {
     			  sendFiles.remove(key);  
     			  nextseq.remove(key);     //��Ӧ�ļ�����һ���Ӱ�����б�ҲҪɾ��
     			  break;
     		    }
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
    public String getIp()
	{  
    	//Adhocģʽ�»�ȡIP
 // /*  
    	String networkIp = "";  
    	try {  
    	    List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());  
    	    for(NetworkInterface iface : interfaces){  
    	        if(iface.getDisplayName().equals("adhoc0")){  
    	            List<InetAddress> addresses = Collections.list(iface.getInetAddresses());  
    	            for(InetAddress address : addresses){  
    	                if(address instanceof Inet4Address){  
    	                    networkIp = address.getHostAddress();
    	                } 
    	            }  
    	        }  
    	     }
    	    } catch (SocketException e)
    	    {  
    	    e.printStackTrace();  
    	    }  
    	
		return networkIp;
	//	*/
		
   //Wifi��ȡIP��ʽ
	/*	
    	WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
		    if(!wm.isWifiEnabled())                     //���Wifi״̬     
		     wm.setWifiEnabled(true);  
		    WifiInfo wi=wm.getConnectionInfo();        //��ȡ32λ����IP��ַ     
		    int IpAdd=wi.getIpAddress(); 
		    String Ip=intToIp(IpAdd);                 //�����͵�ַת���ɡ�*.*.*.*����ַ  
		    return Ip;    
   */
	}  
	public String intToIp(int IpAdd) 
	{  
	    return (IpAdd & 0xFF ) + "." +  
	    ((IpAdd >> 8 ) & 0xFF) + "." +  
	    ((IpAdd >> 16 ) & 0xFF) + "." +  
	    ( IpAdd >> 24 & 0xFF) ;  
	} 
	
	public static void messageHandle(String mess)
	{
		 Message m = new Message();
		 m.obj = mess;
		 m.arg1=0;
		 myHandler.sendMessage(m);
	}

public class OtherTask extends java.util.TimerTask
{
	
	 public void run() 
  	   { 
         synchronized(othersFeedpkt)
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
   }
  @Override
  protected void onDestroy()
  {
   if(mFileObserver!=null ) 
	 mFileObserver.stopWatching(); //ֹͣ����
   if(recvHandler!=null)
     recvHandler.getLooper().quit(); 
   synchronized(subfileTimers)
   {
   if( subfileTimers.size()>0)
	 {
	  Iterator it =  subfileTimers.keySet().iterator();
	  while (it.hasNext())
	   {
	   String key=null;
	   key=(String)it.next();
	   subfileTimers.get(key).cancel();  //��ֹ���м�ʱ��������
	   }
	  subfileTimers.clear();
	}
   }
   synchronized(subfiles)
   {
   if(subfiles.size()>0)
	 {
	  Iterator it =  subfiles.keySet().iterator();
	  while (it.hasNext())
	   {
	   String key=null;
	   key=(String)it.next();
	   subfiles.get(key).cancel();  //��ֹ���м�ʱ��������
	   }
	  subfiles.clear();
	}
   }
 
   rThread.destroy();
   othertimer.cancel();
   RecvSubFiles.clear();
   nextseq.clear();
   Feedpkts.clear();
   othersFeedpkt.clear();
   recvFiles.clear();
   if(sThread!=null)
      sThread.destroy(); 
   listenqueue.ondestroy();
   SendFilequeue.clear();
   writeLog("encode time:"+total_encode_timer+" ms"+"\r\n");
   try {
	bw.close();
} catch (IOException e) {
	e.printStackTrace();
}
   super.onDestroy();
 }

}   
