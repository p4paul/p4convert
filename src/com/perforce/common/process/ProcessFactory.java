package com.perforce.common.process;

import com.perforce.common.depot.DepotConvert;
import com.perforce.common.depot.DepotImport;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.label.LabelConvert;
import com.perforce.common.label.LabelImport;
import com.perforce.common.label.LabelInterface;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.config.ScmType;
import com.perforce.cvs.RevisionEntry;
import com.perforce.svn.change.ChangeConvert;
import com.perforce.svn.change.ChangeImport;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.history.RevisionTree;
import com.perforce.svn.node.NodeConvert;
import com.perforce.svn.node.NodeImport;
import com.perforce.svn.node.NodeInterface;
import com.perforce.svn.query.QueryHistory;
import com.perforce.svn.query.QueryInterface;
import com.perforce.svn.query.QueryPerforce;

public class ProcessFactory {

	private static RevisionTree tree; // don't like this being stored here!
	private static boolean pendingBlock;

	public static NodeInterface getNode(ChangeInterface cl,
			DepotInterface nodeDepot, boolean subBlock) throws ConfigException {

		NodeInterface node;
		if (Config.isImportMode()) {
			// Import mode (front-door)
			node = new NodeImport((ChangeImport) cl, tree, subBlock,
					pendingBlock);
		} else {
			// Convert mode (back-door)
			node = new NodeConvert((ChangeConvert) cl,
					(DepotConvert) nodeDepot, tree, subBlock);
		}

		// update pending block for next node (as 'Import mode' has no state)
		pendingBlock = subBlock;
		return node;
	}

	public static QueryInterface getQuery(DepotInterface depot)
			throws ConfigException {
		if (Config.isImportMode()) {
			return new QueryPerforce((DepotImport) depot, tree);
		} else {
			return new QueryHistory(tree);
		}
	}

	public static ChangeInterface getChange(long c, ChangeInfo info,
			DepotInterface depot) throws Exception {
		if (Config.isImportMode()) {
			return new ChangeImport(c, info, (DepotImport) depot);
		} else {
			switch ((ScmType) Config.get(CFG.SCM_TYPE)) {
			case CVS:
				return new ChangeConvert(c, info, (DepotConvert) depot);
			case SVN:
				return new ChangeConvert(c, info, (DepotConvert) depot);
			default:
				return null;
			}
		}
	}

	public static DepotInterface getDepot(String name, CaseSensitivity caseMode)
			throws Exception {
		if (Config.isImportMode()) {
			tree = new RevisionTree(name, caseMode);
			return new DepotImport(name, caseMode);
		} else {
			tree = new RevisionTree(name, caseMode);
			return new DepotConvert(tree.getName());
		}
	}

	public static LabelInterface getLabel(String label, RevisionEntry entry,
			DepotInterface depot) throws Exception {
		if (Config.isImportMode()) {
			return new LabelImport(label, entry, (DepotImport) depot);
		} else {
			return new LabelConvert(label, entry, (DepotConvert) depot);
		}
	}
}
