package com.perforce.common.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.common.schema.JournalNote.NoteType;


public class Schema {
	private static Map<String, Table> tables = new HashMap<String, Table>(); 
	private static List<String> tableNamesInOrder = new ArrayList<String>();
	
	public static TableVersion GetTableVersion(String name, int version) {
		Table table = tables.get(name);
		if (table == null) {
			return null;
		}
		TableVersion tableVersion = table.getVersion(version);
		
		return tableVersion;
	}
	
	public static int GetTableIndex(String name) {
		return tableNamesInOrder.indexOf(name);
	}
	
	static {
		// list every table
		// every version
		// every attribute
		// then think of a better way 
		
		Table table;
		TableVersion version;
		
		// Counters
		
		// db.counters
		table = new Table("db.counters");
		version = new TableVersion(table, 0);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("value", Domain.INT);

		version = new TableVersion(version, 1);
		version.removeAttribute("value");
		version.addAttribute("value", Domain.TEXT);

		addTable(table);
		table.addIndex("name", true);
		
		// db.logger
		table = new Table("db.logger");
		version = new TableVersion(table, 0);
		version.addAttribute("seq", Domain.INT);
		version.addAttribute("key", Domain.KEY);
		version.addAttribute("attr", Domain.KEY);
		
		addTable(table);
		table.addIndex("seq", true);
		
		// Client data
		
		// db.user
		table = new Table("db.user");
		
		version = new TableVersion(table, 0);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("email", Domain.TEXT);
		version.addAttribute("ojobview", Domain.INT);
		version.addAttribute("update", Domain.DATE);
		version.addAttribute("access", Domain.DATE);
		version.addAttribute("fullname", Domain.TEXT);
		
		version = new TableVersion(version, 1);
		
		version = new TableVersion(version, 2);
		version.removeAttribute("ojobview");
		version.addAttribute("jobview", Domain.TEXT, 2);
		version.addAttribute("password", Domain.TEXT);
		
		version = new TableVersion(version, 3);
		version.addAttribute("strength", Domain.INT);
		version.addAttribute("ticket", Domain.TEXT);
		version.addAttribute("enddate", Domain.DATE);
		
		addTable(table);
		table.addIndex("user", true);
		
		// db.group
		table = new Table("db.group");
		version = new TableVersion(table, 0);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("group", Domain.KEY);
		
		version = new TableVersion(version, 1);
		version.addAttribute("maxr", Domain.INT);
		
		version = new TableVersion(version, 2);
		version.addAttribute("type", Domain.INT, 2);
		
		version = new TableVersion(version, 3);
		version.addAttribute("maxs", Domain.INT);
		
		version = new TableVersion(version, 4);
		version.addAttribute("timeout", Domain.INT);
		
		version = new TableVersion(version, 5);
		version.addAttribute("maxl", Domain.INT, 5);
		
		version = new TableVersion(version, 6);
		version = new TableVersion(version, 7);
		
		addTable(table);
		table.addIndex("user", true);
		table.addIndex("group", true);
		
		// db.depot		
		table = new Table("db.depot");
		version = new TableVersion(table, 0);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("type", Domain.INT);
		version.addAttribute("extra", Domain.TEXT);
		version.addAttribute("map", Domain.TEXT);
		
		version = new TableVersion(version, 1);
		
		addTable(table);
		table.addIndex("name", true);
		
		// db.domain
		table = new Table("db.domain");
		version = new TableVersion(table, 0);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("type", Domain.INT);
		version.addAttribute("extra", Domain.TEXT);
		version.addAttribute("mount", Domain.TEXT);
		version.addAttribute("owner", Domain.KEY);
		version.addAttribute("update", Domain.DATE);
		version.addAttribute("options", Domain.INT);
		version.addAttribute("desc", Domain.TEXT);

		version = new TableVersion(version, 1);
		version.addAttribute("access", Domain.DATE, 6);
		
		version = new TableVersion(version, 2);
		version.addAttribute("mapstate", Domain.INT, 8);
		
		version = new TableVersion(version, 3);
		version.addAttribute("mount2", Domain.TEXT, 4);
		version.addAttribute("mount3", Domain.TEXT, 5);
		
		version = new TableVersion(version, 4);
		version.removeAttribute("mapstate");
		
		version = new TableVersion(version, 5);
		version.addAttribute("stream", Domain.TEXT);
		
		addTable(table);
		table.addIndex("name", true);
		
		// db.view
		
		table = new Table("db.view");
		version = new TableVersion(table, 0);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("seq", Domain.INT);
		version.addAttribute("mapflag", Domain.INT);
		version.addAttribute("vfile", Domain.KEY);
		version.addAttribute("dfile", Domain.KEY);
		
		version = new TableVersion(version, 1);
		
		addTable(table);
		table.addIndex("name", true);
		table.addIndex("seq", true);
		
		// db.review
		
		table = new Table("db.review");
		version = new TableVersion(table, 0);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("seq", Domain.INT);
		version.addAttribute("mapflag", Domain.INT);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("type", Domain.INT);
		
		version = new TableVersion(version, 1);
		
		addTable(table);
		table.addIndex("name", true);
		table.addIndex("seq", true);
		
		// Files data
		
		// db.integ
		
		table = new Table("db.integ");
		version = new TableVersion(table, 0);
		version.addAttribute("tfile", Domain.KEY);
		version.addAttribute("ffile", Domain.KEY);
		version.addAttribute("sfrev", Domain.INT);
		version.addAttribute("efrev", Domain.INT);
		version.addAttribute("tfrev", Domain.INT);
		version.addAttribute("how", Domain.INT);
		version.addAttribute("comm", Domain.INT);
		version.addAttribute("res", Domain.INT);
		version.addAttribute("change", Domain.INT);

		addTable(table);
		table.addIndex("tfile", true);
		table.addIndex("ffile", true);
		table.addIndex("sfrev", true);
		
		// db.integed
		
		table = new Table("db.integed");
		version = new TableVersion(table, 0);
		version.addAttribute("tfile", Domain.KEY);
		version.addAttribute("ffile", Domain.KEY);
		version.addAttribute("sfrev", Domain.INT);
		version.addAttribute("efrev", Domain.INT);
		version.addAttribute("strev", Domain.INT);
		version.addAttribute("etrev", Domain.INT);
		version.addAttribute("how", Domain.INT);
		version.addAttribute("change", Domain.INT);

		addTable(table);
		table.addIndex("tfile", true);
		table.addIndex("ffile", true);
		table.addIndex("sfrev", true);
		table.addIndex("efrev", true);
		table.addIndex("strev", true);
		table.addIndex("etrev", true);
		
		// db.resolve
		
		table = new Table("db.resolve");
		version = new TableVersion(table, 0);
		version.addAttribute("tfile", Domain.KEY);
		version.addAttribute("ffile", Domain.KEY);
		version.addAttribute("sfrev", Domain.INT);
		version.addAttribute("efrev", Domain.INT);
		version.addAttribute("strev", Domain.INT);
		version.addAttribute("etrev", Domain.INT);
		version.addAttribute("how", Domain.INT);
		version.addAttribute("res", Domain.INT);
		version.addAttribute("bfile", Domain.KEY);
		version.addAttribute("brev", Domain.INT);

		addTable(table);
		table.addIndex("tfile", true);
		table.addIndex("ffile", true);
		table.addIndex("sfrev", true);
		
		// db.have
		
		table = new Table("db.have");
		version = new TableVersion(table, 0);
		version.addAttribute("cfile", Domain.KEY);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("rev", Domain.INT);
		
		version = new TableVersion(version, 1);
		version.addAttribute("type", Domain.INT);
		
		version = new TableVersion(version, 2);

		addTable(table);
		table.addIndex("cfile", true);
		
		// db.label
		
		table = new Table("db.label");
		version = new TableVersion(table, 0);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("file", Domain.KEY);
		version.addAttribute("rev", Domain.INT);

		addTable(table);
		table.addIndex("name", true);
		table.addIndex("file", true);

		// db.locks
		
		table = new Table("db.locks");
		version = new TableVersion(table, 0);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("client", Domain.KEY);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("islocked", Domain.INT);

		version = new TableVersion(version, 1);
		version.addAttribute("action", Domain.INT8, 3);
		
		version = new TableVersion(version, 2);
		version.addAttribute("change", Domain.INT	);
		
		addTable(table);
		table.addIndex("dfile", true);
		table.addIndex("client", true);

		// db.archive
		
		table = new Table("db.archive");
		version = new TableVersion(table, 0);
		version.addAttribute("afile", Domain.KEY);
		version.addAttribute("arev", Domain.KEY);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("rev", Domain.INT);
		version.addAttribute("atype", Domain.INT);
		
		version = new TableVersion(version, 1);

		addTable(table);
		table.addIndex("afile", true);
		table.addIndex("arev", false);
		table.addIndex("dfile", true);
		table.addIndex("rev", false);

		// db.archmap
		
		table = new Table("db.archmap");
		version = new TableVersion(table, 0);
		version.addAttribute("afile", Domain.KEY);
		version.addAttribute("dfile", Domain.KEY);

		addTable(table);
		table.addIndex("afile", true);
		table.addIndex("dfile", true);

		// db.rev
		
		table = new Table("db.rev");
		version = new TableVersion(table, 0);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("rev", Domain.INT);
		version.addAttribute("type", Domain.INT);
		version.addAttribute("ishead", Domain.INT);
		version.addAttribute("action", Domain.INT8);
		version.addAttribute("change", Domain.INT);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("afile", Domain.KEY);
		version.addAttribute("arev", Domain.KEY);
		version.addAttribute("atype", Domain.INT);
		
		version = new TableVersion(version, 1);
		version.addAttribute("digest", Domain.OCTET, 7);
		
		version = new TableVersion(version, 2);
		version.removeAttribute("ishead");

		version = new TableVersion(version, 3);
		version.addAttribute("modtime", Domain.DATE, 6);

		version = new TableVersion(version, 4);
		version.addAttribute("traitlot", Domain.INT, 8);

		version = new TableVersion(version, 5);
		version.addAttribute("size", Domain.INT64, 8);

		version = new TableVersion(version, 6);
		
		version = new TableVersion(version, 7);
		version.addAttribute("islazy", Domain.INT, 10);

		version = new TableVersion(version, 8);
		// nothing changed here ... just for downgrade support. Weird.
		
		addTable(table);
		
		Table rev = table; // need a reference to this table for revdx, revhx, revcx, revpx
		table.addIndex("dfile", true);
		table.addIndex("rev", false);

		// db.revcx
		
		table = new Table("db.revcx");
		version = new TableVersion(table, 0);
		version.addAttribute("change", Domain.INT);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("rev", Domain.INT);
		version.addAttribute("action", Domain.INT8);
		
		addTable(table);
		table.addIndex("change", false);
		table.addIndex("dfile", true);

		// TODO: need to add missing versions, at least 4 in total
		
		// db.revdx
		
		table = new Table("db.revdx");
		for (int i = 0; i < 9; i++) {
			version = new TableVersion(table, i);
			version.addAttributes(rev.getVersion(i));
		}
		addTable(table);
		table.addIndex("dfile", true);

		// db.revhx
		
		table = new Table("db.revhx");
		for (int i = 0; i < 9; i++) {
			version = new TableVersion(table, i);
			version.addAttributes(rev.getVersion(i));
		}

		addTable(table);
		table.addIndex("dfile", true);

		// db.revpx
		
		table = new Table("db.revpx");
		for (int i = 0; i < 9; i++) {
			version = new TableVersion(table, i);
			version.addAttributes(rev.getVersion(i));
		}

		addTable(table);
		table.addIndex("dfile", true);
		table.addIndex("rev", false);

		// db.revsx
		
		table = new Table("db.revsx");
		for (int i = 0; i < 9; i++) {
			version = new TableVersion(table, i);
			version.addAttributes(rev.getVersion(i));
		}

		addTable(table);
		table.addIndex("dfile", true);
		table.addIndex("rev", false);

		// db.revsh
		
		table = new Table("db.revsh");
		for (int i = 0; i < 9; i++) {
			version = new TableVersion(table, i);
			version.addAttributes(rev.getVersion(i));
		}

		addTable(table);
		table.addIndex("dfile", true);
		table.addIndex("rev", false);

		// db.working
		
		table = new Table("db.working");
		version = new TableVersion(table, 0);
		version.addAttribute("cfile", Domain.KEY);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("client", Domain.KEY);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("hrev", Domain.INT);
		version.addAttribute("wrev", Domain.INT);
		version.addAttribute("type", Domain.INT);
		version.addAttribute("action", Domain.INT8);
		version.addAttribute("change", Domain.INT);
		version.addAttribute("modtime", Domain.DATE);
		version.addAttribute("islocked", Domain.INT);
		
		version = new TableVersion(version, 1);
		
		version = new TableVersion(version, 2);
		version.addAttribute("virtual", Domain.INT, 6);
		
		version = new TableVersion(version, 3);
		version.addAttribute("digest", Domain.OCTET);
		
		version = new TableVersion(version, 4);
		version.addAttribute("traitlot", Domain.INT);
		
		version = new TableVersion(version, 5);
		version.addAttribute("size", Domain.INT64, 13);
		
		version = new TableVersion(version, 6);
		
		version = new TableVersion(version, 7);
		version.addAttribute("tampered", Domain.INT);
		
		version = new TableVersion(version, 8);
		version.addAttribute("ctype", Domain.INT);
		
		version = new TableVersion(version, 9);
		version.addAttribute("mfile", Domain.KEY);
		
		addTable(table);
		table.addIndex("cfile", true);

		Table working = table;
		
		// db.workingx
		
		table = new Table("db.workingx");
		for (int i = 0; i < 10; i++) {
			version = new TableVersion(table, i);
			version.addAttributes(working.getVersion(i));
		}

		addTable(table);
		table.addIndex("cfile", true);

		// db.traits
		
		table = new Table("db.traits");
		version = new TableVersion(table, 0);
		version.addAttribute("traitlot", Domain.INT);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("type", Domain.INT);
		version.addAttribute("len", Domain.INT);
		version.addAttribute("value", Domain.OCTETS);

		addTable(table);
		table.addIndex("traitlot", true);
		table.addIndex("name", true);

		// db.trigger
		
		table = new Table("db.trigger");
		version = new TableVersion(table, 0);
		version.addAttribute("seq", Domain.INT);
		version.addAttribute("name", Domain.KEY);
		version.addAttribute("mapflag", Domain.INT);
		version.addAttribute("dfile", Domain.KEY);
		version.addAttribute("action", Domain.TEXT);

		version = new TableVersion(version, 1);
		version.addAttribute("trigger", Domain.INT, 4);
		
		version = new TableVersion(version, 2);
		
		addTable(table);
		table.addIndex("seq", true);

		// Change data
		
		// db.change
		
		table = new Table("db.change");
		version = new TableVersion(table, 0);
		version.addAttribute("change", Domain.INT);
		version.addAttribute("key", Domain.INT);
		version.addAttribute("client", Domain.KEY);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("status", Domain.INT8);
		version.addAttribute("desc", Domain.TEXT);
		
		version = new TableVersion(version, 1);
		version.addAttribute("root", Domain.KEY);
		
		Table change = table; // save for changex
		
		addTable(table);
		table.addIndex("change", false);

		// db.changex
		
		table = new Table("db.changex");
		version = new TableVersion(table, 0);
		version.addAttributes(change.getVersion(0));
		
		version = new TableVersion(table, 1);
		version.addAttributes(change.getVersion(1));
		
		addTable(table);
		table.addIndex("change", false);

		// db.desc
		
		table = new Table("db.desc");
		version = new TableVersion(table, 0);
		version.addAttribute("key", Domain.INT);
		version.addAttribute("desc", Domain.TEXT);
		
		addTable(table);
		table.addIndex("key", false);

		// db.job
		
		table = new Table("db.job");
		version = new TableVersion(table, 0);
		version.addAttribute("job", Domain.KEY);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("status", Domain.INT8);
		version.addAttribute("desc", Domain.TEXT);
		
		Table job = table;
		
		addTable(table);
		table.addIndex("job", true);

		// db.jobpend
		
		table = new Table("db.jobpend");
		version = new TableVersion(table, 0);
		version.addAttributes(job.getVersion(0));
		
		addTable(table);
		table.addIndex("job", true);
		table.addIndex("user", true);

		// db.jobdesc
		
		table = new Table("db.jobdesc");
		version = new TableVersion(table, 0);
		version.addAttribute("job", Domain.INT);
		version.addAttribute("desc", Domain.TEXT);
		
		addTable(table);
		table.addIndex("job", true);

		// db.fix
		
		table = new Table("db.fix");
		version = new TableVersion(table, 0);
		version.addAttribute("job", Domain.KEY);
		version.addAttribute("change", Domain.INT);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("stat992", Domain.INT8);
		version.addAttribute("client", Domain.KEY);
		version.addAttribute("user", Domain.KEY);
		
		version = new TableVersion(version, 1);
		version.removeAttribute("stat992");
		version.addAttribute("status", Domain.KEY);
		
		Table fix = table;

		addTable(table);
		table.addIndex("job", true);
		table.addIndex("change", false);

		// db.fixrev
		
		table = new Table("db.fixrev");
		version = new TableVersion(table, 0);
		version.addAttributes(fix.getVersion(0));
		
		version = new TableVersion(table, 1);
		version.addAttributes(fix.getVersion(1));
		
		addTable(table);
		table.addIndex("change", false);
		table.addIndex("job", true);

		// job bodies
		
		// db.boddate
		
		table = new Table("db.boddate");
		version = new TableVersion(table, 0);
		version.addAttribute("key", Domain.KEY);
		version.addAttribute("attr", Domain.INT);
		version.addAttribute("date", Domain.DATE);
		
		addTable(table);
		table.addIndex("key", true);
		table.addIndex("attr", true);

		// db.bodtext
		
		table = new Table("db.bodtext");
		version = new TableVersion(table, 0);
		version.addAttribute("key", Domain.KEY);
		version.addAttribute("attr", Domain.INT);
		version.addAttribute("text", Domain.TEXT);
		
		version = new TableVersion(version, 1);
		version.addAttribute("bulk", Domain.INT, 2);
		
		addTable(table);
		table.addIndex("key", true);
		table.addIndex("attr", true);

		// db.ixdate
		
		table = new Table("db.ixdate");
		version = new TableVersion(table, 0);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("attr", Domain.INT);
		version.addAttribute("value", Domain.KEY);
		
		addTable(table);
		table.addIndex("date", true);
		table.addIndex("attr", true);
		table.addIndex("value", true);

		// db.ixtext
		
		table = new Table("db.ixtext");
		version = new TableVersion(table, 0);
		version.addAttribute("word", Domain.KEY);
		version.addAttribute("attr", Domain.INT);
		version.addAttribute("value", Domain.KEY);
		
		addTable(table);
		table.addIndex("word", true);
		table.addIndex("attr", true);
		table.addIndex("value", true);

		// db.protect
		
		table = new Table("db.protect");
		version = new TableVersion(table, 0);
		version.addAttribute("seq", Domain.INT);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("host", Domain.KEY);
		version.addAttribute("perm", Domain.INT8);
		version.addAttribute("mapflag", Domain.INT);
		version.addAttribute("dfile", Domain.KEY);
		
		version = new TableVersion(version, 1);
		version.addAttribute("group", Domain.INT, 1);
		
		version = new TableVersion(version, 2);
		
		version = new TableVersion(version, 3);
		
		version = new TableVersion(version, 4);
		
		addTable(table);
		table.addIndex("seq", true);

		// db.message
		
		table = new Table("db.message");
		version = new TableVersion(table, 0);
		version.addAttribute("lang", Domain.KEY);
		version.addAttribute("id", Domain.INT);
		version.addAttribute("msg", Domain.TEXT);
		
		addTable(table);
		table.addIndex("lang", true);
		table.addIndex("id", true);

		// db.monitor
		
		table = new Table("db.monitor");
		version = new TableVersion(table, 0);
		version.addAttribute("id", Domain.INT);
		version.addAttribute("user", Domain.KEY);
		version.addAttribute("func", Domain.TEXT);
		version.addAttribute("args", Domain.TEXT);
		version.addAttribute("start", Domain.DATE);
		version.addAttribute("runstate", Domain.INT);
		
		version = new TableVersion(version, 1);
		version.addAttribute("client", Domain.KEY);
		version.addAttribute("host", Domain.TEXT);
		version.addAttribute("app", Domain.TEXT);
		
		addTable(table);
		table.addIndex("id", true);
		
		// note CHECKPOINT_HEADER
		// @nx@ 0 1347379053 @30@ 2 0 0 0 0 @.@ @journal@ @@ @@ @@
		
		table = new Table(NoteType.CKP_HEADER.name());
		version = new TableVersion(table, 0);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("schema", Domain.TEXT);
		version.addAttribute("mode", Domain.INT);
		version.addAttribute("int1", Domain.INT);
		version.addAttribute("int2", Domain.INT);
		version.addAttribute("int3", Domain.INT);
		version.addAttribute("int4", Domain.INT);
		
		version.addAttribute("root", Domain.TEXT);
		version.addAttribute("jnlfile", Domain.TEXT);
		version.addAttribute("str3", Domain.TEXT);
		version.addAttribute("str4", Domain.TEXT);
		version.addAttribute("str5", Domain.TEXT);
		
		addTable(table);
		
		// note JOURNAL_HEADER
		table = new Table(NoteType.JNL_HEADER.name());
		version = new TableVersion(table, 0);
		version.addAttribute("date", Domain.DATE);
		version.addAttribute("schema", Domain.TEXT);
		version.addAttribute("mode", Domain.INT);
		version.addAttribute("flag", Domain.INT);
		version.addAttribute("int1", Domain.INT);
		version.addAttribute("int2", Domain.INT);
		version.addAttribute("int3", Domain.INT);
		
		version.addAttribute("str1", Domain.TEXT);
		version.addAttribute("str2", Domain.TEXT);
		version.addAttribute("str3", Domain.TEXT);
		version.addAttribute("str4", Domain.TEXT);
		version.addAttribute("str5", Domain.TEXT);
		
		addTable(table);
		
}
	
	private static void addTable(final Table table) {
		tableNamesInOrder.add(table.getName());
		tables.put(table.getName(), table);
	}
	
	private static void dumpSchema() {
		for (String name : tableNamesInOrder) {
			Table table = tables.get(name);
			System.out.println(table);
		}
	}

	public static void main(final String[] args) {
		if (args.length == 0) {
			dumpSchema();
		}
		else {
			for (String arg : args) {
				Table table = tables.get(arg);
				if (table != null) {
					System.out.println(table);
				}
				else {
					System.out.println("Table " + arg + " unknown.");
				}
			}
		}
	}
}
