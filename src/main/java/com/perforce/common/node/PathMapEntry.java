package com.perforce.common.node;

public class PathMapEntry {

	private final String scmPath;
	private final String p4Path;

	public PathMapEntry(String scm, String p4) {
		this.scmPath = scm;
		this.p4Path = p4;
	}

	public String getScmPath() {
		return scmPath;
	}

	public String getP4Path() {
		return p4Path;
	}
}
