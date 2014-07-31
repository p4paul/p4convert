package com.perforce.common.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Table {
	private List<TableVersion> versions = new ArrayList<TableVersion>();
	private List<Index> indeces = new ArrayList<Index>();
	
	private String name;
	
	public Table(String name) {
		this.name = name;
	}
	
	public void addVersion(TableVersion version) {
		versions.add(version);
	}
	
	public TableVersion getVersion(int version) {
		return versions.get(version);
	}
	
	public String getName() {
		return name;
	}
	
	public void addIndex(String name, boolean ascending) {
		Index index = new Index(name, ascending);
		
		verifyIndex(index);
		indeces.add(index);
	}
	
	public List<Index> getIndeces() {
		return Collections.unmodifiableList(indeces);
	}
	
	// Paranoia method - verify that the index tags an attribute
	// that exists in all TableVersions
	
	private void verifyIndex(Index index) {
		for (TableVersion tableVersion : versions) {
			Attribute attr = tableVersion.getAttributeByName(index.getName());
			if (attr == null) {
				throw new IllegalArgumentException("verifyIndex failed: " 
						+ index + " not found in " + tableVersion);
			}
		}
	}

	public String toString() {
		StringBuffer buf = new StringBuffer("Table : " + name);
		
		for (TableVersion version : versions) {
			buf.append('\n');
			buf.append(version);
		}
		
		return buf.toString();
	}
}
