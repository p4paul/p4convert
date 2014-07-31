package com.perforce.common.asset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public enum ContentType {

	// ICU4J content types
	US_ASCII("US-ASCII", TranslateCharsetType.TEXT),
	UTF_8("UTF-8", TranslateCharsetType.UTF8),
	UTF_16BE("UTF-16BE", TranslateCharsetType.UTF16),
	UTF_16LE("UTF-16LE", TranslateCharsetType.UTF16),
	UTF_32BE("UTF-32BE", TranslateCharsetType.UTF8),
	UTF_32LE("UTF-32LE", TranslateCharsetType.UTF8),
	Shift_JIS("Shift_JIS", TranslateCharsetType.UTF8),
	ISO_2022_JP("ISO-2022-JP", TranslateCharsetType.UNKNOWN),
	ISO_2022_CN("ISO-2022-CN", TranslateCharsetType.UNKNOWN),
	ISO_2022_KR("ISO-2022-KR", TranslateCharsetType.UNKNOWN),
	GB18030("GB18030", TranslateCharsetType.UTF8), // win cp936
													// (GB2312->GB18030)
	EUC_JP("EUC-JP", TranslateCharsetType.UTF8),
	EUC_KR("EUC-KR", TranslateCharsetType.UTF8), // Windows cp949
	Big5("Big5", TranslateCharsetType.UTF8), // Windows cp950
	ISO_8859_1("ISO-8859-1", TranslateCharsetType.UTF8),
	ISO_8859_2("ISO-8859-2", TranslateCharsetType.UTF8), // text??
	ISO_8859_5("ISO-8859-5", TranslateCharsetType.UTF8),
	ISO_8859_6("ISO-8859-6", TranslateCharsetType.UNKNOWN),
	ISO_8859_7("ISO-8859-7", TranslateCharsetType.UTF8),
	ISO_8859_8("ISO-8859-8", TranslateCharsetType.UTF8),
	windows_1251("windows-1251", TranslateCharsetType.UTF8), // Windows cp1251
	windows_1254("windows-1254", TranslateCharsetType.UTF8),
	windows_1252("windows-1252", TranslateCharsetType.UTF8),
	windows_1256("windows-1256", TranslateCharsetType.UNKNOWN),
	KOI8_R("KOI8-R", TranslateCharsetType.UTF8),
	ISO_8859_9("ISO-8859-9", TranslateCharsetType.UTF8),
	IBM424_rtl("IBM424_rtl", TranslateCharsetType.UNKNOWN),
	IBM424_ltr("IBM424_ltr", TranslateCharsetType.UNKNOWN),
	IBM420_rtl("IBM420_rtl", TranslateCharsetType.UNKNOWN),
	IBM420_ltr("IBM420_ltr", TranslateCharsetType.UNKNOWN),

	// Perforce content types
	P4_TEXT("UTF-8", TranslateCharsetType.TEXT),
	P4_UTF16("UTF-16LE", TranslateCharsetType.UTF16),
	P4_UNICODE("UTF-8", TranslateCharsetType.UTF8),
	P4_BINARY("BINARY", TranslateCharsetType.BINARY),

	// System types and unknown
	SYMLINK("SYMLINK", TranslateCharsetType.SYMLINK),
	UNKNOWN("UNKNOWN", TranslateCharsetType.UNKNOWN);

	final String name;
	final TranslateCharsetType type;

	/**
	 * name stores encoding name for local storage.
	 * 
	 * @param name
	 * @param type
	 */
	ContentType(String name, TranslateCharsetType type) {
		this.name = name;
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public TranslateCharsetType getP4Type() {
		return type;
	}

	public static ContentType parse(String type) {
		if (type != null) {
			for (ContentType t : ContentType.values()) {
				if (type.equalsIgnoreCase(t.getName())) {
					return t;
				}
				if (type.equalsIgnoreCase(t.name())) {
					return t;
				}
			}
		}

		Logger logger = LoggerFactory.getLogger(ContentType.class);
		// Unknown charsets (not listed above) treat as binary
		if (logger.isInfoEnabled()) {
			logger.info("Unknown Charset, using binary");
		}
		return ContentType.P4_BINARY;
	}

}
