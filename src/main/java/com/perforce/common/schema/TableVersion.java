package com.perforce.common.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class TableVersion implements Iterable<Attribute> {
	private int version;
	private Table table;
	private List<Attribute> attributes = new ArrayList<Attribute>();
	
	public TableVersion(final Table table, final int version) {
		this.table = table;
		this.version = version;
		
		table.addVersion(this);
	}

	public TableVersion(final TableVersion previous, final int version) {
		this.table = previous.table;
		this.version = version;
		
		attributes.addAll(previous.attributes);
		
		table.addVersion(this);
	}
	
	public int getVersion() {
		return version;
	}
	
	public Table getTable() {
		return table;
	}
	
	public List<Attribute> getAttributes() {
		return Collections.unmodifiableList(attributes);
	}
	
	public void addAttributes(TableVersion version) {
		attributes.addAll(version.attributes);
	}
	
	public void addAttribute(String name, Domain domain) {
		attributes.add(new Attribute(name, domain));
	}
	
	public void addAttribute(String name, Domain domain, int index) {
		attributes.add(index, new Attribute(name, domain));
	}
	
	public Attribute getAttribute(int index) {
		return attributes.get(index);
	}
	
	public Attribute getAttributeByName(String attrName) {
		for (Attribute attr : attributes) {
			if (attr.getName().equals(attrName)) {
				return attr;
			}
		}
		return null;
	}

	public void removeAttribute(String attrName) {
		Attribute attr = getAttributeByName(attrName);
		if (attr != null) {
			attributes.remove(attr);
		}
		else {
			throw new IllegalArgumentException("Remove Attribute " + attrName + ". Not found");
		}
	}
	
	public String toString() {
		StringBuffer buf = new StringBuffer("Version : " + version + "\n");
		for (Attribute attr : attributes) {
			buf.append("\t");
			buf.append(attr);
			buf.append("\n");
		}
		return buf.toString();
	}

	public Object toJournalString() {
		StringBuffer buf = new StringBuffer();
		
		buf.append(version);
		buf.append(" @");
		buf.append(table.getName());
		buf.append("@");
		
		return buf.toString();
	}

	public Iterator<Attribute> iterator() {
		return attributes.iterator();
	}
}
