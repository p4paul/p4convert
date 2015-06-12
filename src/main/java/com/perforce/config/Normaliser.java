package com.perforce.config;

import java.text.Normalizer.Form;

public enum Normaliser {
	NFC(0),
	NFD(1),
	NFKC(2),
	NFKD(3);

	final int id;

	Normaliser(int i) {
		id = i;
	}

	/**
	 * This is a special wrapper around Normalizer.Form as this is missing a
	 * parse method.
	 * 
	 * @param property
	 * @return
	 */
	public static Object parse(String property) {
		if (property != null) {
			for (Form f : Form.values()) {
				if (property.equalsIgnoreCase(f.name())) {
					return f;
				}
			}
		}
		return null;
	}
}
