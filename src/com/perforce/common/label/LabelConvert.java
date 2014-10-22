package com.perforce.common.label;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.perforce.common.depot.DepotConvert;
import com.perforce.common.journal.BuildLabel;
import com.perforce.cvs.RevisionEntry;

public class LabelConvert implements LabelInterface {

	private DepotConvert depot;
	private String name;
	private String owner;
	private Date date;
	private List<TagConvert> revs = new ArrayList<TagConvert>();

	public LabelConvert(String label, RevisionEntry entry, DepotConvert depot) {
		this.depot = depot;
		this.name = label;
		this.owner = entry.getAuthor();
		this.date = entry.getDate();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public Long getDate() {
		return date.getTime() / 1000;
	}

	@Override
	public String getDesc() {
		StringBuffer sb = new StringBuffer();
		sb.append("Created by ");
		sb.append(owner);
		sb.append(".\n");
		return sb.toString();
	}

	@Override
	public List<TagConvert> getTags() {
		return revs;
	}

	@Override
	public void add(TagConvert tag) {
		revs.add(tag);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name + " by: " + owner + "\n");
		for (TagConvert tags : revs) {
			sb.append("... " + tags + "\n");
		}
		return sb.toString();
	}

	@Override
	public void submit() throws Exception {
		ArrayList<String> journal = BuildLabel.toJournal(depot, this);
		depot.getJournal().write(journal);
		depot.getJournal().flush();
	}
}
