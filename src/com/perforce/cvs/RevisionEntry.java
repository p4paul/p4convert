package com.perforce.cvs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;

public class RevisionEntry implements Comparable<RevisionEntry> {

	private Logger logger = LoggerFactory.getLogger(RevisionEntry.class);

	private RcsObjectNum id;
	private String tmpFile;
	private String path;
	private int nodeID;
	private long cvsChange;

	private Date date;
	private String commitId;
	private String author;
	private String comment;
	private String state;
	private String fromPath;
	private List<String> labels = new ArrayList<String>();

	public RevisionEntry(RcsObjectDelta revision) {
		this.id = revision.getID();
		this.date = revision.getDate();
		this.commitId = revision.getCommitId();
		this.author = revision.getAuthor();
		this.comment = revision.getLog();
		this.state = revision.getState();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof RevisionEntry))
			return false;

		RevisionEntry entry = (RevisionEntry) obj;

		// only test for commit ID if used
		if (commitId != null && entry.getCommitId() != null) {
			if (commitId.contentEquals(entry.getCommitId()))
				return true;
			else
				return false;
		}

		// otherwise, check author
		if (!author.contentEquals(entry.getAuthor())) {
			return false;
		}

		// and comment
		if (!comment.contentEquals(entry.getComment())) {
			return false;
		}

		// and date is within range
		return within(entry);
	}

	@Override
	public int compareTo(RevisionEntry obj) {
		final int EQUAL = 0;
		final int NEWER = 1;
		final int OLDER = -1;

		// compare newer or older by date
		long gap = obj.getDate().getTime() - date.getTime();
		if (gap == 0)
			return EQUAL;
		else if (gap > 0)
			return OLDER;
		else
			return NEWER;
	}

	public RcsObjectNum getId() {
		return id;
	}

	public String getPath() {
		return path;
	}

	public String getCommitId() {
		return commitId;
	}

	public void clearCommitId() {
		commitId = null;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String user) {
		author = user;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String log) {
		comment = log;
	}

	public Date getDate() {
		return date;
	}

	/**
	 * Add milli seconds to date
	 */
	public void addDate(long t) {
		long d = date.getTime() + t;
		date = new Date(d);
	}

	public int getNodeID() {
		return nodeID;
	}

	public void setNodeID(int nodeID) {
		this.nodeID = nodeID;
	}

	public long getCvsChange() {
		return cvsChange;
	}

	public void setCvsChange(long cvsChange) {
		this.cvsChange = cvsChange;
	}

	public String getState() {
		return state;
	}

	public void setState(String s) {
		state = s;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(id);
		sb.append(" ");
		sb.append(date.getTime());
		sb.append(" ");
		sb.append(path);
		sb.append(" ");
		sb.append(commitId);
		sb.append(" ");
		sb.append(author);
		return sb.toString();
	}

	public void setPath(String revPath) {
		path = revPath;
	}

	public String getFromPath() {
		return fromPath;
	}

	public void setFromPath(String fromPath) {
		this.fromPath = fromPath;
	}

	public String getTmpFile() {
		return tmpFile;
	}

	public void setTmpFile(String tmpFile) {
		this.tmpFile = tmpFile;
	}

	public void addLabel(String label) {
		logger.debug("Label tag: " + label + " - " + id);
		labels.add(label);
	}

	public List<String> getLabels() {
		return labels;
	}

	public boolean within(RevisionEntry entry) {
		long window;
		try {
			window = (long) Config.get(CFG.CVS_WINDOW);
		} catch (ConfigException e) {
			window = 20000L;
		}

		long gap = date.getTime() - entry.getDate().getTime();
		if (Math.abs(gap) > window) {
			return false;
		}
		return true;
	}
}
