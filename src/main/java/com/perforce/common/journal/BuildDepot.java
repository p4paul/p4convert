package com.perforce.common.journal;

import java.util.Date;

import com.perforce.common.depot.DepotConvert;
import com.perforce.common.schema.JournalNote;
import com.perforce.common.schema.JournalRecord;
import com.perforce.common.schema.JournalNote.NoteType;
import com.perforce.common.schema.JournalNote.RunMode;
import com.perforce.config.CFG;
import com.perforce.config.Config;

public class BuildDepot {

	public static String toJournal(DepotConvert d) throws Exception {
		StringBuffer sb = new StringBuffer();

		// Only add note record for real journal (not testcases)
		if (!(Boolean) Config.get(CFG.TEST)) {
			JournalNote nxMode = new JournalNote("nx", NoteType.CKP_HEADER, 0);
			Date now = new Date();
			nxMode.addField("date", now.getTime() / 1000);
			nxMode.addField("schema", "0");
			int runMode = 0;
			if ((Boolean) Config.get(CFG.P4_UNICODE)) {
				runMode += RunMode.UNICODE.getValue();
			}
			if ((Boolean) Config.get(CFG.P4_C1_MODE)) {
				runMode += RunMode.C1.getValue();
			} else {
				runMode += RunMode.C0.getValue();
			}
			nxMode.addField("mode", runMode);
			nxMode.addField("int1", 0);
			nxMode.addField("int2", 0);
			nxMode.addField("int3", 0);
			nxMode.addField("int4", 0);
			nxMode.addField("root", ".");
			String jnlName = (String) Config.get(CFG.P4_JNL_PREFIX)
					+ Config.get(CFG.P4_JNL_INDEX);
			nxMode.addField("jnlfile", jnlName);
			nxMode.addField("str3", "");
			nxMode.addField("str4", "");
			nxMode.addField("str5", "");
			sb.append(nxMode.toJournalString() + "\n");
		}

		JournalRecord dbDepot = new JournalRecord("pv", "db.depot", 0);
		dbDepot.addField("name", d.getName());
		dbDepot.addField("type", 0);
		dbDepot.addField("extra", "subdir");
		dbDepot.addField("map", d.getName() + "/...");
		sb.append(dbDepot.toJournalString() + "\n");

		JournalRecord dbDomain = new JournalRecord("pv", "db.domain", 3);
		dbDomain.addField("name", d.getName());
		dbDomain.addField("type", 100);
		dbDomain.addField("extra", "");
		dbDomain.addField("mount", "");
		dbDomain.addField("mount2", "");
		dbDomain.addField("mount3", "");
		dbDomain.addField("owner", d.getUser());
		dbDomain.addField("update", d.getDefaultDate());
		dbDomain.addField("access", d.getDefaultDate());
		dbDomain.addField("options", 0);
		dbDomain.addField("mapstate", 1);
		dbDomain.addField("desc", "Created by " + d.getUser());
		sb.append(dbDomain.toJournalString() + "\n");

		JournalRecord dbUser = new JournalRecord("pv", "db.user", 3);
		dbUser.addField("user", d.getUser());
		dbUser.addField("email", d.getUser() + "@" + d.getClient());
		dbUser.addField("jobview", "");
		dbUser.addField("update", d.getDefaultDate());
		dbUser.addField("access", d.getDefaultDate());
		dbUser.addField("fullname", d.getUser());
		dbUser.addField("password", "");
		dbUser.addField("strength", 0);
		dbUser.addField("ticket", "");
		dbUser.addField("enddate", 0);
		sb.append(dbUser.toJournalString() + "\n");

		JournalRecord dbView = new JournalRecord("pv", "db.view", 0);
		dbView.addField("name", d.getClient());
		dbView.addField("seq", 0);
		dbView.addField("mapflag", 0);
		dbView.addField("vfile", "//" + d.getClient() + "/...");
		dbView.addField("dfile", "//" + d.getName() + "/...");
		sb.append(dbView.toJournalString() + "\n");

		JournalRecord dbUserDomain = new JournalRecord("pv", "db.domain", 3);
		dbUserDomain.addField("name", d.getClient());
		dbUserDomain.addField("type", 99);
		dbUserDomain.addField("extra", "");
		dbUserDomain.addField("mount", d.getDefaultClientRoot());
		dbUserDomain.addField("mount2", "");
		dbUserDomain.addField("mount3", "");
		dbUserDomain.addField("owner", d.getUser());
		dbUserDomain.addField("update", d.getDefaultDate());
		dbUserDomain.addField("access", d.getDefaultDate());
		dbUserDomain.addField("options", 0);
		dbUserDomain.addField("mapstate", 1);
		dbUserDomain.addField("desc", "Created by " + d.getUser());
		sb.append(dbUserDomain.toJournalString() + "\n");

		return sb.toString();
	}
}
