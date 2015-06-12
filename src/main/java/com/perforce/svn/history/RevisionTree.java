package com.perforce.svn.history;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.node.Action;
import com.perforce.config.CaseSensitivity;
import com.perforce.svn.parser.Content;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.query.QueryHistory;
import com.perforce.svn.query.QueryInterface;

public class RevisionTree {

	private static Logger logger = LoggerFactory.getLogger(RevisionTree.class);

	public enum NodeType {
		FILE, DIR, NULL
	}

	private String name;
	private RevisionTree parent;
	private Map<String, RevisionTree> children = new HashMap<String, RevisionTree>();
	private List<ChangeAction> actions = new ArrayList<ChangeAction>();
	private MergeInfo pendingMerge;
	private MergeInfo lastMerge;

	private String path;
	private static CaseSensitivity mode = null;
	private int count = 0;
	private RevisionTree.NodeType nodeType = null;

	// Constructor
	public RevisionTree(String name, CaseSensitivity mode) {
		this.name = name;
		RevisionTree.mode = mode;
	}

	public RevisionTree(String name) {
		this.name = name;
	}

	// private method: create node
	public RevisionTree create(String path, NodeType nodeType) {
		RevisionTree node = this;

		// does node already exist?
		RevisionTree nodeFind = this.getNode(path);
		if (nodeFind != null) {
			node = nodeFind;
		} else {
			// create nodes to link whole path
			for (String pathBit : path.split("/")) {
				if (!pathBit.isEmpty()) {
					RevisionTree nodeBit = node.getNode(pathBit);
					if (nodeBit == null) {
						String nameBit = pathBit;

						switch (mode) {
						case FIRST:
							pathBit = pathBit.toLowerCase();
							break;

						case NONE:
							break;

						case LOWER:
							pathBit = pathBit.toLowerCase();
							nameBit = nameBit.toLowerCase();
							break;

						case UPPER:
							pathBit = pathBit.toUpperCase();
							nameBit = nameBit.toUpperCase();
							break;
						}

						nodeBit = new RevisionTree(nameBit);
						nodeBit.setParent(node);
						node.getChildren().put(pathBit, nodeBit);
					}
					node = nodeBit;
				}
			}
			node.setPath(path);
		}
		node.setNodeType(nodeType);
		return node;
	}

	// ------------------------------------------------------------------------
	// Method: add
	// ------------------------------------------------------------------------
	public ChangeAction add(String path, long change, Action action,
			Content content, RevisionTree.NodeType nodeType, boolean subBlock)
			throws Exception {

		return branch(path, change, path, change, action, content, nodeType,
				subBlock);
	}

	/**
	 * Internal method to find lazy reference from previous revision except
	 * 
	 * @param fromPath
	 * @param fromChange
	 * @param action
	 * @return
	 * @throws Exception
	 */
	private ChangeAction getLazyReference(String fromPath, long fromChange,
			ChangeAction next) throws Exception {

		// Find source action (branch or prop only change)
		QueryInterface query = new QueryHistory(this);
		ChangeAction fromAction = query.findLastAction(fromPath, fromChange);

		// Error if no history (and no content)
		if (fromAction == null) {
			String err = ("Source not found: " + fromPath + "@" + fromChange);
			logger.error(err);
			throw new ConverterException(err);
		} else {
			// Set lazy reference
			if (next.getAction() == Action.REMOVE) {
				return next;
			} else {
				return fromAction.getLazyCopy();
			}
		}
	}

	/**
	 * Branches a node action to another part of the tree. Checks for over layed
	 * actions in the same changelist. Check for content in the action (implied
	 * edit) and will reference archive to content otherwise lazy reference to
	 * archive.
	 * 
	 * @throws Exception
	 */
	public ChangeAction branch(String fromPath, long fromChange, String toPath,
			long toChange, Action action, Content content,
			RevisionTree.NodeType nodeType, boolean subBlock) throws Exception {

		RevisionTree node = create(toPath, nodeType);

		// Create next action to add to the tree node
		ChangeAction nextAction = new ChangeAction(toChange);
		nextAction.setAction(action);
		nextAction.setType(content.getType());
		nextAction.setProps(content.getProps());
		nextAction.setMd5(content.getMd5());
		nextAction.setBlob(content.isBlob());
		nextAction.setParent(node);
		nextAction.hasSubBlock(subBlock);
		nextAction.setCompressed(content.isCompressed());

		// Set next reference from lazy or point to real
		ChangeAction lazyRef = nextAction;
		if (content.isBlob() == false)
			lazyRef = getLazyReference(fromPath, fromChange, nextAction);
		nextAction.setLazyCopy(lazyRef);

		// Check for existing changelists (pending and last submitted)
		ChangeAction pendingAction = null;
		ChangeAction lastAction = null;
		QueryInterface query = new QueryHistory(this);
		ChangeAction queryAction = query.findLastAction(toPath, toChange);
		if (queryAction != null) {
			if (queryAction.getEndChange() == toChange) {
				pendingAction = queryAction;
				lastAction = query.findLastAction(toPath, toChange - 1);
			} else {
				lastAction = queryAction;
			}
		}

		// [IF] Remove pending action if delete
		if (pendingAction != null && action == Action.REMOVE) {

			// remove pending action from node
			pendingAction.getParent().getActions().remove(pendingAction);
			if (logger.isTraceEnabled()) {
				logger.trace("remove pending");
			}

			// Overlay pending action with next action
			if (lastAction != null) {
				if (lastAction.getAction() != Action.REMOVE) {
					// don't overlay deleted actions
					ChangeAction overlayAction;
					overlayAction = overlaySubBlocks(pendingAction, nextAction);
					node.getActions().add(overlayAction);
					return overlayAction;
				}
			}

			// Revert pending action
			nextAction.setAction(Action.REVERT);
			return nextAction;
		}

		// [ELSE IF] Merge pending changes...
		else if (pendingAction != null) {
			// remove pending action from node
			pendingAction.getParent().getActions().remove(pendingAction);

			// replace with new pending action
			node.getActions().add(nextAction);
			if (logger.isTraceEnabled()) {
				logger.trace("replace: " + nextAction);
			}
			return nextAction;
		}

		// [ELSE] No pending change...
		else {
			// add new pending action
			node.getActions().add(nextAction);
			if (logger.isTraceEnabled()) {
				logger.trace("add: " + nextAction);
			}
			return nextAction;
		}
	}

	/**
	 * Overlay pending actions with the next action, use subBlock detection on
	 * pending action.
	 * 
	 * @param pendingAction
	 * @param nextAction
	 * @return
	 */
	private ChangeAction overlaySubBlocks(ChangeAction pendingAction,
			ChangeAction nextAction) {

		// Look for sub-block marker on previous pending action.
		if (pendingAction.isSubBlock()) {
			// next action should overlay pending action
			if (logger.isTraceEnabled()) {
				logger.trace("overlay: " + nextAction);
			}
			return nextAction;
		} else {
			// skip next action and use pending
			if (logger.isTraceEnabled()) {
				logger.trace("skipping: " + nextAction);
			}
			return pendingAction;
		}
	}

	/**
	 * Returns the node point in the tree based on the given path
	 * 
	 * @param path
	 * @return
	 */
	public RevisionTree getNode(String path) {
		RevisionTree node = this;
		if (path.isEmpty())
			return node;

		path = pathSensitivity(path, mode);

		for (String pathBit : path.split("/")) {
			node = node.getChildren().get(pathBit);
			if (node == null)
				break;
		}
		return node;
	}

	/**
	 * Set counter depth
	 * 
	 * @param path
	 * @param inc
	 * @return
	 */
	public int setCount(String path, int inc) {
		RevisionTree node = this;
		if (path == null)
			return 0;

		path = pathSensitivity(path, mode);

		for (String pathBit : path.split("/")) {
			node = node.getChildren().get(pathBit);
			node.count += inc;
		}
		return node.count;
	}

	/**
	 * Counter depth
	 * 
	 * @param path
	 * @return
	 */
	public int getCount(String path) {
		return setCount(path, 0);
	}

	/**
	 * Flatten case based on OS
	 * 
	 * @param path
	 * @param mode
	 * @return
	 */
	private String pathSensitivity(String path, CaseSensitivity mode) {
		switch (mode) {
		case FIRST:
			path = path.toLowerCase();
			break;

		case NONE:
			break;

		case LOWER:
			path = path.toLowerCase();
			break;

		case UPPER:
			path = path.toUpperCase();
			break;
		}

		return path;
	}

	/**
	 * toString and recursive toString method to indent and draw a tree of
	 * actions
	 */
	@Override
	public String toString() {
		return toString("", new StringBuffer()).toString();
	}

	private String toString(String indent, StringBuffer sb) {
		sb.append(indent + "[" + nodeType + "] " + name);
		if ((actions != null) && (actions.size() > 0)) {
			sb.append(" (" + actions.size() + ")");
			sb.append(" <" + count + ">");
			for (ChangeAction act : actions) {
				sb.append(act.toString("\n" + indent + "  - "));
			}
		}
		sb.append("\n");
		if (children != null) {
			for (RevisionTree node : children.values()) {
				node.toString(indent + "  ", sb);
			}
		}
		return sb.toString();
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		// File nodes store paths (but not folders)
		if (path != null)
			return path;

		// Build path from all, but last parent
		else {
			String join = name;
			RevisionTree next = getParent();
			while ((next != null) && (next.getParent() != null)) {
				join = next.getName() + "/" + join;
				next = next.getParent();
			}
			return join;
		}
	}

	public void setPath(String path) {
		this.path = path;
	}

	public RevisionTree getParent() {
		return parent;
	}

	public void setParent(RevisionTree parent) {
		this.parent = parent;
	}

	public Map<String, RevisionTree> getChildren() {
		return children;
	}

	public void setChildren(Map<String, RevisionTree> children) {
		this.children = children;
	}

	public List<ChangeAction> getActions() {
		return actions;
	}

	public void setActions(List<ChangeAction> actions) {
		this.actions = actions;
	}

	public int getHeadRev() {
		return actions.size();
	}

	public RevisionTree.NodeType getNodeType() {
		return this.nodeType;
	}

	public void setNodeType(RevisionTree.NodeType nodeType) {
		this.nodeType = nodeType;
	}

	public void setMergeInfo(MergeInfo merge) {
		lastMerge = pendingMerge;
		pendingMerge = merge;
	}

	public MergeInfo getLastMergeInfo() {
		return lastMerge;
	}
}
