package com.example.filesharing;

import java.util.ArrayList;

public class otherFeedData 
{
	    public String sub_fileID;
	    public ArrayList<Integer> nos=null;
	    public long time;
	    otherFeedData (String fileid,ArrayList<Integer> nos,long time)
	    {
	    this.sub_fileID=fileid;
	    this.nos=new ArrayList<Integer>();
		this.nos=nos;
	    this.time=time;
		}
}