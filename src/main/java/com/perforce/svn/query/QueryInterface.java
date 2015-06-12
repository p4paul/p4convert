package com.perforce.svn.query;

import java.util.List;

import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.process.MergeInfo;

public interface QueryInterface {

	public int findHeadRevision(String path, long change) throws Exception;

	/**
	 * Find last change action based on path and change number, returns null if
	 * none found
	 * 
	 * @param path
	 * @param change
	 * @return
	 * @throws Exception
	 */
	public ChangeAction findLastAction(String path, long change)
			throws Exception;

	public List<ChangeAction> listLastActions(String path, long change)
			throws Exception;

	public String getPath(String path);

	public int getPendingChangeCount() throws Exception;

	public boolean hasChildren(String path) throws Exception;

	public MergeInfo getLastMerge(String path);
}
