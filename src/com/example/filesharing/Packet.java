package com.example.filesharing;

import java.io.Serializable;

public class Packet implements Serializable
{
	String fileID;
	String filename;
	int type;  //0为数据，1为反馈包
	int file_length;
	int coding_blocks;
	int data_blocks;
	int seqno;
	int data_length;
	byte[] data;
	
	
	Packet(int type, int file_length, int coding_blocks, int data_blocks, int seqno, int data_length)
	{
		this.type = type;
		this.file_length = file_length;
		this.coding_blocks = coding_blocks;
		this.data_blocks = data_blocks;
		this.seqno = seqno;
		this.data_length = data_length;
	}
}
