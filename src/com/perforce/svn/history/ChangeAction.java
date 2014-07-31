package com.perforce.svn.history;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.journal.Digest;

public class ChangeAction {

	private Logger logger = LoggerFactory.getLogger(ChangeAction.class);

	public enum Action {
		REVERT(-1),
		ADD(0),
		EDIT(1),
		REMOVE(2),
		BRANCH(3),
		INTEG(4),
		MERGE(4), // Generic merge (only for TO action)
		MERGE_COPY(4), // merge FROM actions
		MERGE_EDIT(4), // merge FROM actions
		MERGE_IGNORE(4), // merge FROM actions
		UPDATE(5), // replace action
		COPY(6); // replace branch action

		final int id;

		Action(int i) {
			id = i;
		}

		public int getValue() {
			return id;
		}
	}

	private Action action;
	private long startChange;
	private long endChange;
	private ChangeAction lazyCopy;
	private RevisionTree parent;
	private boolean subBlock;
	private ContentType type = ContentType.UNKNOWN;
	private List<ContentProperty> props = null;
	private String md5 = Digest.null_MD5;
	private boolean blob;
	private boolean compressed;

	public boolean isBlob() {
		return blob;
	}

	public void setBlob(boolean b) {
		blob = b;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String hash) {
		md5 = hash;
	}

	public List<ContentProperty> getProps() {
		return props;
	}

	public void setProps(List<ContentProperty> p) {
		props = p;
	}

	public ContentType getType() {
		if (logger.isTraceEnabled()) {
			logger.trace("... ... getType: " + type);
		}
		return type;
	}

	public void setType(ContentType t) {
		type = t;
		if (logger.isTraceEnabled()) {
			logger.trace("... ... setType: " + type);
		}
	}

	// Constructor
	public ChangeAction(long change) {
		// startChange is created to include full range, but may get limited
		// for cherry picked integrations
		this.startChange = 1;
		this.endChange = change;
	}

	public void setStartChange(long change) {
		this.startChange = change;
	}

	// Test stimulus constructor
	public ChangeAction(long cng, Action a, ChangeAction l, RevisionTree p,
			ContentType t, List<ContentProperty> ps) {
		endChange = cng;
		action = a;
		lazyCopy = l;
		parent = p;
		type = t;
		props = ps;
	}

	public String getPath() {
		RevisionTree node = getParent();
		return node.getPath();
	}

	public int getEndRev() {
		return getRevFromChange(endChange);
	}

	public int getStartRev() {
		// (-1) get the previous revision to the start change range
		return getRevFromChange(startChange - 1);
	}

	private int getRevFromChange(long cng) {
		int rev = 0;
		for (ChangeAction act : parent.getActions()) {
			if (act.getEndChange() <= cng) {
				rev++;
			}
		}
		return rev;
	}

	public String toString() {
		return toString("");
	}

	public String toString(String indent) {
		return (indent + action.toString() + ":" + getPath() + "@" + endChange
				+ "." + getEndRev() + " " + type + ":" + props);
	}

	// Set and Get methods
	public Action getAction() {
		return action;
	}

	public void setAction(Action action) {
		this.action = action;
	}

	public ChangeAction getLazyCopy() {
		return lazyCopy;
	}

	public void setLazyCopy(ChangeAction lazyCopy) {
		this.lazyCopy = lazyCopy;
	}

	public RevisionTree getParent() {
		return parent;
	}

	public void setParent(RevisionTree parent) {
		this.parent = parent;
	}

	public long getStartChange() {
		return this.startChange;
	}

	public long getEndChange() {
		return this.endChange;
	}

	public void setEndChange(long change) {
		this.endChange = change;
	}

	public void hasSubBlock(boolean subBlock) {
		this.subBlock = subBlock;
	}

	public boolean isSubBlock() {
		return subBlock;
	}

	public void setCompressed(boolean c) {
		this.compressed = c;
	}

	public boolean isCompressed() {
		return this.compressed;
	}
}
