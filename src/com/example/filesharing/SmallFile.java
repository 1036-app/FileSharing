package com.example.filesharing;

public class SmallFile 
{
	public String filename;
	public int filelength=0;
	public String sub_fileid;
	SmallFile (String filename,int length,String fileid)
	{
		this.filename=filename;
		this.filelength=length;
		this.sub_fileid=fileid;
	}
}
