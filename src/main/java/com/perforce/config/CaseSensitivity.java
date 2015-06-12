package com.perforce.config;

public enum CaseSensitivity {
	NONE(0), FIRST(1), LOWER(2), UPPER(3);

	final int id;

	CaseSensitivity(int i) {
		id = i;
	}

	public int getValue() {
		return id;
	}

	public static Object parse(String property) {
		if (property != null) {
			for (CaseSensitivity b : CaseSensitivity.values()) {
				if (property.equalsIgnoreCase(b.name())) {
					return b;
				}
			}
		}
		return null;
	}
}
