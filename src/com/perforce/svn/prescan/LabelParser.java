package com.perforce.svn.prescan;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;

public class LabelParser {

	private static Logger logger = LoggerFactory.getLogger(LabelParser.class);

	private static HashMap<String, Integer> tags = new HashMap<String, Integer>();

	public static void parse(String dumpFile) throws Exception {

		if (logger.isInfoEnabled()) {
			logger.info("Searching for labels...");
		}

		long end = (Long) Config.get(CFG.SVN_END);
		Progress progress = new Progress(end);

		// Open dump file reader and iterate over entries
		RecordReader recordReader = new RecordReader(dumpFile);
		for (Record record : recordReader) {
			switch (record.getType()) {
			case REVISION:
				progress.update(record.getSvnRevision());
				break;

			case NODE:
				String toPath = record.findHeaderString("Node-path");
				processPath(toPath);
				break;

			default:
				break;
			}
		}
	}

	public static boolean isLabel(String path) {
		String id = getId(path);
		if (id.isEmpty()) {
			return false;
		}

		if (tags.containsKey(id)) {
			int count = tags.get(id);
			if (count == 1) {
				// a label candidate
				return true;
			} else {
				// more than one change
				return false;
			}
		} else {
			// no reference
			return false;
		}
	}

	private static void processPath(String path) {
		String id = getId(path);
		if (id.isEmpty()) {
			return;
		}

		if (tags.containsKey(id)) {
			int count = tags.get(id);
			tags.put(id, count + 1);
		} else {
			tags.put(id, 1);
		}
	}

	public static String getId(String path) {
		String id = "";

		// exit early for non taggable paths
		if (path == null || path.isEmpty() || !path.contains("/")) {
			return id;
		}

		// exit early if users wanted path as branch (not skipped)
		if (!ExcludeParser.isSkipped(path)) {
			return id;
		}

		// determine path depth
		int depth;
		try {
			depth = (Integer) Config.get(CFG.SVN_LABEL_DEPTH);
		} catch (ConfigException e) {
			return id;
		}
		
		// exit early if path is too short
		String[] parts = path.split("/", depth + 1);
		if (parts.length < depth) {
			return id;
		}

		// generate id from path
		for (int i = 0; i < depth; i++) {
			id += parts[i] + "/";
		}

		// overlay formatter if defined
		String formatter;
		try {
			formatter = (String) Config.get(CFG.SVN_LABEL_FORMAT);
		} catch (ConfigException e) {
			return id;
		}

		if (!formatter.isEmpty()) {
			id = formatter;
			id = id.replaceAll("\\{depth\\}", "{" + depth + "}");

			// Substitute all '{n}' with values from parts[]
			String regex = "\\{([1-9])\\}";
			Pattern pattern = Pattern.compile(".*?" + regex + ".*");

			Matcher m = pattern.matcher(id);
			while (m.matches()) {
				int i = Integer.parseInt(m.group(1)) - 1;
				id = id.replaceFirst(regex, parts[i]);
				m = pattern.matcher(id);
			}
		}
		return id;
	}

	public static String toLog() {
		StringBuffer sb = new StringBuffer();
		sb.append("Branch map count:\n");
		for (Entry<String, Integer> t : tags.entrySet()) {
			sb.append("... ");
			sb.append(t.getValue());
			sb.append(" ");
			sb.append(t.getKey());
			sb.append("\n");
		}
		return sb.toString();
	}
}
