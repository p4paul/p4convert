package com.perforce.svn.process;

public class MergeRange {
	private long start = 0;
	private long end = 0;

	public MergeRange(long s, long e) {
		start = s;
		end = e;
	}

	public long getRevStart() {
		return start;
	}

	public long getRevEnd() {
		return end;
	}

	public String toString() {
		return "range[" + start + "-" + end + "]";
	}
}
