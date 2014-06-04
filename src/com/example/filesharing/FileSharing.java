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
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;
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
import android.os.Looper;
import android.os.Message;
import eu.vandertil.jerasure.jni.Jerasure;
import eu.vandertil.jerasure.jni.ReedSolomon;

public class FileSharing extends Activity
{
    /** Called when the activity is first created.*/
	/** Called when the activity is first created.*/
	test tt=new test();
	public static String ipaddress="";
	public static String bcastaddress="";
	public sendThread sThread=null;
	public recvThread rThread=null;	
	public Button stop=null;
	public TextView information = null;
	public ScrollView mScrollView =null;
    public static int sendFileID=0;
    public static int port=40000;
    public int maxLoss=0;
    public static Handler myHandler=null;
    public static Handler recvHandler= null;
    public static boolean sendFinished=false;
    public Map<String,Integer>nextseq=new HashMap<String,Integer>();
    public ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //反馈包的文件号，丢包数
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//其他人的反馈包信息
    public HashMap<String,String>sendFiles=new HashMap<String,String>();
    public ArrayList<String>recvFiles=new ArrayList<String>();
	public Map<String,Timer> feedTimers=new HashMap<String,Timer>();
    public Map<String,Timer> subfileTimers=new HashMap<String,Timer>();
    public Map<String,ArrayList<Integer>> recv_subfiels_no=new HashMap<String,ArrayList<Integer>>(); //存储收到的对应文件的块号
    public OtherTask othertask=null;
    public Timer othertimer=null;
    String message=null;
	public FileObserver mFileObserver;
	public static String sharedPath=null;
	public static final int blocklength=1024;      //每块的大小1k
	public static final int packet_numbers=100;  //一次发包的个数
	public static final int maxfilelength=102400;    //文件块大小限制为100k

	public Queue<String> SendFilequeue = new LinkedList<String>();  
	public Queue<String> sub_filequeue = new LinkedList<String>();
	public sub_fileTask  subfiletask=null;
	public Timer subfiletimer=null;
	public Map<String,Timer> subfiles=new HashMap<String,Timer>();
	public final int subtime=2000;    
	public Packet[] SavedencodedPkts=null; 
	public  listenQueue listenqueue=null;
	public String sending_subfileid=null;
	 
	    
	public ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();
	public Map<String,Integer>subFile_nums=new HashMap<String,Integer>();
	public Map<String,Integer>sub_nums=new HashMap<String,Integer>();
	public static final int block_time=50000;  //文件发送块反馈包的时间
	public static BufferedWriter bw=null;
	public  long total_encode_timer=0;
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
    	 writeLog("filename"+",");
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
        		       decode(recvPacket);	  //调用解码函数
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
        sharedPath="//sdcard//SharedFiles";
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
       
        if(mFileObserver==null ) 
        {
            mFileObserver = new SDCardFileObserver(sharedPath);
            mFileObserver.startWatching(); //开始监听
        }
        //定时更新别人的反馈包列表
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,2000,2000); //每3s更新一次
	     
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
		        subfiles.get(fb.sub_fileID).cancel();  //收到反馈包后，就取消块计时器
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
	        	 information.append("开始计时："+fb.sub_fileID+"\n");
	        	 System.out.println("开始计时："+fb.sub_fileID+"\n");
	        	 SendTask sendtask=null;
	        	 Timer feedtimer=null;
			     sendtask=new SendTask(fb.sub_fileID);
			     feedtimer = new Timer(true);
			     feedtimer.schedule(sendtask,1000,1000); //1s的超时
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
   public class SDCardFileObserver extends FileObserver //是一个线程
    {
       //mask:指定要监听的事件类型，默认为FileObserver.ALL_EVENTS
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
           case FileObserver.MODIFY:  //文件内容被修改时触发，如粘贴文件等
               break;
           case FileObserver.CLOSE_WRITE:  //编辑文件后，关闭
            s = "----"+path + " CLOSE_WRITE";
       		m = new Message();
       		m.obj = s;
       		myHandler.sendMessage(m);
       	    System.out.println("----"+path + " CLOSE_WRITE");
         	if(Isrecived==false)  //接收的的文件不再发送
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
      		  if(Isrecived==false)  //接收的的文件不再发送
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
      		    recvFiles.remove(path);  //从接收文件列表删除
      		    System.out.println("发送文件列表的大小："+sendFiles.size());
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
     			  nextseq.remove(key);     //对应文件的下一个子包序号列表也要删除
     			  break;
     		    }
     		   }
      		  }
        	   break;

           }
       }
    }
  public class listenQueue extends Thread
  {
	  boolean running=true;
	  boolean starting=false;
	  ArrayList<SmallFile>smallfiles=new ArrayList<SmallFile>();
	  int total_length=0;
	public void run()
	{
	 while (running)
	 {
	  while(starting)
	  {	
			try {
				sleep(1000);  //计时1秒之后再执行该线程。
			} catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
	   synchronized(SendFilequeue)
	   {
		  if(SendFilequeue.size()!=0) 
		  {
			String filename=SendFilequeue.peek();
			System.out.println("队列的大小："+SendFilequeue.size());
		    while(filename!=null)
			{
			  String id=Integer.toString(sendFileID);
   			  String file=sharedPath+"//"+filename;
   			  int filelength=0;
   			  FileInputStream fis=null;
   				try {
   					fis = new FileInputStream(file);
   					filelength = fis.available();
   				} catch (Exception e) {
   					e.printStackTrace();
   				}
   			  if(filelength>=maxfilelength)  
   				{ 
   				SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
   			    Date curDate = new Date(System.currentTimeMillis());
   			    String m = formatter.format(curDate);
   			    String mm="发送文件 "+ipaddress+"-"+id+"的时间: "+m; 
   			    messageHandle(mm);
   			    
   				 writeLog("###"+filename+",	");
   		         writeLog(filelength+",	");
   		         writeLog(System.currentTimeMillis()+",	");
		         writeLog(m+",	"+"\r\n");
   					int num=0;
   					if(filelength%maxfilelength==0)
   						num=filelength/maxfilelength;
   					else
   						num=filelength/maxfilelength+1;
   					
   					System.out.println("文件分几次发送："+num);
   					for(int i=0;i<num;i++)
   					{
   			         writeLog(id+"--"+i+",	");
   			         long start=System.currentTimeMillis();
   					  String sub_id =ipaddress+"-"+id+"--"+i;
   					  sending_subfileid=sub_id;  //记录正在发送的文件块号
   					  sendToAll(filename,filelength, sub_id,num);
   					  synchronized(this)
   					   {
   					    try {
						wait(); //线程阻塞
					     } catch (InterruptedException e)
					     {	
					     System.out.println(e);
						  e.printStackTrace();
					     }
   					   }
   				long end=System.currentTimeMillis();
   				writeLog("b-"+(end-start)+"ms,	"+"\r\n");
   					} 
   		        }	
   			else	//多个小文件凑成一个大文件
   			{
   				if(SendFilequeue.size()==1)
   				{
   					if(smallfiles.size()==0)
   					{
   					SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
   				    Date curDate = new Date(System.currentTimeMillis());
   				    String m = formatter.format(curDate);
   				    String mm="发送文件 "+ipaddress+"-"+id+"的时间: "+m; 
   				    messageHandle(mm);
   				    
   				 writeLog("###"+filename+",	");
   		         writeLog(filelength+",	");
   		         writeLog(System.currentTimeMillis()+",	");
		         writeLog(m+",	"+"\r\n");
   						String sub_id =ipaddress+"-"+id+"--"+0;
   						sendToAll(filename,filelength, sub_id,1);
   					    synchronized(this)
 				        {
 				         try {
 					      wait(); //线程阻塞
 				           } catch (InterruptedException e)
 				          {	
 				           System.out.println(e);
 					       e.printStackTrace();
 				           }
 				        }
   				    }
   					else
   					{
   					    String sub_id =ipaddress+"-"+id+"--"+0;
   	   				    SmallFile sf=new SmallFile(filename,filelength,sub_id);
   	   					smallfiles.add(sf);
   	   				    total_length=total_length+filelength;
   	   				    sendSmallFiles(smallfiles ,total_length);
   	   				    smallfiles.clear();
   					}
   				}
   				else
   				{
   				    String sub_id =ipaddress+"-"+id+"--"+0;
   					SmallFile sf=new SmallFile(filename,filelength,sub_id);
   					smallfiles.add(sf);	
   					total_length=total_length+filelength;
   				}
   			}
   			SendFilequeue.remove();
   			sendFileID++;  
   		    filename=SendFilequeue.peek();
	       }  //内层while循环结束
		    
		    if(smallfiles.size()>0)
		    {
		    sendSmallFiles(smallfiles,total_length);
	   		smallfiles.clear();
		    }
		 }		
	    } //end synchronized queue
	 }
	}//外层while循环结束
   }
	public synchronized void notifyThread()
	{
		notify();
	}
	public synchronized void onresume()
	{
		starting=true;
	}
	public synchronized void ondestroy()
	{
		running=false;
	}
  }
    public class stopClickListener implements OnClickListener 
    {
		public void onClick(View v) 
		{
			onDestroy();	
		}
    }
    public void sendSmallFiles (ArrayList<SmallFile> sfiles,int total_length)
    {
    	  int number=0;
    	  if(total_length % maxfilelength == 0)
    		  number = total_length/maxfilelength;
    	  else
				number= total_length/maxfilelength+1;
    	  Packet[][] plist =new Packet[number][maxfilelength/blocklength];
    	  System.out.println("number== "+number);   
    	int n=0;
    	int srtartblock=0;
    	int from=0;
    	int to=0;	
    	String []aa=null;
    	for(int ii=0;ii<sfiles.size();ii++)
    	{
    	aa=sfiles.get(ii).sub_fileid.split("--");
    	   SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
		    Date curDate = new Date(System.currentTimeMillis());
		    String m = formatter.format(curDate);
		    String mm="发送文件 "+aa[0]+"的时间: "+m; 
		    messageHandle(mm);
		    
    		 writeLog("###"+sfiles.get(ii).filename+",	");
		     writeLog(sfiles.get(ii).filelength+",	");
		     writeLog(System.currentTimeMillis()+",	");
	         writeLog(m+",	"+"\r\n");
    	sending_subfileid=sfiles.get(ii).sub_fileid;
     	FileInputStream fis=null;
     	BufferedInputStream in=null;
		int sub_no=Integer.parseInt(aa[1]);
		if(!sendFiles.containsKey(aa[0]))
			sendFiles.put(aa[0],sfiles.get(ii).filename);
     	String file=sharedPath+"//"+sfiles.get(ii).filename;
     	int data_blocks;
	    int coding_blocks=0;
	  
	    int lastlength= blocklength;
     	byte[] data = new byte[maxfilelength];
		try {
			fis = new FileInputStream(file);
			in = new BufferedInputStream(fis);
 			in.skip(maxfilelength*sub_no);
 			in.read(data,0,maxfilelength);
 			in.close();
		    }
			catch (IOException e) 
			{
			e.printStackTrace();
			}	
	 			if(sfiles.get(ii).filelength % blocklength == 0)
	 			{
	 				data_blocks = sfiles.get(ii).filelength/blocklength;
	 			}
	 			else
	 			{
	 				data_blocks = sfiles.get(ii).filelength/blocklength+1;
	 				lastlength= sfiles.get(ii).filelength%blocklength;
	 			}
	        	System.out.println("该文件的data_blocks  "+data_blocks);   
	 			if(srtartblock==0)
	 			{
	 				srtartblock=data_blocks;	
	 				from=0;
	 				to=data_blocks;
	 			}
	 			else
	 			{
	 				from =srtartblock;
	 				to=from+data_blocks;
	 				srtartblock=to;
	 				if(to>=100)
	 				{
	 					srtartblock=to-100;
	 					to=100;
	 				}
	 			 }
	 				for(int k=from;k<to;k++)
		 			{
		 				byte[] filedata = new byte[blocklength];
		 				System.arraycopy(data, (k-from)*blocklength, filedata, 0, blocklength);		
		 				int send_blockLength=0;
		 				if(k==data_blocks-1)	
		 					send_blockLength=lastlength;	   
		 				else
		 					send_blockLength=blocklength;
		 			    Packet p = new  Packet(0,sfiles.get(ii).filelength,coding_blocks, data_blocks, (k-from), send_blockLength,sfiles.get(ii).filelength);		
		 				p.data = filedata ;
		 				p.filename=sfiles.get(ii).filename;
		 				p.totalsubFiles=1;
		 				plist[n][k] = p;
		 		        p.sub_fileID=sfiles.get(ii).sub_fileid;	 
		 			}
		           synchronized(nextseq)
		           {
		 			nextseq.put(sfiles.get(ii).sub_fileid, data_blocks); //下一次从plist[0]开始发，其中只放的是冗余包。
		           }	
	 			  if(to==100)
	 			  {
	 				   sendThread st=new sendThread(plist[n],bcastaddress,port,0,100,1);
	 				   st.start();
	 				   n++;
	 				System.out.println("已经够100块开始发送");
	 				for(int j=0;j<srtartblock;j++)
	 				{	
	 					byte[] filedata = new byte[blocklength];
		 				System.arraycopy(data, (j+100-from)*blocklength, filedata, 0, blocklength);		
		 				int send_blockLength=0;
		 				if((100-from+j)==data_blocks-1)	
		 					send_blockLength=lastlength;	   
		 				else
		 					send_blockLength=blocklength;
		 			    Packet p = new  Packet(0,sfiles.get(ii).filelength,coding_blocks, data_blocks, (100-from+j), send_blockLength,sfiles.get(ii).filelength);		
		 				p.data = filedata ;
		 				p.filename=sfiles.get(ii).filename;
		 				p.totalsubFiles=1;
		 				plist[n][j] = p;
		 		        p.sub_fileID=sfiles.get(ii).sub_fileid;	
		 			
	 				}
	 			}
    	}	//for循环结束 	
    	if(plist[0]!=null)
    	{
    	    sendThread st=new sendThread(plist[n],bcastaddress,port,0,srtartblock,1);
		    st.start();
    	}
    }
    public void sendToAll (String filename,int filelength,String sub_fileid,int num)
    {
     	Packet[] sendPacket=null; 
     	FileInputStream fis=null;
     	BufferedInputStream in=null;
     	String []aa=null;
		aa=sub_fileid.split("--");
		int sub_no=Integer.parseInt(aa[1]);
     	String file=sharedPath+"//"+filename;
     	byte[] data = new byte[maxfilelength];
		int length=0;
		try {
			fis = new FileInputStream(file);
			in = new BufferedInputStream(fis);
 			in.skip(maxfilelength*sub_no);
 			length=in.read(data,0,maxfilelength);
		    }
			catch (IOException e) 
			{
			e.printStackTrace();
			}	
 			sendPacket=file_blocks(data,length,sub_fileid,filename,num,filelength);
 			if(sendPacket!=null)
 		     {
 		        sThread=new sendThread(sendPacket,bcastaddress,port,0,sendPacket[0].data_blocks,0);
 		        sThread.start();    
 		        message = "发送的包的个数: "+sendPacket[0].data_blocks;
 				messageHandle(message);
 		     }	
 			if(!sendFiles.containsKey(aa[0]))
 				sendFiles.put(aa[0],sendPacket[0].filename);
 			synchronized(subfiles)
 			{
 			    subfiletask=new sub_fileTask (filename,sub_fileid,num);
 			    subfiletimer=new Timer(true);
 			    subfiletimer.schedule(subfiletask, subtime, subtime);
 			    subfiles.put(sub_fileid,subfiletimer);
 			}
		
    }
    public  Packet[] file_blocks(byte[] filedata,int subFileLength,String sub_fileID,String filename,int totalsubFiles,int FileLength)
    {
       int data_blocks;
       int coding_blocks=0;
       Packet[] plist =null;
       int lastlength= blocklength;
     
 			if(subFileLength % blocklength == 0)
 			{
 				data_blocks = subFileLength/blocklength;
 			}
 			else
 			{
 				data_blocks = subFileLength/blocklength+1;
 				lastlength= subFileLength%blocklength;
 			}
 			plist = new Packet[data_blocks];
 			for(int i=0;i<data_blocks;i++)
 			{
 				byte[] data = new byte[blocklength];
 				System.arraycopy(filedata, i*blocklength, data, 0, blocklength);		
 				int send_blockLength=0;
 				if(i==data_blocks-1)	
 					send_blockLength=lastlength;	   
 				else
 					send_blockLength=blocklength;
 			    Packet p = new  Packet(0,subFileLength,coding_blocks, data_blocks, i, send_blockLength,FileLength);		
 				p.data = data;
 				p.filename=filename;
 				p.totalsubFiles=totalsubFiles;
 				plist[i] = p;
 		        p.sub_fileID=sub_fileID;			
 			}
           synchronized(nextseq)
           {
 			nextseq.put(plist[0].sub_fileID, data_blocks); //下一次从plist[0]开始发，其中只放的是冗余包。
           }	
         return plist;
    }
    
    public synchronized Packet[] encode(byte[] filedata,int subFileLength,String sub_fileID,String filename,int totalsubFiles,int FileLength)
    {   
    	long start=System.currentTimeMillis();
         int data_blocks;
         int coding_blocks;
         Packet[] plist =null;
         int lastlength= blocklength;
          if(subFileLength % blocklength == 0)
			{
				data_blocks = subFileLength/blocklength;
			}
			else
			{
				data_blocks = subFileLength/blocklength+1;
				lastlength= subFileLength%blocklength;
			}
 	
 			coding_blocks = data_blocks;
 			plist = new Packet[coding_blocks+data_blocks];
 			byte[][] origindata = new byte[data_blocks][blocklength];
 			byte[][] encodedata = new byte[coding_blocks][blocklength];
 			
 			for(int i=0;i<data_blocks;i++)
 			{
 				System.arraycopy(filedata, i*blocklength, origindata[i], 0, blocklength);
 				byte[] data = new byte[blocklength];
 				System.arraycopy(origindata[i], 0, data, 0, blocklength);
 				int send_blockLength=0;
 				if(i==data_blocks-1)	
 					send_blockLength=lastlength;	   
 				else
 					send_blockLength=blocklength;
 				Packet p = new Packet(0,subFileLength,coding_blocks, data_blocks, i,send_blockLength,FileLength);
 				p.data = data;
 				p.filename=filename;
 				p.totalsubFiles=totalsubFiles;
 				plist[i] = p;
 				p.sub_fileID=sub_fileID;
 			}
 			
 	        int w=16; 
 	        int[] matrix = ReedSolomon.reed_sol_vandermonde_coding_matrix(data_blocks, coding_blocks, w);
 	        Jerasure.jerasure_matrix_encode(data_blocks, coding_blocks, w, matrix, origindata , encodedata, blocklength);
 	        for(int i=data_blocks;i<data_blocks+coding_blocks;i++)
 			 {
 				byte[] data = new byte[blocklength];
 				System.arraycopy(encodedata[i-data_blocks], 0, data, 0, blocklength);
 				Packet p = new Packet(0,subFileLength,coding_blocks, data_blocks, i, blocklength,FileLength);
 				p.data = data;
 				p.filename=filename;
 				p.totalsubFiles=totalsubFiles;
 				plist[i] = p;
 				p.sub_fileID=sub_fileID;
 			  }	       	
 	      message = plist[0].sub_fileID+" 编码生成冗余包的个数："+plist.length;
 	      messageHandle(message);
 	      long end =System.currentTimeMillis();
 	      total_encode_timer=total_encode_timer+end-start;
			return plist;
	}


	public synchronized void decode(Packet[] plist)
    {
		
		System.out.println("进入解码函数  收到包的个数："+plist.length+" 开始解码");
    	Packet p = plist[plist.length-1];
    	int data_blocks = p.data_blocks;
    	int coding_blocks = 0;
    	int block_length=blocklength;
    	int file_length = p.subFileLength;  //该块文件的长度
    	String recvFilename=p.filename;
    	byte[] writein = new byte[file_length];
    	
    	byte[][] recvdata = new byte[data_blocks][block_length];
    	ArrayList<Integer> reverasure = new ArrayList<Integer>();
    	byte[][] recvcode =null;
    	int len = plist.length;
    	boolean isfirst=true;
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
    			if(isfirst)
    			{
    			 coding_blocks = s.coding_blocks;
    			 recvcode = new byte[coding_blocks][block_length];
    			 isfirst=false;
    			}
    			recvcode[s.seqno-data_blocks] = s.data;
    		}	
    	} 	
    	if(coding_blocks ==0)  //若是coding_blocks =0，不必解码，直接恢复。
    	{	

    		int set=0;
    	    for(int pp=0;pp<plist.length;pp++)
    	    {
    	     System.arraycopy(plist[pp].data, 0, writein, set, plist[pp].data_length);
    	     set=set+plist[pp].data_length;
    	    }
    	}
      else
      {
    	int w=16;
    	int j=0;
    	int[] erasure = new int[data_blocks+coding_blocks+1];
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
      }
    	if(plist[0].totalsubFiles==1)
    	{
    	  message="生成文件";
    	  messageHandle(message);
    	  recvFiles.add(recvFilename);  //放入接收文件列表
    	  try {
    		String encodeFile=sharedPath+"//"+recvFilename;
    		BufferedOutputStream bos;
    		bos = new BufferedOutputStream(new FileOutputStream(encodeFile));
			bos.write(writein);
	        bos.close();
		    } catch (Exception e) 
		   {		
			e.printStackTrace();
		   }
    	 String []aa=plist[0].sub_fileID.split("--");
    	 SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
		 Date curDate = new Date(System.currentTimeMillis());
		 String m = formatter.format(curDate);
		 String mm="发送文件 "+aa[0]+"的时间: "+m; 
		    messageHandle(mm);
		 writeLog(""+"\r\n");
    	 writeLog("%%%"+plist[0].filename+",");
	     writeLog(plist[0].fileLength+",	");
	     writeLog(System.currentTimeMillis()+",	");
         writeLog(m+",	"+"\r\n");
    	}
    	else
    	{
    	SubFileTask subfileTask=null;
    	Timer subfiletimer=null;
    	String []aa=plist[0].sub_fileID.split("--"); //aa[0]中放的是fileid
    	int sub_no=Integer.parseInt(aa[1]);
    	
    	if(subFile_nums.containsKey(aa[0]))
    	{

			RecvSubfileData rsfd=new RecvSubfileData(aa[0],plist[0].filename,writein,sub_no);
	    	RecvSubFiles.add(rsfd);
    		int t=subFile_nums.get(aa[0]);
    		t=t+1;
			subFile_nums.remove(aa[0]);
			subFile_nums.put(aa[0], t);
			System.out.println("收到的块号："+sub_no);
            System.out.println("该文件总共块数："+plist[0].totalsubFiles+" 当前收到了块："+subFile_nums.get(aa[0]));
            subfileTimers.get(aa[0]).cancel(); //只要有新的数据块接收到，就取消块丢失计时器,再加新的计时器
            subfileTimers.remove(aa[0]);
            recv_subfiels_no.get(aa[0]).add(sub_no);
            subfileTask=new SubFileTask(aa[0],recv_subfiels_no.get(aa[0]));
    		subfiletimer = new Timer(true);
    		subfileTimers.put(aa[0], subfiletimer);
    		subfiletimer.schedule(subfileTask,block_time,block_time);
            
           
            if(subFile_nums.get(aa[0])==plist[0].totalsubFiles)
    		{
               recv_subfiels_no.remove(aa[0]);
               subfileTimers.get(aa[0]).cancel();
               subfileTimers.remove(aa[0]);
               
                 message="开始生成文件";
   	    	     messageHandle(message);
   	    	     recvFiles.add(recvFilename);  //放入接收文件列表中
                 String encodeFile=sharedPath+"//"+recvFilename;
                 RandomAccessFile raf=null;	
                   try {
					raf = new RandomAccessFile(encodeFile,"rw");
					raf.setLength(plist[0].fileLength);
				} catch (Exception e) {
					e.printStackTrace();
				}
    						
                 for(int nn=0;nn<RecvSubFiles.size();nn++)
    			 {        	
    				if(RecvSubFiles.get(nn).fileID.equals(aa[0]))
    				{
    				 try {  
    			    	    raf.seek(0);   // 1：绝对定位              
    			    	    raf.skipBytes(maxfilelength*RecvSubFiles.get(nn).sub_num);//如果为负值，不跳过任何字节
    			    	    raf.write(RecvSubFiles.get(nn).data);	
    						} catch (Exception e1) 
    						{
    							e1.printStackTrace();
    						}
    				 RecvSubFiles.remove(nn);
    				 nn--;
    				}
    			 }	
                 try {
 					raf.close();
 				} catch (IOException e) 
 				{
 					e.printStackTrace();
 				}  
    		 subFile_nums.remove(aa[0]);
    		SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
    	    Date curDate = new Date(System.currentTimeMillis());
    	    String m = formatter.format(curDate);
    	    String mm="接收文件"+aa[0]+"的时间 : "+m;
    		messageHandle(mm);
    		 writeLog(""+"\r\n");
    		 writeLog("%%%"+plist[0].filename+",");
		     writeLog(plist[0].fileLength+",	");
		     writeLog(System.currentTimeMillis()+",	");
	         writeLog(m+",	"+"\r\n");
    		} 
    		
    	}  
    	else
    	{
    		if(plist[0].fileLength>1024*10240)  //文件的长度大于10m,直接写出
    		{
    			if(sub_nums.containsKey(aa[0]))
    			{
    				int bb=sub_nums.get(aa[0]);
    				sub_nums.remove(aa[0]);
    				sub_nums.put(aa[0], bb+1);
    				subfileTimers.get(aa[0]).cancel();
    	            subfileTimers.remove(aa[0]);
    	            recv_subfiels_no.get(aa[0]).add(sub_no);      
    			}
    			else
    			{
    			  sub_nums.put(aa[0], 1);
    			  ArrayList<Integer>recvsubfiles=new ArrayList<Integer>();
     		      recvsubfiles.add(sub_no);
     		      recv_subfiels_no.put(aa[0], recvsubfiles);
    			}
    			if(!recvFiles.contains(recvFilename))
    			    recvFiles.add(recvFilename);  //放入接收文件列表中
    		      String encodeFile=sharedPath+"//"+recvFilename;
                  RandomAccessFile raf=null;
 				  try {
 					raf = new RandomAccessFile(encodeFile,"rw");
 					raf.setLength(plist[0].fileLength);
 					raf.seek(0);   // 1：绝对定位              
			    	raf.skipBytes(maxfilelength*sub_no);//如果为负值，不跳过任何字节
			        raf.write(writein);
 				  } catch (Exception e) 
 				 {
 					e.printStackTrace();
 				 }  
 				 if(sub_nums.get(aa[0])>=plist[0].totalsubFiles)
   			     {
 					 System.out.println("接收完毕######## "+aa[0]);
   				     recv_subfiels_no.remove(aa[0]) ;
   				     sub_nums.remove(aa[0]);
  	                 try {
						raf.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
  	             SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
  	 		    Date curDate = new Date(System.currentTimeMillis());
  	 		    String m = formatter.format(curDate);
  	 		    String mm="接收文件 "+aa[0]+"的时间: "+m;
  	 		    messageHandle(mm);
  	 		     writeLog(""+"\r\n");
  	             writeLog("%%%"+plist[0].filename+",");
 		         writeLog(plist[0].fileLength+",	");
 		         writeLog(System.currentTimeMillis()+",	");
		         writeLog(m+",	"+"\r\n");
   			     }
   			    else
   			    {
   			       subfileTask=new SubFileTask(aa[0],recv_subfiels_no.get(aa[0]));
 	    		   subfiletimer = new Timer(true);
 	    		   subfileTimers.put(aa[0], subfiletimer);
 	    		   subfiletimer.schedule(subfileTask,block_time,block_time);
 	    	       System.out.println("开始计时########： "+subfiletimer);
   			    }
 				 
    		}
    		else
    		{
    			 RecvSubfileData rsfd=new RecvSubfileData(aa[0],plist[0].filename,writein,sub_no);
    	    	 RecvSubFiles.add(rsfd);
    		     subFile_nums.put(aa[0], 1);    
    		     ArrayList<Integer>recvsubfiles=new ArrayList<Integer>();
    		     recvsubfiles.add(sub_no);
    		     recv_subfiels_no.put(aa[0], recvsubfiles);
    		     subfileTask=new SubFileTask(aa[0],recvsubfiles);
    		     subfiletimer = new Timer(true);
    		     subfileTimers.put(aa[0], subfiletimer);
    		     subfiletimer.schedule(subfileTask,block_time,block_time);
    		     System.out.println("开始计时： "+subfiletimer);
    		}
    
    	  }
        
    	}
    }
    
    
    public String getIp()
	{  
    	//Adhoc模式下获取IP
//  /*  
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

public class SendTask extends java.util.TimerTask
{
	public String sub_fileID;
	public String fileName;
	SendTask (String fileid)
	{
		this.sub_fileID=fileid;
	}
	 public void run() 
  	 { 
		 int i=0;
		 int number=0;
		 ArrayList<Integer> nos=null;
		 String []aa=null;
		 String fileid=null;
		 int type=0;
		 int sub_no=0;
		 if(feedTimers.containsKey(sub_fileID))
  	     {
         System.out.println("准备发送数据，取消计时器 "+sub_fileID);
  	    	feedTimers.get(sub_fileID).cancel();  
  	    	feedTimers.remove(sub_fileID);
  	     } 
		 String []bb=sub_fileID.split("-");
		 writeLog("*Re-send:"+bb[1]+"--"+bb[bb.length-1]+",	");
	     long start=System.currentTimeMillis();
  
		 int ssize=0;
	  synchronized(Feedpkts)
       {
         for(;i<Feedpkts.size();i++)
         {
      	   if(Feedpkts.get(i).sub_fileID.equals(sub_fileID))
              {
      		     type=Feedpkts.get(i).type;
      		     if(Feedpkts.get(i).type==1)
      		      {
      			   number=Feedpkts.get(i).nos.get(0);
      			   aa=sub_fileID.split("--");  //aa[0]中放的是fileid
                   sub_no=Integer.parseInt(aa[1]); 
                   fileid=aa[0];
      		      }
      		      else
      		      {
      		    	nos=new ArrayList<Integer>();
      		   		nos=Feedpkts.get(i).nos;
      		     	fileid=sub_fileID;
      		      }
      		     ssize=Feedpkts.size();
      	     break;
             }
          } 
         }  
         if(i!=ssize)
         {
     	   if(sendFiles.containsKey(fileid))
		    {
     		 FileInputStream fis=null;
     	     BufferedInputStream in=null;
     	     int filelength=0;
     	     int num=0;
		     fileName=sendFiles.get(fileid);
		     String file=sharedPath+"//"+fileName;
		     try {
					fis = new FileInputStream(file);
					filelength = fis.available();
					in = new BufferedInputStream(fis);
				} catch (Exception e) 
				{
					e.printStackTrace();
				}
		     if(filelength<=maxfilelength)
		    	   num=1;
		     else
		     {
		        if(filelength%maxfilelength==0)
					num=filelength/maxfilelength;
				else
					num=filelength/maxfilelength+1;
		     }
		     int length=0;
	 		 boolean send=true;
	 				if(type==2)
	 				{
	 					for(int mm=0;mm<num;mm++)
	 					{
	 					  byte[] data = new byte[maxfilelength];
	 					  send=true;
	 					  try {
	 				 			 length=in.read(data,0,maxfilelength);
		 						} catch (IOException e) 
		 						{
		 							e.printStackTrace();
		 						}
	 					   for(int nn=0;nn<nos.size();nn++)	
	 					   {
	 						  if(mm==nos.get(nn))
	 						   {
	 						 	send=false;
	 							nos.remove(nn);
	 							nn--;
	 							break;
	 						  }
	 					   }
	 					if(send)
	 					 {
	 						
	 						System.out.println("发送wenjian的快号： "+mm);	
		 		 			System.out.println("##########发送数据长度：  "+length);	
	
		 				String sub_id =sub_fileID+"--"+mm;
		 				Packet[]sendPacket=null;
		 	 			  sendPacket=file_blocks(data,length,sub_id,fileName,num,filelength);
		 	 			   if(sendPacket!=null)
		 		           {
		 		            sThread=new sendThread(sendPacket,bcastaddress,port,0,sendPacket[0].data_blocks,0);
		 		            sThread.start();    
		 		            message = "发送的包的个数: "+sendPacket[0].data_blocks;
		 				    messageHandle(message);			
		 		           }
	 				  }	
	 				}			
	 			}
	 		else if(type==1)
	 		{
	 		byte[] data = new byte[maxfilelength];
	 	    System.out.println("超时，文件丢失的包数目："+number);
	 	     writeLog("miss:"+number+"个,	");
	 		try {
	 			 in.skip(maxfilelength*sub_no);
	 			 length=in.read(data,0,maxfilelength);
				} catch (IOException e) 
				{
				e.printStackTrace();
				}
	 		 message=sub_fileID+" 超时，再发 "+number+" 个包";
             messageHandle(message);
		     Packet[] sPacket=null; 
		     if(SavedencodedPkts!=null&&SavedencodedPkts[0].sub_fileID.equals(sub_fileID))
		     {	 
		    	 sPacket=SavedencodedPkts;
		     }
		     else
		     {
		        sPacket=encode(data,length,sub_fileID,fileName,num,filelength);
		        SavedencodedPkts=sPacket;
		     }
		     synchronized(this)
		     {
		     int Start=nextseq.get(sub_fileID);
		     nextseq.remove(sub_fileID); 
		     System.out.println("本次开始发送的子包序号 "+Start);
		     if(sPacket!=null)
		     {
		    	 int nextStart=Start+number;
				 int total=sPacket[0].data_blocks+sPacket[0].coding_blocks;
				 if(nextStart>=total)
				   {
				       nextStart=nextStart-total;
				       nextseq.put(sub_fileID,nextStart);
				   }
				    else
				   {
				       nextseq.put(sub_fileID,nextStart);
				   }
				System.out.println("下一次开始发送的子包序号： "+nextStart);
		        sThread=new sendThread(sPacket,bcastaddress,port,Start,number,0);
				sThread.start();         
	 			//发送完冗余包后进行计时
	 			if(sending_subfileid.equals(sub_fileID))
	 			{
	 			synchronized(subfiles)
	 			{
	 			subfiletask=new sub_fileTask (fileName,sub_fileID,num);
	 			subfiletimer=new Timer(true);
	 			subfiletimer.schedule(subfiletask, subtime, subtime);
	 			subfiles.put(sub_fileID,subfiletimer);
	 			}
	 		   }
		     }
		    }	
	 	 } 
	 	  synchronized(Feedpkts)
	 	   {
	 		for(int kk=0;kk<Feedpkts.size();kk++)
	 		{
	 		 if(Feedpkts.get(kk).sub_fileID.equals(sub_fileID))
	 		      {
	 			   Feedpkts.remove(kk);
	 			   break;
	 			  }
	 		 }
	 	  }
		}  	
       } 
         long end=System.currentTimeMillis();
         writeLog("r-"+(end-start)+"ms,	"+"\r\n");
  	  } 
 }

public class  SubFileTask extends java.util.TimerTask
{
	public String fileID;  //文件的ID号
	public ArrayList<Integer>sub_nos;  //收到的文件的块号
	SubFileTask (String fileid,ArrayList<Integer>sub_nos)
	{
	  this.fileID=fileid;
	  this.sub_nos=new ArrayList<Integer>();
	  this.sub_nos=sub_nos;
	
	}
	 public void run() 
  	 { 
	 System.out.println("超时，发送块丢失反馈包");
	 sendFeedBackPackFunction sfb=new sendFeedBackPackFunction(fileID,sub_nos,2);
     sfb.sendFeedBack();
  	 }
}

  public class sub_fileTask extends java.util.TimerTask
  {	
	public String filename;
	public String sub_fileid;
	public int num;
	sub_fileTask(String filename,String sub_fileid,int num)
	{
	this.filename=filename;
	this.sub_fileid=sub_fileid;
	this.num=num;
	}
	 public void run() 
  	 { 
		 synchronized(subfiles)
		 {
		   if(subfiles.containsKey(sub_fileid))
		   {
			 System.out.println("超时没收到反馈包，唤醒线程");
			 String message = "超时没收到反馈包，唤醒线程";
			 messageHandle(message);
			 subfiles.get(sub_fileid).cancel();
			 subfiles.remove(sub_fileid);
			 listenqueue.notifyThread();
			 
		   }
		 }
	
  	 }
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
   synchronized(subfiles)
   {
   if(subfiles.size()>0)
	 {
	  Iterator it =  subfiles.keySet().iterator();
	  while (it.hasNext())
	   {
	   String key=null;
	   key=(String)it.next();
	   subfiles.get(key).cancel();  //防止还有计时器在运行
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
