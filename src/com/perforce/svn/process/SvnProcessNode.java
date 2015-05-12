package com.perforce.svn.process;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.ScanArchive;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.journal.Digest;
import com.perforce.common.label.LabelHistory;
import com.perforce.common.node.NodeInterface;
import com.perforce.common.process.AuditLogger;
import com.perforce.common.process.ChangeInfo;
import com.perforce.common.process.ProcessFactory;
import com.perforce.common.process.ProcessLabel;
import com.perforce.common.process.ProcessNode;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.change.ChangeMap;
import com.perforce.svn.history.Action;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;
import com.perforce.svn.parser.Node;
import com.perforce.svn.parser.Property;
import com.perforce.svn.prescan.ExcludeParser;
import com.perforce.svn.query.QueryInterface;
import com.perforce.svn.tag.TagEntry;
import com.perforce.svn.tag.TagParser;

public class SvnProcessNode extends ProcessNode {

	private Logger logger = LoggerFactory.getLogger(SvnProcessNode.class);

	private Node record;
	private ProcessLabel processLabel;
	private boolean isLabels;

	public SvnProcessNode(ChangeInterface changelist, DepotInterface depot,
			Node record) throws Exception {

		super(depot);

		this.record = record;
		this.changelist = changelist;

		// check for delta format
		String delta = record.findHeaderString("Text-delta");
		if (delta != null && "true".equals(delta)) {
			throw new ConverterException(
					"ABORTING: Unable to read SVN Delta format.");
		}

		isLabels = (Boolean) Config.get(CFG.SVN_LABELS);
	}

	/**
	 * Processes the action on a file node, adding the action to the current
	 * changelist
	 * 
	 * @throws ConverterException
	 * @throws
	 */
	protected void processFile() throws Exception {

		ArrayList<MergeSource> fromList = new ArrayList<MergeSource>();

		// find change numbers
		int svnRev = record.getSvnRevision();
		int nodeID = record.getNodeNumber();

		// find sub blocks
		boolean subBlock = record.isSubBlock();
		boolean caseRename = false;

		// find target path
		String nodePath = record.findHeaderString("Node-path");
		nodePath = formatPath(nodePath);

		// find action and node type
		Action nodeAction = Action.parse(record);

		// skip if excluded
		if (ExcludeParser.isSkipped(nodePath) && !isLabels) {
			char a = nodeAction.toString().charAt(0);
			logger.info("skipping " + a + ":F " + nodePath);
			return;
		}

		// find last action using path and Perforce change number
		ChangeAction lastAction = getLastAction(nodePath);

		// if node has archive content (including empty files), then ...
		Content content = record.getContent();
		if (content.isBlob()) {

			// find content type
			ContentType detectedType = findContentType(nodePath, content,
					nodeAction, lastAction);
			content.setType(detectedType);

			// set MD5, if defined
			content.setMd5(getContentMD5());

			// audit content if enabled
			if ((Boolean) Config.get(CFG.AUDIT_ENABLED)) {
				String md5 = content.getMd5();
				long lastChange = ChangeMap.getChange(svnRev - 1);
				String rev = String.valueOf(svnRev);
				AuditLogger.log(nodePath, rev, lastChange + 1, md5);
			}
		} else {
			// non archive content (deletes and lazy copies)
			if (lastAction != null) {
				content.setType(lastAction.getType());
			} else {
				// lazy branched files - type determined when branched. A file
				// that is branch+edit will have content.
				// see: NodeConvert.fileAction() case BRANCH
			}
		}

		// check for MergeSource and add to pending change
		MergeSource from = getFromSource();
		if (from != null) {
			// Trap condition when fromPath is missing; customers will ignore
			// advice on the user of filters. (job062206)
			if (from.getFromNode() == null) {
				StringBuffer msg = new StringBuffer();
				msg.append("CASE-SENSITIVITY ISSUE:\n");
				msg.append("\tFrom node is missing from dataset; skipping!\n");
				msg.append("\tPlease check case options and platform types.\n");
				msg.append("\t  " + CFG.P4_CASE.toString());
				msg.append(" = " + Config.get(CFG.P4_CASE) + "\n");
				msg.append("\t  " + CFG.P4_C1_MODE.toString());
				msg.append(" = " + Config.get(CFG.P4_C1_MODE) + "\n");

				logger.warn(msg.toString());
				Stats.inc(StatsType.warningCount);
				return;
			}

			processMergeCredit(from, content, nodeAction);
			fromList.add(from);
			if (logger.isDebugEnabled()) {
				logger.debug("from:F " + from.toString());
			}
			if (from.getFromPath().equalsIgnoreCase(nodePath)) {
				changelist.setMergeSource(from);
			}
		} else {
			MergeSource pending = changelist.getMergeSource();
			caseRename = isCaseRename(pending, nodePath, nodeAction);
			changelist.setMergeSource(null);
		}

		// check for svn:mergeinfo on file
		MergeInfo fileMergeInfo = processMergeInfo(nodePath);

		// check for svn:mergeinfo pending change, else add file
		List<MergeInfo> mergeInfoList = changelist.getMergeInfoList();
		if (mergeInfoList == null) {
			mergeInfoList = new ArrayList<MergeInfo>();
			mergeInfoList.add(fileMergeInfo);
		}

		// upgrade EDIT to INTEG for svn:mergeinfo
		for (MergeInfo mergeInfo : mergeInfoList) {
			if (mergeInfo != null && !mergeInfo.isEmpty()
					&& nodeAction == Action.EDIT) {

				// check history for latest merge and remove previous
				MergeInfo lastMerge = getQuery().getLastMerge(
						mergeInfo.getPath());
				if (logger.isDebugEnabled()) {
					logger.debug("lastMerge: " + lastMerge);
					logger.debug("currentMerge: " + mergeInfo);
				}

				MergeInfo deltaMerge = mergeInfo.removeLast(lastMerge);
				if (logger.isDebugEnabled()) {
					logger.debug("resultMerge: " + deltaMerge);
				}

				// get merge sources for new merges
				ArrayList<MergeSource> mergeSources = deltaMerge
						.getMergeSources(nodePath, getQuery());

				if (mergeSources != null && !mergeSources.isEmpty()) {
					if (logger.isDebugEnabled()) {
						logger.debug("mergeinfo - upgrading EDIT to INTEG");
					}
					nodeAction = Action.MERGE;

					for (MergeSource src : mergeSources) {
						if (src != null) {
							processMergeCredit(src, content, nodeAction);
							fromList.add(src);
							if (logger.isDebugEnabled()) {
								logger.debug("from:D " + src.toString());
							}
						}
					}
				}
			}
		}

		// down grade edits to adds if new revision
		if (lastAction == null || lastAction.getAction() == Action.REMOVE) {
			if (nodeAction == Action.EDIT) {
				// TODO watch out for case renames (previous remove)
				if (logger.isDebugEnabled()) {
					logger.debug("downgrading to ADD");
				}
				nodeAction = Action.ADD;
			}
		}

		// set property using type map and content
		setContentProp(nodePath, content);

		// Label change if required
		if (isLabels && TagParser.isLabel(nodePath)) {
			switch (nodeAction) {
			case BRANCH:
				TagEntry tag = TagParser.getLabel(nodePath);
				tag.setToPath(nodePath);
				tag.setFromPath(from.getFromPath());
				tag.setFromChange(from.getEndFromChange());

				if (logger.isDebugEnabled()) {
					logger.debug("Label branch with id: " + tag);
					logger.debug("... " + from);
				}

				// Use the author, description, and date from the current
				// change.
				ChangeInfo change = changelist.getChangeInfo();
				nodeAction = Action.LABEL;

				processLabel.labelRev(tag, change);
				break;

			case REMOVE:
				logger.warn("Skipping remove action on label.");
				return;

			default:
				logger.warn("Unknown action on label: " + nodeAction);
				Stats.inc(StatsType.warningCount);
				return;
			}
		}

		// Verbose output for user
		verbose(svnRev, nodeID, nodeAction, NodeType.FILE, nodePath, content,
				subBlock);

		/*
		 * Handle addition of empty files (missed by parser). This supports
		 * corrupt dumpfiles, where 'Text-content-length' is missing. If the
		 * file is empty, plus an ADD action, and there are no other revisions
		 * submitted or pending then create an empty file (using the directory
		 * property file infrastructure)
		 */
		if (!content.isBlob() && nodeAction == Action.ADD) {
			ChangeAction queryAction = getQuery().findLastAction(nodePath, 0);
			if (queryAction == null) {
				content.setAttributes(null);
				if (logger.isDebugEnabled()) {
					logger.debug("create empty file");
				}
			}
		}

		// Create Node object
		NodeInterface node = ProcessFactory.getNode(changelist, getDepot(),
				subBlock);
		node.setTo(nodePath, svnRev);
		node.setFrom(fromList);
		node.setContent(content);
		if (fileMergeInfo != null)
			node.setMergeInfo(fileMergeInfo, nodePath);
		node.action(nodeAction, NodeType.FILE, caseRename);
	}

	/**
	 * Returns the content base type for new revisions (assumes content). Throws
	 * exception if type was not determined. SYMLINK is unusual as its content
	 * is a text string pointing to the link, SVN only sets the property for
	 * SYMLINK once.
	 * 
	 * @param content
	 * @param lastAction
	 * @return
	 * @throws Exception
	 */
	private ContentType findContentType(String nodePath, Content content,
			Action nodeAction, ChangeAction lastAction) throws Exception {

		ContentType type = ContentType.UNKNOWN;

		// SYMLINK case: if property is null (and not just empty) then if
		// previous action was SYMLINK then keep as a SYMLINK
		Property property = record.getProperty();
		if (nodeAction != Action.BRANCH && (property == null)
				&& (lastAction != null)) {
			ContentType last = lastAction.getType();
			if (last == ContentType.SYMLINK)
				return last;
		}

		// Check for SYMLINK type (must be before ScanArchive.detectType as
		// SYMLINK looks like text)
		if (type == ContentType.UNKNOWN) {
			type = processSymlinkProperty();
		}

		// Check if extension is defined in typemap
		if (type == ContentType.UNKNOWN) {
			type = TypeMap.getContentType(nodePath);
			content.setDetectedType(type);
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
	 * Processes the action on a directory node, iterating through all the
	 * affected files. Actions are grouped into an action list and added to the
	 * current changelist.
	 * 
	 * @throws ConverterException
	 * @throws ConfigException
	 */
	protected void processDir() throws Exception {

		ArrayList<MergeSource> fromList = new ArrayList<MergeSource>();

		// find sub blocks
		boolean subBlock = record.isSubBlock();
		boolean caseRename = false;

		// find target path
		String nodePath = record.findHeaderString("Node-path");
		nodePath = formatPath(nodePath);

		// find action and node type
		Action nodeAction = Action.parse(record);

		// skip if excluded
		if (ExcludeParser.isSkipped(nodePath) && !isLabels) {
			char a = nodeAction.toString().charAt(0);
			logger.info("skipping " + a + ":D " + nodePath);
			return;
		}

		// find change numbers
		int nodeRev = record.getSvnRevision();
		int nodeID = record.getNodeNumber();

		// check for MergeSource and add to pending change
		MergeSource from = getFromSource();
		if (from != null) {
			fromList.add(from);
			if (nodePath != null
					&& nodePath.equalsIgnoreCase(from.getFromPath())) {
				changelist.setMergeSource(from);
			}
		} else {
			MergeSource pending = changelist.getMergeSource();
			caseRename = isCaseRename(pending, nodePath, nodeAction);
			changelist.setMergeSource(null);
		}

		// Label change if required
		if (isLabels && TagParser.isLabel(nodePath)) {
			switch (nodeAction) {
			case BRANCH:
				TagEntry tag = TagParser.getLabel(nodePath);
				tag.setToPath(nodePath);
				tag.setFromPath(from.getFromPath());
				tag.setFromChange(from.getEndFromChange());

				if (logger.isDebugEnabled()) {
					logger.debug("Label branch with id: " + tag);
					logger.debug("... " + from);
				}

				// Use the author, description, and date from the current
				// change.
				ChangeInfo change = changelist.getChangeInfo();
				nodeAction = Action.LABEL;
				processLabel.labelChange(tag, change);
				break;

			case REMOVE:
				logger.warn("Skipping remove action on label.");
				return;

			default:
				logger.warn("Unknown action on label: " + nodeAction);
				Stats.inc(StatsType.warningCount);
				return;
			}
		}

		// Verbose output for user
		verbose(nodeRev, nodeID, nodeAction, NodeType.DIR, nodePath, null,
				subBlock);

		// create node for current action
		NodeInterface node = ProcessFactory.getNode(changelist, getDepot(),
				subBlock);
		node.setTo(nodePath, nodeRev); // nodeRev is not used
		node.setFrom(fromList);
		node.setProperty(record.getProperty());

		// check for merge information
		MergeInfo mergeInfo = processMergeInfo(nodePath);
		if (mergeInfo != null && !mergeInfo.isEmpty()) {
			changelist.setMergeInfo(mergeInfo);
			node.setMergeInfo(mergeInfo, nodePath);
		}

		// apply node actions to history
		node.action(nodeAction, NodeType.DIR, caseRename);
	}

	/**
	 * Reads the node kind (file or directory) from the header field in the
	 * Subversion dumpfile and returns the type. If a node kind is not found
	 * then the history of the node is check, otherwise an exception is thrown.
	 * 
	 * @return
	 * @throws ConverterException
	 */
	protected NodeType getNodeType() throws Exception {
		NodeType nodeType = NodeType.NULL;

		String nodeKind = record.findHeaderString("Node-kind");

		if (nodeKind == null) {
			QueryInterface query = ProcessFactory.getQuery(getDepot());
			String nodePath = record.findHeaderString("Node-path");
			nodePath = formatPath(nodePath);

			if (query.hasChildren(nodePath)) {
				nodeType = NodeType.DIR;
			} else {
				nodeType = NodeType.FILE;
			}
		}

		else if (nodeKind.equals("file")) {
			nodeType = NodeType.FILE;
		} else if (nodeKind.equals("dir")) {
			nodeType = NodeType.DIR;
		} else {
			throw new ConverterException("unknown Node-kind(" + record + ")");
		}
		return nodeType;
	}

	/**
	 * Splits properties set on file into identified Perforce types Supports:
	 * Keyword +k, Execute +x and Locked +l
	 * 
	 * @return
	 * @throws Exception
	 */
	protected List<ContentProperty> getContentProp() throws Exception {
		List<ContentProperty> contentProps = new ArrayList<ContentProperty>();
	
		if (record.getProperty() != null) {
			if (record.findPropertyString("svn:keywords") != null) {
				if ((boolean) Config.get(CFG.SVN_KEEP_KEYWORD)) {
					contentProps.add(ContentProperty.KEYWORD);
				}
			}
			if (record.findPropertyString("svn:executable") != null) {
				contentProps.add(ContentProperty.EXECUTE);
			}
			if (record.findPropertyString("svn:needs-lock") != null) {
				contentProps.add(ContentProperty.LOCK);
			}
		}
		return contentProps;
	}

	/**
	 * Test previous action for MergeSource: Only on case-sensitive platforms
	 * (FIRST) and a when remove follows a branch.
	 * 
	 * @param pending
	 * @param nodePath
	 * @param nodeAction
	 * @return
	 * @throws ConfigException
	 */
	private boolean isCaseRename(MergeSource pending, String nodePath,
			Action nodeAction) throws ConfigException {
		boolean rename = false;
		CaseSensitivity mode = (CaseSensitivity) Config.get(CFG.P4_CASE);
		if (pending != null && mode == CaseSensitivity.FIRST
				&& nodePath.equalsIgnoreCase(pending.getFromPath())
				&& nodeAction == Action.REMOVE) {
			logger.info("detected CASE rename operation");
			if (logger.isTraceEnabled()) {
				logger.trace("nodePath: " + nodePath);
				logger.trace("pending" + pending.getFromPath());
			}
			rename = true;
		}
		return rename;
	}

	/**
	 * Look if record contains symlink property
	 * 
	 * @return
	 */
	private ContentType processSymlinkProperty() {
		if (record.getProperty() != null) {
			if (record.findPropertyString("svn:special") != null) {
				return ContentType.SYMLINK;
			}
		}
		return ContentType.UNKNOWN;
	}

	/**
	 * Reads the MD5 value from the header field in the Subversion dumpfile and
	 * returns the value as a String, unless the Content type will be encoded in
	 * UTF8 then a Null MD5 value is returned.
	 * 
	 * @return
	 */
	private String getContentMD5() {
		String md5 = record.findHeaderString("Text-content-md5");
		if (md5 == null || (md5.length() != 32))
			md5 = Digest.null_MD5;

		return md5.toUpperCase();
	}

	/**
	 * Scan record for from sources and create MergeSource
	 * 
	 * @return
	 * @throws Exception
	 */
	private MergeSource getFromSource() throws Exception {
		String fromPath = record.findHeaderString("Node-copyfrom-path");
		fromPath = formatPath(fromPath);
		long fromRev = record.findHeaderLong("Node-copyfrom-rev");

		// Note: there is a case for null fromPath, see case0039; so only need
		// to check if fromRev is valid.
		if (fromRev > 0) {
			MergeSource from = null;

			// Check if fromNode has been labelled
			if (isLabels) {
				from = LabelHistory.find(fromPath, fromRev);
			}

			// Create MergeSource object and populate by calling fetchNode.
			// Only used for branch operation so assumes full range of credit.
			if (from == null) {
				from = new MergeSource(fromPath, 1, fromRev);
			}
			from.fetchNode(getQuery());
			return from;
		}
		return null;
	}

	/**
	 * Scan record for property information containing svn:mergeinfo
	 * 
	 * @param nodePath
	 * @return
	 * @throws ConfigException
	 */
	private MergeInfo processMergeInfo(String nodePath) throws ConfigException {
		MergeInfo mergeInfo = null;
		boolean enabled = (Boolean) Config.get(CFG.SVN_MERGEINFO);
		if (record.getProperty() != null && enabled) {
			String mergeString = record.findPropertyString("svn:mergeinfo");
			if (mergeString != null) {
				mergeInfo = new MergeInfo(nodePath, mergeString);
			}
		}
		return mergeInfo;
	}

	public void setProcessLabel(ProcessLabel processLabel) {
		this.processLabel = processLabel;
	}
}
