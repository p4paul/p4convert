package com.perforce.cvs.history;

import java.util.HashMap;

public class RevisionHead {

	private HashMap<String, Integer> revMap = new HashMap<String, Integer>();

	/**
	 * Calculates Perforce revision numbers on revision Paths. Only stores HEAD
	 * revisions at the point of chronological conversion.
	 * 
	 * @param revPath
	 */
	public void addRev(String revPath) {
		Integer revision = 1;
		if (revMap.containsKey(revPath)) {
			revision = revMap.get(revPath) + 1;
		}
		revMap.put(revPath, revision);
	}

	public int getRev(String path) {
		return revMap.get(path);
	}
}
