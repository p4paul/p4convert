package com.perforce.svn.query;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.process.MergeInfo;

public class QueryHistory implements QueryInterface {

	private Logger logger = LoggerFactory.getLogger(QueryHistory.class);

	private RevisionTree tree;

	public QueryHistory(RevisionTree tree) {
		this.tree = tree;
	}

	public String getPath(String path) {
		RevisionTree node = tree.getNode(path);
		if (node != null) {
			return node.getPath();
		} else {
			return path;
		}
	}

	@Override
	public boolean hasChildren(String path) {
		ChangeAction findNode = findLastAction(path, 0);
		if (findNode != null && findNode.getAction() != Action.REMOVE) {
			if (logger.isTraceEnabled()) {
				logger.trace("hasChildren: FILE(node)");
			}
			return false;
		}

		List<ChangeAction> acts = listLastActions(path, 0);
		if (acts.isEmpty()) {
			if (logger.isTraceEnabled()) {
				logger.trace("hasChildren: FILE(false)");
			}
			return false;
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("hasChildren: DIR(true)");
			}
			return true;
		}
	}

	// ------------------------------------------------------------------------
	// Method: findHeadRevision
	// ------------------------------------------------------------------------
	public int findHeadRevision(String path, long change) {
		RevisionTree node = tree.getNode(path);
		return findHeadRevision(node, change);
	}

	public int findHeadRevision(RevisionTree node, long change) {
		int rev = 0;
		List<ChangeAction> actions = node.getActions();

		for (ChangeAction act : actions) {
			if (act.getEndChange() <= change) {
				rev++;
			}
		}
		return rev;
	}

	// ------------------------------------------------------------------------
	// Method: findLastAction (just on node i.e. not a sub path)
	// ------------------------------------------------------------------------
	public ChangeAction findLastAction(String path, long change) {
		RevisionTree node = tree.getNode(path);
		return findLastAction(node, change);
	}

	private ChangeAction findLastAction(RevisionTree node, long change) {
		ChangeAction actLatest = null;
		if (node != null) {
			for (ChangeAction act : node.getActions()) {
				if (change == 0 || act.getEndChange() <= change) {
					if ((actLatest == null)
							|| (actLatest.getEndChange() <= act.getEndChange())) {
						actLatest = act;
					}
				}
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("... findLastAction: " + actLatest);
		}
		return actLatest;
	}

	// ------------------------------------------------------------------------
	// Method: listLastActions
	// ------------------------------------------------------------------------
	public List<ChangeAction> listLastActions(String path, long change) {
		List<ChangeAction> list = new ArrayList<ChangeAction>();
		RevisionTree node = tree.getNode(path);
		listLastActions(list, node, change);
		return list;
	}

	private void listLastActions(List<ChangeAction> list, RevisionTree node,
			long change) {
		if (node != null) {
			// add all actions at node to list
			ChangeAction act = findLastAction(node, change);
			if (act != null)
				list.add(act);

			// look for children
			if (node.getChildren() != null) {
				for (RevisionTree subNode : node.getChildren().values()) {
					listLastActions(list, subNode, change);
				}
			}
		}
	}

	@Override
	public int getPendingChangeCount() throws Exception {
		// no pending changes to worry about
		return 0;
	}

	@Override
	public MergeInfo getLastMerge(String path) {
		RevisionTree node = tree.getNode(path);
		return node.getLastMergeInfo();
	}
}
