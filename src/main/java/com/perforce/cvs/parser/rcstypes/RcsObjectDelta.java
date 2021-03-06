package com.perforce.cvs.parser.rcstypes;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import com.perforce.common.node.Action;
import com.perforce.common.process.ProcessUser;
import com.perforce.cvs.parser.RcsSchema;

public class RcsObjectDelta extends RcsObject {

	public String getLog() {
		if (containsKey(RcsSchema.LOG)) {
			return (String) get(RcsSchema.LOG);
		} else {
			return "empty";
		}
	}

	public String getAuthor() {
		if (containsKey(RcsSchema.AUTHOR)) {
			String user = (String) get(RcsSchema.AUTHOR);
			return ProcessUser.filter(user);
		} else {
			return "unknown";
		}
	}

	public String getCommitId() {
		if (containsKey(RcsSchema.COMMITID)) {
			return (String) get(RcsSchema.COMMITID);
		} else {
			return "";
		}
	}

	public Date getDate() {
		if (!containsKey(RcsSchema.DATE))
			return null;

		String dateStr = (String) get(RcsSchema.DATE);
		if (dateStr == null)
			return null;

		// Old dates might start with '99' and not '1999'
		if (dateStr.length() < 19) {
			dateStr = "19" + dateStr;
		}

		// format date/time
		String format = "yyyy.MM.dd.HH.mm.ss";
		dateStr = dateStr.substring(0, 19);
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		Date date;
		try {
			date = dateFormat.parse(dateStr);
		} catch (ParseException e) {
			return null;
		}
		return date;
	}

	public RcsObjectNumList getBranches() {
		if (containsKey(RcsSchema.BRANCHES)) {
			RcsObjectNumList br = (RcsObjectNumList) get(RcsSchema.BRANCHES);
			return br;
		}
		return null;
	}

	public RcsObjectNum getNext() {
		if (containsKey(RcsSchema.NEXT)) {
			RcsObjectNum id = (RcsObjectNum) get(RcsSchema.NEXT);
			if (id.getVer() != null)
				return id;
		}
		return null;
	}

	public RcsObjectBlock getBlock() {
		RcsObjectBlock block = (RcsObjectBlock) get(RcsSchema.TEXT);
		return block;
	}

	public Action getState() {
		if (containsKey(RcsSchema.STATE)) {
			String state = (String) get(RcsSchema.STATE);
			if(state.equals("dead")) {
				return Action.REMOVE;
			} else {
				return Action.ADD;
			}
		} else {
			return Action.UNKNOWN;
		}
	}
}
