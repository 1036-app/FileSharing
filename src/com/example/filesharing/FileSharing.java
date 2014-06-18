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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import java.util.Stack;
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
	public static final int blocklength=1024;      //每块的大小1k
	public static final int maxfilelength=102400;    //文件块大小限制为100k
	public static int sendFileID=0;  //文件的id号
	public static String sharedPath="/sdcard/SharedFiles";
	
	public static Handler myHandler=null;
    public static Handler recvHandler= null;
	public static sendThread sThread=new sendThread();
	public recvThread rThread=null;	
	public test tt=new test();  //加载动态库
	public Button stop=null;
	public TextView information = null;
	public ScrollView mScrollView =null;
	public FileObserver mFileObserver;
	public static BufferedWriter bw=null;
	public static long total_encode_timer=0;  //编码总共花费的时间
	public static long total_sending_timer=0; //发送总共花费的时间
	public static long total_sending_length=0; 
	public static int stored_blocks=20;  //存储编好码的块
	public String message=null;
    public OtherTask othertask=null;
    public Timer othertimer=null;
    public int otherPcks_time=2000; //别人反馈包的过期时间 ms
    public int selfFeedPcks_time=1000; //收到对自己反馈包的过期时间 ms
	public static int sleeptime=3000;  //计时3秒之后在开始监控队列。
	
    public static HashMap<String,String>sendFiles=new HashMap<String,String>(); //发送文件列表
    public static ArrayList<String>recvFiles=new ArrayList<String>();        //接收文件列表
    public static Map<String,Integer>nextseq=new HashMap<String,Integer>(); //next packetID
    public static ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //反馈包的文件号，丢包数
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//其他人的反馈包信息
	public static Map<String,Timer> feedTimers=new HashMap<String,Timer>();//收到反馈包时开始计时
   
	public static Queue<String> SendFilequeue = new LinkedList<String>();   
	public static listenQueue listenqueue=null;     
	public static ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();//放置收到的文件块
	public static Map<String,Integer>subFile_nums=new HashMap<String,Integer>(); //收到对应文件（<10m）的块数
	public static Map<String,Integer>sub_nums=new HashMap<String,Integer>();//大于10m的文件接收到的块数
	public static Map<String,ArrayList<Integer>> recv_subfiels_no=new HashMap<String,ArrayList<Integer>>(); //存储收到的对应文件的块号
	public static Map<String,Timer> subfileTimers=new HashMap<String,Timer>();  //每当收到一个文件块就会计时
	public static final int block_time=4000;  //文件发送块反馈包的时间
	public static long currentLength=0;  //当前文件列表中文件的长度
	public long  maxQueueLength=1024*10240; //队列的最大容量
	public static int file_number=0;
	public int maxNumber=20;  //队列容许的最大文件数
	public static fecFuntion fecfunction=new fecFuntion(); //程序中值实现一个fecFuntion实例，且用了synchronized发送，避免内存溢出
	public static ArrayList<Packet[]> encodedPacket=new  ArrayList<Packet[]>(); //编码完成的块
	SDCardFileObserver  fileobver=null;
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
        total_encode_timer=0;  //编码总共花费的时间
    	total_sending_timer=0; //发送总共花费的时间
    	total_sending_length=0; 
        information.append("我的IP地址是："+ipaddress+"\n");
        information.append("我的广播地址是："+bcastaddress+"\n");
        rThread=new recvThread(ipaddress,port);
        rThread.start();
        listenqueue=new listenQueue();
        listenqueue.start();
        
        
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        information.append("手机容许线程内存："+activityManager.getMemoryClass()+"M\n");	
  	    SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss");
	    Date curDate = new Date(System.currentTimeMillis());
	    String m = formatter.format(curDate);
	    String sPath="//sdcard/Log";
        File SDir=new File(sPath);
        if (!SDir.exists())
        {
        	SDir.mkdirs();
        }
        String logPath=sPath+"/log-"+m+".txt";
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
        			//getMeasuredHeight()是实际View的大小，与屏幕无关，而getHeight的大小此时则是屏幕的大小,单位px。
        			if(information.getMeasuredHeight()>mScrollView.getHeight())
        			{
                    int offset = information.getMeasuredHeight()-mScrollView.getHeight(); 
                    mScrollView.scrollTo(0,offset+30); //TextView tishi的高度是30px
        			}
                   
                  }
        		  if(s.equals("生成文件"))
        		   Toast.makeText(FileSharing.this,"生成文件",Toast.LENGTH_LONG ).show();
        		}  
        		else if(Msg.arg1==1)
        		{
        		    Packet[] recvPacket=(Packet[]) Msg.obj;	  
        		    Packet p=recvPacket[0];
        	        if(p.type==0&&recvPacket.length>=p.data_blocks)
        		    {
        		       information.append( "收到的是数据包\n");	
        		       fecfunction.decode(recvPacket);	  //调用解码函数
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
        //要先创建一个文件夹
        File SharedDir=new File(sharedPath);
        if (!SharedDir.exists())
        {
        	SharedDir.mkdirs();
        }
        if(mFileObserver==null ) 
        {
            mFileObserver = new SDCardFileObserver(sharedPath);
            mFileObserver.startWatching(); //开始监听
        }
       
        //定时更新别人的反馈包列表
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,otherPcks_time,otherPcks_time);
	     
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
     * 处理收到的反馈包，包括块丢失反馈包和单个的包丢失反馈包
     */
   public void handleFeedBackPackets (FeedBackData fb)
   {	 
	  String fileid=null;
	  if(fb.type==1) 
        {
		  String []fileID_break=fb.sub_fileID.split("--"); 
		  fileid=fileID_break[0];
        }
	  else
		  fileid=fb.sub_fileID;
	 
	  	if(sendFiles.containsKey(fileid)) 
	  	{ 
             synchronized(Feedpkts)
             {     
	           int i=0;
	           for(;i<Feedpkts.size();i++)
	              {
	        	   if(Feedpkts.get(i).sub_fileID.equals(fb.sub_fileID))
	                {		  
	        		     if(Feedpkts.get(i).nos.get(0)<fb.nos.get(0)||Feedpkts.get(i).nos.size()<fb.nos.size())
	                      {
	        			   Feedpkts.get(i).nos=fb.nos;
	                      }
	        	       break;
	                }
	           }
	           if(i==Feedpkts.size())
	           {
	        	 Feedpkts.add(fb);
	        	 information.append("开始计时："+fb.sub_fileID+"\n");
	        	 System.out.println("开始计时："+fb.sub_fileID+"\n");
	        	 Re_SendTask sendtask=null;
	        	 Timer feedtimer=null;
			     sendtask=new Re_SendTask(fb.sub_fileID);
			     feedtimer = new Timer(true);
			     feedtimer.schedule(sendtask,selfFeedPcks_time,selfFeedPcks_time); 
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
		        	if(othersFeedpkt.get(k).nos.get(0)<fb.nos.get(0)||othersFeedpkt.get(k).nos.size()<fb.nos.size())
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
	   System.out.println("收到反馈包 fb.type="+fb.type);
   }
   
   public class SDCardFileObserver extends FileObserver //是一个线程
    {
       //mask:指定要监听的事件类型，默认为FileObserver.ALL_EVENTS
	   public String directory=null;
	   List<SingleFileObserver> mObservers=null;
	   String mPath=null;
	   int mMask;
       public SDCardFileObserver(String path,int mask) 
       {
           super(path, mask);
       }
       public SDCardFileObserver(String path) 
       {
           super(path);
           directory=path;  //sdcard//Shared//...
           mPath=path;
     System.out.println("当前监听的目录："+directory);
       }
       @Override 
       public void startWatching()
       {
       if (mObservers != null)
          return ;
       mObservers = new ArrayList<SingleFileObserver>();
       Stack <String>  stack= new Stack <String>();
       stack.push(mPath);
       while (!stack.isEmpty())
       {
          String parent = stack.pop();
          mObservers.add(new SingleFileObserver(parent));
          File path = new File(parent);
          File[]files = path.listFiles();
          if (null == files)
           continue;
         
          for (File f: files)
          {
            if(f.isDirectory() && !f.getName().equals(".") && !f.getName() .equals(".."))
            {
            stack.push(f.getPath());
            }
         }
      }
     for (SingleFileObserver sfo: mObservers)
     {
       sfo.startWatching();
     }
  }
    @Override 
   public void stopWatching()
   {
       if (mObservers == null)
         return ;
       synchronized(mObservers)
       {
       for (SingleFileObserver sfo: mObservers)
       {
       sfo.stopWatching();
       }
       mObservers.clear();
       mObservers = null;
       }
  }
       @Override
       public void onEvent(int event, String path) //此时的path就是绝对路径了
       {
           final int action = event & FileObserver.ALL_EVENTS;    
           String s;
           Message m;
           switch (action) 
           {  
           case FileObserver.CREATE:
          		File f=new File(path);
          	    if(f.isDirectory())
          	    {
          	        s = "----"+path + " CREATE";
               		m = new Message();
               		m.obj = s;
               		myHandler.sendMessage(m);
        	        System.out.println("----"+path+" CREATE:"); 
        	        handleDirectory(path);
          	   }    
   			break;
           case FileObserver.MODIFY:  //文件内容被修改时触发，如粘贴文件等
        	   break;
           case FileObserver.CLOSE_WRITE:  //编辑文件后，关闭
               s = "----"+path + " CLOSE_WRITE";
       		   m = new Message();
       		   m.obj = s;
       		   myHandler.sendMessage(m);
       	       System.out.println("----"+path + " CLOSE_WRITE");
        	   addToQueue(path);
        	   break;
           case FileObserver.MOVED_TO:
              	s = "----"+path + " MOVED_TO";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " MOVED_TO");
      		  if(recvFiles.size()!=0) 
      		    addToQueue(path);
              	break;
           case FileObserver.DELETE:
        		s = "----"+path + " DELETE";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " DELETE");
      		    File file=new File(path);
        	    if(mObservers != null)
        	    {
        	    	synchronized(mObservers)
        	    	{
        	    	for(int i=0;i<mObservers.size();i++)
        	    	{
        	    		if(mObservers.get(i).mPath.equals(path))
        	    		{
        	    			System.out.println("停止监听该目录");
        	    			mObservers.get(i).stopWatching();
        	    			mObservers.remove(i);	
       	    			    break;
        	    		}
       
        	    	}
        	      }
        	    }
      		    synchronized(recvFiles)
      		    {
      		    recvFiles.remove(path);  //从接收文件列表删除
      		    }
      		    System.out.println("发送文件列表的大小："+sendFiles.size());
      		    synchronized(sendFiles)
      		    {
      		   if(sendFiles.size()>0)
      		   {
      		   Iterator it = sendFiles.keySet().iterator();
     		   while (it.hasNext())
     		   {
     		   String key=null;
     		   key=(String)it.next();  //第一次调用Iterator的next()方法时，它返回序列的第一个元素
     	       if(sendFiles.get(key).equals(path)) 
     	        {
     			  sendFiles.remove(key); 
     			 synchronized(nextseq)
       		     {
     			  nextseq.remove(key);     //对应文件的下一个子包序号列表也要删除
       		     }
     			  break;
     		     }
     		    }
      		    }
      		   }
        	   break;
           }
       }
       
   public class SingleFileObserver extends FileObserver
   {
       String mPath;
       String newPath=null;
       public SingleFileObserver(String path)
       {
         this(path, ALL_EVENTS);
         mPath = path;
       }
       public SingleFileObserver(String path, int mask)
       {
         super(path, mask);
         mPath = path;
       }
       @Override 
       public void onEvent(int event, String path)
       {
    	 newPath = mPath + "/" + path;
         SDCardFileObserver.this.onEvent(event, newPath);
       }
       @Override 
       public void stopWatching()
       {
    	   System.out.println("stoping watchinging ");
       }
    }
   public synchronized void handleDirectory(String path)
   {
	   synchronized(recvFiles)
		 {
	   if(!recvFiles.contains(path)) //接收的的文件不再发送
	  	{
		 String filepath=path;  //要监听的子目录
		 System.out.println("开始监听子文件夹"+filepath);   
		 SingleFileObserver sfb=new SingleFileObserver(filepath);	 
		 sfb.startWatching();
		 mObservers.add(sfb);
	 
	     File f=new File(filepath);
		 File[] files= f.listFiles();
		 for(int i=0;i<files.length;i++)
		 {   
			 if(files[i].isDirectory())
			 {
				 handleDirectory(filepath+"/"+files[i].getName()); 
			 }
			 else
		      addToQueue(filepath+"/"+files[i].getName());
		 }
	 }	
	}  
   }
  }
 public synchronized void addToQueue(String path)
 {
	 synchronized(recvFiles)
	 {
	   if(!recvFiles.contains(path)&&!SendFilequeue.contains(path))  //接收的的文件不再发送
  	    {
	     	   boolean contain=false;
	     	   Iterator it = sendFiles.keySet().iterator();
	     	   while(it.hasNext())
	     	   {
	     		   String fileid=(String)it.next();
	     		   if(sendFiles.get(fileid).equals(path))
	     			 {
	     			   contain=true;
	     			   break;
	     			 }		   
	     	   }
	      if(contain==false)
	     	{
			synchronized(SendFilequeue)
			{  
			  String filename=path;  //绝对路径
			  SendFilequeue.add(filename);
			  FileInputStream fis;
      	      long filelength=0;
      	 	  try {
      	 	  fis = new FileInputStream(filename); 
      	 	  filelength= fis.available();
      	 	  } catch (Exception e) {
      	 	  e.printStackTrace();
      	 	  }
      	 	  file_number++;
      	 	  currentLength=currentLength+filelength;
      	 	  System.out.println("fileName: "+path);   
      	 	  System.out.println("SendFilequeue Information:"+file_number+" 个 "+currentLength+" Byte");
      		  if(currentLength>=maxQueueLength||file_number>=maxNumber) 
      		  {
      			
      			 synchronized(listenqueue.queueListen_List)
      			  {
      			  for(int i=0;i<listenqueue.queueListen_List.size();i++)
      				 listenqueue.queueListen_List.get(i).cancel();
      			     listenqueue.queueListen_List.clear();
      			  }
      			 Timer queueTimer=new Timer(true);
      			 QueueListenTask queueTask=new QueueListenTask();
      			 queueTimer.schedule(queueTask, FileSharing.sleeptime);
      			 listenqueue.queueListen_List.add(queueTimer);
      			 
      			 System.out.println("###队列超过了最大容量或是文件数量达到了最大数量,进行处理");
      			 System.out.println("重新计时："+System.currentTimeMillis()); 
    		    listenqueue.handleQueue(SendFilequeue); 
      		  }	
      		}
	     }		
  	   } //end 改变的是文件
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
    	//Adhoc模式下获取IP
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
		
   //Wifi获取IP方式
	/*	
    	WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
		    if(!wm.isWifiEnabled())                     //检查Wifi状态     
		     wm.setWifiEnabled(true);  
		    WifiInfo wi=wm.getConnectionInfo();        //获取32位整型IP地址     
		    int IpAdd=wi.getIpAddress(); 
		    String Ip=intToIp(IpAdd);                 //把整型地址转换成“*.*.*.*”地址  
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
			 if(System.currentTimeMillis()-othersFeedpkt.get(k).time>3000) //3s后过期
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
   writeLog("sending time:"+total_sending_timer+" ms"+"\r\n");
   writeLog("sending length:"+total_sending_length/1024+" k"+"\r\n");
   writeLog("encode time:"+total_encode_timer+" ms"+"\r\n");
  
   System.out.println("sending time:"+total_sending_timer+" ms");
   System.out.println("sending length:"+total_sending_length/1024+" k"+"\r\n");
   System.out.println("encode time:"+total_encode_timer+" ms");
  
   encodedPacket.clear();
   if(mFileObserver!=null ) 
	 mFileObserver.stopWatching(); //停止监听
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
	   subfileTimers.get(key).cancel();  //防止还有计时器在运行
	   }
	  subfileTimers.clear();
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
   try {
	bw.close();
} catch (IOException e) {
	e.printStackTrace();
}
   super.onDestroy();
 }

}   
