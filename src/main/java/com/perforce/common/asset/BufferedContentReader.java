package com.perforce.common.asset;

import java.io.IOException;
import java.io.Reader;
import java.nio.CharBuffer;

public class BufferedContentReader {

	private Reader in;
	private CharBuffer cbuf;
	private int blockSize = 8192;
	private EndOfLine eol = EndOfLine.NULL;

	public enum EndOfLine {
		WIN,
		UNIX,
		MAC,
		EOF,
		NULL;
	}

	public BufferedContentReader(Reader in) {
		this.in = in;

	}

	public EndOfLine getEOL() {
		return eol;
	}

	public String readLine() throws IOException {
		StringBuffer sb = new StringBuffer();
		eol = EndOfLine.NULL;
		boolean residual = false;

		while (eol == EndOfLine.NULL) {
			if (cbuf != null && cbuf.hasRemaining()) {
				char c = cbuf.get();

				// win or mac
				if (c == (char) '\r') {
					if (cbuf.hasRemaining()) {
						cbuf.mark();
						char next = cbuf.get();
						if (next == (char) '\n') {
							eol = EndOfLine.WIN;
						} else {
							eol = EndOfLine.MAC;
							cbuf.reset();
						}
					} else {
						// need to carry over if last char is '\r'
						residual = true;
					}
				} else if (c == (char) '\n') {
					eol = EndOfLine.UNIX;
				} else {
					sb.append(c);
				}
			}

			else {
				cbuf = CharBuffer.allocate(blockSize);

				// allocate residual '\r' to new buffer
				if (residual) {
					cbuf.append('\r');
				}

				int len = in.read(cbuf);
				cbuf.flip();

				if (len == -1) {
					if (sb.length() == 0) {
						return null;
					} else {
						// Reached end of file, but no line termination
						if (residual) {
							eol = EndOfLine.MAC;
						} else {
							eol = EndOfLine.EOF;
						}
					}
				}

				residual = false;
			}
		}

		return sb.toString();
	}
}
