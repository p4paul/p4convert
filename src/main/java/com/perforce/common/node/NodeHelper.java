package com.perforce.common.node;

public class NodeHelper {
	/**
	 * Used to re-map the source and target path for each file when branching
	 * directories. Compares paths in lower case, BUT returns in the original
	 * case formatting
	 * 
	 * @param from
	 * @param to
	 * @param path
	 * @return
	 */
	public static String remap(String from, String to, String path) {
		if (from == null || from.isEmpty()) {
			return to + "/" + path;
		} else if (path.toLowerCase().startsWith(from.toLowerCase())) {
			return to + path.substring(from.length());
		} else {
			return null;
		}
	}
}
