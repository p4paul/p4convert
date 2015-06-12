package com.perforce.common.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public abstract class LineReader {

	private long maxLineSize;
	private String fileName;
	private RandomAccessFile rf;
	private FileChannel in;

	public long getFilePointer() {
		long pos = -1;
		try {
			pos = in.position();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pos;
	}

	public void seek(long pos) {
		try {
			in.position(pos);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void returnLine(String line) {
		if (line.isEmpty()) {
			return;
		}
		long pos = getFilePointer();
		pos -= line.length();
		seek(pos);
	}

	public String getLine() {

		ByteArrayOutputStream buf = null;
		try {
			buf = getData(true);
		} catch (ConfigException e) {
			return null;
		}
		if (buf == null) {
			return null;
		}

		// return empty string for empty lines
		if (buf.size() == 0) {
			return "";
		}

		// new buffer with the correct length
		byte[] trim = buf.toByteArray();

		// Normalisation to NDC form
		// a umlaut can be represented in different ways:
		// NFC form: utf8(C3 BC) -> uft16(00FC)
		// NFD form: utf8(75 CC 88) -> uft16(0075 0308)
		String line = null;
		try {
			String utf8 = new String(trim, "UTF-8");
			line = Normalizer.normalize(utf8, Form.NFC);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return line;
	}

	/**
	 * Return a line or blob of utf8 data up to the maximum line size. A null is
	 * returned if the end of the file is reached.
	 * 
	 * @return
	 * @throws ConfigException
	 */
	public ByteArrayOutputStream getData() throws ConfigException {
		return getData(false);
	}

	private ByteArrayOutputStream getData(boolean line) throws ConfigException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		int totalRead = 0;
		while (totalRead < maxLineSize) {
			// read bytes (exit null if EOF)
			try {
				ByteBuffer dst = ByteBuffer.allocate(1024);
				long mark = in.position();
				int bytesRead = in.read(dst);
				totalRead += bytesRead;
				if (bytesRead == -1) {
					return null;
				}
				if (bytesRead == 0) {
					return out;
				}

				// copy bytes to buffer (exit on '\n')
				int pos = 0;
				for (byte b : dst.array()) {
					if (!line || (b != '\n' && b != '\r')) {
						out.write(b);
					}
					pos++;
					if (b == '\n') {
						seek(mark + pos);
						return out;
					}
				}
			} catch (IOException e) {
				return null;
			}
		}
		return out;
	}

	public byte[] getBlob(int remainder) {
		ByteBuffer dst = ByteBuffer.allocate(remainder);
		try {
			in.read(dst);
			return dst.array();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public long length() {
		try {
			return in.size();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void open(String path) {
		fileName = path;
		try {
			maxLineSize = (long) Config.get(CFG.CVS_MAXLINE);
			rf = new RandomAccessFile(fileName, "r");
			in = rf.getChannel();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void close() {
		try {
			in.close();
			rf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFileName() {
		return fileName;
	}
}
