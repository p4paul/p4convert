package com.perforce.cvs;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.asset.ContentProperty;
import com.perforce.common.node.Action;
import com.perforce.common.process.ProcessUser;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.config.UserMapping;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;

public class RevisionEntry implements Comparable<RevisionEntry> {

	transient private static Logger logger = LoggerFactory
			.getLogger(RevisionEntry.class);

	private RcsObjectNum id;
	private Date date;
	private String commitId;
	private String author;
	private String comment;
	private Action state;

	private String tmpFile;
	private String path;
	private int nodeID;
	private long cvsChange;
	private boolean pseudo = false;
	private boolean reverse = false;
	private List<ContentProperty> props = new ArrayList<ContentProperty>();
	private boolean binary;
	private boolean next;

	private String fromPath;
	private List<String> labels = new ArrayList<String>();

	public RevisionEntry(RcsObjectDelta revision) {
		this.id = revision.getID();
		this.date = revision.getDate();
		this.commitId = revision.getCommitId().intern();
		this.author = revision.getAuthor().intern();
		this.comment = revision.getLog().intern();
		this.state = revision.getState();
		this.next = revision.getNext() != null;
	}

	public boolean matches(Object obj) {
		if (!(obj instanceof RevisionEntry))
			return false;

		RevisionEntry entry = (RevisionEntry) obj;

		// only test for commit ID if used
		if (getCommitId() != null && !getCommitId().isEmpty()
				&& entry.getCommitId() != null
				&& !entry.getCommitId().isEmpty()) {
			if (getCommitId().contentEquals(entry.getCommitId()))
				return true;
			else
				return false;
		}

		// otherwise, check author
		if (!getAuthor().contentEquals(entry.getAuthor())) {
			return false;
		}

		// and comment
		if (!getComment().contentEquals(entry.getComment())) {
			return false;
		}

		// and date is within range
		return within(entry, 0);
	}

	@Override
	public int compareTo(RevisionEntry obj) {
		final int EQUAL = 0;
		final int NEWER = 1;
		final int OLDER = -1;

		// compare newer or older by date
		long gap = obj.getDate().getTime() - getDate().getTime();
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
		// Rename users based on Mapping file
		String user = UserMapping.get(author);

		// No choice here; any reserved characters used are purged.
		// Remove '@' and '#', but replace ' ' with '_'
		user = ProcessUser.filter(user);

		if (logger.isTraceEnabled()) {
			logger.trace("username: " + author + " => " + user);
		}
		return user;
	}

	public void setAuthor(String user) {
		author = user.intern();
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String log) {
		comment = log.intern();
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

	public Action getState() {
		return state;
	}

	public void setState(Action s) {
		state = s;
	}

	public boolean isBinary() {
		return binary;
	}

	public void setBinary(String expand) {
		if (expand != null && expand.contains("b")) {
			binary = true;
		} else {
			binary = false;
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(getId());
		sb.append(" ");
		sb.append(getPath());
		sb.append(" <== ");
		sb.append(getFromPath());

		// Change sequence data
		sb.append(" (");
		sb.append(getCommitId());
		sb.append(" ");
		sb.append(getAuthor());
		sb.append(" ");
		sb.append(getDate().getTime());
		sb.append(" ");
		String hex = Integer.toHexString(getComment().hashCode());
		sb.append(hex.toUpperCase());
		sb.append(") ");

		// Flags
		sb.append(isPseudo() ? "P " : "E ");
		sb.append(isReverse() ? "Rev " : "Fwd ");

		return sb.toString();
	}

	public void setPath(String revPath) {
		path = revPath.intern();
	}

	public String getFromPath() {
		return fromPath;
	}

	public void setFromPath(String fromPath) {
		this.fromPath = fromPath.intern();
	}

	public String getTmpFile() {
		return tmpFile;
	}

	public void setTmpFile(String tmpFile) {
		this.tmpFile = tmpFile.intern();
	}

	public void addLabel(String label) {
		if (logger.isDebugEnabled()) {
			logger.debug("Label tag: " + label + " - " + getId());
		}
		getLabels().add(label.intern());
	}

	public List<String> getLabels() {
		return labels;
	}

	public boolean within(RevisionEntry entry, long time) {
		long window;
		try {
			window = (long) Config.get(CFG.CVS_WINDOW);
		} catch (ConfigException e) {
			window = 20000L;
		}
		window = (time > 0) ? time : window;

		long gap = getDate().getTime() - entry.getDate().getTime();
		if (Math.abs(gap) > window) {
			return false;
		}
		return true;
	}

	public boolean isPseudo() {
		return pseudo;
	}

	public void setPseudo(boolean pseudo) {
		this.pseudo = pseudo;
	}

	public boolean isReverse() {
		return reverse;
	}

	public void setReverse(boolean reverse) {
		this.reverse = reverse;
	}

	public List<ContentProperty> getProps() {
		return props;
	}

	public void setProps(List<ContentProperty> props) {
		this.props = props;
	}

	public boolean hasNext() {
		return next;
	}
}
