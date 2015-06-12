package com.perforce.svn.parser;

public interface Record {

	public enum Type {
		REVISION,
		NODE,
		SCHEMA
	};

	public Header getHeader();

	public Property getProperty();

	public Content getContent();

	public long findHeaderLong(String key);

	public long findPropertyLong(String key);

	public String findHeaderString(String key);

	public String findPropertyString(String key);

	public boolean isEmpty();

	public Type getType();

	public int getSvnRevision();

	public int getNodeNumber();

	public boolean isSubBlock();
}
