package com.example.filesharing;

public class SmallFileData 
{
	public String filename;
	public int filelength=0;
	public String sub_fileid;
	SmallFileData (String filename,int length,String fileid)
	{
		this.filename=filename;
		this.filelength=length;
		this.sub_fileid=fileid;
	}
}
