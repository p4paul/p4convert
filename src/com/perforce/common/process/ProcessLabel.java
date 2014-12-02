package com.perforce.common.process;

import java.util.HashMap;
import java.util.Map;

import com.perforce.common.depot.DepotInterface;
import com.perforce.common.label.LabelInterface;
import com.perforce.common.label.TagConvert;
import com.perforce.cvs.RevisionEntry;
import com.perforce.svn.query.QueryInterface;

public class ProcessLabel {

	private QueryInterface query;
	private DepotInterface depot;

	private HashMap<String, LabelInterface> labelMap = new HashMap<String, LabelInterface>();

	public ProcessLabel(DepotInterface depot) throws Exception {
		this.depot = depot;
		this.query = ProcessFactory.getQuery(depot);
	}

	protected QueryInterface getQuery() {
		return query;
	}

	public DepotInterface getDepot() {
		return depot;
	}

	public void labelChange(String tag, ChangeInfo change) throws Exception {
		LabelInterface label;
		label = ProcessFactory.getLabel(tag, change, depot);
		label.setAutomatic(change.getScmChange());
		labelMap.put(tag, label);
	}
	
	public void labelRev(RevisionEntry entry, long change) throws Exception {
		for (String labelName : entry.getLabels()) {
			LabelInterface label;

			if (labelMap.containsKey(labelName)) {
				label = labelMap.get(labelName);
			} else {
				ChangeInfo changeInfo = new ChangeInfo(entry, change);
				label = ProcessFactory.getLabel(labelName, changeInfo, depot);
			}

			String path = entry.getPath();
			int revision = query.findHeadRevision(path, change);
			TagConvert tag = new TagConvert(path, revision);
			label.add(tag);
			labelMap.put(labelName, label);
		}
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, LabelInterface> entry : labelMap.entrySet()) {
			sb.append(entry.getValue());
		}
		return sb.toString();
	}

	public void submit() throws Exception {
		for (Map.Entry<String, LabelInterface> entry : labelMap.entrySet()) {
			entry.getValue().submit();
		}
	}

}
