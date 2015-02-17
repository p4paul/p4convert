package com.perforce.svn.change;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.AssetWriter;
import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.TranslateCharsetType;
import com.perforce.common.client.P4Factory;
import com.perforce.common.depot.DepotImport;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.client.AddFilesOptions;
import com.perforce.p4java.option.client.CopyFilesOptions;
import com.perforce.p4java.option.client.DeleteFilesOptions;
import com.perforce.p4java.option.client.EditFilesOptions;
import com.perforce.p4java.option.client.IntegrateFilesOptions;
import com.perforce.p4java.option.client.ReopenFilesOptions;
import com.perforce.p4java.option.client.ResolveFilesAutoOptions;
import com.perforce.p4java.option.client.RevertFilesOptions;
import com.perforce.p4java.option.client.SyncOptions;
import com.perforce.p4java.option.server.GetDepotFilesOptions;
import com.perforce.p4java.option.server.OpenedFilesOptions;
import com.perforce.svn.history.Action;
import com.perforce.svn.parser.Content;

public class RevisionImport {

	private Logger logger = LoggerFactory.getLogger(RevisionImport.class);

	private IClient iclient;
	private IChangelist ichangelist;
	private DepotImport depot;
	private Date date;

	/**
	 * Constructor: takes current client and changelist
	 * 
	 * @param iclient
	 * @param ichangelist
	 * @param depot
	 * @param date
	 */
	public RevisionImport(IClient iclient, IChangelist ichangelist,
			DepotImport depot, Date date) {
		this.iclient = iclient;
		this.ichangelist = ichangelist;
		this.depot = depot;
		this.date = date;
	}

	public void branchPath(String from, long fromChange, String to)
			throws Exception {

		String toStr = to + "/...";
		String fromStr = from + "/...@" + fromChange;

		if (logger.isDebugEnabled()) {
			logger.debug("branchPath(" + toStr + " from:" + fromStr + ")");
		}

		// build path revision spec
		IFileSpec iSource = new FileSpec(fromStr);
		IFileSpec iTarget = new FileSpec(toStr);

		// Integrate options
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setDoBaselessMerge(true); // flag '-i'
		integOpts.setForceIntegration(true);
		integOpts.setPropagateType(true);
		integOpts.setChangelistId(ichangelist.getId());
		integOpts.setDontCopyToClient(true); // flag '-v'
		integOpts.setDeleteTargetAfterDelete(true); // flag '-Ds'

		// revert any pending actions (typically deletes)
		if (isOpened(toStr)) {
			integOpts.setShowActionsOnly(true); // preview flag '-n'

			// Preview branch files
			List<IFileSpec> preview;
			preview = iclient.integrateFiles(iSource, iTarget, null, integOpts);

			for (IFileSpec p : preview) {
				if (p.getOpStatus() != FileSpecOpStatus.VALID) {
					String msg = p.getStatusMessage();
					String pattern = " - can't integrate \\(already opened";
					String[] parts = msg.split(pattern);
					if (parts.length == 2) {
						revertFile(parts[0]);
					}
				}
			}
		}

		// Branch files
		integOpts.setShowActionsOnly(false);
		List<IFileSpec> brMsg;
		brMsg = iclient.integrateFiles(iSource, iTarget, null, integOpts);
		P4Factory.validateFileSpecs(brMsg, "no such file(s)",
				"already integrated", "no file(s) at that changelist");

		// Resolve target by copying source (accept theirs)
		ResolveFilesAutoOptions rsvOpts = new ResolveFilesAutoOptions();
		rsvOpts.setAcceptTheirs(true);

		List<IFileSpec> iTargetFiles;
		iTargetFiles = FileSpecBuilder.makeFileSpecList(toStr);

		List<IFileSpec> rsvMsg;
		rsvMsg = iclient.resolveFilesAuto(iTargetFiles, rsvOpts);
		P4Factory.validateFileSpecs(rsvMsg, "no file(s) to resolve");

		// look for open source files
		duplicatePath(from, to);
	}

	/**
	 * Duplicates a path to another location when a copy operation is not
	 * possible. For example when a file is added and branched in the same
	 * change. All the duplicated files are opened as ADD operations and content
	 * is copied within the Workspace.
	 * 
	 * @param from
	 * @param to
	 * @throws Exception
	 */
	private void duplicatePath(String from, String to) throws Exception {
		List<IFileSpec> iOpenFile;
		iOpenFile = FileSpecBuilder.makeFileSpecList(from + "/...");

		// Check if file is open
		OpenedFilesOptions openOps = new OpenedFilesOptions();
		openOps.setChangelistId(ichangelist.getId());
		List<IFileSpec> open = iclient.openedFiles(iOpenFile, openOps);

		for (IFileSpec file : open) {
			FileAction a = file.getAction();
			Action act = P4Factory.p4javaToQueryAction(a);

			// get file type and options
			String fileType = file.getFileType();

			// get paths
			String fromFile = file.getDepotPathString();
			String remainder = fromFile.substring(from.length());
			String toFile = to + remainder;
			if (logger.isDebugEnabled()) {
				logger.debug("copy " + fromFile + " -> " + toFile);
			}

			switch (act) {
			case ADD:
			case EDIT:
				// copy file within workspace
				File localFrom = new File(toLocalPath(fromFile));
				File localTo = new File(toLocalPath(toFile));
				localTo.getParentFile().mkdirs();
				Files.copy(localFrom.toPath(), localTo.toPath());

				// Change file modtime
				localTo.setLastModified(date.getTime());

				List<IFileSpec> fileSpec;
				fileSpec = FileSpecBuilder.makeFileSpecList(toFile);

				// ADD options
				AddFilesOptions addOpts = new AddFilesOptions();
				addOpts.setNoUpdate(false);
				addOpts.setUseWildcards(true);
				addOpts.setChangelistId(ichangelist.getId());
				addOpts.setFileType(fileType);

				List<IFileSpec> addMsg = iclient.addFiles(fileSpec, addOpts);
				P4Factory.validateFileSpecs(addMsg, "empty, assuming text");
				break;
			case REMOVE:
			case BRANCH:
				// Do not copy deleted or branched revisions
				break;

			default:
				logger.warn("Unexpected open state: " + act);
				Stats.inc(StatsType.warningCount);
			}
		}
	}

	public void rollBackBranch(String to, String from, long fromChange)
			throws Exception {

		String toStr = to + "/...";
		String fromStr = from + "/...@" + fromChange;

		// revert any pending actions (typically deletes)
		if (isOpened(toStr)) {
			revertFile(toStr);
		}

		int ver = depot.getServerVersion();
		if (ver >= 20111) {
			if (logger.isDebugEnabled()) {
				logger.debug("rollBackBranch(" + fromStr + ")");
			}

			// build path revision spec
			IFileSpec iSource = new FileSpec(fromStr);
			List<IFileSpec> toSpec;
			toSpec = FileSpecBuilder.makeFileSpecList(toStr);

			// Copy options
			CopyFilesOptions copyOpts = new CopyFilesOptions();
			copyOpts.setNoClientSyncOrMod(true); // -v option
			copyOpts.setChangelistId(ichangelist.getId());

			// copy from old branch
			List<IFileSpec> cpMsg = iclient
					.copyFiles(iSource, toSpec, copyOpts);
			P4Factory.validateFileSpecs(cpMsg, "file(s) up-to-date");
		}

		else {
			if (logger.isDebugEnabled()) {
				logger.debug("rollBackSync(" + fromStr + ")");
			}

			// sync to latest
			updateHaveList(toStr, false);

			// sync to roll back point
			List<IFileSpec> update = updateHaveList(fromStr, false);

			// abort if no files to roll back
			if (P4Factory.trapFileSpecs(update, " - no such file(s)")) {
				return;
			}

			// roll back actions
			rollBackActions(update);

			// sync to latest
			updateHaveList(toStr, false);

			// Resolve target (accept yours)
			ResolveFilesAutoOptions rsvOpts = new ResolveFilesAutoOptions();
			rsvOpts.setAcceptYours(true);

			List<IFileSpec> toSpec;
			toSpec = FileSpecBuilder.makeFileSpecList(toStr);

			List<IFileSpec> rsvMsg;
			rsvMsg = iclient.resolveFilesAuto(toSpec, rsvOpts);
			P4Factory.validateFileSpecs(rsvMsg, "no file(s) to resolve");
		}
	}

	/**
	 * Roll back the specific actions based on the output from sync.
	 * 
	 * Assumes a sync to HEAD then sync to 'change' and switches on action.
	 * 
	 * @param update
	 * @throws Exception
	 */
	private void rollBackActions(List<IFileSpec> update) throws Exception {
		// Iterate over updated files
		for (IFileSpec u : update) {

			if (u.getOpStatus() != FileSpecOpStatus.VALID) {
				String msg = u.getStatusMessage();
				Stats.inc(StatsType.warningCount);
				logger.warn("rollBackSync p4java: " + msg);
				continue;
			}

			FileAction action = u.getAction();
			String path = u.getClientPathString();

			if (logger.isDebugEnabled()) {
				logger.debug("\t" + path + ":" + action.toString());
			}

			List<IFileSpec> fileSpec;
			fileSpec = FileSpecBuilder.makeFileSpecList(path);

			switch (action) {
			case ADDED:
				// ADD options
				AddFilesOptions addOpts = new AddFilesOptions();
				addOpts.setNoUpdate(false);
				addOpts.setUseWildcards(true);
				addOpts.setChangelistId(ichangelist.getId());

				List<IFileSpec> addMsg = iclient.addFiles(fileSpec, addOpts);
				P4Factory.validateFileSpecs(addMsg, "empty, assuming text");
				break;

			case DELETED:
				// DELETE options
				DeleteFilesOptions deleteOpts = new DeleteFilesOptions();
				deleteOpts.setChangelistId(ichangelist.getId());

				// not tested - unable to create SVN dumpfile for this case
				List<IFileSpec> delMsg = iclient.deleteFiles(fileSpec,
						deleteOpts);
				P4Factory.validateFileSpecs(delMsg, "file(s) not on client");
				break;

			case UPDATED:
				// EDIT options
				EditFilesOptions editOpts = new EditFilesOptions();
				editOpts.setNoUpdate(false);
				editOpts.setChangelistId(ichangelist.getId());

				// not tested - unable to create SVN dumpfile for this case
				List<IFileSpec> editMsg = iclient.editFiles(fileSpec, editOpts);
				P4Factory.validateFileSpecs(editMsg, "- must sync/resolve");
				break;

			default:
				throw new ConverterException("unknown action '" + action.name()
						+ "'");
			}

			// Change file modtime
			File file = new File(path);
			file.setLastModified(date.getTime());
		}
	}

	public void deletePath(String path) throws Exception {

		path += "/...";
		if (logger.isDebugEnabled()) {
			logger.debug("deletePath(" + path + ")");
		}

		// just revert pending revision
		if (isOpened(path)) {
			Action action = getOpenedAction(path);
			if (action != Action.REMOVE) {
				revertFile(path);
			}
		}

		// delete revision
		else {
			deleteFile(path);
		}
	}

	/**
	 * Remove Perforce escapes from string (The add command takes unescaped
	 * filenames only)
	 * 
	 * @param path
	 * @return
	 */
	private static String unFormatPath(String path) {
		if (path != null) {
			path = path.replace("%25", "%");
			path = path.replace("%40", "@");
			path = path.replace("%23", "#");
			path = path.replace("%2A", "*");
		}
		return path;
	}

	/**
	 * Add a file to Perforce - normal case
	 * 
	 * @param depotToPath
	 * @param content
	 * @throws Exception
	 */
	public void addFile(String depotToPath, Content content) throws Exception {

		String realToPath = unFormatPath(depotToPath);

		if (logger.isDebugEnabled()) {
			logger.debug("addFile(" + realToPath + ")");
		}

		// test if already open (integ -v ?)
		if (isOpened(depotToPath)) {
			isVirtualInteg(depotToPath);
		}

		// write archive (first, for type detection)
		writeClientFile(depotToPath, content);

		// Set options for 'add' action
		AddFilesOptions addOpts = new AddFilesOptions();
		addOpts.setNoUpdate(false);
		addOpts.setUseWildcards(true);
		addOpts.setChangelistId(ichangelist.getId());

		// Open file for 'add'
		List<IFileSpec> iTargetFiles;
		iTargetFiles = FileSpecBuilder.makeFileSpecList(realToPath);
		List<IFileSpec> test = iclient.addFiles(iTargetFiles, addOpts);
		P4Factory.validateFileSpecs(test, "empty, assuming text",
				"using binary instead of ubinary",
				"using text instead of xtext", "using text instead of unicode",
				"currently opened for add");

		// properties
		reopenProps(depotToPath, content);
	}

	/**
	 * Edit the content for an existing file in Perforce - normal case
	 * 
	 * @param localToPath
	 * @param content
	 * @throws Exception
	 */
	public void editFile(String localToPath, Content content) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("editFile(" + localToPath + ")");
		}

		// update have list or workspace
		if (content != null && content.isBlob()) {
			updateHaveList(localToPath, true);
		} else {
			updateHaveList(localToPath, false);
		}

		// write archive
		if (content != null) {
			writeClientFile(localToPath, content);
		}

		// Set options for 'edit' action
		EditFilesOptions editOpts = new EditFilesOptions();
		editOpts.setNoUpdate(false);
		editOpts.setChangelistId(ichangelist.getId());

		// Open file for 'edit'
		List<IFileSpec> iTargetFiles;
		iTargetFiles = FileSpecBuilder.makeFileSpecList(localToPath);
		List<IFileSpec> test = iclient.editFiles(iTargetFiles, editOpts);
		P4Factory.validateFileSpecs(test);

		// properties
		if (content != null) {
			reopenProps(localToPath, content);
		}
	}

	/**
	 * roll back a file from a previous revision
	 * 
	 * @param localToPath
	 * @param content
	 * @param depotFromPath
	 * @param fromChange
	 * @throws Exception
	 */
	public void rollBackFile(String localToPath, Content content,
			String depotFromPath, long fromChange) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("reEditFile(" + localToPath + ")");
		}

		// build file revision spec for target
		List<IFileSpec> iTargetFiles;
		iTargetFiles = FileSpecBuilder.makeFileSpecList(localToPath);
		String iSourcePath = depotFromPath + "@" + fromChange;

		// Only use p4 copy for 11.1 or newer and not a dirty copy
		int ver = depot.getServerVersion();
		if (ver >= 20111 && !content.isBlob()) {
			// build file revision spec for copy source
			IFileSpec iCopySource = new FileSpec(iSourcePath);

			// Copy options
			CopyFilesOptions copyOpts = new CopyFilesOptions();
			copyOpts.setNoClientSyncOrMod(true); // -v option
			copyOpts.setChangelistId(ichangelist.getId());

			// copy from old branch
			List<IFileSpec> cpMsg = iclient.copyFiles(iCopySource,
					iTargetFiles, copyOpts);
			P4Factory.validateFileSpecs(cpMsg, "file(s) up-to-date");

		}

		else {
			// build file revision spec for roll back source
			List<IFileSpec> iSourceFiles;
			iSourceFiles = FileSpecBuilder.makeFileSpecList(iSourcePath);

			// Clean workspace, sync to head
			updateHaveList(depotFromPath, false);

			// Sync revision to roll back
			List<IFileSpec> update = updateHaveList(iSourcePath, false);

			// abort if no files to roll back
			if (P4Factory.trapFileSpecs(update, "- file(s) up-to-date")) {
				if (content.isBlob()) {
					// downgrade to EDIT
					editFile(localToPath, content);
				}
				return;
			}

			// roll back actions
			rollBackActions(update);

			// Check for content (dirty roll back)
			if (content.isBlob()) {
				// Set file type modification bits
				reopenProps(localToPath, content);

				// write archive (any dirty local edits during roll-back)
				writeClientFile(localToPath, content);
			}

			// sync to head to schedule resolve
			updateHaveList(localToPath, false);

			// Propagate type if no content
			if (!content.isBlob()) {

				// get source file type
				List<IFileSpec> srcSpec;
				GetDepotFilesOptions dOpts = new GetDepotFilesOptions();
				srcSpec = depot.getIServer().getDepotFiles(iSourceFiles, dOpts);

				for (IFileSpec rev : srcSpec) {
					String fileType = rev.getFileType();

					ReopenFilesOptions reOpts = new ReopenFilesOptions();
					reOpts.setChangelistId(ichangelist.getId());
					reOpts.setFileType(fileType);

					List<IFileSpec> reSpec = iclient.reopenFiles(iTargetFiles,
							reOpts);
					P4Factory.validateFileSpecs(reSpec,
							"- file(s) not opened on this client");
				}
			}

			// Resolve target (accept yours)
			ResolveFilesAutoOptions rsvOpts = new ResolveFilesAutoOptions();
			rsvOpts.setAcceptYours(true);
			List<IFileSpec> rsvMsg;
			rsvMsg = iclient.resolveFilesAuto(iTargetFiles, rsvOpts);
			P4Factory.validateFileSpecs(rsvMsg, "no file(s) to resolve");
		}
	}

	/**
	 * Test if there are pending files open
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public boolean isOpened(String depotToPath) throws Exception {
		IFileSpec file = openedFile(depotToPath);
		return file != null;
	}

	public Action getOpenedAction(String depotToPath) throws Exception {
		IFileSpec file = openedFile(depotToPath);
		return P4Factory.p4javaToQueryAction(file.getAction());
	}

	private IFileSpec openedFile(String depotToPath) throws Exception {
		List<IFileSpec> iOpenFile;
		iOpenFile = FileSpecBuilder.makeFileSpecList(depotToPath);

		// Check if file is open
		OpenedFilesOptions openOps = new OpenedFilesOptions();
		openOps.setChangelistId(ichangelist.getId());
		List<IFileSpec> open = iclient.openedFiles(iOpenFile, openOps);
		for (IFileSpec file : open) {
			if (file != null && file.getAction() != null) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Test if there are pending files open for the specified Action
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public boolean openedFile(String path, Action action) throws Exception {
		List<IFileSpec> iOpenFile;
		iOpenFile = FileSpecBuilder.makeFileSpecList(path);

		// Check if file is open
		OpenedFilesOptions openOps = new OpenedFilesOptions();
		openOps.setChangelistId(ichangelist.getId());
		List<IFileSpec> open = iclient.openedFiles(iOpenFile, openOps);

		boolean match = false;
		for (IFileSpec file : open) {
			FileAction a = file.getAction();
			Action act = P4Factory.p4javaToQueryAction(a);
			if (act.equals(action)) {
				match = true;
			}
		}

		return match;
	}

	/**
	 * Revert open files
	 * 
	 * @param localToPath
	 * @throws Exception
	 */
	public void revertFile(String localToPath) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("revertFile(" + localToPath + ")");
		}

		// Set options for 'revert' action
		RevertFilesOptions rvtOpts = new RevertFilesOptions();
		rvtOpts.setChangelistId(ichangelist.getId());

		// Revert files form current change
		List<IFileSpec> iRevertFile;
		iRevertFile = FileSpecBuilder.makeFileSpecList(localToPath);
		List<IFileSpec> rvtMsg = iclient.revertFiles(iRevertFile, rvtOpts);
		P4Factory.validateFileSpecs(rvtMsg);
	}

	/**
	 * Delete a file revision from Perforce
	 * 
	 * @param localToPath
	 * @throws Exception
	 */
	public void deleteFile(String localToPath) throws Exception {

		if (isFile(localToPath)) {

			if (logger.isDebugEnabled()) {
				logger.debug("deleteFile(" + localToPath + ")");
			}

			// update have only (no sync)
			updateHaveList(localToPath, true);

			// Set options for 'delete' action
			DeleteFilesOptions deleteOpts = new DeleteFilesOptions();
			deleteOpts.setChangelistId(ichangelist.getId());
			deleteOpts.setDeleteNonSyncedFiles(true);

			// Delete files with -v
			List<IFileSpec> iTargetFiles;
			iTargetFiles = FileSpecBuilder.makeFileSpecList(localToPath);
			List<IFileSpec> delMsg = iclient.deleteFiles(iTargetFiles,
					deleteOpts);
			P4Factory.validateFileSpecs(delMsg, "file(s) not on client");
		}
	}

	public boolean isFile(String localToPath) throws Exception {

		List<IFileSpec> iFiles = FileSpecBuilder.makeFileSpecList(localToPath);
		List<IFileSpec> revList;
		revList = depot.getIServer().getDepotFiles(iFiles, null);

		for (IFileSpec rev : revList) {
			FileAction act = rev.getAction();
			if (act != null && !act.equals(FileAction.DELETE)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Branch files (where target is null or deleted)
	 * 
	 * @param depotToPath
	 * @param depotFromPath
	 * @param fromChange
	 * @throws Exception
	 */
	public void branchFile(String depotToPath, Content content,
			String depotFromPath, long fromChange) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("branchFile(" + depotToPath + ")");
		}

		// File spec paths
		IFileSpec iTarget = new FileSpec(depotToPath);
		IFileSpec iSource = new FileSpec(depotFromPath + "@" + fromChange);
		if (logger.isDebugEnabled()) {
			logger.debug("from: " + depotFromPath);
			logger.debug("to:   " + depotToPath);
		}

		// Integrate options
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setDoBaselessMerge(true); // flag '-i'
		integOpts.setForceIntegration(true);
		integOpts.setPropagateType(true);
		integOpts.setChangelistId(ichangelist.getId());

		// Test for clean branch (optimize branch)
		boolean dirty = (content != null && content.isBlob());
		if (!dirty) {
			integOpts.setDontCopyToClient(true); // flag '-v'
			if (logger.isDebugEnabled()) {
				logger.debug("(virtual integ)");
			}
		}

		// Branch files
		List<IFileSpec> brMsg;
		brMsg = iclient.integrateFiles(iSource, iTarget, null, integOpts);
		P4Factory.validateFileSpecs(brMsg);

		// Check for content (dirty edit) on branch action
		if (dirty) {
			dirtyEdit(depotToPath, content);
		}

		// Resolve target (ignore action)
		ResolveFilesAutoOptions rsvOpts = new ResolveFilesAutoOptions();
		rsvOpts.setAcceptYours(true);

		List<IFileSpec> rsvMsg;
		rsvMsg = iclient.resolveFilesAuto(brMsg, rsvOpts);
		P4Factory.validateFileSpecs(rsvMsg, "Diff chunks:",
				"no file(s) to resolve", " - merging //", " - edit from //",
				" - merge from //", " - ignored //", " - copy from //",
				"tampered with before resolve - edit or revert", "- vs");

	}

	private void dirtyEdit(String localToPath, Content content)
			throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("dirtyEdit(" + localToPath + ")");
		}

		// Set options for 'edit' action
		EditFilesOptions editOpts = new EditFilesOptions();
		editOpts.setNoUpdate(false);
		editOpts.setChangelistId(ichangelist.getId());

		// Open file for 'dirty edit'
		List<IFileSpec> iTargetFiles;
		iTargetFiles = FileSpecBuilder.makeFileSpecList(localToPath);
		List<IFileSpec> editSpec = iclient.editFiles(iTargetFiles, editOpts);
		P4Factory.validateFileSpecs(editSpec, "add of deleted file",
				"currently opened for edit",
				"can't edit (already opened for add)");

		// write archive
		writeClientFile(localToPath, content);

		// properties
		reopenProps(localToPath, content);
	}

	// TODO move this to P4Factory
	private String getBaseType(String type) {
		if (type.contains("text"))
			return "text";
		if (type.contains("winansi"))
			return "text";
		if (type.contains("utf8"))
			return "text";
		if (type.contains("binary"))
			return "binary";
		if (type.contains("symlink"))
			return "symlink";
		if (type.contains("apple"))
			return "apple";
		if (type.contains("resource"))
			return "resource";
		if (type.contains("unicode"))
			return "unicode";
		if (type.contains("utf16"))
			return "utf16";
		// return binary for unknown types.
		Stats.inc(StatsType.warningCount);
		logger.warn("unknown basetype: '" + type + "' storing as binary");
		return "binary";
	}

	private void reopenProps(String localToPath, Content content)
			throws Exception {

		List<IFileSpec> iTargetFiles;
		iTargetFiles = FileSpecBuilder.makeFileSpecList(localToPath);

		String baseType = "null";

		// Use subversion type if known
		ContentType contentType = content.getType();
		if (contentType != ContentType.UNKNOWN) {
			TranslateCharsetType p4type = contentType.getP4Type();
			baseType = getBaseType(p4type.getType());
		}

		// Find base type from Perforce
		else {
			List<IFileSpec> revs;
			OpenedFilesOptions openOpts = new OpenedFilesOptions();
			revs = depot.getIServer().getOpenedFiles(iTargetFiles, openOpts);
			for (IFileSpec rev : revs) {
				if (rev != null && rev.getOpStatus() == FileSpecOpStatus.VALID) {
					baseType = getBaseType(rev.getFileType());
				}
			}
		}

		// Set file type modification bits
		String fileType = baseType + getFileTypeMods(content);
		ReopenFilesOptions reOpts = new ReopenFilesOptions();
		reOpts.setChangelistId(ichangelist.getId());
		if (!fileType.isEmpty()) {
			reOpts.setFileType(fileType);
		}

		if (logger.isTraceEnabled()) {
			logger.trace("reopenProps(" + fileType + ")");
		}

		// Reopen file for 'options'
		List<IFileSpec> reSpec = iclient.reopenFiles(iTargetFiles, reOpts);
		P4Factory.validateFileSpecs(reSpec);
	}

	/**
	 * Integrate file (where target exists) resolve as -at
	 * 
	 * @param depotToPath
	 * @param content
	 * @param depotFromPath
	 * @param fromChange
	 * @param endFromChange
	 * @param action
	 * @throws Exception
	 */
	public void integFile(String depotToPath, Content content,
			String depotFromPath, long startFromChange, long endFromChange,
			Action action) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("integFile(" + depotToPath + ")");
		}

		// File spec paths
		depotFromPath = depotFromPath + "@" + startFromChange + ",@"
				+ endFromChange;

		IFileSpec iTarget = new FileSpec(depotToPath);
		IFileSpec iSource = new FileSpec(depotFromPath);
		if (logger.isDebugEnabled()) {
			logger.debug("    from: " + depotFromPath);
			logger.debug("    to:   " + depotToPath);
		}

		// Integrate options
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setDoBaselessMerge(true); // flag '-i'
		integOpts.setForceIntegration(true);
		integOpts.setPropagateType(true);
		integOpts.setChangelistId(ichangelist.getId());

		// Integrate files
		List<IFileSpec> toRsv;
		toRsv = iclient.integrateFiles(iSource, iTarget, null, integOpts);
		P4Factory.validateFileSpecs(toRsv, "revision(s) already integrated.");

		// Resolve options
		ResolveFilesAutoOptions rsvOpts = new ResolveFilesAutoOptions();
		switch (action) {
		case BRANCH:
		case COPY:
		case MERGE_COPY:
			rsvOpts.setAcceptTheirs(true);
			if (logger.isDebugEnabled()) {
				logger.debug("resolve (-at): " + action);
			}
			break;
		case MERGE_EDIT:
			rsvOpts.setForceResolve(true);
			rsvOpts.setForceTextualMerge(true);
			if (logger.isDebugEnabled()) {
				logger.debug("resolve (-af): " + action);
			}
			break;
		case MERGE_IGNORE:
			rsvOpts.setAcceptYours(true);
			if (logger.isDebugEnabled()) {
				logger.debug("resolve (-ay): " + action);
			}
			break;
		default:
			throw new ConverterException("unhandled resolve action: " + action);
		}

		// Resolve target (accept action)
		List<IFileSpec> rsvMsg;
		rsvMsg = iclient.resolveFilesAuto(toRsv, rsvOpts);
		P4Factory.validateFileSpecs(rsvMsg, "Diff chunks:",
				"no file(s) to resolve", " - merging //", " - edit from //",
				" - merge from //", " - ignored //", " - copy from //",
				"tampered with before resolve - edit or revert", "- vs");

		// if tamper check issue, open for edit then resolve
		if (P4Factory.trapFileSpecs(rsvMsg,
				"tampered with before resolve - edit or revert")) {
			// reopen for edit (don't send content)
			editFile(depotToPath, null);

			// Resolve target (accept action)
			rsvMsg = iclient.resolveFilesAuto(toRsv, rsvOpts);
			P4Factory.validateFileSpecs(rsvMsg, "Diff chunks:",
					"no file(s) to resolve", " - merging //",
					" - merge from //");
		}

		// Downgrade MERGE_EDIT
		if (action == Action.MERGE_EDIT) {
			dirtyEdit(depotToPath, content);
		} else {
			// Change file modtime
			String localPath = toLocalPath(depotToPath);
			File file = new File(localPath);
			file.setLastModified(date.getTime());
		}
	}

	private List<IFileSpec> updateHaveList(String path, boolean clientBypass)
			throws Exception {

		// build file revision spec
		List<IFileSpec> syncFiles;
		syncFiles = FileSpecBuilder.makeFileSpecList(path);

		// Sync revision to re-edit
		SyncOptions syncOpts = new SyncOptions();
		syncOpts.setClientBypass(clientBypass);
		List<IFileSpec> syncMsg = iclient.sync(syncFiles, syncOpts);
		P4Factory.validateFileSpecs(syncMsg, "file(s) up-to-date.",
				"no such file", "- is opened and not being changed",
				"- must resolve", "- is opened for add and can't be replaced");

		return syncMsg;
	}

	private String getFileTypeMods(Content content) throws Exception {
		boolean compress = false;

		// Set FileType property bits
		StringBuffer fileType = new StringBuffer();
		if (content.getProps() != null) {
			fileType.append("+");
			for (ContentProperty p : content.getProps()) {
				fileType.append(p.toString());
			}
			if (content.isCompressed()) {
				compress = true;
				fileType.append("C");
			}
		}

		if ((Boolean) Config.get(CFG.TEST) && !compress) {
			if (fileType.toString().contains("+")) {
				fileType.append("F");
			} else {
				fileType.append("+F");
			}
		}
		return fileType.toString();
	}

	/**
	 * write archive file into client workspace
	 * 
	 * @param toPath
	 * @param content
	 * @throws Exception
	 */
	private void writeClientFile(String path, Content content) throws Exception {

		String localToPath = toLocalPath(path);

		if (content.isBlob()) {
			AssetWriter archive = new AssetWriter(localToPath);
			archive.write(content);
			if (logger.isTraceEnabled()) {
				logger.trace("writeClientFile(" + localToPath + ")");
			}
		}

		// Change file modtime
		File file = new File(localToPath);
		file.setLastModified(date.getTime());

		// hack to fix modtime
		job067069(localToPath, content);
	}

	/**
	 * Java seems to have a bug when setting the modtime for a symlink with no
	 * target. Very minor issue for a user, but a big hassle for unit tests.
	 * 
	 * @param localToPath
	 * @param content
	 * @throws Exception
	 */
	private void job067069(String localToPath, Content content)
			throws Exception {
		if (content.getType() == ContentType.SYMLINK) {
			if ((boolean) Config.get(CFG.TEST)) {
				String dateFormat = "yyyyMMddHHmm.ss";
				SimpleDateFormat simpleDate = new SimpleDateFormat(dateFormat);
				String dateString = simpleDate.format(date);
				String cmd = "touch -mht " + dateString + " " + localToPath;
				logger.info("FIX: " + cmd);
				Runtime.getRuntime().exec(cmd);
			}
		}
	}

	/**
	 * convert depot to local path
	 * 
	 * @param depotPath
	 * @return
	 */
	private String toLocalPath(String depotPath) {
		if (depotPath.startsWith("//")) {
			String realToPath = unFormatPath(depotPath);
			String chomp = "//" + depot.getName() + "/";
			realToPath = realToPath.replace(chomp, "");
			String localToPath = iclient.getRoot() + realToPath;
			return localToPath;
		} else {
			Stats.inc(StatsType.warningCount);
			logger.warn("not a depot path: " + depotPath);
			return depotPath;
		}
	}

	/**
	 * private void writeSymLink(String localToPath, Content content) throws
	 * Exception {
	 * 
	 * ProcessLogger.log(Level.FINER, "writeSymLink(" + localToPath + ")");
	 * 
	 * Archive archive = new Archive(localToPath); archive.symlink(content); }
	 */
	private void reIntegFile(String wsFile) throws Exception {

		List<IFileSpec> iFiles = FileSpecBuilder.makeFileSpecList(wsFile);

		// find resolved
		List<IFileSpec> rsvFiles = iclient.resolvedFiles(iFiles, null);

		// sanity check
		if (rsvFiles.size() != 1)
			throw new ConverterException("resolved failed on file: " + wsFile);

		// get source + target
		String to = rsvFiles.get(0).getToFile();
		String from = rsvFiles.get(0).getFromFile();
		int rev = rsvFiles.get(0).getEndFromRev();

		// revert current integ -v action
		revertFile(to);

		// re-integrate file
		IFileSpec target = new FileSpec(to);
		IFileSpec source = new FileSpec(from + "#" + rev);

		// ... integrate options
		IntegrateFilesOptions integOpts = new IntegrateFilesOptions();
		integOpts.setDoBaselessMerge(true); // flag '-i'
		integOpts.setForceIntegration(true);
		integOpts.setPropagateType(true);
		integOpts.setChangelistId(ichangelist.getId());

		// ... branch files
		List<IFileSpec> brMsg;
		brMsg = iclient.integrateFiles(source, target, null, integOpts);
		// hide message - job057600
		P4Factory.validateFileSpecs(brMsg,
				"all revision(s) already integrated.");

	}

	private void isVirtualInteg(String localToPath) throws Exception {

		if (logger.isDebugEnabled()) {
			logger.debug("isVirtualInteg(" + localToPath + ")");
		}

		// build file spec for open file
		List<IFileSpec> iTarget = FileSpecBuilder.makeFileSpecList(localToPath);
		OpenedFilesOptions openOps = new OpenedFilesOptions();
		openOps.setChangelistId(ichangelist.getId());
		List<IFileSpec> openFiles = iclient.openedFiles(iTarget, openOps);

		// Sanity check
		if (openFiles.size() != 1)
			throw new ConverterException("opened failed on file: "
					+ localToPath);

		if (openFiles.get(0).getAction() == FileAction.BRANCH) {
			// re-integrate file
			reIntegFile(openFiles.get(0).getClientPathString());
		}
	}

}
