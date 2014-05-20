package com.example.filesharing;

import java.io.Serializable;

public class FeedBackData implements Serializable
{
	String sub_fileID;
   	int loss;
   	FeedBackData(String fileid,int l)
   	{
   		this.sub_fileID=fileid;
   		this.loss=l;
   	}
}
