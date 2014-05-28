package com.example.filesharing;

import java.io.Serializable;
import java.util.ArrayList;

public class FeedBackData implements Serializable
{
	String sub_fileID;
	public ArrayList<Integer> nos=null;  //type=1时其中nos[0]放的是丢失包的数量，type=2时放的是接收到的文件块号
    int type=0;
   	FeedBackData(String fileid,ArrayList<Integer> nos,int type)
   	{
   		this.sub_fileID=fileid;
   		this.nos=new ArrayList<Integer>();
		this.nos=nos;
   		this.type=type;
   	}
}