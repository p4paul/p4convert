package com.perforce.svn.parser;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.svn.prescan.ExtractRecord;

public class RecordStateTrace {

	private static Logger logger = LoggerFactory
			.getLogger(RecordStateTrace.class);

	static Record current;

	/**
	 * Updates state with active record. Used for tracing exceptions.
	 * 
	 * @param record
	 */
	public static void update(Record record) {
		current = record;
	}

	public static void dump() throws Exception {

		int node = current.getNodeNumber();
		int rev = current.getSvnRevision();
		String svnDump = (String) Config.get(CFG.SVN_DUMPFILE);

		// Fail info on current node
		String bar = "----------------------------------------------------------------";
		logger.error(bar);
		StringBuffer msg = new StringBuffer();
		msg.append("Failed to convert node: " + rev + "." + node + "\n");
		msg.append(current.toString() + "\n");
		logger.error(msg.toString());
		logger.error(bar);

		// Extract revision and nodes
		ExtractRecord extractRecord = new ExtractRecord(svnDump);
		int last = 8;
		int start = ((node - last) < 0) ? 0 : (node - last);
		List<Record> nodes = extractRecord.findNode(rev, start, node);

		// Save to file
		String revisionName = "extract." + rev + "." + node + ".dump";
		write(nodes, revisionName);
	}

	private static void write(List<Record> nodes, String filename)
			throws Exception {
		SubversionWriter out = new SubversionWriter(filename, true);

		if (logger.isInfoEnabled()) {
			logger.info("Extracting records to: " + filename);
		}
		for (Record r : nodes) {
			out.seperator(r.getSvnRevision() + "." + r.getNodeNumber());
			out.putRecord(r);
		}
		out.flush();
		out.close();
	}
}
