package com.perforce.svn.process;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.ExitCode;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.process.ChangeInfo;
import com.perforce.common.process.ProcessChange;
import com.perforce.common.process.ProcessFactory;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.svn.parser.Node;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;
import com.perforce.svn.prescan.ExcludeParser;
import com.perforce.svn.prescan.LastRevision;
import com.perforce.svn.query.QueryInterface;

public class SvnProcessChange extends ProcessChange {

	private Logger logger = LoggerFactory.getLogger(SvnProcessChange.class);

	protected void processChange() throws Exception {
		// Read configuration settings for locals
		String dumpFile = (String) Config.get(CFG.SVN_DUMPFILE);
		long revStart = (Long) Config.get(CFG.SVN_START);
		long revEnd = (Long) Config.get(CFG.SVN_END);
		String depotPath = (String) Config.get(CFG.P4_DEPOT_PATH);
		CaseSensitivity caseMode;
		caseMode = (CaseSensitivity) Config.get(CFG.P4_CASE);
		if (logger.isDebugEnabled()) {
			logger.debug(Config.summary());
		}

		// Create revision tree and depot
		DepotInterface depot = ProcessFactory.getDepot(depotPath, caseMode);

		// Check for pending changes, abort if any are found
		QueryInterface query = ProcessFactory.getQuery(depot);
		int pending = query.getPendingChangeCount();
		if (pending > 0) {
			String err = "Pending change detected, conversion aborted";
			logger.error(err);
			throw new ConverterException(err);
		}

		// Find last revision
		LastRevision rev = new LastRevision(dumpFile);
		String revLastString = rev.find();
		rev.close();
		if (revLastString == null) {
			String err = "Cannot find last revision in dumpfile";
			logger.error(err);
			throw new ConverterException(err);
		}
		long revLast = Long.parseLong(revLastString);

		// Test start and end revisions
		if (revStart > revLast || revEnd > revLast) {
			String err = "Specified revision range exceeds last revision";
			logger.error(err);
			throw new ConverterException(err);
		}

		// Auto set end revision
		if (revLastString != null && revEnd == 0) {
			Config.set(CFG.SVN_END, revLast);
			revEnd = revLast;
		}

		// Log import range
		if (logger.isInfoEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append("importing revs: \t");
			sb.append(revStart + " to " + revEnd);
			sb.append(" out of " + revLast);
			logger.info(sb.toString());
		}

		// Check offset
		long offset = (Long) Config.get(CFG.P4_OFFSET);
		if (offset != 0) {
			if (logger.isInfoEnabled()) {
				logger.info("change offset: \t\t" + offset);
			}
			if (Config.isImportMode()) {
				logger.warn("offset ignored in Import mode!");
				Config.set(CFG.P4_OFFSET, 0);
			}
		}

		// Initialise node path excluder, if required
		boolean filter = ExcludeParser.load();
		if (filter && !ExcludeParser.parse(dumpFile)) {
			System.exit(ExitCode.USAGE.value());
		}

		// Initialise counters
		super.setCurrentChange(null);
		long nextChange = 1L;

		// Open dump file reader and iterate over entries
		RecordReader recordReader = new RecordReader(dumpFile);
		super.setClean(false); // now we want to wait for the end of a change
		Record lastRecord = null;
		for (Record record : recordReader) {
			switch (record.getType()) {
			case REVISION:
				// special case: skip revision 0
				if (record.getSvnRevision() == 0) {
					break;
				}

				// skip revision outside of starting position
				if (record.getSvnRevision() < revStart) {
					if (logger.isInfoEnabled()) {
						logger.info("skipping change " + nextChange + "...");
					}
					nextChange++;
					break;
				}

				// Submit change
				submit();

				if (super.isStop() || (nextChange > revEnd && revEnd != 0)) {
					if (super.isStop()) {
						// Premature stop -- update end rev
						Config.set(CFG.SVN_END, nextChange - 1);
					}
					// Close changelist
					close();
					return;
				}

				/*
				 * Create new changelist and increment count
				 */
				long change = nextChange;
				change += (Long) Config.get(CFG.P4_OFFSET);
				ChangeInfo info = new ChangeInfo(record);
				super.setCurrentChange(ProcessFactory.getChange(change, info,
						depot));
				nextChange++;
				break;

			case NODE:
				// Process node - add actions to tree and current changelist
				if (record.getSvnRevision() >= revStart) {

					((Node) record).setSubBlock(isSubNode(lastRecord, record));

					SvnProcessNode node = new SvnProcessNode(
							super.getCurrentChange(), depot, (Node) record);
					node.process();
				}
				break;

			case SCHEMA:
				int schemaVersion = (int) record
						.findHeaderLong("SVN-fs-dump-format-version");
				if (schemaVersion > 3) {
					throw new RuntimeException("Incompatable Schema version: "
							+ schemaVersion);
				}
				break;
			}
			lastRecord = record;
		}

		// Submit last change and close
		submit();
		close();
	}

	/**
	 * Sub block are not documented in the schema, but seem to occur only
	 * following 'delete' Node-actions.
	 * 
	 * @param last
	 * @param node
	 * @return
	 */
	private boolean isSubNode(Record last, Record node) {
		if (last == null)
			return false;

		if (!last.getType().equals(Record.Type.NODE))
			return false;

		if (!last.findHeaderString("Node-action").contains("delete"))
			return false;

		if (node.findHeaderString("Node-copyfrom-path") == null)
			return false;

		return node.isSubBlock();
	}

}
