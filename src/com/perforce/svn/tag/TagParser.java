package com.perforce.svn.tag;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.history.Action;
import com.perforce.svn.parser.Content;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;
import com.perforce.svn.prescan.ExcludeParser;
import com.perforce.svn.prescan.Progress;

public class TagParser {

	private static Logger logger = LoggerFactory.getLogger(TagParser.class);

	private static HashMap<String, TagEntry> tags;

	public static void parse(String dumpFile) throws Exception {

		logger.info("Searching for labels...");
		tags = new HashMap<String, TagEntry>();

		long end = (Long) Config.get(CFG.SVN_END);
		Progress progress = new Progress(end);

		// Initialise per change tag counter
		HashMap<String, TagEntry> changeTags = new HashMap<String, TagEntry>();

		// Open dump file reader and iterate over entries
		RecordReader recordReader = new RecordReader(dumpFile);
		for (Record record : recordReader) {
			switch (record.getType()) {
			case REVISION:
				progress.update(record.getSvnRevision());
				aggregateTags(changeTags);

				// reset tags for next change
				changeTags = new HashMap<String, TagEntry>();
				break;

			case NODE:
				String toPath = record.findHeaderString("Node-path");
				Action act = Action.parse(record);
				String id = getId(toPath);
				if (id.isEmpty()) {
					continue;
				}

				Content content = record.getContent();
				if (content.isBlob()) {
					// if content, force BRANCH
					countTag(id, TagType.BRANCH, changeTags);
				} else if (act == Action.REMOVE) {
					// if REMOVE, delete
					countTag(id, TagType.DELETED, changeTags);
				} else {
					// else keep a count
					countTag(id, TagType.UNKNOWN, changeTags);
				}
				break;

			default:
				break;
			}
		}

		// Aggregate tags from remaining nodes
		aggregateTags(changeTags);
	}

	public static void aggregateTags(HashMap<String, TagEntry> changeTags) {
		for (Entry<String, TagEntry> t : changeTags.entrySet()) {
			String id = t.getKey();
			TagEntry entry = t.getValue();

			TagType type = entry.getType();
			if (type == TagType.UNKNOWN) {
				if (entry.getCount() == 1) {
					type = TagType.AUTOMATIC;
				} else if (entry.getCount() > 1) {
					type = TagType.STATIC;
				}
			}
			countTag(id, type, tags);

			// Check aggregated result
			TagEntry aggEntry = tags.get(id);
			TagType aggType = aggEntry.getType();
			if (aggType != TagType.BRANCH && aggEntry.getCount() > 1) {
				if (type == TagType.DELETED) {
					aggEntry.setType(TagType.DELETED);
				} else {
					aggEntry.setType(TagType.BRANCH);
				}
			}
		}
	}

	public static boolean isLabel(String path) {
		String id = getId(path);
		if (id.isEmpty()) {
			return false;
		}

		if (tags.containsKey(id)) {
			TagEntry entry = tags.get(id);
			TagType type = entry.getType();
			switch (type) {
			case AUTOMATIC:
			case STATIC:
			case DELETED:
				return true;
			default:
				return false;
			}
		} else {
			// no reference
			return false;
		}
	}

	private static void countTag(String id, TagType type,
			HashMap<String, TagEntry> tags) {
		if (tags.containsKey(id)) {
			TagEntry tag = tags.get(id);
			tag.increment();
		} else {
			TagEntry tag = new TagEntry(id);
			tag.setType(type);
			tags.put(id, tag);
		}
	}

	public static TagEntry getLabel(String path) {
		String id = getId(path);
		if (!id.isEmpty()) {
			return tags.get(id);
		}
		return null;
	}

	private static String getId(String path) {
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
		for (Entry<String, TagEntry> t : tags.entrySet()) {
			sb.append("... ");
			sb.append(t.getValue());
			sb.append("\n");
		}
		return sb.toString();
	}
}
