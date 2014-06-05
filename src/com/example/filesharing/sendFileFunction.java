package com.example.filesharing;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;

public class sendFileFunction {
	public notify_sendingTask subfiletask = null;
	public Timer subfiletimer = null;
	public String sharedPath = "//sdcard//SharedFiles";

	public void sendSmallFiles(ArrayList<SmallFileData > sfiles, int total_length) {
		int number = 0;
		if (total_length % FileSharing.maxfilelength == 0)
			number = total_length / FileSharing.maxfilelength;
		else
			number = total_length / FileSharing.maxfilelength + 1;
		Packet[][] plist = new Packet[number][FileSharing.maxfilelength/ FileSharing.blocklength];
		System.out.println("number== " + number);
		int n = 0;
		int srtartblock = 0;
		int from = 0;
		int to = 0;
		String[] aa = null;
		for (int ii = 0; ii < sfiles.size(); ii++) {
			aa = sfiles.get(ii).sub_fileid.split("--");
			SimpleDateFormat formatter = new SimpleDateFormat("HH-mm-ss-SSS");
			Date curDate = new Date(System.currentTimeMillis());
			String m = formatter.format(curDate);
			String mm = "发送文件 " + aa[0] + "的时间: " + m;
			FileSharing.messageHandle(mm);

			FileSharing.writeLog("###" + sfiles.get(ii).filename + ",	");
			FileSharing.writeLog(aa[0].split("-")[1] + ",	");
			FileSharing.writeLog(sfiles.get(ii).filelength + ",	");
			FileSharing.writeLog(System.currentTimeMillis() + ",	");
			FileSharing.writeLog(m + ",	" + "\r\n");
			FileSharing.writeLog("\r\n");
			FileSharing.sending_subfileid = sfiles.get(ii).sub_fileid;
			FileInputStream fis = null;
			BufferedInputStream in = null;
			int sub_no = Integer.parseInt(aa[1]);
			if (!FileSharing.sendFiles.containsKey(aa[0]))
				FileSharing.sendFiles.put(aa[0], sfiles.get(ii).filename);
			String file = sharedPath + "//" + sfiles.get(ii).filename;
			int data_blocks;
			int coding_blocks = 0;

			int lastlength = FileSharing.blocklength;
			byte[] data = new byte[FileSharing.maxfilelength];
			try {
				fis = new FileInputStream(file);
				in = new BufferedInputStream(fis);
				in.skip(FileSharing.maxfilelength * sub_no);
				in.read(data, 0, FileSharing.maxfilelength);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (sfiles.get(ii).filelength % FileSharing.blocklength == 0) {
				data_blocks = sfiles.get(ii).filelength/ FileSharing.blocklength;
			} else {
				data_blocks = sfiles.get(ii).filelength/ FileSharing.blocklength + 1;
				lastlength = sfiles.get(ii).filelength% FileSharing.blocklength;
			}
			System.out.println("该文件的data_blocks  " + data_blocks);
			if (srtartblock == 0) {
				srtartblock = data_blocks;
				from = 0;
				to = data_blocks;
			} else {
				from = srtartblock;
				to = from + data_blocks;
				srtartblock = to;
				if (to >= 100) {
					srtartblock = to - 100;
					to = 100;
				}
			}
			for (int k = from; k < to; k++) {
				byte[] filedata = new byte[FileSharing.blocklength];
				System.arraycopy(data, (k - from) * FileSharing.blocklength,filedata, 0, FileSharing.blocklength);
				int send_blockLength = 0;
				if (k == data_blocks - 1)
					send_blockLength = lastlength;
				else
					send_blockLength = FileSharing.blocklength;
				Packet p = new Packet(0, sfiles.get(ii).filelength,
						coding_blocks, data_blocks, (k - from),
						send_blockLength, sfiles.get(ii).filelength);
				p.data = filedata;
				p.filename = sfiles.get(ii).filename;
				p.totalsubFiles = 1;
				plist[n][k] = p;
				p.sub_fileID = sfiles.get(ii).sub_fileid;
			}
			synchronized (FileSharing.nextseq) {
				FileSharing.nextseq.put(sfiles.get(ii).sub_fileid, data_blocks); // 下一次从plist[0]开始发，其中只放的是冗余包。
			}
			if (to == 100) {
				sendThread st = new sendThread(plist[n],FileSharing.bcastaddress, FileSharing.port, 0, 100, 1);
				st.start();
				n++;
				System.out.println("已经够100块开始发送");
				for (int j = 0; j < srtartblock; j++) {
					byte[] filedata = new byte[FileSharing.blocklength];
					System.arraycopy(data, (j + 100 - from)* FileSharing.blocklength, filedata, 0,FileSharing.blocklength);
					int send_blockLength = 0;
					if ((100 - from + j) == data_blocks - 1)
						send_blockLength = lastlength;
					else
						send_blockLength = FileSharing.blocklength;
					Packet p = new Packet(0, sfiles.get(ii).filelength,
							coding_blocks, data_blocks, (100 - from + j),
							send_blockLength, sfiles.get(ii).filelength);
					p.data = filedata;
					p.filename = sfiles.get(ii).filename;
					p.totalsubFiles = 1;
					plist[n][j] = p;
					p.sub_fileID = sfiles.get(ii).sub_fileid;

				}
			}
		} // for循环结束
		if (plist[0] != null) {
			sendThread st = new sendThread(plist[n], FileSharing.bcastaddress,FileSharing.port, 0, srtartblock, 1);
			st.start();
		}
	}

	public void sendToAll(String filename, int filelength, String sub_fileid,int num) 
	{
		Packet[] sendPacket = null;
		FileInputStream fis = null;
		BufferedInputStream in = null;
		String[] aa = null;
		aa = sub_fileid.split("--");
		int sub_no = Integer.parseInt(aa[1]);
		String file = sharedPath + "//" + filename;
		byte[] data = new byte[FileSharing.maxfilelength];
		int length = 0;
		try {
			fis = new FileInputStream(file);
			in = new BufferedInputStream(fis);
			in.skip(FileSharing.maxfilelength * sub_no);
			length = in.read(data, 0, FileSharing.maxfilelength);
		} catch (IOException e) {
			e.printStackTrace();
		}
		sendPacket = file_blocks(data, length, sub_fileid, filename, num,
				filelength);
		if (sendPacket != null) {
			sendThread sThread = new sendThread(sendPacket,
					FileSharing.bcastaddress, FileSharing.port, 0,
					sendPacket[0].data_blocks, 0);
			sThread.start();
			String message = "发送的包的个数: " + sendPacket[0].data_blocks;
			FileSharing.messageHandle(message);
		}
		if (!FileSharing.sendFiles.containsKey(aa[0]))
			FileSharing.sendFiles.put(aa[0], sendPacket[0].filename);
		synchronized (FileSharing.subfiles) {
			subfiletask = new notify_sendingTask(filename, sub_fileid, num);
			subfiletimer = new Timer(true);
			subfiletimer.schedule(subfiletask, FileSharing.subtime,
					FileSharing.subtime);
			FileSharing.subfiles.put(sub_fileid, subfiletimer);
		}

	}

	public Packet[] file_blocks(byte[] filedata, int subFileLength,
			String sub_fileID, String filename, int totalsubFiles,
			int FileLength) {
		int data_blocks;
		int coding_blocks = 0;
		Packet[] plist = null;
		int lastlength = FileSharing.blocklength;

		if (subFileLength % FileSharing.blocklength == 0) {
			data_blocks = subFileLength / FileSharing.blocklength;
		} else {
			data_blocks = subFileLength / FileSharing.blocklength + 1;
			lastlength = subFileLength % FileSharing.blocklength;
		}
		plist = new Packet[data_blocks];
		for (int i = 0; i < data_blocks; i++) {
			byte[] data = new byte[FileSharing.blocklength];
			System.arraycopy(filedata, i * FileSharing.blocklength, data, 0,
					FileSharing.blocklength);
			int send_blockLength = 0;
			if (i == data_blocks - 1)
				send_blockLength = lastlength;
			else
				send_blockLength = FileSharing.blocklength;
			Packet p = new Packet(0, subFileLength, coding_blocks, data_blocks,
					i, send_blockLength, FileLength);
			p.data = data;
			p.filename = filename;
			p.totalsubFiles = totalsubFiles;
			plist[i] = p;
			p.sub_fileID = sub_fileID;
		}
		synchronized (FileSharing.nextseq) {
			FileSharing.nextseq.put(plist[0].sub_fileID, data_blocks); // 下一次从plist[0]开始发，其中只放的是冗余包。
		}
		return plist;
	}
}
