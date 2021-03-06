package com.perforce.cvs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.node.Action;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;

public class RevisionNavigator extends RcsNavigator {

	private Logger logger = LoggerFactory.getLogger(RevisionNavigator.class);

	private RevisionSorter revList;
	private BranchSorter branchList;
	private boolean isLabel;

	public RevisionNavigator(RevisionSorter revisions, BranchSorter branches) {
		revList = revisions;
		branchList = branches;
		try {
			isLabel = (boolean) Config.get(CFG.CVS_LABELS);
		} catch (ConfigException e) {
			isLabel = false;
		}
	}

	@Override
	protected void foundBranchEntry(String tagName, RevisionEntry entry) {
		if (logger.isTraceEnabled()) {
			logger.trace("... RevisionNavigator entry: " + tagName + " " + entry.getId());
		}

		revList.add(entry);
	}

	@Override
	protected void foundBranchPoint(String toTag, RevisionEntry from) {
		if (logger.isTraceEnabled()) {
			logger.trace("... RevisionNavigator point: " + toTag + " " + from.getId());
		}

		if (branchList.isBranch(toTag) || !isLabel) {
			RevisionEntry brRev = createBranch(toTag, from);
			revList.add(brRev);
		} else {
			String format = formatName(toTag);
			from.addLabel(format);
		}
	}

	/**
	 * Create a pseudo change for #1 revision entries
	 * 
	 * @param tagName
	 * @param id
	 * @return
	 * @throws Exception
	 */
	private RevisionEntry createBranch(String toTag, RevisionEntry from) {
		String basePath = getRcsRevision().getPath();
		RcsObjectDelta revision = getRcsRevision().getDelta(from.getId());

		RevisionEntry branch = new RevisionEntry(revision);
		branch.setState(Action.BRANCH);
		branch.setPseudo(true);
		branch.setReverse(from.isReverse());
		branch.addDate(1L);
		branch.setProps(from.getProps());
		branch.setTmpFile(from.getTmpFile());

		String toPath = toTag + "/" + basePath;
		String fromPath = from.getFromPath();

		if (from.isReverse()) {
			branch.setPath(fromPath);
			branch.setFromPath(toPath);
		} else {
			branch.setPath(toPath);
			branch.setFromPath(fromPath);
		}
		return branch;
	}
}
