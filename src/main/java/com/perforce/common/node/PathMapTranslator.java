package com.perforce.common.node;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.depot.DepotInterface;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class PathMapTranslator {

	private static Logger logger = LoggerFactory
			.getLogger(PathMapTranslator.class);
	private static ArrayList<PathMapEntry> list = new ArrayList<PathMapEntry>();

	public static void clear() {
		list = new ArrayList<PathMapEntry>();
	}

	public static void setDefault() throws ConfigException {
		PathMapEntry map = null;

		String scmPath = "(.*)";
		String p4Path = depotPrefix() + "{1}";
		map = new PathMapEntry(scmPath, p4Path);

		list = new ArrayList<PathMapEntry>();
		list.add(map);
	}

	private static String depotPrefix() throws ConfigException {
		String depot = (String) Config.get(CFG.P4_DEPOT_PATH);

		String sub = (String) Config.get(CFG.P4_DEPOT_SUB);
		if (!sub.endsWith("/")) {
			sub = new String(sub + "/");
		}
		if (!sub.startsWith("/")) {
			sub = new String("/" + sub);
		}

		return "//" + depot + sub;
	}

	public static void add(PathMapEntry entry) {
		list.add(0, entry);
	}

	public static void stash(File file) throws ConfigException {
		int hashInt = file.getName().hashCode();
		String hash = Integer.toHexString(hashInt);
		String remap = depotPrefix() + "remapped/file_" + hash;
		PathMapEntry entry = new PathMapEntry(file.getName(), remap);
		PathMapTranslator.add(entry);
	}

	public static String getLbrPath(String path, DepotInterface depot)
			throws ConfigException {
		String p4Path = translate(path);
		String lbrPath = p4Path.replaceFirst("//", depot.getRoot());
		return lbrPath;
	}

	public static String translate(String path) {
		String expanded = "";

		// use empty string for null paths
		if (path == null) {
			path = "";
		}

		// terminate '{' from path with '{}'
		if (path.contains("{")) {
			path = path.replaceAll("\\{", "{}");
		}

		for (PathMapEntry map : list) {
			String regex = map.getScmPath();
			Pattern pattern = Pattern.compile(regex + ".*");
			Matcher m = pattern.matcher(path);
			ArrayList<String> group = new ArrayList<String>();
			if (m.matches()) {
				String trans = map.getP4Path();
				for (int i = 1; i <= m.groupCount(); i++) {
					group.add(m.group(i));
				}
				expanded = expandGroup(trans, group);
				break;
			}
		}

		// restore path and remove terminator '{}'
		if (expanded.contains("{}")) {
			expanded = expanded.replaceAll("\\{\\}", "{");
		}
		return expanded;
	}

	private static String expandGroup(String trans, ArrayList<String> group) {
		String regex = "\\{([1-9])\\}";
		Pattern pattern = Pattern.compile(".*?" + regex + ".*");

		Matcher m = pattern.matcher(trans);
		while (m.matches()) {
			int i = Integer.parseInt(m.group(1)) - 1;
			String quote = Matcher.quoteReplacement(group.get(i));
			trans = trans.replaceFirst(regex, quote);
			m = pattern.matcher(trans);
		}

		return trans;
	}

	public static boolean load(String filename) throws Exception {
		// clear previous entries
		clear();

		RandomAccessFile rf;
		try {
			rf = new RandomAccessFile(filename, "r");
			String line = null;

			logger.info("loading PathMap: \t" + filename);

			while ((line = rf.readLine()) != null) {
				// ignore comments starting with '#' and add lines to table
				if (!line.startsWith("#")) {
					line = line.trim();
					if (!line.isEmpty()) {
						String[] parts = line.split(",\\s+");
						if (parts.length == 2) {
							PathMapEntry map;
							map = new PathMapEntry(parts[0], parts[1]);
							list.add(map);
						} else {
							logger.warn("bad format, ignoring line: " + line);
						}
					}
				}
			}
			rf.close();
			return true;
		} catch (FileNotFoundException e) {
			// no file, then return
			return false;
		}
	}
}
