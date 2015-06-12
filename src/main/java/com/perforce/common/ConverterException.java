package com.perforce.common;

public class ConverterException extends Exception {

	public ConverterException(String string, Exception e) {
		System.err.println(string + "\n");
		e.printStackTrace();
	}
	
	public ConverterException(String string) {
		try {
			throw new Exception(string);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static final long serialVersionUID = 1L;

}
