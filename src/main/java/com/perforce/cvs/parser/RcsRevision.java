package com.perforce.cvs.parser;


import java.util.Date;

import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;

public class RcsRevision {

	private int revision; // TODO perforce revision number
	private String author;
	private Date date;
	private String commitid;
	private String log;


	public RcsRevision(int rev, RcsObjectDelta data) {
		revision = rev;
		author = data.getAuthor();
		date = data.getDate();
		commitid = data.getCommitId();
		log = data.getLog();
	}

	public int getRevision() {
		return revision;
	}

	public String getLog() {
		return log;
	}

	public String getAuthor() {
		return author;
	}

	public Date getDate() {
		return date;
	}

	public String getCommitid() {
		return commitid;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Revision: " + revision + "\n");
		sb.append("   author:   " + author + "\n");
		sb.append("   commitid: " + commitid + "\n");
		sb.append("   date:     " + date.toString() + "\n");
		sb.append("-------------- LOG ---------------\n");
		sb.append(log);
		sb.append("----------------------------------\n");
		return sb.toString();
	}
}
