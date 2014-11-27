package com.perforce.svn.change;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.depot.DepotConvert;
import com.perforce.common.journal.BuildChange;
import com.perforce.common.journal.BuildCounter;
import com.perforce.common.journal.FileRevision;
import com.perforce.common.process.ChangeInfo;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergeSource;

public class ChangeConvert implements ChangeInterface {

	private static Logger logger = LoggerFactory.getLogger(ChangeConvert.class);

	private long change;
	private ChangeInfo changeInfo;
	private DepotConvert depot;

	private List<MergeInfo> mergeInfoList = new ArrayList<MergeInfo>();
	private MergeSource mergeSource;
	private Map<String, FileRevision> fileRevisions = new HashMap<String, FileRevision>();

	public ChangeConvert(long c, ChangeInfo i, DepotConvert d) throws Exception {
		this.change = c;
		this.changeInfo = i;
		this.depot = d;
	}

	public long submit() throws Exception {
		depot.getJournal().write(BuildChange.toJournal(depot, this));
		return change;
	}

	public void close() throws Exception {
		setCounter("change", Long.toString(change));
		setCounter("svn2p4.version", (String) Config.get(CFG.VERSION));
		depot.getJournal().flush();
	}

	public void setCounter(String key, String value) throws ConverterException {
		depot.getJournal().write(BuildCounter.toJournal(key, value));
	}

	public void addRevision(ChangeAction target, ArrayList<MergeSource> fromList)
			throws ConfigException {

		// Get down grade options for revision actions
		Boolean downgrade = (Boolean) Config.get(CFG.P4_DOWNGRADE);

		// if target reverted, remove from pending revisions
		if (target.getAction() == Action.REVERT) {
			if (fileRevisions.containsKey(target.getPath())) {
				fileRevisions.remove(target.getPath());
				if (logger.isTraceEnabled()) {
					logger.trace("addRevision: REVERT");
				}
				return;
			}
		}

		// Look for pending revisions to overlay target
		FileRevision r = fileRevisions.get(target.getPath());
		if (r != null) {
			// carry source (integration credit) from overlay
			if (fromList == null || fromList.isEmpty()) {
				fromList = r.getFrom();
			}

			// overlay action for edits
			Action pendingAction = r.getTo().getAction();
			if (target.getAction() == Action.EDIT
					&& pendingAction != Action.REMOVE) {
				target.setAction(pendingAction);
			}

			// remove credit on delete actions
			if (target.getAction() == Action.REMOVE) {
				fromList = null;
			}

			// update pending revision
			FileRevision rev = new FileRevision(target, fromList);

			if (logger.isTraceEnabled()) {
				logger.trace("addRevision: remove - " + r);
			}
			fileRevisions.remove(target.getPath());

			if (logger.isTraceEnabled()) {
				logger.trace("addRevision: added - " + rev);
			}
			fileRevisions.put(target.getPath(), rev);

			return;
		}

		// Check if revision has real archive content (not a property)
		if (fromList != null && downgrade == false && target.isBlob() == false) {
			if (target.getAction() != Action.BRANCH
					&& target.getAction() != Action.INTEG) {
				// remove source (and credit) for ADD actions
				fromList = null;
				if (logger.isTraceEnabled()) {
					logger.trace("addRevision: downgrade - " + target);
				}
			}
		}

		// Normal case - add revision to pending list
		FileRevision rev = new FileRevision(target, fromList);
		fileRevisions.put(rev.getTo().getPath(), rev);

		if (logger.isTraceEnabled()) {
			logger.trace("addRevision: new - " + rev);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		sb.append("Change: " + change + "\n");
		sb.append("User: " + changeInfo.getUser() + "\n");
		sb.append("Date: " + changeInfo.getDate() + "\n");
		sb.append("Summary:\n");
		sb.append(changeInfo.getSummary());
		sb.append("\nRevisions: (" + fileRevisions.size() + ")\n");

		if (fileRevisions.size() > 0) {
			for (FileRevision rev : fileRevisions.values()) {
				sb.append("\t" + rev.toString());
			}
		}
		return sb.toString();
	}

	/**
	 * Setters / Getters
	 */
	public long getChange() {
		return change;
	}

	public long getSvnRevision() {
		return changeInfo.getScmChange();
	}

	public String getDescription() {
		return changeInfo.getDescription();
	}

	public String getUser() {
		return changeInfo.getUser();
	}

	public long getDate() {
		return changeInfo.getDateLong();
	}

	public String getSummary() {
		return changeInfo.getSummary();
	}

	public int getNumberOfRevisions() {
		return fileRevisions.size();
	}

	public List<FileRevision> getFileRevisions() {
		return new ArrayList<FileRevision>(fileRevisions.values());
	}

	@Override
	public void delete() {
		if (logger.isDebugEnabled()) {
			logger.debug("empty changelist");
		}
	}

	@Override
	public void setMergeInfo(MergeInfo m) {
		mergeInfoList.add(m);
	}

	@Override
	public List<MergeInfo> getMergeInfoList() {
		return mergeInfoList;
	}

	@Override
	public void setMergeSource(MergeSource m) {
		mergeSource = m;

	}

	@Override
	public MergeSource getMergeSource() {
		return mergeSource;
	}

	@Override
	public boolean isPendingRevision(String path) {
		return fileRevisions.containsKey(path);
	}

	@Override
	public Action getPendingAction(String path) {
		FileRevision rev = fileRevisions.get(path);
		return rev.getTo().getAction();
	}
}
