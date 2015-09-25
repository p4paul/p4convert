package com.perforce.svn.process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.node.Action;
import com.perforce.svn.change.ChangeMap;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.query.QueryInterface;

public class MergeSource {

	private Logger logger = LoggerFactory.getLogger(MergeSource.class);

	private long startFromSvnRev;
	private long endFromSvnRev;
	private String fromPath;
	private ChangeAction fromNode;
	private Action mergeAction;

	public MergeSource(String path, long startChange, long endChange) {
		this.fromPath = path;
		this.startFromSvnRev = startChange;
		this.endFromSvnRev = endChange;
	}

	public boolean fetchNode(QueryInterface query) throws Exception {
		// convert subversion revision to Perforce change for query
		long change = ChangeMap.getChange((int) endFromSvnRev);

		ChangeAction node = query.findLastAction(fromPath, change);
		if (node != null && node.getAction() != Action.REMOVE) {

			if (logger.isDebugEnabled()) {
				String md5 = node.getMd5();
				logger.debug("\tmerge source: Node: " + node.toString());
				logger.debug("\tmerge source: MD5: " + md5);
			}

			fromNode = node;
			fromNode.setStartChange(startFromSvnRev);
			return true;
		}
		fromNode = node;
		return false;
	}

	public ChangeAction getFromNode() {
		return fromNode;
	}

	public void setFromNode(ChangeAction from) {
		fromNode = from;
	}

	public long getStartFromChange() {
		return ChangeMap.getChange((int) startFromSvnRev);
	}

	public long getEndFromChange() {
		return ChangeMap.getChange((int) endFromSvnRev);
	}

	public long getStartFromSvnRev() {
		return startFromSvnRev;
	}

	public long getEndFromSvnRev() {
		return endFromSvnRev;
	}

	public String getFromPath() {
		return fromPath;
	}

	public Action getMergeAction() {
		return mergeAction;
	}

	public void setMergeAction(Action how) {
		mergeAction = how;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(fromPath + " r" + startFromSvnRev + "-" + endFromSvnRev
				+ ": " + mergeAction);
		return sb.toString();
	}

}
