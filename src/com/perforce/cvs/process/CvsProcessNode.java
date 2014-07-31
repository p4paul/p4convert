package com.perforce.cvs.process;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.ScanArchive;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.process.ProcessFactory;
import com.perforce.common.process.ProcessNode;
import com.perforce.cvs.RevisionEntry;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.node.NodeInterface;
import com.perforce.svn.parser.Content;
import com.perforce.svn.process.MergeSource;
import com.perforce.svn.query.QueryInterface;

public class CvsProcessNode extends ProcessNode {

	private Logger logger = LoggerFactory.getLogger(CvsProcessNode.class);

	private ChangeInterface changelist;
	private DepotInterface depot;
	private RevisionEntry revEntry;
	private QueryInterface query;

	public CvsProcessNode(ChangeInterface changelist, DepotInterface depot,
			RevisionEntry revEntry) throws Exception {

		super(depot);
		this.depot = super.getDepot();
		this.changelist = changelist;
		this.revEntry = revEntry;
		this.query = super.getQuery();
	}

	protected void processFile() throws Exception {

		// Get revision path and format
		String nodePath = revEntry.getPath();
		nodePath = formatPath(nodePath);
		revEntry.setPath(nodePath);

		long cvsChange = revEntry.getCvsChange();
		int nodeID = revEntry.getNodeID();

		ArrayList<MergeSource> fromList = new ArrayList<MergeSource>();

		// find action and node type
		Action nodeAction = getNodeAction();
		
		// find last action using path and Perforce change number
		ChangeAction lastAction = query.findLastAction(nodePath,
				changelist.getChange());

		// if node has archive content (including empty files), then ...
		Content content = new Content(revEntry);
		if (content.isBlob()) {

			// find content type
			ContentType detectedType = findContentType(nodePath, content,
					lastAction);
			content.setType(detectedType);
		}
		
		// add from sources (for branches etc...)
		if (nodeAction == Action.BRANCH) {
			String fromPath = revEntry.getFromPath();
			fromPath = formatPath(fromPath);
			revEntry.setPath(fromPath);
			
			MergeSource from = new MergeSource(fromPath, 1, cvsChange - 1);
			processMergeCredit(from, content, nodeAction);
			from.fetchNode(query);
			fromList.add(from);
		}

		// upgrade edits from ADD to EDIT
		if (nodeAction == Action.ADD) {
			if (lastAction != null && lastAction.getAction() != Action.REMOVE) {
				if (logger.isTraceEnabled()) {
					logger.trace("upgrading to EDIT");
				}
				nodeAction = Action.EDIT;
			}
		}



		boolean subBlock = false;
		boolean caseRename = false;

		// Verbose output for user
		verbose(cvsChange, nodeID, nodeAction, NodeType.FILE, nodePath,
				content, subBlock);

		// Create Node object
		NodeInterface node = ProcessFactory
				.getNode(changelist, depot, subBlock);
		node.setTo(nodePath, cvsChange);
		node.setFrom(fromList);
		node.setContent(content);
		node.action(nodeAction, NodeType.FILE, caseRename);
	}

	protected void processDir() throws Exception {
		logger.error("directories are not processed");
		throw new Exception();
	}

	protected NodeType getNodeType() throws Exception {
		return NodeType.FILE;
	}

	private ContentType findContentType(String nodePath, Content content,
			ChangeAction lastAction) throws Exception {

		ContentType type = ContentType.UNKNOWN;

		// Check if extension is defined in typemap
		if (type == ContentType.UNKNOWN) {
			type = TypeMap.getContentType(nodePath);
		}

		// Check history for empty files
		if (type == ContentType.UNKNOWN) {
			if (content.getLength() == 0) {
				// Cannot assume its a SYMLINK as the earlier conditions were
				// not met; see test case 090.
				if (lastAction != null
						&& lastAction.getType() != ContentType.SYMLINK) {
					type = lastAction.getType();
				} else {
					type = ContentType.P4_TEXT;
				}
				if (logger.isDebugEnabled()) {
					logger.debug("empty file using type: " + type);
				}
			}
		}

		// detect type if not set
		if (type == ContentType.UNKNOWN) {
			type = ScanArchive.detectType(content);
		}

		// Throw exception if no type found
		if (type == ContentType.UNKNOWN) {
			throw new ConverterException("Type not detected.  size: "
					+ content.getLength());
		}

		return type;
	}

	/**
	 * Reads parsed STATE field from the RCS file and returns the Perforce
	 * action
	 * 
	 * @return
	 */
	private ChangeAction.Action getNodeAction() {
		ChangeAction.Action action = null;

		// Set node condition ('add', 'change' or 'delete')
		String s = revEntry.getState();
		if (s != null) {
			if ("Exp".equals(s))
				action = ChangeAction.Action.ADD;
			else if ("Stab".equals(s))
				action = ChangeAction.Action.ADD;
			else if ("Rel".equals(s))
				action = ChangeAction.Action.ADD;
			else if ("dead".equals(s))
				action = ChangeAction.Action.REMOVE;
			else if ("BRANCH".equals(s))
				action = ChangeAction.Action.BRANCH;
			else
				throw new RuntimeException("unknown STATE " + s);
		} else {
			throw new RuntimeException("no STATE in RCS");
		}

		return action;
	}
}
