package com.perforce.svn.tag;

public class TagEntry {

	private final String id;

	private TagType type = TagType.UNKNOWN;
	private int branchCount;

	public TagEntry(String id) {
		this.id = id;
		branchCount = 1;
	}

	public void increment() {
		branchCount++;
	}

	public int getCount() {
		return branchCount;
	}

	public TagType getType() {
		return type;
	}

	public void setType(TagType type) {
		this.type = type;
	}

	public String toString() {
		return id + ":" + type + " " + branchCount;
	}
}
