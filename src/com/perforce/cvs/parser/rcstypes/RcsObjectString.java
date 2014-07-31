package com.perforce.cvs.parser.rcstypes;

public class RcsObjectString {

	private StringBuffer msg = new StringBuffer();

	public RcsObjectString(String str) {
		if (str.startsWith("@") && str.endsWith("@")) {
			str = str.substring(1, str.length() - 1);
			str = str.replaceAll("@@", "@");
			msg.append(str);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("@");
		String str = msg.toString();
		str = str.replaceAll("@", "@@");
		sb.append(str);
		sb.append("@");
		return sb.toString();
	}
	
	public String getString() {
		return msg.toString();
	}
}
