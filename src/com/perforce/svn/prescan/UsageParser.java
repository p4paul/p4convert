package com.perforce.svn.prescan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.history.Action;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;
import com.perforce.svn.prescan.UsageTree.UsageType;

public class UsageParser {

	private Logger logger = LoggerFactory.getLogger(UsageParser.class);

	private UsageTree tree = new UsageTree("depot");
	private int pathLength;
	private long emptyNodes;

	public UsageParser(String dumpFile) throws ConfigException {
		long endRev = (long) Config.get(CFG.P4_END);
		Progress progress = new Progress(endRev);
		pathLength = 0;
		emptyNodes = 0;

		logger.info("Scanning: " + dumpFile);

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

				if (toPath == null) {
					emptyNodes++;
					int rev = record.getSvnRevision();
					int node = record.getNodeNumber();
					logger.info("... empty node: " + rev + ":" + node);
					break;
				}

				if (toPath.length() > pathLength) {
					pathLength = toPath.length();
				}

				UsageType type = getNodeType(record);
				Action action = Action.parse(record);
				tree.add(toPath, fromPath, type, action);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Reads the node kind (file or directory) from the header field in the
	 * Subversion dumpfile and returns the type. If a node kind is not found
	 * then FILE is assumed.
	 * 
	 * @param tree
	 * @param record
	 * @return
	 * @throws ConverterException
	 */
	public static UsageType getNodeType(Record record) {
		UsageType nodeType = UsageType.UNKNOWN;

		String nodeKind = record.findHeaderString("Node-kind");

		if (nodeKind == null) {
			nodeType = UsageType.FILE;
		}

		else if (nodeKind.equals("file")) {
			nodeType = UsageType.FILE;
		} else if (nodeKind.equals("dir")) {
			nodeType = UsageType.DIR;
		}

		return nodeType;
	}

	public UsageTree getTree() {
		return tree;
	}

	public int getPathLength() {
		return pathLength;
	}

	public long getEmptyNodes() {
		return emptyNodes;
	}
}
