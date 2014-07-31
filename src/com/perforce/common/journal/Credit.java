package com.perforce.common.journal;

import com.perforce.common.ConverterException;
import com.perforce.svn.history.ChangeAction;

public class Credit {

	public enum Dir {
		FORWARD,
		REVERSE;
	}

	public enum How {

		MERGE_FROM(0), // merge from: integration with other changes
		MERGE_INTO(1), // merge into: reverse merge
		BRANCH_FROM(2), // branch from: integration was branch of file
		BRANCH_INTO(3), // branch into: reverse branch
		COPY_FROM(4), // copy from: integration took source file whole
		COPY_INTO(5), // copy into: reverse take
		IGNORED(6), // ignored: integration ignored source changes
		IGNORED_BY(7), // ignored by: reverse copy
		DELETE_FROM(8), // delete from: integration of delete
		DELETE_INTO(9), // delete into: reverse delete
		EDIT_INTO(10), // edit into: reverse of integration downgraded to edit
		ADD_INTO(11), // add into: reverse of branch downgraded to add
		EDIT_FROM(12), // edit from; merge that the user edited
		ADD_FROM(13), // add from; branch downgraded to add
		MOVED_INTO(14), // moved into; file was renamed
		MOVED_FROM(15), // moved from: reverse of renamed file
		NULL(-1);

		final int id;

		How(int i) {
			id = i;
		}

		public int value() {
			return id;
		}
	}

	// Used by From or the Source
	public static How forward(ChangeAction.Action action, boolean edit)
			throws ConverterException {
		if (edit) {
			switch (action) {
			case ADD:
				return How.BRANCH_FROM;
			case EDIT:
				return How.EDIT_FROM;
			case REMOVE:
				return How.DELETE_FROM;
			case BRANCH:
				return How.BRANCH_FROM;
			case INTEG:
				return How.EDIT_FROM;
			case MERGE_EDIT:
				return How.EDIT_FROM;
			case MERGE_COPY:
				return How.COPY_FROM;
			case MERGE_IGNORE:
				return How.IGNORED;
			default:
				throw new ConverterException("Forward credit (edit): " + action);
			}
		} else {
			switch (action) {
			case ADD:
				return How.BRANCH_FROM;
			case EDIT:
				return How.EDIT_FROM;
			case REMOVE:
				return How.DELETE_FROM;
			case BRANCH:
				return How.BRANCH_FROM;
			case INTEG:
				return How.COPY_FROM;
			case MERGE_EDIT:
				return How.EDIT_FROM;
			case MERGE_COPY:
				return How.COPY_FROM;
			case MERGE_IGNORE:
				return How.IGNORED;
			default:
				throw new ConverterException("Forward credit (lazy): " + action);
			}
		}
	}

	// Used by To or the Target
	public static How reverse(ChangeAction.Action action, boolean edit)
			throws ConverterException {
		if (edit) {
			switch (action) {
			case ADD:
				return How.ADD_INTO;
			case EDIT:
				return How.EDIT_INTO;
			case REMOVE:
				return How.DELETE_INTO;
			case BRANCH:
				return How.ADD_INTO;
			case INTEG:
				return How.EDIT_INTO;
			case MERGE_EDIT:
				return How.EDIT_INTO;
			case MERGE_COPY:
				return How.COPY_INTO;
			case MERGE_IGNORE:
				return How.IGNORED_BY;
			default:
				throw new ConverterException("Reverse credit (lazy): " + action);
			}
		} else {
			switch (action) {
			case ADD:
				return How.ADD_INTO;
			case EDIT:
				return How.EDIT_INTO;
			case REMOVE:
				return How.DELETE_INTO;
			case BRANCH:
				return How.BRANCH_INTO;
			case INTEG:
				return How.COPY_INTO;
			case MERGE_EDIT:
				return How.EDIT_INTO;
			case MERGE_COPY:
				return How.COPY_INTO;
			case MERGE_IGNORE:
				return How.IGNORED_BY;
			default:
				throw new ConverterException("Reverse credit (lazy): " + action);
			}
		}
	}
}