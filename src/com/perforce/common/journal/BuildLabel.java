package com.perforce.common.journal;

import java.util.ArrayList;

import com.perforce.common.depot.DepotInterface;
import com.perforce.common.label.LabelConvert;
import com.perforce.common.label.TagConvert;
import com.perforce.common.schema.JournalRecord;

public class BuildLabel {

	public static ArrayList<String> toJournal(DepotInterface d, LabelConvert lbl)
			throws Exception {
		ArrayList<String> label = new ArrayList<String>();
		StringBuffer sb = new StringBuffer();

		// Build label view
		for (String view : lbl.getView()) {
			label.add(viewToJournal(lbl.getName(), view));
		}

		// Build label domain spec
		JournalRecord dbLabel = new JournalRecord("pv", "db.domain", 5);
		dbLabel.addField("name", lbl.getName());
		dbLabel.addField("type", 108);
		dbLabel.addField("extra", lbl.getAutomatic());
		dbLabel.addField("mount", "");
		dbLabel.addField("mount2", "");
		dbLabel.addField("mount3", "");
		dbLabel.addField("owner", lbl.getOwner());
		dbLabel.addField("update", lbl.getDate());
		dbLabel.addField("access", lbl.getDate());
		dbLabel.addField("options", 0);
		dbLabel.addField("desc", lbl.getDesc());
		dbLabel.addField("stream", "");
		sb.append(dbLabel.toJournalString() + "\n");
		label.add(sb.toString());

		// Build label revisions
		for (TagConvert t : lbl.getTags()) {
			label.add(toJournal(d, lbl.getName(), t));
		}

		return label;
	}

	private static String toJournal(DepotInterface d, String name,
			TagConvert tag) throws Exception {
		StringBuffer sb = new StringBuffer();

		JournalRecord dbTag = new JournalRecord("pv", "db.label", 0);
		dbTag.addField("name", name);
		dbTag.addField("file", depotPath(d, tag.getPath()));
		dbTag.addField("rev", tag.getRevision());
		sb.append(dbTag.toJournalString() + "\n");

		return sb.toString();
	}

	private static String viewToJournal(String name, String view)
			throws Exception {
		StringBuffer sb = new StringBuffer();

		JournalRecord dbView = new JournalRecord("pv", "db.view", 0);
		dbView.addField("name", name);
		dbView.addField("seq", 0);
		dbView.addField("mapflag", 0);
		dbView.addField("vfile", "");
		dbView.addField("dfile", view);
		sb.append(dbView.toJournalString() + "\n");

		return sb.toString();
	}

	private static String depotPath(DepotInterface d, String path) {
		return ("//" + d.getBase() + path);
	}
}
