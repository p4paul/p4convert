package com.perforce.svn.tag;

public class TagEntry {

	private final String id;

	private String toPath;
	private String fromPath;
	private long fromChange;
	private TagType type = TagType.UNKNOWN;
	
	
	private int branchCount;

	public TagEntry(String id) {
		this.id = id;
		branchCount = 1;
	}

	public void increment() {
		branchCount++;
	}

	public String getId() {
		return id;
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

	public String getToPath() {
		return toPath;
	}

	public void setToPath(String toPath) {
		this.toPath = toPath;
	}

	public String getFromPath() {
		return fromPath;
	}

	public void setFromPath(String fromPath) {
		this.fromPath = fromPath;
	}

	public long getFromChange() {
		return fromChange;
	}

	public void setFromChange(long fromChange) {
		this.fromChange = fromChange;
	}
}
