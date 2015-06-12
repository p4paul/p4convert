package com.perforce.svn.prescan;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.perforce.svn.parser.SubversionReader;

public class LastRevision {

	private RandomAccessFile rf;
	private String dumpFile;
	private long position;
	private long next;
	private static final int ReadBlockOffset = 1024 * 8;
	private static final int ReadBlockSize = ReadBlockOffset + 128;

	public LastRevision(String path) throws IOException {
		dumpFile = path;
		rf = new RandomAccessFile(path, "r");
	}

	public String find() throws IOException {
		SubversionReader reader = new SubversionReader(dumpFile);

		// Calculate starting position
		position = reader.length();
		next = position - ReadBlockSize;
		next = (next < 0) ? 0 : next;

		do {
			position = next;
			rf.seek(position);
			long readSize = reader.length() - position;
			if (readSize > ReadBlockSize)
				readSize = ReadBlockSize;
			byte[] b = new byte[(int) readSize]; // TODO: ReadBlockSize can read
													// past end of file
			rf.readFully(b);
			String s = new String(b);
			String pattern = "Revision-number: ";
			int beginIndex = s.lastIndexOf(pattern);
			if (beginIndex > 0) {
				int endIndex = s.indexOf('\n', beginIndex);
				int offset = beginIndex + pattern.length();
				String line = s.substring(offset, endIndex);
				line = line.replace("\r", "");
				return line;
			}

			next -= ReadBlockOffset;
			next = (next < 0) ? 0 : next;
		} while (position > 0);

		return null;
	}

	public void close() throws IOException {
		rf.close();
	}

}
