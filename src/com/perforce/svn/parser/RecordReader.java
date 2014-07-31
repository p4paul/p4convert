package com.perforce.svn.parser;

import java.util.Iterator;

public class RecordReader implements Iterable<Record> {

	private String filename;

	public RecordReader(String filename) {
		this.filename = filename;
	}

	@Override
	public Iterator<Record> iterator() {
		return new SubversionReader(filename);
	}

}
