package com.example.filesharing;

import java.util.ArrayList;

public class send_FbpTask extends java.util.TimerTask
{
	public String fileID;  //�ļ���ID��
	public ArrayList<Integer>sub_nos;  //�յ����ļ��Ŀ��
	send_FbpTask(String fileid,ArrayList<Integer>sub_nos)
	{
	  this.fileID=fileid;
	  this.sub_nos=new ArrayList<Integer>();
	  this.sub_nos=sub_nos;
	
	}
	 public void run() 
  	 { 
	 System.out.println("��ʱ�����Ϳ鶪ʧ������");
	 sendFeedBackPack sfb=new sendFeedBackPack(fileID,sub_nos,2);
     sfb.sendFeedBack();
  	 }
}
