package com.perforce.common.asset;

import java.util.List;

public enum ContentProperty {
	NONE		(0x00000000), // clear mask
	KEYWORD		(0x00000020), // '+k' keyword expansion
	EXECUTE		(0x00020000), // '+x' execute bit
	MODTIME		(0x00200000), // '+m' mod time bit
	WRITABLE	(0x00100000), // '+w' writable bit
	LOCK		(0x00000040), // '+l' lock bit
	FULL		(0x00000001), // '+F' full archive
	COMPRESS	(0x00000002); // '+C' gzip archive

	final int id;

	ContentProperty(int i) {
		id = i;
	}

	public int getValue() {
		return id;
	}

	public static int getSum(List<ContentProperty> props) {
		if (props == null)
			return 0;

		int value = 0;
		for (ContentProperty p : props) {
			value |= p.getValue();
		}
		return value;
	}

	public String toString() {
		switch (this) {
		case KEYWORD:
			return "k";
		case EXECUTE:
			return "x";
		case MODTIME:
			return "m";
		case WRITABLE:
			return "w";
		case LOCK:
			return "l";
		case FULL:
			return "F";
		case COMPRESS:
			return "C";
		default:
			return "?";
		}
	}
}
