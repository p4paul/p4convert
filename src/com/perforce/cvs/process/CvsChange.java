package com.perforce.cvs.process;

import java.util.ArrayList;
import java.util.List;

import com.perforce.cvs.RevisionEntry;

public class CvsChange {

	private final long change;
	private ArrayList<RevisionEntry> list = new ArrayList<RevisionEntry>();

	public CvsChange(long change) {
		this.change = change;
	}

	public void addEntry(RevisionEntry rev) {
		list.add(rev);
	}

	public boolean isPending(RevisionEntry rev) {
		for (RevisionEntry l : list) {
			String listPath = l.getPath();
			String revPath = rev.getPath();
			if (listPath.equalsIgnoreCase(revPath)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isEmpty() {
		return list.isEmpty();
	}

	public long getChange() {
		return change;
	}

	public List<RevisionEntry> getRevisions() {
		return list;
	}

	public RevisionEntry getChangeInfo() {
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Change: " + change);
		sb.append(" [" + list.size() + "]");
		return sb.toString();
	}
}
