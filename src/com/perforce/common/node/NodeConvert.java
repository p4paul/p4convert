package com.perforce.common.node;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.AssetWriter;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.TranslateCharsetType;
import com.perforce.common.depot.DepotConvert;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.change.ChangeConvert;
import com.perforce.svn.history.Action;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;
import com.perforce.svn.parser.Property;
import com.perforce.svn.parser.RecordStateTrace;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergeSource;
import com.perforce.svn.query.QueryHistory;
import com.perforce.svn.query.QueryInterface;

public class NodeConvert implements NodeInterface {

	private Logger logger = LoggerFactory.getLogger(NodeConvert.class);

	private ChangeConvert changelist;
	private DepotConvert depot;
	private RevisionTree tree;

	private String toPath;
	private long toChange;
	private ArrayList<MergeSource> fromList;
	private Property property;
	private Content content;
	private boolean pendingBlock;

	/**
	 * Constructor
	 * 
	 * @param nodePath
	 * @param nodeChange
	 * @param nodeAction
	 */
	public NodeConvert(ChangeConvert cl, DepotConvert nodeDepot,
			RevisionTree nodeTree, boolean subBlock) {
		changelist = cl;
		depot = nodeDepot;
		tree = nodeTree;
		pendingBlock = subBlock;
	}

	@Override
	public void setTo(String path, long change) {
		toPath = path;
		toChange = change;
	}

	@Override
	public void setFrom(ArrayList<MergeSource> from) {
		// Copy mergeinfo sources into new ChangeAction list
		fromList = from;
	}

	@Override
	public void action(Action nodeAction, NodeType type, boolean caseRename)
			throws Exception {
		switch (type) {
		case FILE:
			fileAction(nodeAction);
			break;
		case DIR:
			dirAction(nodeAction);
			break;
		default:
			throw new ConverterException("unknown NodeType(" + type + ")");
		}
	}

	private void fileAction(Action nodeAction) throws Exception {
		QueryInterface query = new QueryHistory(tree);

		ChangeAction act = null;
		// ChangeAction from = null;

		// get type and properties from last revision
		ChangeAction lastAction = query.findLastAction(toPath, toChange);

		switch (nodeAction) {

		case ADD:
		case EDIT:
			act = tree.add(toPath, toChange, nodeAction, content,
					NodeType.FILE, pendingBlock);
			break;

		case UPDATE:
			fileAction(Action.REMOVE);
			if (lastAction != null
					&& !lastAction.getAction().equals(Action.REMOVE)) {
				act = tree.add(toPath, toChange, Action.EDIT, content,
						NodeType.FILE, pendingBlock);
			} else {
				act = tree.add(toPath, toChange, Action.ADD, content,
						NodeType.FILE, pendingBlock);
			}
			break;

		case REMOVE:
			// Remove action should have no real content
			if (!content.isBlob()) {
				if (lastAction != null) {
					content.setType(lastAction.getType());
					content.setProps(lastAction.getProps());
				} else {
					StringBuffer msg = new StringBuffer();
					msg.append("SKIPPING: cannot delete a non-existant revision.\n");
					logger.warn(msg.toString());
					Stats.inc(StatsType.warningCount);
					return;
				}

				if (logger.isTraceEnabled()) {
					StringBuffer sb = new StringBuffer();
					sb.append("no content: ");
					sb.append("action: ");
					sb.append("(" + lastAction.getAction() + ") ");
					sb.append(content.getType());
					logger.trace(sb.toString());
				}
			}

			act = tree.add(toPath, toChange, nodeAction, content,
					NodeType.FILE, pendingBlock);
			break;

		case COPY:
		case BRANCH:
			if (fromList != null && fromList.size() == 1) {
				ChangeAction from = fromList.get(0).getFromNode();

				if (isCopy(toPath, from)) {
					// Use from Type and Properties if no real content
					if (!content.isBlob()) {
						content.setType(from.getType());
						content.setProps(from.getProps());
					}
					// determine branch type and add action to tree
					act = processBranch(depot, from, toPath, toChange, content);
				} else {
					// Can't get here unless case-sensitivity issue job053572
					StringBuffer msg = new StringBuffer();
					msg.append("SKIPPING: cannot branch a deleted revision to");
					msg.append(" a non-existant or deleted target.\n");
					logger.warn(msg.toString());
					Stats.inc(StatsType.warningCount);
					return;
				}
			} else {
				throw new ConverterException("Unexpected number of sources");
			}
			break;

		case MERGE:
			if (fromList != null && !fromList.isEmpty()) {
				// determine branch type and add action to tree
				act = tree.add(toPath, toChange, nodeAction, content,
						NodeType.FILE, pendingBlock);
			} else {
				throw new ConverterException("Expected one or more sources");
			}
			break;

		case LABEL:
			return;

		default:
			throw new ConverterException("Node-action(" + nodeAction + ")");
		}

		if (act != null && act.getAction() != null) {
			// add action to changelist
			changelist.addRevision(act, fromList);
		} else {
			RecordStateTrace.dump();

			StringBuffer msg = new StringBuffer();
			msg.append("Unknown issue:\n");
			msg.append("\tPlease send log files and report the issue to Support.\n");
			msg.append("\t  support@perforce.com and CC:pallen@perforce.com\n\n");

			logger.error(msg.toString());
			throw new RuntimeException(msg.toString());
		}

		// Create archive if node has content
		if (content.isBlob()) {
			AssetWriter archive = new AssetWriter(depot, act);
			archive.write(content);
		}
	}

	private void dirAction(Action nodeAction) throws Exception {
		QueryInterface query = new QueryHistory(tree);

		// Pseudo content
		Content content = new Content();

		switch (nodeAction) {

		case ADD:
		case EDIT:
			// find properties
			processDirProperty(content);
			break;

		case UPDATE:
			dirAction(Action.REMOVE);

			// find properties
			processDirProperty(content);
			break;

		case COPY:
			dirAction(Action.REMOVE);
			dirAction(Action.BRANCH);

			// find properties
			processDirProperty(content);
			break;

		case REMOVE:
			List<ChangeAction> removeActions = query.listLastActions(toPath,
					toChange);
			for (ChangeAction remove : removeActions) {
				if (remove.getAction() != Action.REMOVE) {
					String removePath = remove.getPath();

					// get type and properties from last revision
					ChangeAction lastAction = query.findLastAction(removePath,
							toChange);

					if (lastAction != null) {
						content.setType(lastAction.getType());
						content.setProps(lastAction.getProps());
					}

					if (logger.isTraceEnabled()) {
						StringBuffer sb = new StringBuffer();
						sb.append("remove: " + remove.getPath());
						if (lastAction != null) {
							sb.append("(" + lastAction.getAction() + ") ");
						} else {
							sb.append("(null) ");
						}
						sb.append("type: " + remove.getType());
						logger.trace(sb.toString());
					}

					// add action to tree
					ChangeAction act = tree
							.add(removePath, toChange, Action.REMOVE, content,
									NodeType.FILE, pendingBlock);

					// add action to changelist
					changelist.addRevision(act, null);
				}
			}
			break;

		case BRANCH:
			// Trap condition when more than one source is detected, could be a
			// strange 'unsupported' mergeinfo case.
			if (fromList.size() != 1) {
				StringBuffer sb = new StringBuffer();
				sb.append("fromList has more than one source:");
				sb.append(fromList.toString());
				throw new ConverterException(sb.toString());
			}

			String fromPath = fromList.get(0).getFromPath();
			long fromSvnRev = fromList.get(0).getEndFromSvnRev();
			List<ChangeAction> fromActions = query.listLastActions(fromPath,
					fromSvnRev);
			for (ChangeAction from : fromActions) {

				// wrap from change action into list for changelist
				ArrayList<MergeSource> copyActs = new ArrayList<MergeSource>();
				MergeSource mergeFrom = new MergeSource(from.getPath(),
						from.getStartChange(), from.getEndChange());
				mergeFrom.setFromNode(from);
				copyActs.add(mergeFrom);

				// get 'to' action using branch source and target
				String toFilePath = NodeHelper.remap(fromPath, toPath,
						from.getPath());

				// build new ChangeAction
				if (isCopy(toFilePath, from)) {
					// Use from Type and Properties as there is no content
					content.setType(from.getType());
					content.setProps(from.getProps());

					if (logger.isTraceEnabled()) {
						StringBuffer sb = new StringBuffer();
						sb.append("copy: " + from.getPath());
						sb.append("(" + from.getAction() + ") ");
						sb.append("to " + toFilePath + "\n");
						sb.append("Content type: " + content.getType());
						logger.trace(sb.toString());
					}

					ChangeAction act = processBranch(depot, from, toFilePath,
							toChange, content);

					// add action to changelist
					changelist.addRevision(act, copyActs);
				}
			}

			// find properties
			processDirProperty(content);
			break;

		case LABEL:
			return;

		default:
			throw new ConverterException("Node-action(" + nodeAction + ")");
		}
	}

	/**
	 * Helper method to check if branch action is a copy (prevents population of
	 * deleted revisions if there is no existing target.
	 * 
	 * @param toPath
	 * @param from
	 * @return
	 * @throws Exception
	 */
	private boolean isCopy(String toPath, ChangeAction from) throws Exception {

		QueryInterface query = new QueryHistory(tree);

		// check if target exists
		ChangeAction lastToAction = query.findLastAction(toPath, toChange - 1);

		// copy all non deleted source actions, but if target exists and
		// is not deleted then copy delete.
		if (from.getAction() != Action.REMOVE) {
			return true;
		} else {
			if (lastToAction != null) {
				if (lastToAction.getAction() != Action.REMOVE) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Creates a versioned file containing the directories properties
	 * 
	 * @param content
	 * @throws Exception
	 */
	private void processDirProperty(Content content) throws Exception {

		if (!(Boolean) Config.get(CFG.SVN_PROP_ENABLED) || property == null)
			return;

		// Property file path
		String propPath = toPath;
		if (!toPath.isEmpty()) {
			propPath += "/";
		}
		propPath += Config.get(CFG.SVN_PROP_NAME);

		// Create attributes content and node
		NodeAttributes attributes = new NodeAttributes(property);
		content.setAttributes(attributes);

		Action action = null;
		ChangeAction act = null;

		QueryInterface query = new QueryHistory(tree);
		ChangeAction lastAction = query.findLastAction(propPath, toChange);

		if (!property.isEmpty()) {
			if ((lastAction != null)
					&& (lastAction.getAction() != Action.REMOVE)) {
				action = Action.EDIT;
			} else {
				action = Action.ADD;
			}

			act = tree.add(propPath, toChange, action, content, NodeType.FILE,
					pendingBlock);
			AssetWriter archiveProperty = new AssetWriter(depot, act);
			content.getAttributes().setHeader(act.getPath(),
					act.getEndChange(), act.getEndRev());

			archiveProperty.write(content);
		} else {
			if ((lastAction != null)
					&& (lastAction.getAction() != Action.REMOVE)) {
				action = Action.REMOVE;

				act = tree.add(propPath, toChange, action, content,
						NodeType.FILE, pendingBlock);
			}
		}

		if (act != null) {
			changelist.addRevision(act, null);
		}
	}

	/**
	 * Handles per-file branching and will up/down grade as required
	 * 
	 * has content -> ADD or INTEG same to/from path -> ADD or EDIT from is
	 * REMOVE -> target is REMOVE
	 * 
	 * Returns a modified ChangeAction
	 * 
	 * @param depot
	 * @param from
	 * @param to
	 * @param nodeChange
	 * @param content
	 * @return
	 * @throws Exception
	 */
	private ChangeAction processBranch(DepotConvert depot, ChangeAction from,
			String toPath, long nodeChange, Content content) throws Exception {

		Action action = null;
		ChangeAction act = null;

		if (from.getAction() != Action.REMOVE) {
			QueryInterface query = new QueryHistory(tree);
			ChangeAction lastTo = query.findLastAction(toPath, nodeChange - 1);

			// Branch or Integ
			if (lastTo == null || lastTo.getAction() == Action.REMOVE) {
				action = Action.BRANCH;
			} else {
				action = Action.INTEG;
			}

			// CornerCase: if content, downgrade Action
			if (content != null && content.isBlob()) {
				// ADD action: if no previous rev or a REMOVE action
				if (lastTo == null || lastTo.getAction() == Action.REMOVE) {
					action = Action.ADD;
				} else {
					action = Action.INTEG;
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Dirty edit action to " + action);
				}
			}

			// CornerCase: if same file, downgrade Action
			if (from.getPath().equalsIgnoreCase(toPath) && !pendingBlock) {
				// ADD action: if no previous rev or a REMOVE action
				if (lastTo == null || lastTo.getAction() == Action.REMOVE) {
					action = Action.ADD;
				} else {
					action = Action.EDIT;
				}
				if (logger.isTraceEnabled()) {
					logger.trace("Downgrading action to " + action);
				}
			}

			// Add branch details to RevisionTree
			act = tree.branch(from.getPath(), from.getEndChange(), toPath,
					nodeChange, action, content, NodeType.FILE, pendingBlock);

			// If no content then check lazy copy has an archive file
			if (content.isBlob() == false) {
				AssetWriter archive = new AssetWriter(depot, act);
				archive.check(act.getLazyCopy());
			}
		}

		// Propagate REMOVE action by deleting target
		else {
			act = tree.branch(from.getPath(), from.getEndChange(), toPath,
					nodeChange, Action.REMOVE, content, NodeType.FILE,
					pendingBlock);
		}

		return act;
	}

	@Override
	public void setContent(Content c) throws Exception {
		content = c;
		downgradeTypes();
	}

	@Override
	public void setProperty(Property property) {
		this.property = property;
	}

	/**
	 * Down grade content type for non unicode servers from utf8 to text or
	 * utf32 to binary, this MUST occur before the ChangeAction is added to the
	 * revision tree.
	 * 
	 * @throws ConfigException
	 */
	private void downgradeTypes() throws ConfigException {
		if ((Boolean) Config.get(CFG.P4_UNICODE) == false && content != null) {
			if ((content.getType() == ContentType.UTF_32BE)
					|| (content.getType() == ContentType.UTF_32LE)) {
				if (logger.isInfoEnabled()) {
					logger.info("Non-unicode server, downgrading utf32 file to binary");
				}
				content.setType(ContentType.P4_BINARY);
			}
			if (content.getType().getP4Type() == TranslateCharsetType.UTF8) {
				if (logger.isInfoEnabled()) {
					logger.info("Non-unicode server, downgrading file to text");
				}
				content.setType(ContentType.P4_TEXT);
			}
		}
	}

	@Override
	public void setMergeInfo(MergeInfo merge, String path) {
		RevisionTree node = tree.create(path, NodeType.NULL);
		node.setMergeInfo(merge);
		if (logger.isDebugEnabled()) {
			logger.debug("setMergeInfo on : " + path);
			logger.debug("... " + merge);
		}
	}
}
