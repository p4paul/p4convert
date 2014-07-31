package com.perforce.svn.parser;

public class ParserException extends Exception {

	public ParserException(String string) {
		System.err.println(string + "\n");
	}

	private static final long serialVersionUID = 1L;
}
