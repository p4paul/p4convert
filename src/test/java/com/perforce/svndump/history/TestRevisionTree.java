package com.perforce.svndump.history;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.common.ConverterException;
import com.perforce.common.node.Action;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;

public class TestRevisionTree {

	static {
		try {
			Config.setDefault();
			Config.set(CFG.TEST, true);
			Config.set(CFG.VERSION, "alpha/TestMode");
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a node and then get its name
	 * 
	 * @throws Exception
	 */
	@Test
	public void testAddFileName() throws Exception {
		String path = "Trunk/Proj1/Makefile";

		Content content = new Content();
		content.setAttributes(null);

		RevisionTree revisionTree = new RevisionTree("Depot",
				CaseSensitivity.FIRST);

		ChangeAction node = revisionTree.add(path, 1, Action.ADD, content,
				NodeType.FILE, false);

		String actual = node.getParent().getName();
		Assert.assertEquals("Makefile", actual);
	}

	@Test
	public void testBranchLazyReference() throws Exception {
		String fromPath = "Trunk/Proj1/Makefile";
		String toPath = "Trunk/Proj2/Makefile";

		Content content = new Content();
		content.setAttributes(null);
		Content noContent = new Content();

		RevisionTree revisionTree = new RevisionTree("Depot",
				CaseSensitivity.FIRST);

		ChangeAction add = revisionTree.add(fromPath, 1, Action.ADD, content,
				NodeType.FILE, false);

		ChangeAction lazy = revisionTree.branch(fromPath, 1, toPath, 2,
				Action.BRANCH, noContent, NodeType.FILE, false);

		Assert.assertEquals(add, lazy.getLazyCopy());
	}

	@Test
	public void testBranchWithEdit() throws Exception {
		String fromPath = "Trunk/Proj1/Makefile";
		String toPath = "Trunk/Proj2/Makefile";

		Content content = new Content();
		content.setAttributes(null);

		RevisionTree revisionTree = new RevisionTree("Depot",
				CaseSensitivity.FIRST);

		ChangeAction add = revisionTree.add(fromPath, 1, Action.ADD, content,
				NodeType.FILE, false);

		ChangeAction lazy = revisionTree.branch(fromPath, 1, toPath, 2,
				Action.BRANCH, content, NodeType.FILE, false);

		Assert.assertNotSame(add, lazy.getLazyCopy());
	}

	/**
	 * Same Overlay Actions in same change list
	 * 
	 * @throws ConverterException
	 */
	@Test
	public void testBranchOverlayRemove() throws Exception {
		String fromPath = "Trunk/Proj1/Makefile";
		String toPath = "Trunk/Proj2/Makefile";

		Content content = new Content();
		content.setAttributes(null);
		Content noContent = new Content();

		RevisionTree revisionTree = new RevisionTree("Depot",
				CaseSensitivity.FIRST);

		revisionTree
				.add(fromPath, 1, Action.ADD, content, NodeType.FILE, false);

		ChangeAction action = revisionTree.branch(fromPath, 1, toPath, 2,
				Action.BRANCH, noContent, NodeType.FILE, false);

		// This should remove previous action
		revisionTree.add(toPath, 2, Action.REMOVE, noContent, NodeType.FILE,
				false);

		int actions = action.getParent().getActions().size();
		Assert.assertEquals(0, actions);
	}

	/**
	 * Check revision count
	 * 
	 * @throws ConverterException
	 */
	@Test
	public void testChangeActionRevision() throws Exception {
		String fromPath = "Trunk/Proj1/Makefile";
		String toPath = "Trunk/Proj2/Makefile";

		Content content = new Content();
		content.setAttributes(null);
		Content noContent = new Content();

		RevisionTree revisionTree = new RevisionTree("Depot",
				CaseSensitivity.FIRST);

		// fromPath#1 (add @1)
		revisionTree
				.add(fromPath, 1, Action.ADD, content, NodeType.FILE, false);

		// fromPath#2 (edit @2)
		ChangeAction base = revisionTree.add(fromPath, 2, Action.EDIT, content,
				NodeType.FILE, false);

		// toPath#1 (branch @20)
		revisionTree.branch(fromPath, 1, toPath, 20, Action.BRANCH, noContent,
				NodeType.FILE, false);

		// toPath#1 (overlay branch+edit @20)
		revisionTree.branch(fromPath, 2, toPath, 20, Action.BRANCH, content,
				NodeType.FILE, false);

		// toPath#2 (edit @21)
		ChangeAction action = revisionTree.add(toPath, 21, Action.EDIT,
				content, NodeType.FILE, false);

		int head = action.getParent().getHeadRev();
		Assert.assertEquals("head rev:", 2, head);

		int revision = base.getEndRev();
		Assert.assertEquals(2, revision);
	}

}
