package com.perforce.common;

public enum ExitCode {

	OK(0),
	USAGE(1),
	SHUTDOWN(2),
	WARNING(3),
	EXCEPTION(4),
	UNKNOWN(5);

	final int code;

	ExitCode(int c) {
		code = c;
	}

	public int value() {
		return code;
	}

	public static ExitCode parse(int code) {
		for (ExitCode v : ExitCode.values()) {
			if (code == v.value()) {
				return v;
			}
		}
		return UNKNOWN;
	}
}
