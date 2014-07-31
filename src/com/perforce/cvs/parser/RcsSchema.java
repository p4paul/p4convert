package com.perforce.cvs.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum RcsSchema {

	UNKNOWN("unknown", RcsSchemaType.STRING),
	
	// 'admin' block [1:required]
	HEAD("head", RcsSchemaType.NUM),
	BRANCH("branch", RcsSchemaType.NUM),
	ACCESS("access", RcsSchemaType.STRING),
	SYMBOLS("symbols", RcsSchemaType.TAGS),
	LOCKS("locks", RcsSchemaType.TAGS),
	STRICT("strict", RcsSchemaType.EMPTY),
	COMMENT("comment", RcsSchemaType.STRING),
	EXPAND("expand", RcsSchemaType.STRING),

	// 'delta' block [n:optional]
	ID("id", RcsSchemaType.NUM),
	DATE("date", RcsSchemaType.STRING),
	AUTHOR("author", RcsSchemaType.STRING),
	STATE("state", RcsSchemaType.STRING),
	BRANCHES("branches", RcsSchemaType.NUMS),
	NEXT("next", RcsSchemaType.NUM),
	
	// extended
	COMMITID("commitid", RcsSchemaType.STRING),
	DELTATYPE("deltatype", RcsSchemaType.STRING),
	PERMISSIONS("permissions", RcsSchemaType.STRING),
	KOPT("kopt", RcsSchemaType.STRING),
	FILENAME("filename", RcsSchemaType.STRING),

	// 'desc' block [1:required]
	DESC("desc", RcsSchemaType.STRING),

	// 'deltatext' block [n:optional]
	LOG("log", RcsSchemaType.STRING),
	TEXT("text", RcsSchemaType.BLOCK);

	String name;
	final RcsSchemaType tag;

	RcsSchema(String n, RcsSchemaType t) {
		tag = t;
		name = n;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String n) {
		name = n;
	}

	public RcsSchemaType getTag() {
		return tag;
	}

	public static RcsSchema parse(String type) {
		if (type != null) {
			for (RcsSchema t : RcsSchema.values()) {
				if (type.equalsIgnoreCase(t.getName())) {
					return t;
				}
			}
		}
		
		Logger logger = LoggerFactory.getLogger(RcsSchema.class);
		logger.info("Unknown RCS type: " + type);
		RcsSchema unknown = RcsSchema.UNKNOWN;
		unknown.setName(type);
		return unknown;
	}

}
