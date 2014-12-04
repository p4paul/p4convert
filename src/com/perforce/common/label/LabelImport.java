package com.perforce.common.label;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.perforce.common.client.P4Factory;
import com.perforce.common.depot.DepotImport;
import com.perforce.common.process.ChangeInfo;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.impl.generic.core.Label.LabelMapping;
import com.perforce.p4java.option.server.TagFilesOptions;
import com.perforce.p4java.server.IOptionsServer;

public class LabelImport implements LabelInterface {

	private final DepotImport depot;
	private final String name;
	private final ChangeInfo change;

	private ILabel ilabel;
	private IOptionsServer iserver;
	private long automatic;

	private ArrayList<String> views = new ArrayList<String>();
	private ArrayList<TagConvert> revs = new ArrayList<TagConvert>();

	public LabelImport(String label, ChangeInfo change, DepotImport depot)
			throws Exception {
		this.depot = depot;
		this.name = label;
		this.change = change;

		iserver = depot.getIServer();
		ilabel = iserver.getLabel(name);

		if (ilabel == null) {
			Label l = new Label();
			l.setName(name);
			l.setOwnerName(depot.getUser());

			ViewMap<ILabelMapping> viewMap = new ViewMap<ILabelMapping>();
			Label.LabelMapping lMap = new Label.LabelMapping();
			lMap.setLeft("//" + depot.getName() + "/...");
			viewMap.addEntry(lMap);
			l.setViewMapping(viewMap);

			iserver.createLabel(l);
			ilabel = iserver.getLabel(name);
		}

		ilabel.setOwnerName(getOwner());
		ilabel.setLastAccess(change.getDate());
		ilabel.setLastUpdate(change.getDate());
		ilabel.setDescription(getDesc());
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
		ilabel.setRevisionSpec(getAutomatic());
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
		// not required
		return null;
	}

	@Override
	public void add(TagConvert tag) throws Exception {
		revs.add(tag);
	}

	@Override
	public void addView(String view) throws Exception {
		views.add(view);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name + " by: " + getOwner() + "\n");
		int i = 0;
		for(String view : views) {
			sb.append("   view[" + i + "] " + view);
			i++;
		}
		for (TagConvert tag : revs) {
			sb.append("... " + tag + "\n");
		}
		return sb.toString();
	}

	@Override
	public void submit() throws Exception {
		for (TagConvert tag : revs) {
			tagLabel(tag);
		}
		buildViewMap();
		ilabel.update();
	}

	private void buildViewMap() {
		ViewMap<ILabelMapping> viewMap = new ViewMap<ILabelMapping>();
		int i = 0;
		for (String view : views) {
			LabelMapping map = new LabelMapping(i, view);
			viewMap.addEntry(map);
			i++;
		}
		ilabel.setViewMapping(viewMap);
	}

	private void tagLabel(TagConvert tag) throws Exception {
		StringBuffer fileStr = new StringBuffer();
		fileStr.append("//" + depot.getName() + "/");
		fileStr.append(tag.getPath());

		List<IFileSpec> fileSpecs;
		fileSpecs = FileSpecBuilder.makeFileSpecList(fileStr.toString());

		TagFilesOptions tagOpts = new TagFilesOptions();
		List<IFileSpec> tagSpec = iserver.tagFiles(fileSpecs, name, tagOpts);
		P4Factory.validateFileSpecs(tagSpec, "label in sync.");
	}
}
