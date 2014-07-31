package com.perforce.svn.parser;

//public class Schema extends AbstractRecord {
public class Schema implements Record {
	public Header header;

	public Schema(String line, SubversionReader dump) throws RuntimeException,
			ParserException {
		header = Header.readHeader(line, dump);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(header);
		return sb.toString();
	}

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	public Property getProperty() {
		return null;
	}

	public Content getContent() {
		return null;
	}

	public long findHeaderLong(String key) {
		return header.findLong(key);
	}

	public String findHeaderString(String key) {
		return header.findString(key);
	}

	public long findPropertyLong(String key) {
		return Long.MIN_VALUE;
	}

	public String findPropertyString(String key) {
		return null;
	}

	@Override
	public Type getType() {
		return Type.SCHEMA;
	}

	@Override
	public int getSvnRevision() {
		return 0;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean isSubBlock() {
		return false;
	}

	@Override
	public int getNodeNumber() {
		return 0;
	}
}
