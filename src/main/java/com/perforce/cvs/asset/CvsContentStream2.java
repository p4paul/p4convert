package com.perforce.cvs.asset;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import com.perforce.common.asset.ContentStream;
import com.perforce.common.asset.ContentType;
import com.perforce.svn.parser.Content;

public class CvsContentStream2 extends ContentStream {

	private RandomAccessFile rf;
	private long mark = 0L;
	private long end = 0L;

	private ContentType contentType;

	public CvsContentStream2(Content content) {
		this.contentType = content.getType();

		try {
			String name = content.getFileName();
			rf = new RandomAccessFile(name, "r");
			mark = rf.getFilePointer();
			end = rf.length();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (rf.getFilePointer() >= end)
			return -1;

		if (rf.getFilePointer() + len > end) {
			len = (int) (end - rf.getFilePointer());
		}

		int r = rf.read(b, off, len);
		scanText(b, r);
		return r;
	}

	@Override
	public int read(byte[] b) throws IOException {
		if (rf.getFilePointer() >= end)
			return -1;

		int len = b.length;
		if (rf.getFilePointer() + len > end) {
			len = (int) (end - rf.getFilePointer());
			int r = rf.read(b, 0, len);
			scanText(b, r);
			return r;
		}
		int r = rf.read(b);
		scanText(b, r);
		return r;
	}

	@Override
	public int read() throws IOException {
		throw new IOException("Byte read not supported");
	}

	@Override
	public void mark(int readlimit) {
		try {
			mark = rf.getFilePointer();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void reset() {
		try {
			rf.seek(mark);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void close() throws IOException {
		rf.close();
	}

	@Override
	public boolean removeBOM() throws IOException {
		byte[] bom = null;
		byte[] b = null;
		switch (contentType) {
		case UTF_8:
			bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			b = new byte[bom.length];
			rf.read(b);
			break;

		case UTF_16LE:
			bom = new byte[] { (byte) 0xFF, (byte) 0xFE };
			b = new byte[2];
			rf.read(b);
			break;

		case UTF_16BE:
			bom = new byte[] { (byte) 0xFE, (byte) 0xFF };
			b = new byte[2];
			rf.read(b);
			break;

		default:
			return false;
		}

		// return to start position if no match
		if (!Arrays.equals(b, bom)) {
			rf.seek(mark);
			return false;
		}
		return true;
	}

}
