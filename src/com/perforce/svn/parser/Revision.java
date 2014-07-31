package com.perforce.svn.parser;

//public class Revision extends AbstractRecord {
public class Revision implements Record {
	public Header header;
	public Property property;

	private int changeNumber = 0;

	public Revision(String line, SubversionReader dump) throws Exception {
		// Read Header
		header = Header.readHeader(line, dump);

		// Read Properties if set
		long length = header.findLong("Prop-content-length");
		if (length > 0) {
			property = Property.readProperty(dump, length);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(header);
		sb.append(property);
		return sb.toString();
	}

	public long findHeaderLong(String key) {
		return header.findLong(key);
	}

	public String findHeaderString(String key) {
		return header.findString(key);
	}

	public long findPropertyLong(String key) {
		return property.findLong(key);
	}

	public String findPropertyString(String key) {
		return property.findString(key);
	}

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public Content getContent() {
		return null;
	}

	@Override
	public Type getType() {
		return Type.REVISION;
	}

	public int getSvnRevision() {
		return changeNumber;
	}

	public void setChangeNumber(int currentChangeNumber) {
		this.changeNumber = currentChangeNumber;
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
