package com.example.filesharing;

import java.io.Serializable;
import java.util.ArrayList;

public class FeedBackData implements Serializable
{
	String sub_fileID;
	public ArrayList<Integer> nos=null;  //type=1ʱ����nos[0]�ŵ��Ƕ�ʧ����������type=2ʱ�ŵ��ǽ��յ����ļ����
    int type=0;
   	FeedBackData(String fileid,ArrayList<Integer> nos,int type)
   	{
   		this.sub_fileID=fileid;
   		this.nos=new ArrayList<Integer>();
		this.nos=nos;
   		this.type=type;
   	}
}