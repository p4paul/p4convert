package com.perforce.cvs.parser.rcstypes;

import java.util.ArrayList;
import java.util.List;

public class RcsObjectNumList {
	private List<RcsObjectNum> list = new ArrayList<RcsObjectNum>();

	public RcsObjectNumList(String str) {
		for (String n : str.split("\\s+")) {
			if (n.contains(".")) {
				RcsObjectNum num = new RcsObjectNum(n);
				list.add(num);
			}
		}
	}

	public List<RcsObjectNum> getList() {
		return list;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (RcsObjectNum t : list) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append(t.toString());
		}
		return sb.toString();
	}
}
