package com.perforce.cvs.process;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.ScanArchive;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.node.Action;
import com.perforce.common.node.NodeInterface;
import com.perforce.common.process.AuditLogger;
import com.perforce.common.process.ProcessFactory;
import com.perforce.common.process.ProcessNode;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.cvs.RevisionEntry;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;
import com.perforce.svn.process.MergeSource;

public class CvsProcessNode extends ProcessNode {

	private Logger logger = LoggerFactory.getLogger(CvsProcessNode.class);

	private RevisionEntry revEntry;

	public CvsProcessNode(ChangeInterface changelist, DepotInterface depot,
			RevisionEntry revEntry) throws Exception {

		super(depot);
		this.changelist = changelist;
		this.revEntry = revEntry;
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
		Action nodeAction = revEntry.getState();

		// find last action using path and Perforce change number
		ChangeAction lastAction = getLastAction(nodePath);

		// if node has archive content (including empty files), then ...
		Content content = new Content(revEntry);

		// add from sources (for branches etc...)
		if (nodeAction == Action.BRANCH) {
			String fromPath = revEntry.getFromPath();
			fromPath = formatPath(fromPath);

			// Find branch from point
			long lastChange = cvsChange - 1;
			for (long c = lastChange; c > 0; c--) {
				ChangeAction next = getQuery().findLastAction(fromPath, c);

				if (next == null) {
					logger.warn("No history (branch from label): "
							+ revEntry.toString());
					logger.info("... downgrade to ADD");
					nodeAction = Action.ADD;
					revEntry.setPseudo(false);
					content = new Content(revEntry);
					break;
				}

				if (!next.getAction().equals(Action.REMOVE)) {
					MergeSource from = new MergeSource(fromPath, 1, c);
					processMergeCredit(from, content, nodeAction);
					from.fetchNode(getQuery());
					fromList.add(from);
					break;
				} else {
					// nodeAction = Action.MERGE;
					revEntry.setPseudo(false);
					content = new Content(revEntry);
				}
			}
		}

		// look for content type
		if (content.isBlob()) {
			// find content type
			ContentType detectedType = findContentType(nodePath, content,
					lastAction);
			content.setType(detectedType);

			// audit content if enabled
			if ((Boolean) Config.get(CFG.AUDIT_ENABLED)) {
				String md5 = content.getMd5();
				String rev = revEntry.getId().toString();
				AuditLogger.log(nodePath, rev, cvsChange, md5);
			}
		}

		// set property using type map and content
		setContentProp(nodePath, content);

		// upgrade edits from ADD to EDIT
		if (nodeAction == Action.ADD) {
			if (lastAction != null && lastAction.getAction() != Action.REMOVE) {
				if (logger.isTraceEnabled()) {
					logger.trace("upgrading to EDIT");
				}
				nodeAction = Action.EDIT;
			}
		}

		// skip REMOVE action on all first revisions in a branch
		if (nodeAction == Action.REMOVE) {
			RcsObjectNum id = revEntry.getId();
			if (id.getMinor() == 1) {
				logger.info("skipping dead revision: " + id);
				return;
			}
		}

		boolean subBlock = false;
		boolean caseRename = false;

		// Verbose output for user
		verbose(cvsChange, nodeID, nodeAction, NodeType.FILE, nodePath,
				content, subBlock);

		// Create Node object
		NodeInterface node = ProcessFactory.getNode(changelist, getDepot(),
				subBlock);
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

	protected List<ContentProperty> getContentProp() {
		Content content = new Content(revEntry);
		return content.getProps();
	}

	private ContentType findContentType(String nodePath, Content content,
			ChangeAction lastAction) throws Exception {

		ContentType type = ContentType.UNKNOWN;

		// Check if RCS 'expand' for binary file
		if (revEntry.isBinary()) {
			type = ContentType.P4_BINARY;
		}

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
}
