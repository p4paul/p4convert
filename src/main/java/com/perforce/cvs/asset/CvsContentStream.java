package com.perforce.cvs.asset;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.asset.ContentStream;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.ScanContentStream;
import com.perforce.svn.parser.Content;

public class CvsContentStream extends ContentStream {

	private Logger logger = LoggerFactory.getLogger(CvsContentStream.class);

	private ScanContentStream scan = new ScanContentStream();

	private Iterator<ByteArrayOutputStream> blockIterator;
	private byte[] remainder;

	private Iterator<ByteArrayOutputStream> markIterator;
	private byte[] markRemainder;

	private ContentType contentType;

	public CvsContentStream(Content content) {
		this.contentType = content.getType();
		this.blockIterator = content.getBlock().iterator();
		setFilter(true);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int count = 0;
		while (remainder != null || blockIterator.hasNext()) {
			if (remainder != null) {
				byte[] bytes = remainder;
				count = into(bytes, b, len, count);
			} else {
				ByteArrayOutputStream line = blockIterator.next();
				byte[] bytes = line.toByteArray();
				count = into(bytes, b, len, count);
			}

			// keep adding to provided buffer until there is a remainder
			if (remainder != null) {
				scan.read(b, count);
				return count;
			}
		}

		// all done!
		if (count > 0) {
			// return what was read
			scan.read(b, count);
			return count;
		} else {
			// nothing read, return EOF
			return -1;
		}
	}

	private int into(byte[] from, byte[] to, int to_limit, int to_pos) {
		if (to_pos + from.length > to_limit) {
			// more bytes than limit, copy part
			int part = to_limit - to_pos;
			System.arraycopy(from, 0, to, to_pos, part);
			to_pos += part;

			// save leftover to remainder
			int left = from.length - part;
			remainder = new byte[left];
			System.arraycopy(from, part, remainder, 0, left);
		} else {
			// less bytes than limit, copy all
			System.arraycopy(from, 0, to, to_pos, from.length);
			to_pos += from.length;
			remainder = null;
		}
		return to_pos;
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

/*		byte[] bom = null;
		byte[] b = null;
		switch (contentType) {
		case UTF_8:
			bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			b = new byte[bom.length];
			break;

		case UTF_16LE:
			bom = new byte[] { (byte) 0xFF, (byte) 0xFE };
			b = new byte[bom.length];
			break;

		case UTF_16BE:
			bom = new byte[] { (byte) 0xFE, (byte) 0xFF };
			b = new byte[bom.length];
			break;

		default:
			return false;
		}

		// return to start position if no match
		try {
			mark(0);
			read(b);
			if (Arrays.equals(b, bom)) {
				return true;
			} else {
				reset();
				return false;
			}
		} catch (IOException e) {
			return false;
		}
		*/
		return true;
	}

	@Override
	public boolean isText() {
		return scan.isText();
	}
}
