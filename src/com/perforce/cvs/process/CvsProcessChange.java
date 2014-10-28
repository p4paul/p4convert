package com.perforce.cvs.process;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.process.ChangeInfo;
import com.perforce.common.process.ProcessChange;
import com.perforce.common.process.ProcessFactory;
import com.perforce.common.process.ProcessLabel;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.cvs.BranchNavigator;
import com.perforce.cvs.BranchSorter;
import com.perforce.cvs.RevisionEntry;
import com.perforce.cvs.RevisionNavigator;
import com.perforce.cvs.RevisionSorter;
import com.perforce.cvs.asset.CvsContentReader;
import com.perforce.cvs.parser.RcsFileFinder;
import com.perforce.cvs.parser.RcsReader;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.prescan.Progress;
import com.perforce.svn.query.QueryInterface;

public class CvsProcessChange extends ProcessChange {

	private Logger logger = LoggerFactory.getLogger(CvsProcessChange.class);

	protected void processChange() throws Exception {
		// Read configuration settings for locals
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

		// Find all revisions in CVSROOT
		String cvsroot = (String) Config.get(CFG.CVS_ROOT);
		if (!new File(cvsroot).exists()) {
			String err = "Invalid path for CVSROOT: " + cvsroot;
			logger.error(err + "\n");
			throw new Exception();
		} else {
			if (!cvsroot.endsWith("/")) {
				cvsroot = new String(cvsroot + "/");
			}
			if (!cvsroot.startsWith("/")) {
				cvsroot = new String("/" + cvsroot);
			}
		}

		// Append module to search path
		String module = (String) Config.get(CFG.CVS_MODULE);
		String cvsSearch = cvsroot + module + "/";
		if (!new File(cvsSearch).exists()) {
			cvsSearch = cvsroot;
		}
		logger.info("Searching for RCS files...");
		RcsFileFinder rcsFiles = new RcsFileFinder(cvsSearch);

		logger.info("Building branch list...");
		BranchSorter brSort = new BranchSorter();
		BranchNavigator brNav = new BranchNavigator(brSort);
		for (File file : rcsFiles.getFiles()) {
			try {
				RcsReader rcs = new RcsReader(file);
				brNav.add(rcs);

			} catch (Exception e) {
				logger.warn("Unable to process file: " + file.getAbsolutePath());
				Stats.inc(StatsType.warningCount);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Sorted branch list:");
			logger.debug(brSort.toString());
		}

		logger.info("Building revision list...");
		RevisionSorter revSort = new RevisionSorter();
		RevisionNavigator revNav = new RevisionNavigator(revSort, brSort);
		Progress progress = new Progress(rcsFiles.getFiles().size());
		int count = 0;
		for (File file : rcsFiles.getFiles()) {
			try {
				RcsReader rcs = new RcsReader(file);
				revNav.add(rcs);

				// Extract all RCS deltas to tmp store
				CvsContentReader content = new CvsContentReader(rcs);
				content.cacheContent();
				count++;
				progress.update(count);
			} catch (Exception e) {
				logger.warn("Unable to process file: " + file.getAbsolutePath());
				Stats.inc(StatsType.warningCount);
			}
		}

		// Sort revisions by date/time
		logger.info("Sorting revisions...");
		revSort.sort();

		// Enable labels
		boolean isLabel = (Boolean) Config.get(CFG.CVS_LABELS);
		ProcessLabel processLabel = null;

		// Initialise counters
		int nodeID = 0;
		long nextChange = 1;

		RevisionEntry entry = null;
		RevisionEntry changeEntry = revSort.next();
		revSort.reset();

		// construct first change
		long cvsChange = 0;
		ChangeInterface ci = null;

		do {
			entry = changeEntry;

			// initialise labels
			if (isLabel) {
				processLabel = new ProcessLabel(depot);
			}

			// construct change
			cvsChange = nextChange;
			long p4Change = nextChange + (Long) Config.get(CFG.P4_OFFSET);
			ChangeInfo info = new ChangeInfo(changeEntry, cvsChange);
			ci = ProcessFactory.getChange(p4Change, info, depot);
			super.setCurrentChange(ci);

			while (entry != null && entry.within(changeEntry)) {
				String path = entry.getPath();
				boolean opened = ci.isPendingRevision(path);

				if (entry.equals(changeEntry) && !opened) {
					if (logger.isTraceEnabled()) {
						logger.trace(">>> adding: " + entry.toString());
					}

					// update entry
					entry.setNodeID(nodeID);
					entry.setCvsChange(cvsChange);

					// and add node to current change
					CvsProcessNode node;
					node = new CvsProcessNode(ci, depot, entry);
					node.process();
					revSort.drop(entry);

					// tag any labels
					if (isLabel) {
						processLabel.labelRev(entry, ci.getChange());
					}

					nodeID++;
				} else {
					if (logger.isTraceEnabled()) {
						logger.trace("<<< leaving: " + entry.toString());
					}
				}
				entry = revSort.next();
			}

			// submit current change
			submit();

			// submit labels
			if (isLabel) {
				if (logger.isDebugEnabled()) {
					logger.debug(processLabel.toString());
				}
				processLabel.submit();
			}

			// update current change
			revSort.reset();
			changeEntry = revSort.next();
			revSort.reset();
			nodeID = 0;
			nextChange++;

		} while (changeEntry != null);

		// finish up conversion
		close();
	}
}
