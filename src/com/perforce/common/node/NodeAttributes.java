package com.perforce.common.node;

import com.perforce.svn.parser.Property;

public class NodeAttributes {

	private Property property;
	private String path;
	private int rev;
	private long change;

	public NodeAttributes(Property p) {
		property = p;
	}

	public void setHeader(String p, long c, int r) {
		path = p;
		rev = r;
		change = c;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();

		sb.append("# Properties File: " + path);
		sb.append("#" + rev + "\n");
		sb.append("# SVN revision: " + change + "\n");

		for (String key : property.getKeySet()) {
			sb.append("[" + key + "]\n");
			sb.append(property.findString(key) + "\n");
		}

		return sb.toString();
	}
}
