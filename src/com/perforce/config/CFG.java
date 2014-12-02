package com.perforce.config;

import com.perforce.common.asset.ContentType;

public enum CFG {

	TEST("core.test", Boolean.class),
	VERSION("core.version", String.class),
	SCHEMA("core.schema", String.class),
	SCM_TYPE("core.scmType", ScmType.class),

	// General audit and logging
	AUDIT_ENABLED("log.audit.enabled", Boolean.class),
	AUDIT_FILE("log.audit.filename", String.class),
	CHANGE_MAP("log.changeMap", String.class),

	// Perforce connection
	P4_MODE("p4.mode", String.class),
	P4_ROOT("p4.root", String.class),
	P4_PORT("p4.port", String.class),
	P4_USER("p4.user", String.class),
	P4_PASSWD("p4.passwd", String.class),
	P4_CLIENT("p4.client", String.class),
	P4_CLIENT_ROOT("p4.clientRoot", String.class),
	P4_UNICODE("p4.unicode", Boolean.class),
	P4_CHARSET("p4.charset", String.class),
	P4_DEPOT_PATH("p4.depotPath", String.class),
	P4_DEPOT_SUB("p4.subPath", String.class),
	P4_JNL_PREFIX("p4.jnlPrefix", String.class),
	P4_JNL_INDEX("p4.jnlIndex", Integer.class),
	P4_LOG_ID("p4.logRevID", String.class),
	P4_OFFSET("p4.offset", Long.class),
	P4_CASE("p4.caseMode", CaseSensitivity.class),
	P4_LINEEND("p4.lineEnding", Boolean.class),
	P4_C1_MODE("p4.lowerCase", Boolean.class),
	P4_SKIP_EMPTY("p4.skipEmpty", Boolean.class),
	P4_NORMALISATION("p4.normalisation", Normaliser.class),
	P4_DOWNGRADE("p4.downgrade", Boolean.class),

	// Subversion specific modes
	SVN_DUMPFILE("svn.dumpFile", String.class),
	SVN_START("svn.start", Long.class),
	SVN_END("svn.end", Long.class),
	SVN_PROP_NAME("svn.propName", String.class),
	SVN_PROP_ENCODE("svn.propEncoding", String.class),
	SVN_PROP_ENABLED("svn.propEnabled", Boolean.class),
	SVN_PROP_TYPE("svn.propTextType", ContentType.class),
	SVN_DIR_NAME("svn.emptyDirName", String.class),
	SVN_DIR_ENABLED("svn.emptyDirEnabled", Boolean.class),
	SVN_KEEP_KEYWORD("svn.keepKeyword", Boolean.class),
	SVN_MERGEINFO("svn.mergeInfoEnabled", Boolean.class),
	SVN_LABELS("svn.labels", Boolean.class),
	SVN_LABEL_DEPTH("svn.labelDepth", Integer.class),
	SVN_LABEL_FORMAT("svn.labelFormat", String.class),

	// CVS specific modes
	CVS_ROOT("cvs.cvsroot", String.class),
	CVS_MODULE("cvs.cvsmodule", String.class),
	CVS_WINDOW("cvs.timeWindow", Long.class),
	CVS_TMPDIR("cvs.tmpDir", String.class),
	CVS_LABELS("cvs.labels", Boolean.class),

	// Hidden properties
	EXCLUDE_MAP("hidden.excludeMap", String.class),
	INCLUDE_MAP("hidden.includeMap", String.class),
	ISSUE_MAP("hidden.issueMap", String.class),
	USER_MAP("hidden.userMap", String.class),
	TYPE_MAP("hidden.typeMap", String.class),
	P4_LARGE_FILE("hidden.largeFile", Long.class),
	CVS_MAXLINE("hidden.maxLineBuffer", Long.class);

	final private String base = "com.p4convert.";
	final private String id;
	final private Class<?> type;

	CFG(String s, Class<?> t) {
		id = base + s;
		type = t;
	}

	public static CFG parse(String property) {
		if (property != null) {
			for (CFG c : CFG.values()) {
				if (property.equalsIgnoreCase(c.toString())) {
					return c;
				}
			}
		}
		return null;
	}

	public String toString() {
		return id;
	}

	public Class<?> getType() {
		return type;
	}

}
