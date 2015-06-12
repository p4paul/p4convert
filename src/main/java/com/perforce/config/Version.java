package com.perforce.config;

public class Version {

	private String version;
	
	public Version() {
		Package p = this.getClass().getPackage();
		version = p.getSpecificationVersion();
		if(version == null)
			version = "UNSET";
	}
	
	public String getVersion() {
		return version;
	}
}
