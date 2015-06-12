package com.perforce.cvs.parser.rcstypes;

import java.util.ArrayList;
import java.util.List;

public class RcsObjectTagList {
	
	private List<RcsObjectTag> list = new ArrayList<RcsObjectTag>();
	
	public RcsObjectTagList(String str) {
		for(String s : str.split("\\s+")) {
			if(s.contains(":")) {
				RcsObjectTag tag = new RcsObjectTag(s);
				list.add(tag);
			}
		}
	}
	
	public List<RcsObjectTag> getList() {
		return list;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for(RcsObjectTag t : list) {
			if(sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(t.toString());
		}
		return sb.toString();
	}
}
