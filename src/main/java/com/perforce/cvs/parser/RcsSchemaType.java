package com.perforce.cvs.parser;

import com.perforce.cvs.parser.rcstypes.RcsObjectBlock;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.cvs.parser.rcstypes.RcsObjectNumList;
import com.perforce.cvs.parser.rcstypes.RcsObjectTagList;

public enum RcsSchemaType {
	EMPTY(Boolean.class),
	NUM(RcsObjectNum.class),
	NUMS(RcsObjectNumList.class),
	// ID(String.class),
	TAGS(RcsObjectTagList.class),
	STRING(String.class),
	BLOCK(RcsObjectBlock.class);

	final private Class<?> type;

	RcsSchemaType(Class<?> t) {
		type = t;
	}

	public Class<?> getType() {
		return type;
	}
}
