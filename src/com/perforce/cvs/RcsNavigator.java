package com.perforce.cvs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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

public abstract class RcsNavigator {

	private Logger logger = LoggerFactory.getLogger(RcsNavigator.class);

	protected abstract void foundBranchEntry(String name, RevisionEntry entry);

	protected abstract void foundBranchPoint(String name, RevisionEntry entry);

	private RcsReader rcsRevision;
	private Map<RcsObjectNum, String> labelMap = new HashMap<RcsObjectNum, String>();
	private Map<RcsObjectNum, String> branchMap = new HashMap<RcsObjectNum, String>();

	protected RcsReader getRcsRevision() {
		return rcsRevision;
	}

	protected Set<Entry<RcsObjectNum, String>> getBranchMap() {
		return branchMap.entrySet();
	}

	protected Set<Entry<RcsObjectNum, String>> getLabelMap() {
		return labelMap.entrySet();
	}

	/**
	 * Takes an RCS file; navigates the RCS to find all revisions, then adds
	 * them to the list for sorting.
	 * 
	 * @param rcs
	 * @throws Exception
	 */
	public void add(RcsReader rcs) throws Exception {

		rcsRevision = rcs;

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
	}

	/**
	 * Follow all the revisions in code-line starting with RcsObjectNum 'id'
	 * 
	 * @return
	 * 
	 * @throws Exception
	 */
	protected void followCodeLine(RcsObjectNum id, RevisionEntry parent)
			throws Exception {
		do {
			RcsObjectDelta revision = rcsRevision.getDelta(id);
			RevisionEntry entry = new RevisionEntry(revision);

			// add label, before revision is added by foundBranch[Point|Entry]
			String labelName = getLabelName(id);
			if (labelName != null) {
				entry.addLabel(labelName);
			}

			// look for IDs matching a tag in the branch map
			for (Entry<RcsObjectNum, String> br : getBranchMap()) {
				String tagName = br.getValue();
				RcsObjectNum brId = getBranchId(br.getKey());
				if (brId.equals(id)) {
					String state = revision.getState();
					if (!"dead".equals(state)) {
						foundBranchPoint(tagName, entry);
					}
				}
			}

			// get tmp content name
			String tmp = (String) Config.get(CFG.CVS_TMPDIR);
			String basePath = rcsRevision.getPath();
			entry.setTmpFile(tmp + "/" + basePath + "/" + id.toString());

			// fetch from id
			RcsObjectNum from = null;
			if (parent != null) {
				from = parent.getId();
			}

			// if branch, get name and add to list of revisions to sort
			String branchName = getBranchName(id, from);
			if (branchName != null) {
				if (!"main".equals(branchName) && id.getMinor() == 1) {
					String fromBranch = getParentName(from);
					String fromPath = fromBranch + "/" + basePath;
					entry.setFromPath(fromPath);
				}
				entry.setPath(branchName + "/" + basePath);
				foundBranchEntry(branchName, entry);
			}

			// follow tags off code line [recursive]
			RcsObjectNumList tagList = revision.getBranches();
			if (!tagList.isEmpty()) {
				for (RcsObjectNum tag : tagList.getList()) {
					followCodeLine(tag, entry);
				}
			}

			// next...
			id = revision.getNext();
		} while (id != null);
	}

	/**
	 * Check if a branch ID contains '0' and return short branch ID. e.g.
	 * 1.56.0.2 ==> 1.56.2, else leave as-is for label
	 * 
	 * @param id
	 * @return
	 */
	protected void buildMaps(RcsObjectTag tag) {
		RcsObjectNum id = tag.getId();
		String name = tag.getTag();

		if (id.getValues().contains(0)) {
			List<Integer> branch = new ArrayList<Integer>(id.getValues());
			int index = branch.size() - 2;
			branch.remove(index);

			RcsObjectNum branchId = new RcsObjectNum(branch);

			if (logger.isDebugEnabled()) {
				logger.debug("\tbranch: '" + name + "' " + branchId.toString());
			}

			branchMap.put(branchId, name);
		} else {
			if (logger.isDebugEnabled()) {
				logger.debug("\tlabel: '" + name + "'");
			}

			labelMap.put(id, name);
		}

	}

	/**
	 * Return shortened branch ID e.g. 1.56.2.1 ==> 1.56.2
	 * 
	 * @param id
	 * @return
	 */
	protected RcsObjectNum getBranchId(RcsObjectNum id) {
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

	protected String getParentName(RcsObjectNum from) {
		if (from == null) {
			return null;
		}
		if (branchMap.containsKey(from)) {
			return branchMap.get(from);
		}
		return "main";
	}

}
