package com.perforce.cvs;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.cvs.parser.RcsReader;

public class BranchNavigator extends RcsNavigator {

	private Logger logger = LoggerFactory.getLogger(BranchNavigator.class);

	private BranchSorter sort;
	private HashMap<String, Integer> brCount;

	public BranchNavigator(BranchSorter brSort) {
		sort = brSort;
	}

	public void add(RcsReader rcs) throws Exception {
		// for each RCS file reset branch counters
		brCount = new HashMap<String, Integer>();

		// add all revisions in RCS file and count branches
		super.add(rcs);

		// update branch max counts
		sort.join(brCount);
	}

	@Override
	protected void foundBranchEntry(String name, RevisionEntry entry) {
		countBranch(name);
		if (logger.isTraceEnabled()) {
			int count = brCount.get(name);
			logger.trace("... BranchNavigator entry: " + name + " (" + count
					+ ")");
		}
	}

	@Override
	protected void foundBranchPoint(String name, RevisionEntry entry) {
		countBranch(name);
		if (logger.isTraceEnabled()) {
			int count = brCount.get(name);
			logger.trace("... BranchNavigator point: " + name + " (" + count
					+ ")");
		}
	}

	private void countBranch(String name) {
		boolean all = false;
		try {
			all = (Boolean) Config.get(CFG.CVS_BRANCH_ALL);
		} catch (ConfigException e) {
		}

		int count = (all) ? 1 : 0;
		if (brCount.containsKey(name)) {
			count = brCount.get(name) + 1;
		}
		brCount.put(name, count);
	}
}
