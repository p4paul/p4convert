package com.perforce.svn.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.ExitCode;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.process.ChangeInfo;
import com.perforce.common.process.ProcessChange;
import com.perforce.common.process.ProcessFactory;
import com.perforce.common.process.ProcessLabel;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.parser.Node;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;
import com.perforce.svn.prescan.ExcludeParser;
import com.perforce.svn.prescan.LastRevision;
import com.perforce.svn.query.QueryInterface;
import com.perforce.svn.tag.TagParser;

public class SvnProcessChange extends ProcessChange {

	private Logger logger = LoggerFactory.getLogger(SvnProcessChange.class);

	private ChangeInfo changeInfo;

	private final String dumpFile;

	public SvnProcessChange() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug(Config.summary());
		}

		// Read configuration settings
		dumpFile = (String) Config.get(CFG.SVN_DUMPFILE);
		revStart = (Long) Config.get(CFG.P4_START);
		revEnd = (Long) Config.get(CFG.P4_END);

		// Set imported change range
		long revLast = getLastChange();
		setChangeRange(revLast);
	}

	private long getLastChange() throws Exception {
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
		return revLast;
	}

	protected void processChange() throws Exception {
		// Initialise labels
		isLabels = (Boolean) Config.get(CFG.SVN_LABELS);

		String depotPath = (String) Config.get(CFG.P4_DEPOT_PATH);
		CaseSensitivity caseMode;
		caseMode = (CaseSensitivity) Config.get(CFG.P4_CASE);

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
		boolean isFilter = ExcludeParser.load();
		if (isFilter && !isLabels) {
			if (!ExcludeParser.parse(dumpFile)) {
				System.exit(ExitCode.USAGE.value());
			}
		}

		// Scan Subversion tags for label candidates
		processLabel = new ProcessLabel(depot);
		if (isLabels) {
			TagParser.parse(dumpFile);
			logger.info(TagParser.toLog());
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
					logger.info("skipping change " + nextChange + "...");
					nextChange++;
					break;
				}

				// Submit change
				submit();

				// reset label for next change
				processLabel = new ProcessLabel(depot);

				if (super.isStop() || nextChange > revEnd) {
					if (super.isStop()) {
						// Premature stop -- update end rev
						Config.set(CFG.P4_END, nextChange - 1);
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
				changeInfo = new ChangeInfo(record);
				ChangeInterface ci = ProcessFactory.getChange(change,
						changeInfo, depot);
				super.setCurrentChange(ci);
				nextChange++;
				break;

			case NODE:
				// Process node - add actions to tree and current changelist
				if (record.getSvnRevision() >= revStart) {

					((Node) record).setSubBlock(isSubNode(lastRecord, record));

					ci = super.getCurrentChange();
					SvnProcessNode node = new SvnProcessNode(ci, depot,
							(Node) record);
					node.setProcessLabel(processLabel);
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
