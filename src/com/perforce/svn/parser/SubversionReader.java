package com.perforce.svn.parser;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Iterator;

public class SubversionReader implements Iterator<Record> {

	private Record currentValue;
	private int currentChangeNumber = 0;
	private int currentNodeNumber = 0;

	private String fileName;
	private RandomAccessFile rf;
	private boolean subBlock = false;

	// Constructor
	public SubversionReader(String path) {
		fileName = path;
		try {
			rf = new RandomAccessFile(fileName, "r");

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
	}

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

	public String getLine() {
		byte[] buf = new byte[1024];

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
					buf[c] = b;
					c++;
				}
			} catch (IOException e) {
				return null;
			}
		} while (c < 1024 && !eol);

		// return empty string for empty lines
		if (c == 0)
			return "";

		// new buffer with the correct length
		byte[] trim = new byte[c];
		for (int i = 0; i < c; i++) {
			trim[i] = buf[i];
		}

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

	@Override
	public Record next() {
		RecordStateTrace.update(currentValue);
		return currentValue;
	}

	@Override
	public boolean hasNext() {

		String line = new String();
		int readlines = 0;
		do {
			line = getLine();
			if (line == null) {
				return false;
			}
			readlines++;
		} while (line.isEmpty());

		String[] args = line.split(": ");

		if (args[0].contains("Revision-number")) {
			try {
				Revision revision = new Revision(line, this);
				currentChangeNumber = (int) revision
						.findHeaderLong("Revision-number");
				revision.setChangeNumber(currentChangeNumber);
				currentNodeNumber = 0;
				currentValue = revision;
				subBlock = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		else if (args[0].contains("Node-path")) {
			try {
				if (subBlock)
					subBlock = false;
				if (readlines == 1)
					subBlock = true;

				Node node = new Node(line, this);
				node.setSvnRevision(currentChangeNumber);
				node.setNodeNumber(currentNodeNumber++);

				if (node.isSubBlock())
					node.setSubBlock(false);
				if (readlines == 1)
					node.setSubBlock(true);

				currentValue = node;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;
		}

		else {
			try {
				Schema schema = new Schema(line, this);
				currentValue = schema;
			} catch (ParserException e) {
				e.printStackTrace();
			}
			return true;
		}
	}

	@Override
	public void remove() {
	}

	public String getFileName() {
		return fileName;
	}

	public long length() {
		try {
			return rf.length();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return -1;
	}

	public void close() {
		try {
			rf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
