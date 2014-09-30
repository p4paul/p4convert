package com.perforce.cvs.asset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.asset.ContentStream;
import com.perforce.common.asset.ScanContentStream;
import com.perforce.svn.parser.Content;

public class CvsContentStream extends ContentStream {

	private Logger logger = LoggerFactory.getLogger(CvsContentStream.class);

	private ScanContentStream scan = new ScanContentStream();

	private Iterator<ByteArrayOutputStream> blockIterator;
	private byte[] remainder;

	private Iterator<ByteArrayOutputStream> markIterator;
	private byte[] markRemainder;

	public CvsContentStream(Content content) {
		this.blockIterator = content.getBlock().iterator();
		setFilter(true);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int count = 0;

		if (blockIterator.hasNext()) {
			ByteArrayOutputStream bs = new ByteArrayOutputStream();

			do {
				ByteArrayOutputStream line = blockIterator.next();
				byte[] bytes = line.toByteArray();
				if (count + bytes.length > len) {
					int part = len - count;
					bs.write(bytes, 0, part);
					count += part;

					int left = bytes.length - part;
					remainder = new byte[left];
					System.arraycopy(bytes, part, remainder, 0, left);
				} else {
					bs.write(bytes);
					count += bytes.length;
					remainder = null;
				}
			} while (blockIterator.hasNext() && remainder != null);

			System.arraycopy(bs.toByteArray(), 0, b, 0, count);
			bs.close();
		} else {
			return -1;
		}

		scan.read(b, count);
		return count;
	}

	@Override
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException {
		throw new IOException("Byte read not supported");
	}

	@Override
	public void mark(int readlimit) {
		this.markIterator = this.blockIterator;
		this.markRemainder = this.remainder;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public void reset() {
		this.blockIterator = this.markIterator;
		this.remainder = this.markRemainder;
	}

	@Override
	public void close() throws IOException {
		// TODO nothing?
	}

	@Override
	public boolean removeBOM() {
		logger.warn("NOT IMPLEMENTED YET!");

		return true;
	}

	@Override
	public boolean isText() {
		return scan.isText();
	}
}
