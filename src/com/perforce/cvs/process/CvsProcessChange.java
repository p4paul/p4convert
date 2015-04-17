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

	private static Logger logger = LoggerFactory
			.getLogger(CvsProcessChange.class);

	private DepotInterface depot;
	private int nodeID = 0;

	protected void processChange() throws Exception {
		// Initialise labels
		isLabels = (Boolean) Config.get(CFG.CVS_LABELS);

		// Create revision tree and depot
		String depotPath = (String) Config.get(CFG.P4_DEPOT_PATH);
		CaseSensitivity caseMode = (CaseSensitivity) Config.get(CFG.P4_CASE);
		depot = ProcessFactory.getDepot(depotPath, caseMode);

		if (logger.isDebugEnabled()) {
			logger.debug(Config.summary());
		}

		// Check for pending changes, abort if any are found
		QueryInterface query = ProcessFactory.getQuery(depot);
		int pending = query.getPendingChangeCount();
		if (pending > 0) {
			String err = "Pending change detected, conversion aborted";
			logger.error(err);
			throw new ConverterException(err);
		}

		// Find all RCS files in CVSROOT
		String cvsroot = (String) Config.get(CFG.CVS_ROOT);
		RcsFileFinder rcsFiles = find(cvsroot);

		// Sort branches
		BranchSorter brSort = buildBranchList(rcsFiles);

		// Build revision list
		RevisionSorter revSort = buildRevisionList(rcsFiles, brSort);

		// Sort revisions by date/time
		revSort.sort();

		// Sort revisions into changes
		ChangeSorter changeSort = new ChangeSorter();
		// changeSort.load("cvsChanges.json");
		changeSort.build(revSort);
		changeSort.store("cvsChanges.json");

		// Iterate over changes and submit
		for (CvsChange cvsChange : changeSort.getChanges()) {

			// initialise labels
			if (isLabels) {
				processLabel = new ProcessLabel(depot);
			}

			ChangeInterface change;
			long sequence = cvsChange.getChange();
			long p4Change = sequence + (Long) Config.get(CFG.P4_OFFSET);

			RevisionEntry changeEntry = cvsChange.getChangeInfo();
			ChangeInfo info = new ChangeInfo(changeEntry, sequence);
			change = ProcessFactory.getChange(p4Change, info, depot);
			super.setCurrentChange(change);

			for (RevisionEntry rev : cvsChange.getRevisions()) {
				buildChange(rev, change, sequence);
			}
			submit();

			// update counters
			nodeID = 0;
		}

		// finish up conversion
		close();
	}

	/**
	 * Adds Entry to current change-list
	 * 
	 * @param entry
	 * @param revs
	 * @param change
	 * @throws Exception
	 */
	private void buildChange(RevisionEntry entry, ChangeInterface change,
			long sequence) throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("... adding: " + entry);
		}

		// update entry
		entry.setNodeID(nodeID);
		entry.setCvsChange(sequence);

		// and add node to current change
		CvsProcessNode node;
		node = new CvsProcessNode(change, depot, entry);
		node.process();

		// tag any labels
		if (isLabels) {
			String id = entry.getId().toString();
			if (entry.getState().equals("dead") && id.equals("1.1")) {
				logger.info("skip labelling dead revision: 1.1");
			} else {
				processLabel.labelRev(entry, change.getChange());
			}
		}
		nodeID++;
	}

	/**
	 * Returns a list of RCS files under a path defined by the CVSROOT and
	 * MODULE if set. Only returns ',v' files.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static RcsFileFinder find(String cvsroot) throws Exception {
		// Find all revisions in CVSROOT
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

		logger.info("Searching for RCS files:");
		RcsFileFinder files = new RcsFileFinder(cvsSearch);

		int count = files.getFiles().size();
		logger.info("... found " + count + " RCS files\n");

		return files;
	}

	/**
	 * Finds Labels/Branches and builds a list of true branches.
	 * 
	 * @return
	 */
	private BranchSorter buildBranchList(RcsFileFinder rcsFiles) {
		logger.info("Building branch list:");
		BranchSorter brSort = new BranchSorter();
		BranchNavigator brNav = new BranchNavigator(brSort);

		Progress progress = new Progress(rcsFiles.getFiles().size());
		int count = 0;
		for (File file : rcsFiles.getFiles()) {
			try {
				RcsReader rcs = new RcsReader(file);
				brNav.add(rcs);
				progress.update(++count);
			} catch (Exception e) {
				logger.warn("Unable to process file: " + file.getAbsolutePath());
				Stats.inc(StatsType.warningCount);
				e.printStackTrace();
			}
		}
		logger.info("... done          \n");

		if (logger.isDebugEnabled()) {
			logger.debug("Sorted branch list:");
			logger.debug(brSort.toString());
		}
		return brSort;
	}

	/**
	 * Return a list of all CVS revisions
	 * 
	 * @param brSorter
	 * @return
	 */
	private RevisionSorter buildRevisionList(RcsFileFinder rcsFiles,
			BranchSorter brSorter) {
		logger.info("Building revision list:");

		RevisionSorter revSorter = new RevisionSorter(false);
		RevisionNavigator revNav = new RevisionNavigator(revSorter, brSorter);

		Progress progress = new Progress(rcsFiles.getFiles().size());
		int count = 0;
		for (File file : rcsFiles.getFiles()) {
			try {
				RcsReader rcs = new RcsReader(file);
				revNav.add(rcs);

				// Extract all RCS deltas to tmp store
				CvsContentReader content = new CvsContentReader(rcs);
				content.cacheContent();
				progress.update(++count);
			} catch (Exception e) {
				logger.warn("Unable to process file: " + file.getAbsolutePath());
				Stats.inc(StatsType.warningCount);
				e.printStackTrace();
			}
		}
		logger.info("... done          \n");

		return revSorter;
	}
}
