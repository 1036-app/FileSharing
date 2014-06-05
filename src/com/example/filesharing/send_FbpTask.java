package com.example.filesharing;

import java.util.ArrayList;

public class send_FbpTask extends java.util.TimerTask
{
	public String fileID;  //文件的ID号
	public ArrayList<Integer>sub_nos;  //收到的文件的块号
	send_FbpTask(String fileid,ArrayList<Integer>sub_nos)
	{
	  this.fileID=fileid;
	  this.sub_nos=new ArrayList<Integer>();
	  this.sub_nos=sub_nos;
	
	}
	 public void run() 
  	 { 
	 System.out.println("超时，发送块丢失反馈包");
	 sendFeedBackPack sfb=new sendFeedBackPack(fileID,sub_nos,2);
     sfb.sendFeedBack();
  	 }
}
