package com.perforce.svn.process;

public class MergePoint implements Comparable<MergePoint> {

	private String mergePath;
	private long revStart;
	private long revEnd;

	public MergePoint(String merge, MergeRange range) {
		this.mergePath = merge;
		this.revStart = range.getRevStart();
		this.revEnd = range.getRevEnd();
	}

	public String getMergePath() {
		return mergePath;
	}

	public long getRevStart() {
		return revStart;
	}

	public long getRevEnd() {
		return revEnd;
	}

	public String toString() {
		String str = mergePath + ":" + revStart + "-" + revEnd;
		return str;
	}

	@Override
	public int compareTo(MergePoint point) {
		final int EQUAL = 0;
		final int NOT = 1;

		if (!point.getMergePath().contentEquals(mergePath))
			return NOT;
		if (point.getRevStart() != revStart)
			return NOT;
		if (point.getRevEnd() != revEnd)
			return NOT;

		return EQUAL;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MergePoint))
			return false;

		if (compareTo((MergePoint) obj) == 0)
			return true;

		return false;
	}
}
