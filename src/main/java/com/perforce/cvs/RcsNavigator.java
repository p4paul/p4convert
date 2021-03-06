package com.perforce.cvs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.node.Action;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.cvs.parser.RcsReader;
import com.perforce.cvs.parser.RcsSchema;
import com.perforce.cvs.parser.rcstypes.RcsObjectAdmin;
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
	private Map<String, RcsObjectNum> labelsMap;
	private Map<RcsObjectNum, String> branchMap;

	protected RcsReader getRcsRevision() {
		return rcsRevision;
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
		labelsMap = new HashMap<String, RcsObjectNum>();
		branchMap = new HashMap<RcsObjectNum, String>();

		if (logger.isDebugEnabled()) {
			logger.debug("RCS file: " + rcs.getRcsFile().getName());
			logger.debug("Process symbols:");
		}

		// get label and branch names
		RcsObjectAdmin admin = rcs.getAdmin();
		RcsObjectTagList tags = admin.getSymbols();
		for (RcsObjectTag tag : tags.getList()) {
			buildMaps(tag);
		}

		// get (default) branch if defined
		if (rcs.getAdmin().containsKey(RcsSchema.BRANCH)) {
			RcsObjectNum branch = (RcsObjectNum) admin.get(RcsSchema.BRANCH);
			String tag = "branch_" + branch;
			branchMap.put(branch, tag);
		}

		// Process HEAD code-line and recurse branches
		RcsObjectNum head = rcs.getAdmin().getHead();
		followCodeLine(head, null);
	}

	/**
	 * Check if a branch ID contains '0' and return short branch ID. e.g.
	 * 1.56.0.2 ==> 1.56.2, else leave as-is for label
	 * 
	 * @param id
	 * @return
	 * @throws ConfigException
	 */
	private void buildMaps(RcsObjectTag tag) throws ConfigException {
		RcsObjectNum id = tag.getId();
		List<Integer> ids = id.getValues();
		String name = tag.getTag();

		if (ids.size() >= 4 && ids.contains(0)) {
			List<Integer> branch = new ArrayList<Integer>(ids);
			branch.remove(branch.lastIndexOf(0));
			RcsObjectNum branchId = new RcsObjectNum(branch);
			branchMap.put(branchId, name);

			if (logger.isDebugEnabled()) {
				logger.debug("... Symbol: (branch) " + name + ":"
						+ branchId.toString());
			}
		} else if (ids.size() % 2 != 0 && !ids.contains(0)) {
			branchMap.put(id, name);

			if (logger.isDebugEnabled()) {
				logger.debug("... Symbol: (feature) " + name + ":"
						+ id.toString());
			}
		} else {
			labelsMap.put(name, id);

			if (logger.isDebugEnabled()) {
				logger.debug("... Symbol: (label) " + name);
			}
		}
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

			// set file permission properties for entry
			entry.setProps(rcsRevision.getProps());

			// set binary if 'expand' is set
			RcsObjectAdmin admin = rcsRevision.getAdmin();
			entry.setBinary(admin.getExpand());

			// add label, before revision is added by foundBranch[Point|Entry]
			List<String> labels = getLabels(id);
			if (!labels.isEmpty()) {
				for (String l : labels) {
					String format = formatName(l);
					entry.addLabel(format);
				}
			}

			// get tmp content name
			String tmp = (String) Config.get(CFG.CVS_TMPDIR);
			String basePath = rcsRevision.getPath();
			entry.setTmpFile(tmp + "/" + basePath + "/" + id.toString());

			// fetch from id
			RcsObjectNum parentId = (parent != null) ? parent.getId() : null;
			NodeTarget node = getNodeName(id, parentId);

			// Process Symbol list for branches
			for (Entry<RcsObjectNum, String> br : getBranchMap()) {
				// Look for a match
				RcsObjectNum brId = getBranchId(br.getKey());
				if (brId.equals(id)) {
					String tag = br.getValue();
					if (node != null) {
						String fromBranch = node.getName();
						if (node.isReverse()) {
							fromBranch = node.getFrom();
						}
						String fromPath = fromBranch + "/" + basePath;
						entry.setFromPath(fromPath);
						entry.setReverse(node.isReverse());

						if (entry.getState() == Action.REMOVE) {
							logger.debug("skipping dead source: " + entry);
						} else {
							foundBranchPoint(tag, entry);
						}
					}
				}
			}

			// if branch, get name and add to list of revisions to sort
			if (node != null) {
				String nodeName = node.getName();
				String toPath = nodeName + "/" + basePath;
				// node is a branch

				if (!nodeName.equals("main") && id.getMinor() == 1) {
					String fromBranch = getParentName(parentId);
					String fromPath = fromBranch + "/" + basePath;
					entry.setFromPath(fromPath);
				}

				entry.setPath(toPath);
				foundBranchEntry(nodeName, entry);
			} else {
				if (logger.isTraceEnabled()) {
					logger.trace("labeled? not adding: " + entry);
				}
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
	 * Format label name, by substituting '{symbol}' with the tag symbol.
	 * 
	 * @param name
	 * @return
	 */
	protected String formatName(String name) {
		String formatter;
		try {
			formatter = (String) Config.get(CFG.CVS_LABEL_FORMAT);
		} catch (ConfigException e) {
			return name;
		}

		if (!formatter.isEmpty()) {
			String label = formatter;
			name = label.replaceAll("\\{symbol\\}", name);

			// replace all spaces with '_'
			name = name.replaceAll(" ", "_");
		}
		return name;
	}

	private Set<Entry<RcsObjectNum, String>> getBranchMap() {
		return branchMap.entrySet();
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

	private NodeTarget getNodeName(RcsObjectNum id, RcsObjectNum from) {
		// check for mainline
		if (from == null) {
			return new NodeTarget("main", "main", false);
		}

		String fromName = getParentName(from);
		// check for branch
		RcsObjectNum branchId = getBranchId(id);
		if (branchMap.containsKey(branchId)) {
			String name = branchMap.get(branchId);
			return new NodeTarget(name, fromName, false);
		}
		return null;
	}

	private List<String> getLabels(RcsObjectNum id) {
		ArrayList<String> list = new ArrayList<String>();
		if (labelsMap.containsValue(id)) {
			for (Entry<String, RcsObjectNum> e : labelsMap.entrySet()) {
				if (e.getValue().equals(id)) {
					list.add(e.getKey());
				}
			}
		}
		return list;
	}

	private String getParentName(RcsObjectNum from) {
		if (from == null) {
			return null;
		}
		RcsObjectNum branchId = getBranchId(from);
		// check the list of known branches.
		if (branchMap.containsKey(branchId)) {
			return branchMap.get(branchId);
		}
		// guess it must be from the 'main'
		return "main";
	}

}
