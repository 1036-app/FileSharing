package com.example.filesharing;

import java.io.Serializable;

public class Packet implements Serializable
{
	String sub_fileID; //文件ID号和文件的块号
	String filename;
	int totalsubFiles;
	int type;  //0为数据，1为反馈包
	int coding_blocks;
	int data_blocks;
	int seqno;
	int data_length;
	byte[] data;
	int subFileLength=0; //文件块的大小
	int fileLength=0;
	
	Packet(int type,int subFileLength, int coding_blocks, int data_blocks, int seqno, int data_length,int filelength)
	{
		this.type = type;
		this.subFileLength=subFileLength;
		this.coding_blocks = coding_blocks;
		this.data_blocks = data_blocks;
		this.seqno = seqno;
		this.data_length = data_length;
		this.fileLength=filelength;
	}
}
