package com.perforce.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class Stats {
	private static Properties stats = new Properties();
	private static Map<String, Integer> users = new HashMap<String, Integer>();

	public static void setDefault() {
		stats.setProperty(StatsType.currentRevision.name(), "0");
		stats.setProperty(StatsType.archiveCount.name(), "0");
		stats.setProperty(StatsType.branchActionCount.name(), "0");
		stats.setProperty(StatsType.stopAtNextRevision.name(), "0");
		stats.setProperty(StatsType.warningCount.name(), "0");
	}

	public static long getLong(StatsType key) {
		return Long.parseLong(stats.getProperty(key.name()));
	}

	public static String getString(StatsType key) {
		return stats.getProperty(key.name());
	}

	public static void setLong(StatsType key, long value) {
		if (stats.containsKey(key.name()))
			stats.setProperty(key.name(), Long.toString(value));
	}

	public static void inc(StatsType key) {
		if (stats.containsKey(key.name())) {
			long value = Long.parseLong(stats.getProperty(key.name())) + 1;
			stats.setProperty(key.name(), Long.toString(value));
		}
	}

	public static void addUser(String user) {
		if (!users.containsKey(user)) {
			users.put(user, 0);
		} else {
			int count = users.get(user);
			users.put(user, count + 1);
		}
	}

	public static String summary(long l) throws ConfigException {
		String line = "--------------------------------------------------------------------------------";

		StringBuffer sb = new StringBuffer();
		sb.append(line + "\n");
		sb.append("\t\t\t\t" + "Conversion Summary" + "\n");
		sb.append(line + "\n\n");

		sb.append("  Last changelist    : ");
		sb.append(l + "\n");
		sb.append("  Converted revs     : ");
		long lastRev = Stats.getLong(StatsType.currentRevision);
		sb.append(lastRev + "\n");
		sb.append("  Branched files     : ");
		sb.append(Stats.getString(StatsType.branchActionCount) + "\n");
		sb.append("  Archive revisions  : ");
		sb.append(Stats.getString(StatsType.archiveCount) + "\n");
		sb.append("  Active users       : ");
		sb.append(users.size() + "\n");

		sb.append("  Warning count      : ");
		long warn = Stats.getLong(StatsType.warningCount);
		if (warn > 0) {
			sb.append(warn + "\t\t\t**** WARNING ****\n");
		} else {
			sb.append(warn + "\n");
		}

		sb.append("  Script version     : " + Config.get(CFG.VERSION) + "\n");
		sb.append("\n" + line + "\n\n");

		// Raise warnings
		if (warn > 0) {
			String warnLine = "**** WARNING ";
			warnLine = new String(new char[6]).replace("\0", warnLine);
			sb.append(warnLine + "****\n\n");
			sb.append("\t Please note that although the conversion completed it may not \n");
			sb.append("\t be suitable for use. All warnings should be investigated.\n\n");
			sb.append("\t Please contact support@perforce.com for assistance. \n\n");
			sb.append(warnLine + "****\n\n");
		}
		
		
		return sb.toString();
	}
}
