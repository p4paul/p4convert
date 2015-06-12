package com.perforce.common.schema;

public class Attribute {
	private String name;
	private Domain domain;
	
	public Attribute(String name, Domain domain) {
		this.name = name;
		this.domain = domain;
	}

	public Domain getDomain() {
		return domain;
	}

	public String getName() {
		return name;
	}
	
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		else {
			if (o.getClass() == getClass()) {
				Attribute other = (Attribute) o;
				return (other.name.equals(name));
			}
		}
		
		return false;
	}
	
	public String toString() {
		return name + "(" + domain + ")";
	}
}
