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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    /** Called when the activity is first created. */
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
    public ArrayList<FeedBackData> Feedpkts=new ArrayList<FeedBackData>(); //���������ļ��ţ�������
    public static ArrayList<otherFeedData>othersFeedpkt=new ArrayList<otherFeedData >();//�����˵ķ�������Ϣ
    public HashMap<String,String>sendFiles=new HashMap<String,String>();
    public ArrayList<String>recvFiles=new ArrayList<String>();
    public SendTask sendtask=null;
    public Timer feedtimer=null;
	public Map<String,Timer> feedTimers=new HashMap<String,Timer>();
    public OtherTask othertask=null;
    public Timer othertimer=null;
    String message=null;
	public FileObserver mFileObserver;
	public static String sharedPath=null;
	public static final int blocklength=1024;      //ÿ��Ĵ�С1k
	public static final int packet_numbers=100;  //һ�η����ĸ���
	public static final int maxfilelength=102400;    //�ļ����С����Ϊ100k

	
	public ArrayList<RecvSubfileData>RecvSubFiles=new ArrayList<RecvSubfileData>();
	public Map<String,Integer>subFile_nums=new HashMap<String,Integer>();
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
        bcastaddress="255.255.255.255";
        /* WIFI:255.255.255.255
         * Adhoc:192.168.1.255
         */
        information.append("�ҵ�IP��ַ�ǣ�"+ipaddress+"\n");
        information.append("�ҵĹ㲥��ַ�ǣ�"+bcastaddress+"\n");
        rThread=new recvThread(ipaddress,port);
        rThread.start();
   
        
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        information.append("�ֻ������߳��ڴ棺"+activityManager.getMemoryClass()+"M\n");	

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
        		    System.out.println("�յ������̷߳������İ�");		  
        		    Packet p=recvPacket[0];
        	        if(p.type==0&&recvPacket.length>=p.data_blocks)
        		    {
        		       information.append( "�յ��������ݰ�\n");	
        		       decode(recvPacket);	  //���ý��뺯��
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
        		    	
        		        System.out.println("��������ʾ��ʧ��������  "+fb.loss);		  
        			    information.append( "��������ʾ��ʧ��������  "+fb.loss+"\n");
        			    String []aa=fb.sub_fileID.split("--"); //aa[0]�зŵ���fileID
        		    	
        		    	if(sendFiles.containsKey(aa[0])) 
        		        {
        		           information.append( "**�յ����Ƕ��Լ��ķ�����\n");		
        		           int i=0;
        		           for(;i<Feedpkts.size();i++)
        		           {
        		        	   if(Feedpkts.get(i).sub_fileID.equals(fb.sub_fileID))
        		                {
        		        		   System.out.println("�Ѿ��յ��˷����� ");
        		        		   if(Feedpkts.get(i).loss<fb.loss)
        		                   {
        		        			   Feedpkts.get(i).loss=fb.loss;
        		                   }
        		        	     break;
        		                }
        		           }
        		           if(i==Feedpkts.size())
        		           {
        		        System.out.println("��һ���յ����ļ��ķ�����");
        		        	   Feedpkts.add(fb);
        		        	   information.append("��ʼ��ʱ��"+fb.sub_fileID+"\n");
        		        	   System.out.println("��ʼ��ʱ��"+fb.sub_fileID+"\n");
        				       sendtask=new SendTask(fb.sub_fileID);
        				       feedtimer = new Timer(true);
        				       feedtimer.schedule(sendtask,1000,1000); //1s�ĳ�ʱ
        				       feedTimers.put(fb.sub_fileID, feedtimer);
        		           }
        		           
        		      }
        	           else
        	           {
        	        	information.append("$$�յ����Ǳ��˵ķ�����\n");
        	         	int k=0;
        	        	for(;k<othersFeedpkt.size();k++)
        		        {
        			        if(othersFeedpkt.get(k).sub_fileID.equals(p.sub_fileID))
        		          	{ 
        			        	System.out.println("�Ѿ��յ�guo������ ");
        			        	if(othersFeedpkt.get(k).loss<fb.loss)
        			         	{
        					     othersFeedpkt.get(k).loss=fb.loss;
        					     othersFeedpkt.get(k).time=System.currentTimeMillis();
        				         }
        				        break;
        			        }	
        		        }
        		       if(othersFeedpkt.size()==k)
        		        {
        		    System.out.println("��һ���յ����ڸ��ļ��ı��˵ķ�����");
        		           otherFeedData ofd=new otherFeedData(fb.sub_fileID,fb.loss,System.currentTimeMillis());
        			       othersFeedpkt.add(ofd);
        		        }
        	           }
                  }
          }
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
     	        {
     			  sendFiles.remove(key);   //�ӷ����ļ��б�ɾ��
     			  nextseq.remove(key);     //��Ӧ�ļ�����һ���Ӱ�����б�ҲҪɾ��
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
    public void sendToAll (String filename)
    {
     	Packet[] sendPacket=null; 
     	FileInputStream fis=null;
     	BufferedInputStream in=null;
     	int filelength=0;
     	String file=sharedPath+"//"+filename;
		try {
			fis = new FileInputStream(file);
			filelength = fis.available();
			in = new BufferedInputStream(fis);
		} catch (Exception e) {
			e.printStackTrace();
		}
		byte[]abc=new byte[filelength];
		if(filelength>maxfilelength)  //���͵��ļ�̫����Ҫ�ֿ鷢��
		{ 
			int num=0;
			if(filelength%maxfilelength==0)
				num=filelength/maxfilelength;
			else
				num=filelength/maxfilelength+1;
			
			System.out.println("�ļ��ּ��η��ͣ�"+num);
			for(int i=0;i<num;i++)
			{
				byte[] data = new byte[maxfilelength];
				int length=0;
 				try {
 				   length=in.read(data,0,maxfilelength);
 				   System.arraycopy(data, 0, abc, i*1024, data.length);
				} catch (IOException e) 
				{
					e.printStackTrace();
				}
 				//���ñ��뺯�������ͺ���
 				String id=Integer.toString(sendFileID);
 				String sub_id =ipaddress+"-"+id+"--"+i;
 				sendPacket=encode(data,length,sub_id,filename,i,num);
 				if(sendPacket!=null)
 		        {
 			    String []aa=sendPacket[0].sub_fileID.split("--"); //aa[0]�зŵ���fileid
 	 		    System.out.println("�ļ�ID    ��"+aa[0]);
 	 			System.out.println("���͵Ŀ��    ��"+aa[1]);
 		        sThread=new sendThread(sendPacket,bcastaddress,port,0,sendPacket[0].data_blocks);
 		        sThread.start();    //����������˼���£����͵��ļ�������ַ���˿ڣ���ʼ���Ӱ���ţ����Ͱ�������
 		        message = "���͵İ��ĸ���: "+sendPacket[0].data_blocks;
 				messageHandle(message);
 				nextseq.put(sendPacket[0].sub_fileID, sendPacket[0].data_blocks);
 				
 				if(!sendFiles.containsKey(aa[0]))
 					sendFiles.put(aa[0],sendPacket[0].filename);
 			
 		        }	
			}
			System.out.println("���鳤�� ��"+abc.length);
		   sendFileID++;
		}
		else if(filelength>0)
		{
			byte[] data = new byte[maxfilelength];
			int length=0;
		    try {
				length=in.read(data,0,maxfilelength);
			} catch (IOException e) 
			{
				e.printStackTrace();
			}	
		    //���ñ��뺯�������ͺ���
		    String id=Integer.toString(sendFileID);
		    String sub_id =ipaddress+"-"+id+"--"+0;
		    sendPacket=encode(data,length,sub_id,filename,0,1);
		    if(sendPacket!=null)
	        {
	        sThread=new sendThread(sendPacket,bcastaddress,port,0,sendPacket[0].data_blocks);
	        sThread.start();    //����������˼���£����͵��ļ�������ַ���˿ڣ���ʼ���Ӱ���ţ���������ţ����Ͱ�������
	        message = "���͵İ��ĸ���: "+sendPacket[0].data_blocks;
			messageHandle(message);
			nextseq.put(sendPacket[0].sub_fileID, sendPacket[0].data_blocks);
		    String []aa=sendPacket[0].sub_fileID.split("--"); //aa[0]�зŵ���fileid
			if(!sendFiles.containsKey(aa[0]))
				sendFiles.put(aa[0],sendPacket[0].filename);
	        }
			sendFileID++;
		} 		
      
    }
    public Packet[] encode(byte[] filedata,int subFileLength,String sub_fileID,String filename,int sub_ID,int totalsubFiles)
    {
         int data_blocks;
         int coding_blocks;
         Packet[] plist =null;
         System.out.println("���δ�������ļ���Ĵ�С��"+subFileLength);
     try {
 			if(subFileLength % blocklength == 0)
 			{
 				data_blocks = subFileLength/blocklength;
 			}
 			else
 			{
 				data_blocks = subFileLength/blocklength+1;
 			}
 			
 			coding_blocks = data_blocks;
 			plist = new Packet[data_blocks+coding_blocks];
 			
 			byte[][] origindata = new byte[data_blocks][blocklength];
 			byte[][] encodedata = new byte[coding_blocks][blocklength];
 			for(int i=0;i<data_blocks;i++)
 			{
 				byte[] data = new byte[blocklength];
 			//	in.read(data,0,blocklength);
 				System.arraycopy(filedata, i*blocklength, data, 0, blocklength);
 				System.arraycopy(filedata, i*blocklength, origindata[i], 0, blocklength);
 				Packet p = new Packet(0,subFileLength,coding_blocks, data_blocks, i, blocklength);
 				p.data = data;
 				p.seqno = i;
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
 				Packet p = new Packet(0,subFileLength,coding_blocks, data_blocks, i, blocklength);
 				p.data = data;
 				p.seqno = i;
 				p.filename=filename;
 				p.totalsubFiles=totalsubFiles;
 				plist[i] = p;
 				p.sub_fileID=sub_fileID;
 			}
 	        	       	
 	    	  message = plist[0].sub_fileID+" �������ɵİ��ĸ�����"+plist.length;
 	          messageHandle(message);
	   
 		
 		} catch (Exception e1) 
 		{
 			System.out.println("û���ҵ����ļ�");
 			e1.printStackTrace();
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
    	int file_length = p.subFileLength;
    	String recvFilename=p.filename;
    	int w=16;
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
    	if(plist[0].totalsubFiles==1)
    	{
    	  message="�����ļ�";
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
    	else
    	{
        System.out.println("���ļ��Ѿ��ֳ��˶����");
    	String []aa=plist[0].sub_fileID.split("--"); //aa[0]�зŵ���fileid
    	int sub_no=Integer.parseInt(aa[1]);
    	RecvSubfileData rsfd=new RecvSubfileData(aa[0],plist[0].filename,writein,sub_no);
    	RecvSubFiles.add(rsfd);
    	int off=RecvSubFiles.size()-1; //��ǰ�յ����ļ�����RecvSubFiles���±�
    	if(subFile_nums.containsKey(aa[0]))
    	{
    		int t=subFile_nums.get(aa[0]);
    		t=t+1;
			subFile_nums.remove(aa[0]);
			subFile_nums.put(aa[0], t);
            System.out.println("���ļ��ܹ�������"+plist[0].totalsubFiles+" ��ǰ�յ��˿飺"+subFile_nums.get(aa[0]));
    		if(subFile_nums.get(aa[0])==plist[0].totalsubFiles)
    		{
    			//���������ļ�
                 System.out.println("���������ļ�");
                 //�ļ����ʱ���᲻������Խ�磬�Ų���
                 byte[] encodedData=new byte[maxfilelength*plist[0].totalsubFiles];
                 int totalLength=0;
                 for(int nn=0;nn<RecvSubFiles.size();nn++)
    			 {
    				if(RecvSubFiles.get(nn).fileID.equals(aa[0]))
    				{
    				 totalLength=totalLength+RecvSubFiles.get(nn).data.length;
    				 System.arraycopy(RecvSubFiles.get(nn).data, 0,  encodedData, 102400*nn, RecvSubFiles.get(nn).data.length);
    				 RecvSubFiles.remove(nn);
    				 nn--;
    				}
    			  
    			 }
    			  message="��ʼ�����ļ�";
    	    	  messageHandle(message);
    	    	  recvFiles.add(recvFilename);  //��������ļ��б���
    	    	  System.out.println("�����ļ��б�Ĵ�С��"+ recvFiles.size());
    	    	
    	    	  try {
    	      		String encodeFile=sharedPath+"//"+recvFilename;
    	      		BufferedOutputStream bos;
    	      		bos = new BufferedOutputStream(new FileOutputStream(encodeFile));
    	  			bos.write(encodedData,0,totalLength);
    	  	        bos.close();
    	  		    } catch (FileNotFoundException e) 
    	  		   {		
    	  			e.printStackTrace();
    	  		    } catch (IOException e)
    	  		   {
    	  			e.printStackTrace();
    	  		   }
    		 subFile_nums.remove(aa[0]);

    		} //�����ļ�����
    		
    	}  //end subFile_nums�и��ļ��Ĳ���
    	else
    	  {
    		System.out.println("������RecvSubFiles�����ļ�ȫ�����ܹ��ˣ��Ϳ�ʼ�ָ��ļ�");
    		subFile_nums.put(aa[0], 1);
    	  }
        
    	}
    }
    
    
    public String getIp()
	{  
    	//Adhocģʽ�»�ȡIP
  /*  	String networkIp = "";  
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
		*/
		
   //Wifi��ȡIP��ʽ
	//*	
    	WifiManager wm=(WifiManager)getSystemService(Context.WIFI_SERVICE);  
		    if(!wm.isWifiEnabled())                     //���Wifi״̬     
		     wm.setWifiEnabled(true);  
		    WifiInfo wi=wm.getConnectionInfo();        //��ȡ32λ����IP��ַ     
		    int IpAdd=wi.getIpAddress(); 
		    String Ip=intToIp(IpAdd);                 //�����͵�ַת���ɡ�*.*.*.*����ַ  
		    return Ip;    
   //*/
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
		 if(feedTimers.containsKey(sub_fileID))
  	     {
System.out.println("ȡ����ʱ�� "+sub_fileID);
  	    	feedTimers.get(sub_fileID).cancel();  
  	    	feedTimers.remove(sub_fileID);
  	     } 
         for(;i<Feedpkts.size();i++)
         {
      	   if(Feedpkts.get(i).sub_fileID.equals(sub_fileID))
              {
      			number=Feedpkts.get(i).loss;
      			Feedpkts.remove(i);
      			i--;
              }
      	     break;
          } 
        
  System.out.println("��ʱ���ļ���ʧ�İ���Ŀ��"+number);
         if(i!=Feedpkts.size())
         {
     	   String []aa=sub_fileID.split("--");  //aa[0]�зŵ���fileid
           int sub_no=Integer.parseInt(aa[1]);  //*********
     	   if(sendFiles.containsKey(aa[0]))
		    {
 // System.out.println("�����ļ��б����и��ļ�");
     		 FileInputStream fis=null;
     	     BufferedInputStream in=null;
     	     int filelength=0;
     	     int num=0;
		     fileName=sendFiles.get(aa[0]);
		     String file=sharedPath+"//"+fileName;
  //System.out.println("�����ļ������ƣ�"+file);
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

//System.out.println("�ļ��ܹ��ֿ飺  "+num);
		       byte[] data = new byte[maxfilelength];
		       int length=0;
		       for(int j=0;j<=sub_no;j++)
				{
	 				try {
	 				   length=in.read(data,0,maxfilelength);
					} catch (IOException e) 
					{
						e.printStackTrace();
					}
				}
System.out.println("##########�������ݳ��ȣ�  "+length);	
             message=sub_fileID+" ��ʱ���ٷ� "+number+" ����";
             messageHandle(message);
		     Packet[] sPacket=null; 
		     sPacket=encode(data,length,sub_fileID,fileName,sub_no,num);
		        
		     int Start=nextseq.get(sub_fileID);
		     nextseq.remove(sub_fileID); 
		     System.out.println("���ο�ʼ���͵��Ӱ����  "+Start);
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
				System.out.println("��һ�ο�ʼ���͵İ���ţ� "+nextStart);
		        sThread=new sendThread(sPacket,bcastaddress,port,Start,number);
				sThread.start();          
				message = "���Ͱ��ĸ���"+number;
	 			messageHandle(message);
		     }
		       
		    }
     	
         } //���ͷ���������   	  
  	  } //run��������
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
   if(recvHandler!=null)
     recvHandler.getLooper().quit(); 
   rThread.destroy();
   othertimer.cancel();
   RecvSubFiles.clear();
   nextseq.clear();
   Feedpkts.clear();
   othersFeedpkt.clear();
   recvFiles.clear();
   if(sThread!=null)
      sThread.destroy(); 
   super.onDestroy();
 }

}   
