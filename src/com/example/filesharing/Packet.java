package com.example.filesharing;

import java.io.Serializable;

public class Packet implements Serializable
{
	String sub_fileID; //�ļ�ID�ź��ļ��Ŀ��
	String filename;
	int totalsubFiles;
	int type;  //0Ϊ���ݣ�1Ϊ������
	int coding_blocks;
	int data_blocks;
	int seqno;
	int data_length;
	byte[] data;
	int subFileLength=0; //�ļ���Ĵ�С
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
