package com.perforce.common;

public class OutOfOrderException extends Exception {

	private static final long serialVersionUID = 1L;

	public OutOfOrderException() {
		super();
	}

	public OutOfOrderException(String message) {
		super(message);
	}

	public OutOfOrderException(String message, Throwable cause) {
		super(message, cause);
	}

	public OutOfOrderException(Throwable cause) {
		super(cause);
	}
}
