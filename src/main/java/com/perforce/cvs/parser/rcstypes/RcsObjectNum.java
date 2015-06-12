package com.perforce.cvs.parser.rcstypes;

import java.util.ArrayList;
import java.util.List;

public class RcsObjectNum implements Comparable<RcsObjectNum> {

	private List<Integer> ver = null;

	public RcsObjectNum(String str) {
		if (str.isEmpty())
			return;

		ver = new ArrayList<Integer>();
		String[] parts = str.split("\\.");
		for (String s : parts) {
			ver.add(Integer.parseInt(s));
		}
	}

	public RcsObjectNum(List<Integer> list) {
		ver = list;
	}

	public List<Integer> getValues() {
		return ver;
	}

	public RcsObjectNum subtract(RcsObjectNum from) {
		List<Integer> fromList = from.getValues();
		List<Integer> remainder = new ArrayList<Integer>();

		int pos = 0;
		boolean match = true;
		int matchCount = 0;
		for (int v : ver) {
			if (pos < fromList.size() && fromList.get(pos) == v) {
				if (match) {
					matchCount++;
				}
			} else {
				match = false;
				remainder.add(ver.get(pos));
			}
			pos++;
		}

		if (matchCount == fromList.size()) {
			return new RcsObjectNum(remainder);
		} else {
			return null;
		}
	}

	public String toString() {
		if (ver == null)
			return "null-string";

		StringBuffer sb = new StringBuffer();
		for (Integer i : ver) {
			if (sb.length() != 0) {
				sb.append(".");
			}
			sb.append(i);
		}
		return sb.toString();
	}

	public List<Integer> getVer() {
		return ver;
	}

	public int getMinor() {
		int last = ver.get(ver.size() - 1);
		return last;
	}

	@Override
	public int compareTo(RcsObjectNum obj) {
		final int EQUAL = 0;
		final int NOT = 1;

		if (obj.getVer().equals(ver)) {
			return EQUAL;
		}
		return NOT;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RcsObjectNum))
			return false;

		if (compareTo((RcsObjectNum) obj) == 0)
			return true;

		return false;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ver == null) ? 0 : ver.hashCode());
		return result;
	}
}
