package com.perforce.config;

import java.util.Scanner;

public class Version {

	private String version;

	public Version() {
		Package p = this.getClass().getPackage();
		version = p.getSpecificationVersion();
		if (version == null)
			version = "UNSET";
	}

	public String getVersion() {
		return version;
	}

	public static int compair(String ver1, String ver2) {
		Scanner s1 = new Scanner(ver1);
		Scanner s2 = new Scanner(ver2);
		try {
			s1.useDelimiter("[\\._]");
			s2.useDelimiter("[\\._]");

			while (s1.hasNextInt() && s2.hasNextInt()) {
				int v1 = s1.nextInt();
				int v2 = s2.nextInt();
				if (v1 < v2) {
					return -1;
				} else if (v1 > v2) {
					return 1;
				}
			}

			if (s1.hasNextInt()) {
				// str1 has an additional lower-level version number
				return 1;
			}
			return 0;
		} finally {
			s1.close();
			s2.close();
		}
	}
}
