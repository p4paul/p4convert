package com.perforce.svn.parser;

public class Node implements Record {
	private Header header;
	private Property property;
	private Content content;

	private int svnRevision = 0;
	private int nodeNumber = 0;
	private boolean subBlock = false;

	private long propLength;
	private long contentLength;

	public Node(String line, SubversionReader dump) throws Exception {
		// Read Header
		header = Header.readHeader(line, dump);

		// Read Properties if set
		propLength = header.findLong("Prop-content-length");
		if (propLength > 0) {
			property = Property.readProperty(dump, propLength);
		}

		// Read Content if set
		if (header.hasKey("Text-content-length")) {
			contentLength = header.findLong("Text-content-length");
			content = new Content(dump, contentLength);
		} else {
			content = new Content();
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(header);
		if (property != null) {
			sb.append(property);
			if (property.isEmpty() && (contentLength <= 0))
				sb.append("\n");
		}
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
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	@Override
	public Type getType() {
		return Type.NODE;
	}

	public int getSvnRevision() {
		return svnRevision;
	}

	public void setSvnRevision(int svnRevision) {
		this.svnRevision = svnRevision;
	}

	public int getNodeNumber() {
		return nodeNumber;
	}

	public void setNodeNumber(int nodeNumber) {
		this.nodeNumber = nodeNumber;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	public boolean isSubBlock() {
		return subBlock;
	}

	public void setSubBlock(boolean state) {
		subBlock = state;
	}

}
