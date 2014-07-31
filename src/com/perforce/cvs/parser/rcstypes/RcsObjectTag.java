package com.perforce.cvs.parser.rcstypes;

public class RcsObjectTag {

	private String tag;

	private RcsObjectNum id;

	public RcsObjectTag(String str) {
		if(str.isEmpty()) {
			return;
		}
		str = str.replace(";", "");		
		if (!str.contains(":")) {
			tag = str;
			return;
		}
			
		tag = str.substring(0, str.indexOf(":"));
		String num = str.substring(str.indexOf(":") + 1);
		id = new RcsObjectNum(num);
	}

	public String getTag() {
		return tag;
	}

	public RcsObjectNum getId() {
		return id;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(tag);
		sb.append(":");
		sb.append(id);
		return sb.toString();
	}
}
