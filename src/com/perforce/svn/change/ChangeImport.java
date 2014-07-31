package com.perforce.svn.change;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.TranslateCharsetType;
import com.perforce.common.client.Connection;
import com.perforce.common.client.ConnectionFactory;
import com.perforce.common.client.P4Factory;
import com.perforce.common.depot.DepotImport;
import com.perforce.common.process.ChangeInfo;
import com.perforce.common.process.ProcessFactory;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.node.NodeAttributes;
import com.perforce.svn.parser.Content;
import com.perforce.svn.parser.Property;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergeSource;
import com.perforce.svn.query.QueryPerforce;

public class ChangeImport implements ChangeInterface {

	private static Logger logger = LoggerFactory.getLogger(ChangeImport.class);

	private IClient iclient;
	private IChangelist ichangelist;

	private long change;
	private DepotImport depot;
	private ChangeInfo changeInfo;
	private List<MergeInfo> mergeInfoList = new ArrayList<MergeInfo>();
	private MergeSource mergeSource;

	public ChangeImport(long c, ChangeInfo i, DepotImport d) throws Exception {

		changeInfo = i;
		depot = d;
		String description = i.getDescription();

		Connection p4 = ConnectionFactory.getConnection();
		iclient = p4.getIclient();

		IChangelist implChangelist = new Changelist();
		implChangelist.setClientId((String) Config.get(CFG.P4_CLIENT));
		implChangelist.setDescription(description);
		implChangelist.setDate(changeInfo.getDate());

		ichangelist = iclient.createChangelist(implChangelist);
		change = ichangelist.getId();
	}

	@Override
	public void setCounter(String key, String value) throws Exception {
		Connection p4 = ConnectionFactory.getConnection();
		IOptionsServer iserver = p4.getIserver();
		iserver.setCounter(key, value, false);
	}

	@Override
	public void submit() throws Exception {

		ichangelist.refresh();

		// check for empty changelist
		if (ichangelist.getFiles(true).isEmpty()) {
			delete(); // client won't submit empty change :-(
		}

		// submit change
		else {
			if (logger.isDebugEnabled()) {
				StringBuffer msg = new StringBuffer();
				msg.append("  submitting changelist ");
				msg.append(ichangelist.getId());
				msg.append(" by user ");
				msg.append(changeInfo.getUser());
				logger.debug(msg.toString());
			}

			// call submit and check returned spec
			List<IFileSpec> submitted = ichangelist.submit(null);
			String ignore = "Submitted as change";
			P4Factory.validateFileSpecs(submitted, ignore);

			// Clean up workspace
			cleanWorkspace();

			ichangelist.setDate(changeInfo.getDate());
			ichangelist.setUsername(changeInfo.getUser());
			ichangelist.update(true);

			// check if there was a problem
			if (ichangelist.getStatus() == ChangelistStatus.PENDING) {
				throw new ConverterException("Could not submit changelist!");
			}
		}
		if (logger.isTraceEnabled()) {
			logger.trace("done.\n\n");
		}
	}

	/**
	 * Remove (unsync) files from clients local workspace
	 * 
	 * @throws Exception
	 */
	private void cleanWorkspace() throws Exception {
		List<IFileSpec> fileSpecs;
		fileSpecs = FileSpecBuilder.makeFileSpecList(depot.getBase()
				+ "...#none");
		List<IFileSpec> syncMsg = iclient.sync(fileSpecs, null);
		P4Factory.validateFileSpecs(syncMsg, "file(s) up-to-date.");
	}

	/**
	 * Delete pending changelist
	 */
	@Override
	public void delete() throws Exception {
		if (getNumberOfRevisions() > 0)
			throw new ConverterException("Changelist contains files");

		// logging
		if (logger.isDebugEnabled()) {
			StringBuffer msg = new StringBuffer();
			msg.append("deleting changelist ");
			msg.append(ichangelist.getId());
			msg.append(" by user ");
			msg.append(ichangelist.getUsername());
			logger.debug(msg.toString());
		}

		IOptionsServer iserver = depot.getIServer();
		iserver.setUserName(ichangelist.getUsername());
		iserver.deletePendingChangelist(ichangelist.getId());
	}

	public void addPath(Action nodeAction, String toPath,
			ArrayList<MergeSource> fromList, Property property, boolean subBlock)
			throws Exception {

		// Path syntax translation
		String depotToPath = depot.getBase() + toPath;

		// Deal with null paths and don't add extra '/'
		if (toPath == null)
			depotToPath = "//" + depot.getName();

		// get a RevisionImport helper
		RevisionImport rev = new RevisionImport(iclient, ichangelist, depot,
				changeInfo.getDate());

		switch (nodeAction) {
		case ADD:
		case EDIT:
			// find properties and store if required
			createDirProperty(toPath, property);
			break;

		case UPDATE:
			// remove directory or file
			rev.deletePath(depotToPath);
			removeAction(rev, depotToPath, false);
			addPath(Action.EDIT, toPath, fromList, property, subBlock);
			break;

		case COPY:
			// remove directory or file
			rev.deletePath(depotToPath);
			removeAction(rev, depotToPath, false);
			addPath(Action.BRANCH, toPath, fromList, property, subBlock);
			break;

		case REMOVE:
			// remove directory or file
			rev.deletePath(depotToPath);
			removeAction(rev, depotToPath, false);
			break;

		case BRANCH:
			for (MergeSource from : fromList) {
				String fromPath = from.getFromPath();
				long fromChange = from.getEndFromChange();
				String depotFromPath = depot.getBase() + fromPath;
				// Deal with null paths and don't add extra '/'
				if (fromPath == null || fromPath.isEmpty())
					depotFromPath = "//" + depot.getName();

				if (pathMatch(depotToPath, depotFromPath)) {
					rev.rollBackBranch(depotToPath, depotFromPath, fromChange);
				} else {
					rev.branchPath(depotFromPath, fromChange, depotToPath);
				}
			}

			// find properties and store if required
			createDirProperty(toPath, property);

			break;

		default:
			throw new ConverterException("Node-action(" + nodeAction + ")");
		}
	}

	private void createDirProperty(String toPath, Property property)
			throws Exception {

		if (!(Boolean) Config.get(CFG.SVN_PROP_ENABLED) || property == null)
			return;

		// Property file path
		String propPath = toPath;
		if (!toPath.isEmpty()) {
			propPath += "/";
		}
		propPath += Config.get(CFG.SVN_PROP_NAME);

		// Create attributes content and node
		Content content = new Content();
		NodeAttributes attributes = new NodeAttributes(property);
		content.setAttributes(attributes);

		// Query perforce for last action
		QueryPerforce query = (QueryPerforce) ProcessFactory.getQuery(depot);
		ChangeAction lastProp = query.findLastAction(propPath, change);
		int headRev = query.findHeadRevision(propPath, change);

		// Determine necessary action
		Action act = null;
		if (!property.isEmpty()) {
			if ((lastProp != null) && (lastProp.getAction() != Action.REMOVE))
				act = Action.EDIT;
			else
				act = Action.ADD;
		} else {
			if ((lastProp != null) && (lastProp.getAction() != Action.REMOVE))
				act = Action.REMOVE;
		}

		if (act != null) {
			// Write header to content
			headRev += 1; // add as pending revision is not added on
			content.getAttributes().setHeader(propPath, change, headRev);
			addRevision(act, propPath, null, content, false, false);
		}
	}

	public void addRevision(Action nodeAction, String toPath,
			ArrayList<MergeSource> fromList, Content content, boolean subBlock,
			boolean pendingBlock) throws Exception {

		// Path syntax translation
		String depotToPath = depot.getBase() + toPath;

		// Get last 'to' action
		QueryPerforce query = (QueryPerforce) ProcessFactory.getQuery(depot);
		ChangeAction lastTo = query.findLastAction(toPath, change - 1);

		// get a RevisionImport helper
		RevisionImport rev = new RevisionImport(iclient, ichangelist, depot,
				changeInfo.getDate());
		if (logger.isTraceEnabled()) {
			logger.trace("addRevision (" + nodeAction + ")");
		}

		// down grade content type for non unicode servers from utf8 to text or
		// utf32 to binary
		if ((Boolean) Config.get(CFG.P4_UNICODE) == false) {
			if ((content.getType() == ContentType.UTF_32BE)
					|| (content.getType() == ContentType.UTF_32LE)) {
				if (logger.isInfoEnabled()) {
					logger.info("... Non-unicode server, downgrading utf32 file to binary");
				}
				content.setType(ContentType.P4_BINARY);
			}
			if (content.getType().getP4Type() == TranslateCharsetType.UTF8) {
				if (logger.isInfoEnabled()) {
					logger.info("... Non-unicode server, downgrading file to text");
				}
				content.setType(ContentType.P4_TEXT);
			}
		}

		// Determine action and down grade if required
		switch (nodeAction) {
		case ADD:
		case EDIT:
			if (rev.isFile(depotToPath)) {

				// revert any pending actions (typically deletes)
				if (rev.openedFile(depotToPath, Action.REMOVE)) {
					rev.revertFile(depotToPath);
					if (logger.isDebugEnabled()) {
						logger.debug("reverting file: " + depotToPath);
					}
				}
				rev.editFile(depotToPath, content);
			} else {
				rev.addFile(depotToPath, content);
			}
			break;

		case UPDATE:
			// remove directory or file
			rev.deletePath(depotToPath);
			removeAction(rev, depotToPath, false);
			addRevision(Action.EDIT, toPath, fromList, content, subBlock,
					pendingBlock);
			break;

		case COPY:
			// remove directory or file
			rev.deletePath(depotToPath);
			removeAction(rev, depotToPath, false);
			addRevision(Action.BRANCH, toPath, fromList, content, subBlock,
					pendingBlock);
			break;

		case REMOVE:
			removeAction(rev, depotToPath, true);
			break;

		case BRANCH:
		case MERGE:
			// revert any pending actions (typically deletes)
			if (rev.openedFile(depotToPath)) {
				rev.revertFile(depotToPath);
				if (logger.isDebugEnabled()) {
					logger.debug("reverting file: " + depotToPath);
				}
			}

			for (MergeSource from : fromList) {
				String fromPath = from.getFromPath();
				long startFromChange = from.getStartFromChange();
				long endFromChange = from.getEndFromChange();
				String depotFromPath = depot.getBase() + fromPath;

				if (pathMatch(depotToPath, depotFromPath)) {
					// roll back action
					rev.rollBackFile(depotToPath, content, depotFromPath,
							endFromChange);
					continue;
				}

				// Normal branch (no target or deleted target)
				if (lastTo == null || lastTo.getAction() == Action.REMOVE) {
					rev.branchFile(depotToPath, content, depotFromPath,
							endFromChange);
				} else {
					rev.integFile(depotToPath, content, depotFromPath,
							startFromChange, endFromChange,
							from.getMergeAction());
				}
			}
			break;

		default:
			throw new ConverterException("Node-action(" + nodeAction + ")");
		}
	}

	private void removeAction(RevisionImport rev, String depotToPath,
			boolean remove) throws Exception {
		if (rev.openedFile(depotToPath)) {
			if (!remove) {
				if (logger.isTraceEnabled()) {
					logger.trace("addRevision (reverting): " + depotToPath);
				}
				rev.revertFile(depotToPath);
			} else {
				rev.revertFile(depotToPath);
				rev.deleteFile(depotToPath);
			}
		} else {
			rev.deleteFile(depotToPath);
		}
	}

	@Override
	public int getNumberOfRevisions() throws Exception {
		List<IFileSpec> files = ichangelist.getFiles(true);
		return files.size();
	}

	@Override
	public String getUser() {
		return changeInfo.getUser();
	}

	@Override
	public long getChange() {
		return change;
	}

	@Override
	public void close() throws Exception {
		setCounter("p4-convert.svn.version", (String) Config.get(CFG.VERSION));
		ConnectionFactory.close();
	}

	@Override
	public long getSvnRevision() {
		return changeInfo.getScmChange();
	}

	private boolean pathMatch(String toPath, String fromPath)
			throws ConfigException {
		boolean match = false;
		CaseSensitivity mode = (CaseSensitivity) Config.get(CFG.P4_CASE);

		// For case insensitive platforms ignore case when down grading
		if (mode == CaseSensitivity.NONE) {
			match = toPath.equals(fromPath);
		} else {
			match = toPath.equalsIgnoreCase(fromPath);
		}
		return match;
	}

	@Override
	public void setMergeInfo(MergeInfo m) {
		mergeInfoList.add(m);
	}

	@Override
	public List<MergeInfo> getMergeInfoList() {
		return mergeInfoList;
	}

	@Override
	public void setMergeSource(MergeSource m) {
		mergeSource = m;

	}

	@Override
	public MergeSource getMergeSource() {
		return mergeSource;
	}

	@Override
	public boolean isPendingRevision(String toPath) throws Exception {
		RevisionImport rev = new RevisionImport(iclient, ichangelist, depot,
				changeInfo.getDate());
		String depotToPath = depot.getBase() + toPath;
		return rev.openedFile(depotToPath);
	}
}
