package com.perforce.svn.asset;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.perforce.svn.parser.Content;

public class SvnContentReader {

	private String name;
	private RandomAccessFile rf;
	private long readCount = 0L;
	private long blockCount = 0L;
	private long length;
	private static final long ReadBlockSize = 1024L;

	public SvnContentReader(Content content) {
		name = content.getFileName();
		length = content.getLength();

		try {
			rf = new RandomAccessFile(name, "r");
			rf.seek(content.getPosition());
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public byte[] nextBlock() {
		return nextBlock(ReadBlockSize);
	}
	
	public byte[] nextBlock(long blockSize) {

		int readLength = (int) (length - readCount);
		if (readLength > blockSize) {
			readLength = (int) blockSize;
		}
		if (readLength <= 0) {
			return null;
		}

		byte[] block = new byte[readLength];
		try {
			rf.readFully(block);
			readCount += readLength;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		blockCount++;
		return block;
	}

	public void close() {
		try {
			rf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public long getBlockCount() {
		return blockCount;
	}
}
