package com.perforce.cvs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.svn.history.ChangeAction.Action;

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
		if (logger.isDebugEnabled()) {
			logger.debug("tag entry: " + tagName + " " + entry.getId());
		}

		if (branchList.isBranch(tagName) || "main".equals(tagName) || !isLabel) {
			revList.add(entry);
		} else {
			// added by parent in foundTagEntry()
		}
	}

	@Override
	protected void foundBranchPoint(String tagName, RevisionEntry entry) {
		if (logger.isDebugEnabled()) {
			logger.debug("tag point: " + tagName + " " + entry.getId());
		}

		if (branchList.isBranch(tagName) || !isLabel) {
			RevisionEntry brRev = createBranch(tagName, entry);
			revList.add(brRev);
		} else {
			entry.addLabel(tagName);
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
	private RevisionEntry createBranch(String tagName, RevisionEntry entry) {
		String basePath = getRcsRevision().getPath();
		RcsObjectDelta revision = getRcsRevision().getDelta(entry.getId());

		RevisionEntry branch = new RevisionEntry(revision);
		branch.setState(Action.BRANCH.toString());
		branch.setPath(tagName + "/" + basePath);
		branch.setPseudo(true);
		String fromBranch = getParentName(entry.getId());
		String fromPath = fromBranch + "/" + basePath;
		branch.setFromPath(fromPath);
		return branch;
	}
}
