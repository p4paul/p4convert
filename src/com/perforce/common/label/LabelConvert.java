package com.perforce.common.label;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.perforce.common.depot.DepotConvert;
import com.perforce.common.journal.BuildLabel;
import com.perforce.common.process.ChangeInfo;

public class LabelConvert implements LabelInterface {

	private final DepotConvert depot;
	private final String name;
	private final ChangeInfo change;

	private long automatic = 0;

	private ArrayList<String> views = new ArrayList<String>();
	private List<TagConvert> revs = new ArrayList<TagConvert>();

	public LabelConvert(String label, ChangeInfo change, DepotConvert depot) {
		this.depot = depot;
		this.name = label;
		this.change = change;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOwner() {
		return change.getUser();
	}

	@Override
	public Long getDate() {
		Date date = change.getDate();
		long time = date.getTime() / 1000;
		return time;
	}

	@Override
	public void setAutomatic(long automatic) {
		this.automatic = automatic;
	}

	@Override
	public String getAutomatic() {
		if (automatic > 0) {
			return "@" + automatic;
		}
		return "";
	}

	@Override
	public String getDesc() {
		return change.getDescription();
	}

	@Override
	public List<TagConvert> getTags() {
		return revs;
	}

	@Override
	public void add(TagConvert tag) {
		revs.add(tag);
	}

	@Override
	public void addView(String view) throws Exception {
		views.add(view);
	}
	
	public ArrayList<String> getView() {
		return views;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name + " by: " + getOwner() + "\n");
		int i = 0;
		for(String view : views) {
			sb.append("   view[" + i + "] " + view);
			i++;
		}
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
