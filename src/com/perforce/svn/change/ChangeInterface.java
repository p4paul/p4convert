package com.perforce.svn.change;

import java.util.List;

import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergeSource;

public interface ChangeInterface {

	/**
	 * Gets the current change list number (long)
	 * 
	 * @return
	 */
	public long getChange();

	/**
	 * Submits the revisions into a change. ChangeConvert - writes a journal or
	 * ChangeImport submits revisions to server
	 * 
	 * @throws Throwable
	 */
	public void submit() throws Exception;

	/**
	 * Sets the Perforce counter; key and value pair.
	 * 
	 * @param key
	 * @param value
	 * @throws Exception
	 */
	public void setCounter(String key, String value) throws Exception;

	/**
	 * Closes final change-list. ChangeConvert - flushes JournalWriter
	 * 
	 * @throws Exception
	 */
	public void close() throws Exception;

	/**
	 * Returns the number of revisions currently in change-list
	 * 
	 * @return
	 */
	public int getNumberOfRevisions() throws Exception;

	/**
	 * Current Subversion revision number
	 * 
	 * @return
	 */
	public long getSvnRevision();

	/**
	 * Clean up empty change lists
	 */
	public void delete() throws Exception;

	public String getUser();

	public void setMergeInfo(MergeInfo m);

	public List<MergeInfo> getMergeInfoList();

	public void setMergeSource(MergeSource m);

	public MergeSource getMergeSource();

	public boolean isPendingRevision(String path) throws Exception;

	public Action getPendingAction(String path) throws Exception;
}
