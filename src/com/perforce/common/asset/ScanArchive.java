package com.perforce.common.asset;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.svn.parser.Content;

public class ScanArchive {

	private static Logger logger = LoggerFactory.getLogger(ScanArchive.class);

	/**
	 * Unicode file type detection by scanning content in blocks.
	 * 
	 * @param content
	 * @return
	 * @throws IOException
	 */
	public static ContentType detectType(Content content) throws Exception {
		ContentType type = ContentType.UNKNOWN;

		// ICU4J charsetDetector to find all matches
		ContentStream contentStream = ContentStreamFactory.scanContentStream(
				content, 1048576L);
		BufferedInputStream bufContent = new BufferedInputStream(
				(InputStream) contentStream);
		CharsetDetector charsetDetector = new CharsetDetector();
		charsetDetector.setText(bufContent);
		CharsetMatch cm;
		try {
			cm = charsetDetector.detect();
		} catch (ArrayIndexOutOfBoundsException e) {
			cm = null;
		}

		// Set confidence (smaller data sets are harder to spot)
		int minConfidenceLevel = 30;
		if (content.getLength() < 128)
			minConfidenceLevel = 8;

		// Get detected types
		int confidence = 0;
		if (cm != null) {
			confidence = cm.getConfidence();

			if (confidence > minConfidenceLevel) {
				type = ContentType.parse(cm.getName());

				// If translation is disabled, use RAW for unicode files.
				if (!(Boolean) Config.get(CFG.P4_TRANSLATE)) {
					switch (type) {
					case UTF_16LE:
					case UTF_16BE:
					case UTF_32LE:
					case UTF_32BE:
						break;

					default:
						type = ContentType.P4_RAW;
						break;
					}
				}
			} else {
				type = ContentType.P4_BINARY;
			}

			if (logger.isTraceEnabled()) {
				logger.trace("icu4j detected:" + cm.getName() + " conf%:"
						+ confidence);
			}
		} else {
			// for unknown or unparseable types
			type = ContentType.P4_BINARY;
		}

		// Check for plain text
		if (contentStream.isText()) {
			type = ContentType.P4_TEXT;
		}

		content.setDetectedType(type);

		if (logger.isTraceEnabled()) {
			logger.trace("setDetectedType:" + type + " (p4type:"
					+ type.getP4Type().toString() + " isText:"
					+ contentStream.isText() + ")");
		}

		// Clean up and return type
		contentStream.close();
		return type;
	}
}
