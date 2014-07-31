package com.perforce.svn.prescan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;
import com.perforce.svn.prescan.UsageTree.UsageType;

public class UsageParser {

	private Logger logger = LoggerFactory.getLogger(UsageParser.class);

	private UsageTree tree = new UsageTree("depot");
	private int pathLength;
	private long emptyNodes;

	public UsageParser(String dumpFile, long endRev) {
		super();

		Progress progress = new Progress(endRev);
		pathLength = 0;
		emptyNodes = 0;

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
				ChangeAction.Action action = getNodeAction(record);
				tree.add(toPath, fromPath, type, action);
				break;
			default:
				break;
			}
		}
	}

	public static ChangeAction.Action getNodeAction(Record record) {
		ChangeAction.Action action = null;

		// Set node condition ('add', 'change' or 'delete')
		String s = record.findHeaderString("Node-action");
		if (s != null) {
			if (s.contains("add"))
				action = ChangeAction.Action.ADD;
			if (s.contains("change"))
				action = ChangeAction.Action.EDIT;
			if (s.contains("replace"))
				action = ChangeAction.Action.EDIT;
			if (s.contains("delete"))
				action = ChangeAction.Action.REMOVE;
		} else {
			throw new RuntimeException("unknown Node-action(" + record + ")");
		}

		// Test for branch condition (overload 'add' condition)
		if ((record.findHeaderString("Node-copyfrom-path")) != null
				|| (record.findHeaderString("Node-copyfrom-rev")) != null) {
			action = ChangeAction.Action.BRANCH;
		}

		return action;
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
