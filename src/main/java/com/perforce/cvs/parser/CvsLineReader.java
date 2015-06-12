package com.perforce.cvs.parser;

import com.perforce.common.parser.LineReader;

public class CvsLineReader extends LineReader {

	public CvsLineReader(String path) {
		open(path);
	}
	

}
