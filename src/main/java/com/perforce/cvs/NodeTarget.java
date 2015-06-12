package com.perforce.cvs;

public class NodeTarget {

	private final String name;
	private final String from;
	private final boolean reverse;

	public NodeTarget(String name, String from, boolean reverse) {
		this.name = name;
		this.from = from;
		this.reverse = reverse;
	}

	public String getName() {
		return name;
	}

	public String getFrom() {
		return from;
	}

	public boolean isReverse() {
		return reverse;
	}
}
