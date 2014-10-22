package com.perforce.common.label;

public class TagConvert {

	private int revision;
	private String path;

	public TagConvert(String path, int revision) {
		this.revision = revision;
		this.path = path;
	}

	public int getRevision() {
		return revision;
	}

	public String getPath() {
		return path;
	}

	public String toString() {
		if (revision > 0) {
			return path + "#" + revision;
		} else {
			return path;
		}
	}
}
