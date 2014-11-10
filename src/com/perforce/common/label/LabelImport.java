package com.perforce.common.label;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.perforce.common.client.P4Factory;
import com.perforce.common.depot.DepotImport;
import com.perforce.cvs.RevisionEntry;
import com.perforce.p4java.core.ILabel;
import com.perforce.p4java.core.ILabelMapping;
import com.perforce.p4java.core.ViewMap;
import com.perforce.p4java.core.file.FileSpecBuilder;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.Label;
import com.perforce.p4java.option.server.TagFilesOptions;
import com.perforce.p4java.server.IOptionsServer;

public class LabelImport implements LabelInterface {

	private DepotImport depot;
	private ILabel ilabel;
	private IOptionsServer iserver;
	private String name;
	private String owner;
	private Date date;

	private ArrayList<TagConvert> revs = new ArrayList<TagConvert>();

	public LabelImport(String label, RevisionEntry entry, DepotImport depot)
			throws Exception {
		this.depot = depot;
		this.name = label;
		this.owner = entry.getAuthor();
		this.date = entry.getDate();

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

		ilabel.setOwnerName(owner);
		ilabel.setLastAccess(date);
		ilabel.setLastUpdate(date);
		ilabel.setDescription(getDesc());
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
		// not required
		return null;
	}

	@Override
	public void add(TagConvert tag) throws Exception {
		revs.add(tag);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(name + " by: " + owner + "\n");
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
		ilabel.update();
	}

	private void tagLabel(TagConvert tag) throws Exception {
		StringBuffer fileStr = new StringBuffer();
		fileStr.append("//" + depot.getName() + "/");
		fileStr.append(tag.getPath());
		// fileStr.append("#" + tag.getRevision());

		List<IFileSpec> fileSpecs;
		fileSpecs = FileSpecBuilder.makeFileSpecList(fileStr.toString());

		TagFilesOptions tagOpts = new TagFilesOptions();
		List<IFileSpec> tagSpec = iserver.tagFiles(fileSpecs, name, tagOpts);
		P4Factory.validateFileSpecs(tagSpec);
	}
}
