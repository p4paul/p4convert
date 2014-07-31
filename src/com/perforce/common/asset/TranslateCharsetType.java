package com.perforce.common.asset;

public enum TranslateCharsetType {

	TEXT("ltext", 0x00000001), // 'text+F' only ascii in sample
	BINARY("ubinary", 0x00000101), // 'binary+F'
	UTF8("unicode+F", 0x00080001), // 'unicode+F'
	UTF16("utf16+F", 0x01080001), // 'utf16+F'
	SYMLINK("symlink+F", 0x00040001), // 'symlink+F'
	UNKNOWN("ubinary", 0x00000101); // 'binary+F' Unknown types default to
									// binary

	final String type;
	final int id;

	TranslateCharsetType(String n, int i) {
		type = n;
		id = i;
	}

	public String getType() {
		return type;
	}

	public int getValue() {
		return id;
	}

	public String toString() {
		return this.name().toLowerCase();
	}
}
