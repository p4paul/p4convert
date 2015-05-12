package com.perforce.cvs.process;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.cvs.RevisionEntry;
import com.perforce.cvs.RevisionSorter;

public class CvsChangeList {

	private static Logger logger = LoggerFactory.getLogger(CvsChangeList.class);

	transient private RevisionSorter delayedBranch = new RevisionSorter(true);

	private ArrayList<CvsChange> list = new ArrayList<CvsChange>();

	public CvsChangeList(RevisionSorter revSort) throws Exception {

		// Initialise counters
		long sequence = 1;

		RevisionEntry changeEntry = revSort.next();
		RevisionEntry entry;
		revSort.reset();
		RevisionSorter revs = revSort;

		long revStart = (Long) Config.get(CFG.P4_START);
		long revEnd = (Long) Config.get(CFG.P4_END);

		// Iterate over all revisions
		do {
			entry = changeEntry;
			if (logger.isTraceEnabled()) {
				logger.trace("... next change uses: " + entry);
				logger.trace("... from remainder: " + revs.isRemainder());
			}

			// construct next change
			CvsChange cvsChange = new CvsChange(sequence);

			// add matching entries to current change
			while (entry != null && entry.within(changeEntry, revs.getWindow())) {
				if (entry.matches(changeEntry)) {
					buildCvsChange(entry, revs, cvsChange);
				} else {
					// else, revision belongs in another change
					if (logger.isTraceEnabled()) {
						logger.trace("... leaving: " + entry + "(outside)");
					}
				}
				entry = revs.next();
			}

			// update revision list
			revs = revSort;
			revSort.reset();
			delayedBranch.reset();
			if (delayedBranch.hasNext()) {
				revs = delayedBranch;
				if (revSort.hasNext()) {
					long rDate = revSort.next().getDate().getTime();
					long dDate = delayedBranch.next().getDate().getTime();
					if (rDate < dDate) {
						revs = revSort;
					} else {
						revs.setWindow(rDate - dDate);
					}
				}
			}
			revs.reset();

			// fetch revision for next change
			changeEntry = revs.next();
			revs.reset();

			// add change and update counters
			if (!cvsChange.isEmpty()) {
				list.add(cvsChange);
				System.out.print("Creating change: " + sequence + "\r");
				sequence++;
			}
		} while (changeEntry != null);
	}

	/**
	 * Calculates next entry.
	 * 
	 * @param entry
	 * @param changeEntry
	 * @throws Exception
	 */
	private void buildCvsChange(RevisionEntry entry, RevisionSorter revs,
			CvsChange change) throws Exception {

		if (entry.isPseudo() && !revs.isRemainder()) {
			if (logger.isTraceEnabled()) {
				logger.trace("... delaying: " + entry);
			}

			delayedBranch.add(entry);
			revs.drop(entry);
		} else {
			// if no pending revisions...
			if (!change.isPending(entry)) {
				// add entry to current change
				change.addEntry(entry);
				revs.drop(entry);
			} else {
				// if pending revision is a REMOVE and current is a PSEUDO
				// branch
				if (entry.isPseudo() && "dead".equals(entry.getState())) {
					// overlay REMOVE with branch and down-grade to ADD
					entry.setState("Exp");
					change.addEntry(entry);
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

	public int size() {
		return list.size();
	}

	public List<CvsChange> getChanges() {
		return list;
	}
}
