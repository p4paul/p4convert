package com.perforce.cvs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevisionSorter {

	private static Logger logger = LoggerFactory
			.getLogger(RevisionSorter.class);

	private int index = 0;
	private long window = 0;
	private final boolean remainder;
	private List<RevisionEntry> list = new ArrayList<RevisionEntry>();

	public RevisionSorter(boolean b) {
		remainder = b;
	}

	public void add(RevisionEntry entry) {
		list.add(entry);
	}

	public void sort() {
		logger.info("Sorting revisions:");

		Collections.sort((List<RevisionEntry>) list);

		if (logger.isTraceEnabled()) {
			logger.trace(toString());
		}
		logger.info("... found " + size() + " revisions\n");
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

	public int size() {
		return list.size();
	}

	public boolean isRemainder() {
		return remainder;
	}

	public long getWindow() {
		return window;
	}

	public void setWindow(long window) {
		this.window = window;
	}
}
