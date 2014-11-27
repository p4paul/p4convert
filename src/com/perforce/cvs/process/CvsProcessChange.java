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
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.prescan.Progress;
import com.perforce.svn.query.QueryInterface;

public class CvsProcessChange extends ProcessChange {

	private Logger logger = LoggerFactory.getLogger(CvsProcessChange.class);

	private DepotInterface depot;
	private boolean isLabel;
	private ProcessLabel processLabel;

	private int nodeID = 0;
	private long cvsChange = 1;

	private RevisionSorter revSort;
	private RevisionSorter delayedBranch = new RevisionSorter(true);

	protected void processChange() throws Exception {
		// Initialise labels
		isLabel = (Boolean) Config.get(CFG.CVS_LABELS);

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

		// Find all revisions in CVSROOT
		RcsFileFinder rcsFiles = findRcsFiles();

		// Sort branches
		BranchSorter brSort = buildBranchList(rcsFiles);

		// Sort revisions by date/time
		revSort = buildRevisionList(rcsFiles, brSort);
		logger.info("Sorting revisions:");
		revSort.sort();
		if (logger.isTraceEnabled()) {
			logger.trace(revSort.toString());
		}
		logger.info("... found " + revSort.size() + " revisions\n");

		// Initialise counters
		RevisionEntry changeEntry = revSort.next();
		RevisionEntry entry;
		revSort.reset();
		RevisionSorter revs = revSort;

		// Iterate over all revisions
		do {
			entry = changeEntry;
			if (logger.isTraceEnabled()) {
				logger.trace("... next change uses: " + entry);
				logger.trace("... from remainder: " + revs.isRemainder());
			}

			// initialise labels
			if (isLabel) {
				processLabel = new ProcessLabel(depot);
			}

			// construct next change
			ChangeInterface change;
			long p4Change = cvsChange + (Long) Config.get(CFG.P4_OFFSET);
			ChangeInfo info = new ChangeInfo(changeEntry, cvsChange);
			change = ProcessFactory.getChange(p4Change, info, depot);
			super.setCurrentChange(change);

			// add matching entries to current change
			while (entry != null && entry.within(changeEntry)) {
				if (entry.equals(changeEntry)) {
					nextEntry(entry, revs, change);
				} else {
					// else, revision belongs in another change
					if (logger.isTraceEnabled()) {
						logger.trace("... leaving: " + entry + "(outside)");
					}
				}
				entry = revs.next();
			}

			// submit changes
			submit();

			// submit labels
			if (isLabel) {
				if (logger.isTraceEnabled()) {
					logger.trace("Submitting labels:");
					logger.trace(processLabel.toString());
				}
				processLabel.submit();
			}

			// update revision list
			revs.reset();
			if (delayedBranch.hasNext()) {
				revs = delayedBranch;
			} else {
				revs = revSort;
			}
			revs.reset();

			// fetch revision for next change
			changeEntry = revs.next();
			revs.reset();

			// update counters
			nodeID = 0;
			cvsChange++;
		} while (changeEntry != null);

		// finish up conversion
		close();
	}

	/**
	 * Returns a list of RCS files under a path defined by the CVSROOT and
	 * MODULE if set. Only returns ',v' files.
	 * 
	 * @return
	 * @throws Exception
	 */
	private RcsFileFinder findRcsFiles() throws Exception {
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

	/**
	 * Calculates next entry.
	 * 
	 * @param entry
	 * @param changeEntry
	 * @throws Exception
	 */
	private void nextEntry(RevisionEntry entry, RevisionSorter revs,
			ChangeInterface change) throws Exception {

		String path = entry.getPath();

		if (entry.isPseudo() && !revs.isRemainder()) {
			if (logger.isTraceEnabled()) {
				logger.trace("... delaying: " + entry);
			}

			delayedBranch.add(entry);
			revs.drop(entry);
		} else {
			// if no pending revisions...
			if (!change.isPendingRevision(path)) {
				// add entry to current change
				addEntry(entry, change);
				revs.drop(entry);
			} else {
				// if pending revision is a REMOVE and current is a PSEUDO
				// branch
				Action pendingAct = change.getPendingAction(path);
				if (entry.isPseudo() && pendingAct == Action.REMOVE) {
					// overlay REMOVE with branch and down-grade to ADD
					entry.setState("Exp");
					addEntry(entry, change);
					revs.drop(entry);
				} else {
					// else, revision belongs in another change
					if (logger.isTraceEnabled()) {
						logger.trace("... leaving: " + entry + "(opened)");
					}
				}
			}
		}
	}

	/**
	 * Adds Entry to current change-list
	 * 
	 * @param entry
	 * @param revs
	 * @param change
	 * @throws Exception
	 */
	private void addEntry(RevisionEntry entry, ChangeInterface change)
			throws Exception {

		if (logger.isTraceEnabled()) {
			logger.trace("... adding: " + entry);
		}

		// update entry
		entry.setNodeID(nodeID);
		entry.setCvsChange(cvsChange);

		// and add node to current change
		CvsProcessNode node;
		node = new CvsProcessNode(change, depot, entry);
		node.process();

		// tag any labels
		if (isLabel) {
			processLabel.labelRev(entry, change.getChange());
		}
		nodeID++;
	}
}
