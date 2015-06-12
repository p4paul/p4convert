package com.perforce.cvs.parser.rcstypes;

import com.perforce.cvs.parser.RcsSchema;

public class RcsObjectAdmin extends RcsObject {

	public RcsObjectNum getHead() {
		return (RcsObjectNum) get(RcsSchema.HEAD);
	}

	public RcsObjectNum getBranch() {
		return (RcsObjectNum) get(RcsSchema.BRANCH);
	}

	public String getAccess() {
		return (String) get(RcsSchema.ACCESS);
	}

	public RcsObjectTagList getSymbols() {
		return (RcsObjectTagList) get(RcsSchema.SYMBOLS);
	}

	public RcsObjectTag getLocks() {
		return (RcsObjectTag) get(RcsSchema.LOCKS);
	}

	public Boolean getStrict() {
		return (Boolean) get(RcsSchema.STRICT);
	}

	public String getComment() {
		return (String) get(RcsSchema.COMMENT);
	}

	public String getExpand() {
		return (String) get(RcsSchema.EXPAND);
	}
}
