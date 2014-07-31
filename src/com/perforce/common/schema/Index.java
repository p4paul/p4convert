package com.perforce.common.schema;

// Index class holds one attribute and information about ascending/descending

public class Index {
	private String name;
	private boolean ascending;
	
	public Index(String name, boolean ascending) {
		this.name = name;
		this.ascending = ascending;
	}

	public boolean isAscending() {
		return ascending;
	}

	public String getName() {
		return name;
	}
	
	public String toString() {
		return "Index " + name + (ascending ? " ASC" : " DESC");
	}
	
}
