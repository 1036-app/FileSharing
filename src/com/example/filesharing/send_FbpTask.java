package com.example.filesharing;

import java.util.ArrayList;

public class send_FbpTask extends java.util.TimerTask
{
	public String fileID;  //文件的ID号
	public ArrayList<Integer>sub_nos;  //收到的文件的块号
	public int totalblocks=0;
	public  ArrayList<Integer> miss_nos=new ArrayList<Integer>();
	send_FbpTask(String fileid,int totalblocks)
	{
	  this.fileID=fileid;
	  this.sub_nos=new ArrayList<Integer>();
	  this.totalblocks=totalblocks;
	}
	public void getMiss_nos()
	{
		this.sub_nos=FileSharing.recv_subfiels_no.get(fileID);
		for(int i=0;i<totalblocks;i++)
		{
			if(!this.sub_nos.contains(i))
			{
				System.out.println(fileID+" missing  "+i);
				miss_nos.add(i);
			}
		}
	}
	 public void run() 
  	 { 
	 getMiss_nos();
	 System.out.println("超时，发送块丢失反馈包");
	 sendFeedBackPack sfb=new sendFeedBackPack(fileID,miss_nos,2);
     sfb.sendFeedBack();
  	 }
}
