package com.perforce.common.asset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScanContentStream {

	private Logger logger = LoggerFactory.getLogger(ScanContentStream.class);

	private boolean text = true;
	private boolean active = true;

	public void read(byte[] bytes, int len) {
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

	public boolean isText() {
		return text;
	}

	public void setFilter(boolean active) {
		this.active = active;
		if (logger.isDebugEnabled())
			logger.debug("scanning for vaild ASCII: " + active);
	}
}
