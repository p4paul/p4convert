package com.perforce.common.process;

import java.util.Date;

import com.perforce.cvs.RevisionEntry;
import com.perforce.svn.change.ChangeParser;
import com.perforce.svn.parser.Record;

public class ChangeInfo {

	private long scmChange;
	private Date date;
	private String user;
	private String description;

	/**
	 * Constructor for Subversion Record to a Perforce change information object
	 * 
	 * @param r
	 * @throws Exception
	 */
	public ChangeInfo(Record r) throws Exception {
		this.scmChange = ChangeParser.getSvnRevision(r);
		this.date = ChangeParser.getDate(r);
		this.user = ChangeParser.getUser(r);
		this.description = ChangeParser.getDescription(r);
	}

	/**
	 * Constructor for CVS Record to a Perforce change information object
	 * 
	 * @param r
	 * @throws Exception
	 */
	public ChangeInfo(RevisionEntry r, long scmChange) throws Exception {
		this.scmChange = scmChange;
		this.date = r.getDate();
		this.user = r.getAuthor();
		this.description = r.getComment();
	}

	public void setScmChange(long change) {
		scmChange = change;
	}
	
	public long getScmChange() {
		return scmChange;
	}

	public Date getDate() {
		return date;
	}

	public long getDateLong() {
		return date.getTime() / 1000; // convert from ms to s
	}

	public String getUser() {
		return user;
	}

	public String getDescription() {
		return description;
	}

	public String getSummary() {
		int len = 32;
		if ((description.length() > len) && (len > 0)) {
			return description.substring(0, (len - 1));
		} else {
			return description;
		}
	}
}
