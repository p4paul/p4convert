package com.perforce.cvs;

import java.util.HashMap;
import java.util.Map.Entry;

public class BranchSorter {

	private HashMap<String, Integer> sort = new HashMap<String, Integer>();

	/**
	 * Joins the HashMaps and updates the highest count of revisions for that
	 * branch.
	 * 
	 * @param brCount
	 */
	public void join(HashMap<String, Integer> brCount) {

		for (Entry<String, Integer> br : brCount.entrySet()) {
			if (br.getValue() > 0) {
				int count = br.getValue();
				String name = br.getKey();

				if (sort.containsKey(name)) {
					int v = sort.get(name);
					if (v > count) {
						count = v;
					}
				}
				sort.put(name, count);
			}
		}
	}
	
	public boolean isBranch(String name) {
		if(sort.containsKey(name)) {
			if(sort.get(name) > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Entry<String, Integer> b : sort.entrySet()) {
			sb.append("   branch: ");
			sb.append(b.getKey());
			sb.append(" (");
			sb.append(b.getValue());
			sb.append(")\n");
		}
		return sb.toString();
	}
}
