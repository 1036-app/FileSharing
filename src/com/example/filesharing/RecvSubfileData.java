package com.example.filesharing;

public class RecvSubfileData
{
  public String fileID;
  public String filename;
  public byte[] data=null;
  public int sub_num;
  public RecvSubfileData(String fileID,String filename,byte[]data,int sub_num)
  {
	this.fileID=fileID;
	this.filename=filename; 
	this.sub_num=sub_num;
	this.data=new byte[data.length];
	System.arraycopy(data, 0, this.data, 0, data.length);
  }
}