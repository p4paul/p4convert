package com.perforce.cvs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.cvs.parser.RcsReader;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.cvs.parser.rcstypes.RcsObjectNumList;
import com.perforce.cvs.parser.rcstypes.RcsObjectTag;
import com.perforce.cvs.parser.rcstypes.RcsObjectTagList;
import com.perforce.svn.history.ChangeAction.Action;

public class RevisionSorter {

	private Logger logger = LoggerFactory.getLogger(RevisionSorter.class);

	private int index = 0;
	private List<RevisionEntry> list = new ArrayList<RevisionEntry>();
	private Map<RcsObjectNum, String> labelMap;
	private Map<RcsObjectNum, String> branchMap;

	private RcsReader rcsRevision;

	public void add(RcsReader rcs) throws Exception {

		rcsRevision = rcs;
		labelMap = new HashMap<RcsObjectNum, String>();
		branchMap = new HashMap<RcsObjectNum, String>();

		if (logger.isDebugEnabled()) {
			logger.debug("RCS file: " + rcs.getRcsFile().getName());
		}

		// get label and branch names
		RcsObjectTagList tags = rcs.getAdmin().getSymbols();
		for (RcsObjectTag tag : tags.getList()) {
			buildMaps(tag);
		}

		// Process HEAD code-line and recurse branches
		RcsObjectNum head = rcs.getAdmin().getHead();
		followCodeLine(head, null);

		populateFullBranch();
	}

	/**
	 * Check if a branch ID contains '0' and return short branch ID. e.g.
	 * 1.56.0.2 ==> 1.56.2, else leave as-is for label
	 * 
	 * @param id
	 * @return
	 */
	private void buildMaps(RcsObjectTag tag) {
		RcsObjectNum id = tag.getId();
		String name = tag.getTag();

		if (id.getValues().contains(0)) {
			List<Integer> branch = new ArrayList<Integer>(id.getValues());
			int index = branch.size() - 2;
			branch.remove(index);

			RcsObjectNum branchId = new RcsObjectNum(branch);

			logger.debug("\tbranch: '" + name + "' " + branchId.toString());
			branchMap.put(branchId, name);
		} else {

			// TODO --------------------- LABEL ----------------------------
			// Could add tag all revs against label - static label, or
			// create an automatic label. Need to somehow insert a dummy rev for
			// change-list sorting?

			logger.debug("\tlabel: '" + name + "'");
			labelMap.put(id, name);
		}

	}

	/**
	 * Identify all CVS branches and ensure all non-deleted revisions are
	 * branched.
	 * 
	 * @throws Exception
	 */
	private void populateFullBranch() throws Exception {
		for (Entry<RcsObjectNum, String> branch : branchMap.entrySet()) {
			// branch keys are truncated (1.56.2), so trim by one more to get
			// from ID (1.56)
			RcsObjectNum from = getBranchId(branch.getKey());
			String tagName = branch.getValue();

			// check revision is not deleted 'dead'
			RcsObjectDelta revision = rcsRevision.getDelta(from);
			if (revision == null) {
				logger.debug("cannot find: [" + from + "]");
			}
			String state = revision.getState();
			if (!"dead".equals(state)) {
				RevisionEntry entry = createBranch(tagName, from);
				list.add(entry);
			}
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
	private RevisionEntry createBranch(String tagName, RcsObjectNum id)
			throws Exception {
		String basePath = rcsRevision.getPath();
		RcsObjectDelta revision = rcsRevision.getDelta(id);

		RevisionEntry branch = new RevisionEntry(revision);
		branch.setComment("Branch: ");
		branch.setAuthor("branch");
		branch.clearCommitId();
		branch.setState(Action.BRANCH.toString());
		branch.setPath(tagName + "/" + basePath);
		branch.addDate(1L);
		String fromBranch = getParentName(id);
		String fromPath = fromBranch + "/" + basePath;
		branch.setFromPath(fromPath);
		return branch;
	}

	/**
	 * Follow all the revisions in code-line starting with RcsObjectNum 'id'
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	private void followCodeLine(RcsObjectNum id, RcsObjectNum from)
			throws Exception {
		do {
			RcsObjectDelta revision = rcsRevision.getDelta(id);
			RevisionEntry change = new RevisionEntry(revision);

			// get tmp content name
			String tmp = (String) Config.get(CFG.CVS_TMPDIR);
			String basePath = rcsRevision.getPath();
			change.setTmpFile(tmp + "/" + basePath + "/" + id.toString());

			// if branch, get name and add to list of revisions to sort
			String branchName = getBranchName(id, from);
			if (branchName != null) {
				if (!"main".equals(branchName) && id.getMinor() == 1) {
					// change.setState(Action.BRANCH.toString());
					String fromBranch = getParentName(from);
					String fromPath = fromBranch + "/" + basePath;
					change.setFromPath(fromPath);
				}
				change.setPath(branchName + "/" + basePath);
				list.add(change);
			}

			// add label to revision
			String labelName = getLabelName(id);
			if (labelName != null) {
				change.addLabel(labelName);
			}

			// follow tags off code line [recursive]
			RcsObjectNumList tagList = revision.getBranches();
			if (!tagList.isEmpty()) {
				for (RcsObjectNum tag : tagList.getList()) {
					followCodeLine(tag, id);
				}
			}

			// next...
			id = revision.getNext();
		} while (id != null);
	}

	/**
	 * Return shortened branch ID e.g. 1.56.2.1 ==> 1.56.2
	 * 
	 * @param id
	 * @return
	 */
	private RcsObjectNum getBranchId(RcsObjectNum id) {
		List<Integer> branch = new ArrayList<Integer>(id.getValues());
		int index = branch.size() - 1;
		branch.remove(index);

		return new RcsObjectNum(branch);
	}

	private String getBranchName(RcsObjectNum id, RcsObjectNum from) {
		// check for mainline
		if (from == null) {
			return "main";
		}

		// return branch name
		RcsObjectNum branchId = getBranchId(id);
		if (branchMap.containsKey(branchId)) {
			return branchMap.get(branchId);
		}
		return null;
	}

	private String getLabelName(RcsObjectNum id) {
		if (labelMap.containsKey(id)) {
			return labelMap.get(id);
		}
		return null;
	}

	private String getParentName(RcsObjectNum from) {
		if (from == null) {
			return null;
		}
		if (branchMap.containsKey(from)) {
			return branchMap.get(from);
		}
		return "main";
	}

	public void sort() {
		Collections.sort((List<RevisionEntry>) list);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (RevisionEntry c : list) {
			sb.append(c.toString());
			sb.append("\n");
		}
		return sb.toString();
	}

	public RevisionEntry next() {
		if (hasNext()) {
			RevisionEntry entry = list.get(index);
			index++;
			return entry;
		}
		return null;
	}

	public boolean hasNext() {
		return (index < list.size());
	}

	public boolean isEmpty() {
		return list.isEmpty();
	}

	public void drop(RevisionEntry entry) {
		list.remove(entry);
		if (index > 0) {
			index--;
		}
	}

	public void reset() {
		index = 0;
	}
}
