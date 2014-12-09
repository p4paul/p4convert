package com.perforce.common.node;

import java.util.ArrayList;

import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;
import com.perforce.svn.parser.Property;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergeSource;

public interface NodeInterface {

	/**
	 * Set target node; path and change.
	 * 
	 * @param path
	 * @param change
	 */
	public void setTo(String path, long change);

	/**
	 * Set source node; path and change.
	 * 
	 * @param path
	 * @param change
	 */
	public void setFrom(ArrayList<MergeSource> from);

	/**
	 * Apply node's action to the current change-list, expanding directories to
	 * their individual files
	 * 
	 * @param nodeAction
	 * @param type
	 * @throws Exception
	 */
	public void action(Action nodeAction, NodeType type, boolean caseRename)
			throws Exception;

	/**
	 * Sets the content for file based nodes
	 * 
	 * @param content
	 * @throws Exception
	 */
	public void setContent(Content content) throws Exception;

	/**
	 * Sets the properties for SVN directories what will be stored as versioned
	 * files in Perforce
	 * 
	 * @param property
	 */
	public void setProperty(Property property);

	/**
	 * Stores merge info on node specified by path
	 * 
	 * @param merge
	 * @param path
	 */
	public void setMergeInfo(MergeInfo merge, String path);
}
