package com.perforce.config;

public class ConfigException extends Exception {

	public ConfigException(String string, Exception e) {
		System.err.println(string + "\n");
		e.printStackTrace();
	}

	public ConfigException(String string) {
		try {
			throw new Exception(string);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final long serialVersionUID = 1L;

}
