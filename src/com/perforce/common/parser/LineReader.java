package com.perforce.common.parser;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.text.Normalizer.Form;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public abstract class LineReader {

	private String fileName;
	private RandomAccessFile rf;

	public long getFilePointer() {
		long pos = -1;
		try {
			pos = rf.getFilePointer();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return pos;
	}

	public void seek(long pos) {
		try {
			rf.seek(pos);
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

		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		boolean eol = false;
		int c = 0;
		do {
			try {
				byte b = rf.readByte();
				if (b == '\n') {
					eol = true;
				} else if (b == '\n' || b == '\r') {
					// chomp
				} else {
					buf.write(b);
					c++;
				}
			} catch (IOException e) {
				return null;
			}
		} while (!eol);

		// return empty string for empty lines
		if (c == 0)
			return "";

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
		long maxLineSize = (long) Config.get(CFG.CVS_MAXLINE);

		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int c = 0;
		byte b;
		do {
			try {
				b = rf.readByte();
				buf.write(b);
				c++;
			} catch (EOFException e) {
				if (c == 0) {
					// End of file and empty buffer
					return null;
				} else {
					// End of file, but buffer to process
					break;
				}
			} catch (IOException e) {
				return null;
			}
		} while (c < maxLineSize && b != '\n');
		return buf;
	}

	public byte[] getBlob(int remainder) {
		byte b[] = new byte[remainder];
		try {
			int readCount = 0;
			do {
				readCount += rf.read(b, readCount, remainder - readCount);
			} while (readCount < remainder);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return b;
	}

	public long length() {
		try {
			return rf.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void open(String path) {
		fileName = path;
		try {
			rf = new RandomAccessFile(fileName, "r");
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void close() {
		try {
			rf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getFileName() {
		return fileName;
	}
}
