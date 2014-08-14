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
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
	public static int control_port=40001; //���Ϳ�����Ϣ�˿ںţ����ļ�ɾ��
	public static int sync_port=40004;    //�¼���ڵ�Ҫ��ͬ���Ķ˿ں�
	public static final int blocklength=1024;      //ÿ���Ĵ�С1k
	public static final int maxfilelength=102400;    //�ļ����С����Ϊ100k
	public static int sendFileID=0;  //�ļ���id��
	public static String sharedPath="/mnt/sdcard/Seafile";
	
	public static Handler myHandler=null;
    public static Handler recvHandler= null;
	public static sendThread sThread=new sendThread();
	public recvThread rThread=null;	
    public respondSync resp=null;
	public test tt=new test();  //���ض�̬��
	public Button stop=null;
	public Button sync=null;
	public TextView information = null;
	public ScrollView mScrollView =null;
	public FileObserver mFileObserver=null;
	public static BufferedWriter bw=null;
	public static long total_encode_timer=0;  //�����ܹ����ѵ�ʱ��
	public static long total_sending_timer=0; //�����ܹ����ѵ�ʱ��
	public static long total_sending_length=0; 
	public static int stored_blocks=20;  //�洢�����Ŀ�
	public String message=null;
    public OtherTask othertask=null;
    public Timer othertimer=null;
    public int otherPcks_time=2000; //���˷������Ĺ���ʱ�� ms
    public int selfFeedPcks_time=1000; //�յ����Լ��������Ĺ���ʱ�� ms
	public static int sleeptime=3000;  //��ʱ3��֮���ڿ�ʼ��ض��С�
	
	public static ArrayList<String>exitsFiles=new ArrayList<String>();        //��ǰĿ¼�Ѿ����ڵ��ļ��б�
    public static HashMap<String,String>sendFiles=new HashMap<String,String>(); //�����ļ��б�
    public static ArrayList<String>recvFiles=new ArrayList<String>();        //�����ļ��б�
    public static Map<String,Integer>nextseq=new HashMap<String,Integer>(); //next packetID
    public static ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //���������ļ��ţ�������
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//�����˵ķ�������Ϣ
	public static Map<String,Timer> feedTimers=new HashMap<String,Timer>();//�յ�������ʱ��ʼ��ʱ
   
	public static Queue<String> SendFilequeue = new LinkedList<String>();   
	public static listenQueue listenqueue=null;     
	public static ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();//�����յ����ļ���
	public static Map<String,Integer>subFile_nums=new HashMap<String,Integer>(); //�յ���Ӧ�ļ���<10m���Ŀ���
	public static Map<String,Integer>sub_nums=new HashMap<String,Integer>();//����10m���ļ����յ��Ŀ���
	public static Map<String,ArrayList<Integer>> recv_subfiels_no=new HashMap<String,ArrayList<Integer>>(); //�洢�յ��Ķ�Ӧ�ļ��Ŀ��
	public static Map<String,Timer> subfileTimers=new HashMap<String,Timer>();  //ÿ���յ�һ���ļ���ͻ��ʱ
	public static final int block_time=4000;  //�ļ����Ϳ鷴������ʱ��
	public static long currentLength=0;  //��ǰ�ļ��б����ļ��ĳ���
	public static long  maxQueueLength=1024*10240; //���е��������
	public static int file_number=0;
	public static int maxNumber=20;  //�������������ļ���
	public static int maxStoredLength=1024*10240; //����10m���ļ�����յ��ļ���ֱ�����
	public static fecFuntion fecfunction=new fecFuntion(); //������ֵʵ��һ��fecFuntionʵ����������synchronized���ͣ������ڴ����
	public static ArrayList<Packet[]> encodedPacket=new  ArrayList<Packet[]>(); //������ɵĿ�
	SDCardFileObserver  fileobver=null;
	sendContInfo sendInfo=new sendContInfo();
	recv_control_info recvInfo=new recv_control_info();
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.main);
    	information=(TextView) findViewById(R.id.information);
        information.setMovementMethod(ScrollingMovementMethod.getInstance());
        information.setScrollbarFadingEnabled(false); 
        sync=(Button)findViewById(R.id.syncbutton);
        sync.setOnClickListener(new syncClickListener());
        stop=(Button)findViewById(R.id.stopbutton);
        stop.setOnClickListener(new stopClickListener());
        mScrollView = (ScrollView)findViewById(R.id.sv_show);
        ipaddress=getIp();
        total_encode_timer=0;
        bcastaddress="192.168.1.255";
        /* WIFI:255.255.255.255
         * Adhoc:192.168.1.255
         */
        total_encode_timer=0;  //�����ܹ����ѵ�ʱ��
    	total_sending_timer=0; //�����ܹ����ѵ�ʱ��
    	total_sending_length=0; 
        information.append("�ҵ�IP��ַ�ǣ�"+ipaddress+"\n");
        information.append("�ҵĹ㲥��ַ�ǣ�"+bcastaddress+"\n");
        rThread=new recvThread(ipaddress,port);
        rThread.start();
        listenqueue=new listenQueue();
        listenqueue.start();
        sendInfo= new sendContInfo();
        recvInfo.start();
       resp=new respondSync();
       resp.start();
        
        
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        information.append("�ֻ������߳��ڴ棺"+activityManager.getMemoryClass()+"M\n");	
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
        		       fecfunction.decode(recvPacket);	  //���ý��뺯��
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
        		 else if(Msg.arg1==2)
          		{
        			 String filename=(String)Msg.obj;
        			 File f=new File(filename);
        			 String mess ="ɾ���ļ�";
        			  messageHandle(mess);
        			 if(f.isDirectory())
        			 {
        				 String me ="ɾ��Ŀ¼";
           			    messageHandle(me);
        				 File [] files=f.listFiles();
        				 for (File file: files)
        		          {
        					 if(file!=null)
        					 {
        					    file.delete();
        					    deletefile(file.getAbsolutePath());
        					 }
        		          }
        				 f.delete();
        				 deletefile(filename);
        			 }
        			else if(f.exists())
        			{
        				deletefile(filename);
        				f.delete();	
        			} 
          		}
        		 else if(Msg.arg1==3)  //�յ�����Ҫ��ͬ������Ϣ
           		{
        			 boolean hasInternet=true;
        			 boolean canConnect= false;
        		    ConnectivityManager mConnectivity = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE); 
       			    TelephonyManager mTelephony = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE); 
       			  
       			    //����������ӣ������������ã��Ͳ���Ҫ�������������� 
       			    NetworkInfo info = mConnectivity.getActiveNetworkInfo(); 
       			    if (info == null || !mConnectivity.getBackgroundDataSetting())
       			    	hasInternet=false; 
       				 if(hasInternet)
       				 {
       				   //�ж������������ͣ�ֻ����3G��wifi�����һЩ���ݸ��¡� 
       				   int netType = info.getType(); 
       				   int netSubtype = info.getSubtype(); 

       				   if (netType == ConnectivityManager.TYPE_WIFI)  //wifi��������
       				    { 
       			         canConnect= info.isConnected(); 
       				    } 
       				    else if (netType == ConnectivityManager.TYPE_MOBILE && netSubtype == TelephonyManager.NETWORK_TYPE_UMTS 
       				    && !mTelephony.isNetworkRoaming())        // 3G�����������
       				   { 
       				    canConnect= info.isConnected(); 
       				   } 
       				 }
       				 if(hasInternet&&canConnect)   //�Լ��ǿ������ӷ������Ľڵ�
       				 { //�Լ����ļ�Ŀ¼�в�Ϊ��
       				   System.out.println("�Լ��ǿ������ӷ������Ľڵ� " );
       				   String mess ="�յ�Ҫ��ͬ������Ϣ";
       			       messageHandle(mess); 
       					ArrayList<String> preSendFiles=new ArrayList<String>();
       					String currStoredFiles="";
       					File f=new File(FileSharing.sharedPath);
       					Stack <String>  stack= new Stack <String>();
       					if(f.exists()&&f.isDirectory())
       					  stack.push(f.getAbsolutePath());
       					while(!stack.empty())
       					{
       						String filename=stack.pop();
       						File file=new File(filename);
       						File ff[]=file.listFiles();
       						if(ff!=null)
       						for(int i=0;i<ff.length;i++)
       							if(ff[i].isDirectory()&& !ff[i].getName().equals(".") && !ff[i].getName() .equals(".."))
       								stack.push(ff[i].getAbsolutePath());
       							else 
       								currStoredFiles+=ff[i].getAbsolutePath()+"|";
       					}
       					currStoredFiles=currStoredFiles.substring(0, currStoredFiles.length()-1);
       					String [] Storednames=currStoredFiles.split("\\|");
       					String filenames=(String)Msg.obj;
       					if(filenames.startsWith("**��"))
       					{
       						//ֱ�ӷ����Լ�Ŀ¼�е��ļ�
       						for(int i=0;i< Storednames.length;i++)
       						{
       						    Storednames[i].trim();
       						    if ( Storednames[i]!=null&&Storednames[i]!="")
            				       addToQueue( Storednames[i],true);
       						}
       					}
       					else
       					{
       					String []names=filenames.split("\\|");
       					preSendFiles=compareStrings(Storednames,names);//������Ŀ¼�д��ڵģ��Է�Ŀ¼�в����ڵķ����Է���
       					System.out.println("����׼��Ҫ���͵��ļ���С�� "+preSendFiles.size());
       					String MissFiles=compareStringsReturnString(names,Storednames);//���Է�Ŀ¼�д��ڵģ�����Ŀ¼�в����ڵ��ļ��������Է���
       					
       					System.out.println("����ȱ�ٵ��ļ���"+MissFiles);
       					if(MissFiles!="")
       					{
       					requestSyncFunction rsf=new requestSyncFunction("***"+MissFiles);
       					rsf.start();
       					}
       					if(preSendFiles.size()>0)
       					{
       					for(int i=0;i<preSendFiles.size();i++)
       					   addToQueue(preSendFiles.get(i),true);
       					preSendFiles.clear();
       					}
       					if(MissFiles==null&&preSendFiles.size()==0)
       					{
       						//�Ѿ�ͬ��
       						String mess1 ="��Ϣ�Ѿ�����";
            			       messageHandle(mess1);
       					}
       					}
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
       
        //��ʱ���±��˵ķ������б�
        othertimer = new Timer(true);
	    othertask=new OtherTask();
		othertimer.schedule(othertask,otherPcks_time,otherPcks_time);
	     
   }
	public ArrayList<String> compareStrings(String[] s1,String[] s2)
	{
		ArrayList<String> Files=new ArrayList<String>();
		boolean isEqual=false;
			for(int j=0;j<s1.length;j++)
			{ 
			    for(int i=0;i<s2.length;i++)
			    {
					isEqual=false;
					s1[j].trim();
					s2[i].trim();
					if(s1[j]!="" &&s1[j].equals(s2[i]))
					{
						isEqual=true;
						break;		
					}
				}
				if(isEqual==false)	
					Files.add(s1[j]);  
			}
		return Files;
	}
	public String compareStringsReturnString(String[] s1,String[] s2)
	{
		String Files="";
		boolean isEqual=false;
			for(int j=0;j<s1.length;j++)
			{ 
			    for(int i=0;i<s2.length;i++)
			    {
					isEqual=false;
					s1[j].trim();
					s2[i].trim();
					if(s1[j]!="" &&s1[j].equals(s2[i]))
					{
						isEqual=true;
						break;		
					}
				}
				if(isEqual==false)	
					Files+=s1[j]+"|";
			}
		return Files;
	}
	  public void deletefile(String path)
	  {
		    synchronized(recvFiles)
  		    {
  		    	if(recvFiles.contains(path))
  		           recvFiles.remove(path);  //�ӽ����ļ��б�ɾ��
  		    }
		    synchronized(exitsFiles)
 		    {
   		     if(exitsFiles.contains(path))
   			   exitsFiles.remove(path); 
 		    } 
  		   synchronized(sendFiles)
  		   {
  		     if(sendFiles.size()>0)
  		      {
  		      Iterator it = sendFiles.keySet().iterator();
 		       while (it.hasNext())
 		       {
 		       String key=null;
 		       key=(String)it.next();  //��һ�ε���Iterator��next()����ʱ�����������еĵ�һ��Ԫ��
 	            if(sendFiles.get(key).equals(path)) 
 	             {
 			        sendFiles.remove(key); 
 			        synchronized(nextseq)
   		            {
 			           nextseq.remove(key);     //��Ӧ�ļ�����һ���Ӱ�����б�ҲҪɾ��
   		             }
 			        break;
 		           }
 		         }
  		       }
  		   } 
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
	        	 information.append("��ʼ��ʱ��"+fb.sub_fileID+"\n");
	        	 System.out.println("��ʼ��ʱ��"+fb.sub_fileID+"\n");
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
	   System.out.println("�յ������� fb.type="+fb.type);
   }
   
   public class SDCardFileObserver extends FileObserver //��һ���߳�
    {
       //mask:ָ��Ҫ�������¼����ͣ�Ĭ��ΪFileObserver.ALL_EVENTS
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
     System.out.println("��ǰ������Ŀ¼��"+directory);
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
            stack.push(f.getAbsolutePath());
            }
            exitsFiles.add(f.getAbsolutePath());
         }
      }
     for (SingleFileObserver sfo: mObservers)
     {
    	// System.out.println("AAAAAAAAAAAA "+mObservers.size());
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
    	 //  System.out.println("BBBBBBBBBBBB "+mObservers.size());
           sfo.stopWatching();
       }
          mObservers.clear();
          mObservers = null;
       }
       super.stopWatching();
  }
       @Override
       public void onEvent(int event, String path) //��ʱ��path���Ǿ���·����
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
          	    	exitsFiles.add(path);
          	        s = "----"+path + " CREATE";
               		m = new Message();
               		m.obj = s;
               		myHandler.sendMessage(m);
        	        System.out.println("----"+path+" CREATE:"); 
        	        handleDirectory(path);
          	   }    
   			break;
           case FileObserver.MODIFY:  //�ļ����ݱ��޸�ʱ��������ճ���ļ���
        	   System.out.println("----"+path + " MODIFY");
        	   break;
           case FileObserver.CLOSE_WRITE:  //�༭�ļ��󣬹ر�
               s = "----"+path + " CLOSE_WRITE";
       		   m = new Message();
       		   m.obj = s;
       		   myHandler.sendMessage(m);
       	       System.out.println("----"+path + " CLOSE_WRITE");
        	   addToQueue(path,false);
        	   break;
           case FileObserver.MOVED_TO:
              	s = "----"+path + " MOVED_TO";
      			m = new Message();
      			m.obj = s;
      			myHandler.sendMessage(m);
      		    System.out.println("----"+path + " MOVED_TO");
      		  if(recvFiles.size()!=0) 
      		    addToQueue(path,false);
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
        	    			System.out.println("ֹͣ������Ŀ¼");
        	    			mObservers.get(i).stopWatching();
        	    			mObservers.remove(i);	
       	    			    break;
        	    		}
       
        	    	}
        	      }
        	    }
        	    boolean issend=false;
        	    boolean issending=false;
        	    boolean isexits=false;
      		    synchronized(recvFiles)
      		    {
      		    	if(recvFiles.contains(path))
      		    	{
      		           recvFiles.remove(path);  //�ӽ����ļ��б�ɾ��
      		           issend=true;
      		    	}
      		    	else
       		           issend=false;
    
      		    }
      		    System.out.println("�����ļ��б�Ĵ�С��"+sendFiles.size());
      		    synchronized(sendFiles)
      		    {
      		   if(sendFiles.size()>0)
      		   {
      		   Iterator it = sendFiles.keySet().iterator();
     		   while (it.hasNext())
     		   {
     		   String key=null;
     		   key=(String)it.next();  //��һ�ε���Iterator��next()����ʱ�����������еĵ�һ��Ԫ��
     	       if(sendFiles.get(key).equals(path)) 
     	        {
     	    	  issending=true;
     			  sendFiles.remove(key); 
     			 synchronized(nextseq)
       		     {
     			  nextseq.remove(key);     //��Ӧ�ļ�����һ���Ӱ�����б�ҲҪɾ��
       		     }
     			  break;
     		     }
     		    }
      		    }
      		   }
      		   synchronized(exitsFiles)
    		   {
      		     if(exitsFiles.contains(path))
      		     {
      			   exitsFiles.remove(path); 
      			   isexits=true;
      		      }
    		    } 
      		    if(issending==true|| issend==true||isexits==true)
      		    {
      		    	 sendInfo.init(path);
      		         sendInfo.sendcontrolInfo();
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
    	   System.out.println("stoping watchinging: "+mPath);
    	   super.stopWatching();
       }
    }
   public synchronized void handleDirectory(String path)
   {
	   synchronized(recvFiles)
		 {
	   if(!recvFiles.contains(path)) //���յĵ��ļ����ٷ���
	  	{
		 String filepath=path;  //Ҫ��������Ŀ¼
		 System.out.println("��ʼ�������ļ���"+filepath);   
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
		      addToQueue(filepath+"/"+files[i].getName(),false);
		 }
	  }	
	   else
	   {
		 //���յ��ļ����ͷ�������
		   
	   }
	}  
   }
  }
 public static synchronized void addToQueue(String path,boolean directSend)
 {
	 synchronized(recvFiles)
	 { 
		 boolean contain=false;
		 if(directSend==false)
		 { 
	     if(!recvFiles.contains(path)&&!SendFilequeue.contains(path))  //���յĵ��ļ����ٷ���
  	      {	
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
  	      }
	   else  
	    	 contain=true; 
		 }
	      if(contain==false)
	     	{
			synchronized(SendFilequeue)
			{  
			  String filename=path;  //����·��
			  SendFilequeue.add(filename);
			  FileInputStream fis;
      	      long filelength=0;
      	 	  try 
      	 	  {
      	 	  fis = new FileInputStream(filename); 
      	 	  filelength= fis.available();
      	 	  } catch (Exception e)
      	 	  {
      	 	  e.printStackTrace();
      	 	  }
      	 	  file_number++;
      	 	  currentLength=currentLength+filelength;
      	 	  System.out.println("fileName: "+path);   
      	 	  System.out.println("SendFilequeue Information:"+file_number+" �� "+currentLength+" Byte");
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
      			 
      			 System.out.println("###���г�����������������ļ������ﵽ���������,���д���");
      			 System.out.println("���¼�ʱ��"+System.currentTimeMillis()); 
    		    listenqueue.handleQueue(SendFilequeue); 
      		  }	
      		}
	     }		
  	   } //end �ı�����ļ�
	 
 }
 public class syncClickListener implements OnClickListener 
 {
		public void onClick(View v) 
		{
			 requestSyncFunction rsf=new  requestSyncFunction();
			 rsf.start();
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
   writeLog("sending time:"+total_sending_timer+" ms"+"\r\n");
   writeLog("sending length:"+total_sending_length/1024+" k"+"\r\n");
   writeLog("encode time:"+total_encode_timer+" ms"+"\r\n");
  
   System.out.println("sending time:"+total_sending_timer+" ms");
   System.out.println("sending length:"+total_sending_length/1024+" k"+"\r\n");
   System.out.println("encode time:"+total_encode_timer+" ms");
  
   encodedPacket.clear();
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
   recvInfo.onDestrtoy();
   rThread.destroy();
   resp.onDestrtoy();
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
