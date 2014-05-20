package com.example.filesharing;

import java.io.Serializable;

public class Packet implements Serializable
{
	String sub_fileID;
	String filename;
	int totalsubFiles;
	int type;  //0为数据，1为反馈包
	int coding_blocks;
	int data_blocks;
	int seqno;
	int data_length;
	byte[] data;
	int subFileLength=0;
	
	Packet(int type,int subFileLength, int coding_blocks, int data_blocks, int seqno, int data_length)
	{
		this.type = type;
		this.subFileLength=subFileLength;
		this.coding_blocks = coding_blocks;
		this.data_blocks = data_blocks;
		this.seqno = seqno;
		this.data_length = data_length;
	}
}
