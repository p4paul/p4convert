package com.perforce.cvs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RevisionSorter {

	private int index = 0;
	private final boolean remainder;
	private List<RevisionEntry> list = new ArrayList<RevisionEntry>();

	public RevisionSorter(boolean b) {
		remainder = b;
	}

	public void add(RevisionEntry entry) {
		list.add(entry);
	}

	public void sort() {
		Collections.sort((List<RevisionEntry>) list);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (RevisionEntry c : list) {
			sb.append(c.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public RevisionEntry next() {
		if (hasNext()) {
			RevisionEntry entry = list.get(index);
			index++;
			return entry;
		}
		return null;
	}

	public boolean hasNext() {
		return (index < list.size());
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public void drop(RevisionEntry entry) {
		list.remove(entry);
		if (index > 0) {
			index--;
		}
	}

	public void reset() {
		index = 0;
	}

	public boolean isRemainder() {
		return remainder;
	}
}
