package com.perforce.common.depot;

import java.io.File;
import java.util.Date;

import com.perforce.common.journal.BuildCounter;
import com.perforce.common.journal.BuildDepot;
import com.perforce.common.journal.JournalWriter;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class DepotConvert implements DepotInterface {

	private String name;
	private String depotSub;
	private long defaultDate;
	private JournalWriter journal;

	public DepotConvert(String name) throws Exception {
		this.name = name; // Depot name = parent's name of tree

		if ((Boolean) Config.get(CFG.TEST) == false) {
			Date date = new Date();
			defaultDate = date.getTime() / 1000;
		} else {
			defaultDate = 0;
		}

		depotSub = (String) Config.get(CFG.P4_DEPOT_SUB);
		if (!depotSub.endsWith("/")) {
			depotSub = new String(depotSub + "/");
		}
		if (!depotSub.startsWith("/")) {
			depotSub = new String("/" + depotSub);
		}

		// Create Depot root directory
		String p4root = (String) Config.get(CFG.P4_ROOT);
		if (!p4root.endsWith("/")) {
			p4root = new String(p4root + "/");
		}

		File directory = new File(p4root);
		directory.mkdirs();

		// Create journal
		String jnlPath = p4root + Config.get(CFG.P4_JNL_PREFIX)
				+ Config.get(CFG.P4_JNL_INDEX);
		journal = new JournalWriter(jnlPath);

		// Add unicode counter if required
		if ((Boolean) Config.get(CFG.P4_UNICODE)) {
			journal.write(BuildCounter.toJournal("unicode", "1"));
		}

		// Create depot and write to journal
		journal.write(BuildDepot.toJournal(this));
		journal.flush();
	}

	@Override
	public String getClient() {
		try {
			return (String) Config.get(CFG.P4_CLIENT);
		} catch (ConfigException e) {
			return "p4-client";
		}
	}

	@Override
	public long getDefaultDate() {
		return defaultDate;
	}

	@Override
	public String getUser() {
		try {
			return (String) Config.get(CFG.P4_USER);
		} catch (ConfigException e) {
			return "p4-user";
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath(String scmPath) {
		String p4Path = PathMapTranslator.translate(scmPath);
		return p4Path;
	}

	@Override
	public String getRoot() throws ConfigException {
		String depotPath = (String) Config.get(CFG.P4_ROOT);
		if (!depotPath.endsWith("/") && !depotPath.endsWith("\\")) {
			depotPath = new String(depotPath + "/");
			Config.set(CFG.P4_ROOT, depotPath);
		}
		return depotPath;
	}

	public JournalWriter getJournal() {
		return journal;
	}

	@Override
	public String getDefaultClientRoot() {
		try {
			return (String) Config.get(CFG.P4_CLIENT_ROOT);
		} catch (ConfigException e) {
			return "/ws";
		}
	}
}
