package com.perforce.common.asset;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ContentStream extends InputStream {

	private Logger logger = LoggerFactory.getLogger(ContentStream.class);
	
	private boolean text = true;
	private boolean active = true;

	public boolean removeBOM() throws IOException {
		logger.error("common.ContentStream.removeBOM() should be extended");
		throw new RuntimeException();
	}
	
	public void scanText(byte[] bytes, int len) {
		if (!active)
			return;

		// test for ascii only when scanning content
		for (int i = 0; i < len; i++) {
			Byte b = bytes[i];
			if (b == (byte) 0x09) // TAB key
				continue;
			if (b == (byte) 0x0A) // LF \n
				continue;
			if (b == (byte) 0x0D) // CR \r
				continue;
			if (b.compareTo((byte) 0x20) < 0) {
				text = false; // lower limit
				continue;
			}
			if (b.compareTo((byte) 0x7E) > 0) {
				text = false; // upper limit
				continue;
			}
		}
	}
	
	public void setFilter(boolean active) {
		this.active = active;
		if (logger.isTraceEnabled())
			logger.trace("scanning for vaild ASCII: " + active);
	}
	
	public boolean isText() {
		return text;
	}

}
