package com.perforce.svn.node;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.svn.change.ChangeImport;
import com.perforce.svn.history.RevisionTree;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;
import com.perforce.svn.parser.Property;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergeSource;

public class NodeImport implements NodeInterface {

	private Logger logger = LoggerFactory.getLogger(NodeImport.class);

	private String toPath = null;
	private ArrayList<MergeSource> fromList;
	private Property property;
	private Content content;

	private ChangeImport currentChange;
	private RevisionTree tree;
	private boolean subBlock;
	private boolean pendingBlock;

	public NodeImport(ChangeImport change, RevisionTree tree, boolean subBlock, boolean pendingBlock) {
		currentChange = change;
		this.tree = tree;
		this.subBlock = subBlock;
		this.pendingBlock = pendingBlock;
	}

	@Override
	public void action(Action nodeAction, NodeType type, boolean caseRename)
			throws Exception {

		if (!caseRename) {
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
		} else {
			if (logger.isTraceEnabled()) {
				logger.trace("skipping delete part of case rename");
			}
		}
	}

	private void dirAction(Action nodeAction) throws Exception {
		// Add path to current change
		currentChange.addPath(nodeAction, toPath, fromList, property,
				pendingBlock);
	}

	private void fileAction(Action nodeAction) throws Exception {
		// Add file to current change
		currentChange.addRevision(nodeAction, toPath, fromList, content,
				subBlock, pendingBlock );
	}

	@Override
	public void setTo(String path, long change) {
		toPath = path;
	}

	@Override
	public void setFrom(ArrayList<MergeSource> from) {
		fromList = from;
	}

	@Override
	public void setContent(Content content) {
		this.content = content;
	}

	@Override
	public void setProperty(Property property) {
		this.property = property;
	}

	@Override
	public void setMergeInfo(MergeInfo merge, String path) {
		RevisionTree node = tree.create(path, NodeType.NULL);
		node.setMergeInfo(merge);
	}
}
