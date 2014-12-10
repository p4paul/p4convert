package com.perforce.common.journal;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.depot.DepotConvert;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.journal.Credit.How;
import com.perforce.common.schema.JournalRecord;
import com.perforce.svn.change.ChangeConvert;
import com.perforce.svn.history.ChangeAction.Action;
import com.perforce.svn.process.MergeSource;

public class BuildChange {

	private static Logger logger = LoggerFactory.getLogger(BuildChange.class);

	public static ArrayList<String> toJournal(DepotConvert d, ChangeConvert c)
			throws Exception {

		ArrayList<String> results = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();

		// Construct db.desc
		JournalRecord dbDesc = new JournalRecord("pv", "db.desc", 0);
		dbDesc.addField("key", c.getChange());
		dbDesc.addField("desc", c.getDescription());
		sb.append(dbDesc.toJournalString() + "\n");

		// Construct db.change
		JournalRecord dbChange = new JournalRecord("pv", "db.change", 0);
		dbChange.addField("change", c.getChange());
		dbChange.addField("key", c.getChange());
		dbChange.addField("client", d.getClient());
		dbChange.addField("user", c.getUser());
		dbChange.addField("date", c.getDate());
		dbChange.addField("status", 1);
		dbChange.addField("desc", c.getSummary());
		sb.append(dbChange.toJournalString() + "\n");

		results.add(sb.toString());

		// Construct all revisions
		for (FileRevision rev : c.getFileRevisions()) {
			results.add(toJournal(d, c, rev));
		}

		return results;
	}

	private static String toJournal(DepotConvert d, ChangeConvert c,
			FileRevision rev) throws Exception {
		StringBuffer sb = new StringBuffer();

		// Construct db.integ, if integration
		if (rev.getFrom() != null && !rev.getFrom().isEmpty()) {
			for (MergeSource from : rev.getFrom()) {

				// If INTEG look for extended merge Action
				Action action = rev.getTo().getAction();
				if (action == Action.MERGE) {
					action = from.getMergeAction();
				}

				boolean edit = rev.getTo().isBlob();

				// Forward credit (from->to)
				How forward = Credit.forward(action, edit);
				JournalRecord dbIntFwd;
				dbIntFwd = new JournalRecord("pv", "db.integed", 0);
				dbIntFwd.addField("tfile", depotPath(d, rev.getTo().getPath()));
				dbIntFwd.addField("ffile", depotPath(d, from.getFromPath()));
				dbIntFwd.addField("sfrev", from.getFromNode().getStartRev());
				dbIntFwd.addField("efrev", from.getFromNode().getEndRev());
				dbIntFwd.addField("strev", rev.getTo().getEndRev() - 1);
				dbIntFwd.addField("etrev", rev.getTo().getEndRev());
				dbIntFwd.addField("how", forward.value());
				dbIntFwd.addField("change", c.getChange());
				sb.append(dbIntFwd.toJournalString() + "\n");

				if (logger.isDebugEnabled()) {
					logger.debug("FWD: " + from.getFromPath() + "@"
							+ from.getStartFromChange() + "-"
							+ from.getEndFromChange() + " "
							+ from.getFromNode().getStartRev() + ", "
							+ from.getFromNode().getEndRev());
				}

				// Reverse credit (to->from)
				How reverse = Credit.reverse(action, edit);
				JournalRecord dbIntRvs;
				dbIntRvs = new JournalRecord("pv", "db.integed", 0);
				dbIntRvs.addField("tfile", depotPath(d, from.getFromPath()));
				dbIntRvs.addField("ffile", depotPath(d, rev.getTo().getPath()));
				dbIntRvs.addField("sfrev", rev.getTo().getEndRev() - 1);
				dbIntRvs.addField("efrev", rev.getTo().getEndRev());
				dbIntRvs.addField("strev", from.getFromNode().getStartRev());
				dbIntRvs.addField("etrev", from.getFromNode().getEndRev());
				dbIntRvs.addField("how", reverse.value());
				dbIntRvs.addField("change", c.getChange());
				sb.append(dbIntRvs.toJournalString() + "\n");

				if (logger.isDebugEnabled()) {
					logger.debug("REV: " + rev.getTo().getPath() + "@"
							+ rev.getTo().getStartChange() + "-"
							+ rev.getTo().getEndChange() + " "
							+ (rev.getTo().getEndRev() - 1) + ", "
							+ rev.getTo().getEndRev());
				}
			}
		}

		// Construct db.rev
		JournalRecord dbRev = new JournalRecord("pv", "db.rev", 3);
		dbRev.addField("dfile", depotPath(d, rev.getTo().getPath()));
		dbRev.addField("rev", rev.getTo().getEndRev());
		dbRev.addField("type", rev.getTypeValue());
		dbRev.addField("action", rev.getTo().getAction().getValue());
		dbRev.addField("change", c.getChange());
		dbRev.addField("date", c.getDate());
		dbRev.addField("modtime", c.getDate());
		dbRev.addField("digest", rev.getMd5());
		dbRev.addField("afile",
				depotPath(d, rev.getTo().getLazyCopy().getPath()));
		dbRev.addField("arev", "1." + rev.getTo().getLazyCopy().getEndChange());
		dbRev.addField("atype", rev.getTypeValue());
		sb.append(dbRev.toJournalString() + "\n");

		// Construct db.revcx
		JournalRecord dbRevCX = new JournalRecord("pv", "db.revcx", 0);
		dbRevCX.addField("change", c.getChange());
		dbRevCX.addField("dfile", depotPath(d, rev.getTo().getPath()));
		dbRevCX.addField("rev", rev.getTo().getEndRev());
		dbRevCX.addField("action", rev.getTo().getAction().getValue());
		sb.append(dbRevCX.toJournalString() + "\n");

		return sb.toString();
	}

	private static String depotPath(DepotInterface d, String path) {
		return (d.getPath(path));
	}
}
