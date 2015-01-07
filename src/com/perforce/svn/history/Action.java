package com.perforce.svn.history;

import com.perforce.svn.parser.Record;

public enum Action {
	REVERT(-1), //
	ADD(0), //
	EDIT(1), //
	REMOVE(2), //
	BRANCH(3), //
	INTEG(4), //
	MERGE(4), // Generic merge (only for TO action)
	MERGE_COPY(4), // merge FROM actions
	MERGE_EDIT(4), // merge FROM actions
	MERGE_IGNORE(4), // merge FROM actions
	UPDATE(5), // replace action
	COPY(6), // replace branch action
	LABEL(-1),
	UNKNOWN(-1);

	final int id;

	Action(int i) {
		id = i;
	}

	public int getValue() {
		return id;
	}

	/**
	 * Reads parsed "Node-action" header field from the Subversion dumpfile and
	 * returns the Perforce action
	 * 
	 * @return
	 */
	public static Action parse(Record record) {
		Action action = null;

		// Set node condition ('add', 'change' or 'delete')
		String s = record.findHeaderString("Node-action");
		if (s != null) {
			if ("add".equals(s))
				action = Action.ADD;
			else if ("change".equals(s))
				action = Action.EDIT;
			else if ("replace".equals(s))
				action = Action.UPDATE;
			else if ("delete".equals(s))
				action = Action.REMOVE;
			else
				throw new RuntimeException("unknown action " + s);
		} else {
			throw new RuntimeException("No node-action(" + record + ")");
		}

		// Test for branch condition (overload 'copy' condition)
		String path = record.findHeaderString("Node-copyfrom-path");
		String rev = record.findHeaderString("Node-copyfrom-rev");
		if (path != null || rev != null) {
			if (action.equals(Action.UPDATE)) {
				action = Action.COPY;
			} else {
				action = Action.BRANCH;
			}
		}
		return action;
	}
}
