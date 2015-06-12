package com.perforce.svn.prescan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.NodeFilterMap;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;

public class ExcludeParser {

	private static Logger logger = LoggerFactory.getLogger(ExcludeParser.class);

	private static NodeFilterMap exclude;
	private static NodeFilterMap include;
	private static NodeFilterMap issues;

	public static void preload(String line) throws Exception {
		exclude = new NodeFilterMap();
		include = new NodeFilterMap();
		issues = new NodeFilterMap();
		exclude.add(line);
	}

	public static boolean load() throws Exception {
		String excludeFile = (String) Config.get(CFG.EXCLUDE_MAP);
		String includeFile = (String) Config.get(CFG.INCLUDE_MAP);
		String issueFile = (String) Config.get(CFG.ISSUE_MAP);

		exclude = new NodeFilterMap(excludeFile);
		boolean filter = exclude.load();

		include = new NodeFilterMap(includeFile);
		include.load();

		issues = new NodeFilterMap(issueFile);
		issues.clean();

		return filter;
	}

	public static boolean parse(String dumpFile) throws Exception {

		if (logger.isInfoEnabled()) {
			logger.info("Verifying exclusion map...");
		}

		long end = (Long) Config.get(CFG.P4_END);
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
				String fromPath = record.findHeaderString("Node-copyfrom-path");

				// only add actions for paths that are not excluded
				if (!isSkipped(toPath)) {

					// check branch from path
					if (fromPath != null) {
						if (!isSkipped(fromPath)) {
						} else {
							issues.add(fromPath);
							if (logger.isInfoEnabled()) {
								logger.info("issue: " + fromPath);
							}
						}
					}
				}
				break;

			default:
				break;
			}

			if (progress.done()) {
				break;
			}
		}

		if (issues.isEmpty()) {
			if (logger.isInfoEnabled()) {
				logger.info("No issues found, ready to convert.");
			}
			return true;
		} else {
			if (logger.isInfoEnabled()) {
				logger.info("Issues found, saving issue map...");
			}
			issues.store();
			return false;
		}
	}

	/**
	 * Returns true if node path is to be excluded
	 * 
	 * @param path
	 * @return
	 */
	public static boolean isSkipped(String path) {
		if (exclude.contains(path)) {
			if (include.contains(path)) {
				return false;
			}
			return true;
		}
		return false;
	}
}
