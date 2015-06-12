package com.perforce.config;

public enum ScmType {
	P4, SVN, CVS;
	
	public static ScmType parse(String property) throws Exception {
		if (property != null) {
			for (ScmType t : ScmType.values()) {
				if (property.equalsIgnoreCase(t.name())) {
					return t;
				}
			}
		}
		throw new Exception("Unknown type: " + property);
	}
}
