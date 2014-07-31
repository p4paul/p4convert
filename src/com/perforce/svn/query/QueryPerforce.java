package com.perforce.svn.query;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.client.P4Factory;
import com.perforce.common.depot.DepotImport;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.FileStatAncilliaryOptions;
import com.perforce.p4java.core.file.IExtendedFileSpec;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.GetExtendedFilesOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.svn.change.ChangeMap;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.process.MergeInfo;

public class QueryPerforce implements QueryInterface {

	private Logger logger = LoggerFactory.getLogger(QueryPerforce.class);

	private DepotImport depot;
	private RevisionTree tree;

	public QueryPerforce(DepotImport nodeDepot, RevisionTree tree) {
		this.depot = nodeDepot;
		this.tree = tree;
	}

	@Override
	public int findHeadRevision(String path, long svnRev) throws Exception {
		// convert subversion revision to Perforce change for query
		long change = ChangeMap.getChange((int) svnRev);

		// Find revision at specified change
		IExtendedFileSpec rev = findRevision(path, change);
		int head = 0;
		if (rev != null) {
			head = rev.getHeadRev();
		}
		return head;
	}

	@Override
	public ChangeAction findLastAction(String path, long svnRev)
			throws Exception {
		// convert subversion revision to Perforce change for query
		long change = ChangeMap.getChange((int) svnRev);

		// Find revision at specified change
		IExtendedFileSpec rev = findRevision(path, change);

		// Build pending or ChangeAction for query return
		return buildChangeAction(rev);
	}

	@Override
	public List<ChangeAction> listLastActions(String path, long svnRev)
			throws Exception {
		List<ChangeAction> cngActList = new ArrayList<ChangeAction>();

		// convert subversion revision to Perforce change for query
		long change = ChangeMap.getChange((int) svnRev);
				
		// Get connection and build file spec
		IOptionsServer iserver = depot.getIServer();
		if (change == 0) {
			path = depot.getBase() + path + "/...";
		} else {
			path = depot.getBase() + path + "/...@" + change;
		}

		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(path);

		// Query Perforce for list of revisions
		List<IExtendedFileSpec> list = iserver.getExtendedFiles(fileSpec, null);

		// Create Change action for each revision
		if (list.isEmpty()) {
			return null;
		} else {
			for (IExtendedFileSpec rev : list) {
				if (rev != null && rev.getOpStatus() == FileSpecOpStatus.VALID) {
					cngActList.add(buildChangeAction(rev));
				}
			}
		}
		return cngActList;
	}

	private IExtendedFileSpec findRevision(String path, long change)
			throws Exception {
		// Get connection and build file spec
		IOptionsServer iserver = depot.getIServer();
		if (change != 0) {
			path = depot.getBase() + path + "@" + change;
		} else {
			path = depot.getBase() + path;
		}
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(path);

		// Iterate through revisions to get last revision (should only be one)
		List<IExtendedFileSpec> revs;
		GetExtendedFilesOptions revOpts = new GetExtendedFilesOptions();
		FileStatAncilliaryOptions aOpts = new FileStatAncilliaryOptions();
		aOpts.setFileSizeDigest(true);
		revOpts.setAncilliaryOptions(aOpts);
		revs = iserver.getExtendedFiles(fileSpec, revOpts);
		for (IExtendedFileSpec rev : revs) {
			if (rev != null && rev.getOpStatus() == FileSpecOpStatus.VALID) {
				return rev;
			}
		}
		return null;
	}

	private ChangeAction buildChangeAction(IExtendedFileSpec rev)
			throws Exception {
		ChangeAction changeAction = null;

		if (rev != null) {
			// Get revision details
			long change = rev.getHeadChange();
			FileAction fileAction = rev.getHeadAction();
			String fileType = rev.getHeadType();
			String path = rev.getDepotPathString();

			// If a pending change then getHeadAction will return null
			if (fileType == null) {
				fileAction = rev.getAction();
				fileType = rev.getFileType();
			}

			// Convert depot path to SVN path
			String chomp = "//" + depot.getName() + "/";
			path = path.replace(chomp, "");

			// Convert p4java FileAction to Query Action
			Action action = P4Factory.p4javaToQueryAction(fileAction);
			if (action == null)
				return changeAction;

			// Convert content type and content properties
			ContentType type = P4Factory.p4javaToContentType(fileType);
			List<ContentProperty> props = P4Factory
					.p4javaToContentProperty(fileType);

			// Create pseudo revision with path set
			CaseSensitivity caseMode = (CaseSensitivity) Config
					.get(CFG.P4_CASE);
			RevisionTree revTree = new RevisionTree(depot.getName(), caseMode);
			revTree.setPath(path);

			changeAction = new ChangeAction(change, action, changeAction,
					revTree, type, props);

			String md5 = rev.getDigest();
			changeAction.setMd5(md5);
		}
		return changeAction;
	}

	@Override
	public String getPath(String path) {
		return path;
	}

	@Override
	public int getPendingChangeCount() throws Exception {
		String path = depot.getBase() + "...";
		List<IFileSpec> fileSpec = FileSpecBuilder.makeFileSpecList(path);

		// Filter for pending changes
		GetChangelistsOptions opts = new GetChangelistsOptions();
		opts.setType(IChangelist.Type.PENDING);

		// Query Perforce for list of pending changes
		IOptionsServer iserver = depot.getIServer();
		List<IChangelistSummary> pendingChanges;
		pendingChanges = iserver.getChangelists(fileSpec, opts);

		return pendingChanges.size();
	}

	@Override
	public boolean hasChildren(String path) throws Exception {
		ChangeAction findNode = findLastAction(path, 0);
		if (findNode != null && findNode.getAction() != Action.REMOVE) {
			if (logger.isTraceEnabled()) {
				logger.trace("hasChildren: FILE(node)");
			}
			return false;
		}

		else {
			if (logger.isTraceEnabled()) {
				logger.trace("assume: DIR(true)");
			}
			return true;
		}
	}

	@Override
	public MergeInfo getLastMerge(String path) {
		RevisionTree node = tree.getNode(path);
		return node.getLastMergeInfo();
	}
}
