package com.perforce.svn.parser;

public enum NodeEntry {

	REVISION("Revision-"), //
	NODE("Node-"), //
	TEXT("Text-"), //
	CONTENT("Content-"), //
	PROP("Prop-"), //
	UNKNOWN("");

	private String token;

	private NodeEntry(String token) {
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public static NodeEntry parse(String token) {
		if (token != null) {
			for (NodeEntry e : NodeEntry.values()) {
				if (token.startsWith(e.getToken())) {
					return e;
				}
			}
		}
		return UNKNOWN;
	}
}
