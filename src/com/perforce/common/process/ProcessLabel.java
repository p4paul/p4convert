package com.perforce.common.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.perforce.common.depot.DepotInterface;
import com.perforce.common.label.LabelInterface;
import com.perforce.common.label.TagConvert;
import com.perforce.cvs.RevisionEntry;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.query.QueryInterface;
import com.perforce.svn.tag.TagEntry;
import com.perforce.svn.tag.TagType;

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

	/**
	 * Add SVN directory to an AUTOMATIC label or each file revision to a STATIC
	 * label.
	 * 
	 * @param tag
	 * @param change
	 * @throws Exception
	 */
	public void labelChange(TagEntry tagEntry, ChangeInfo change) throws Exception {
		LabelInterface label;
		String id = tagEntry.getId();
		
		if (labelMap.containsKey(id)) {
			label = labelMap.get(id);
		} else {
			label = ProcessFactory.getLabel(id, change, depot);
		}
		
		if (tagEntry.getType() == TagType.AUTOMATIC) {
			label.setAutomatic(change.getScmChange());
		} else {
			String fromPath = tagEntry.getFromPath();
			long fromChange = tagEntry.getFromChange();
			
			List<ChangeAction> list = query.listLastActions(fromPath, fromChange);
			for(ChangeAction a : list) {
				TagConvert tag = new TagConvert(a.getPath(), a.getEndRev());
				label.add(tag);
			}
		}
		
		labelMap.put(id, label);
	}

	/**
	 * Add SVN file revisions to a STATIC label
	 * 
	 * @param tagEntry
	 * @param change
	 * @throws Exception
	 */
	public void labelRev(TagEntry tagEntry, ChangeInfo change) throws Exception {
		LabelInterface label;
		String id = tagEntry.getId();

		if (labelMap.containsKey(id)) {
			label = labelMap.get(id);
		} else {
			label = ProcessFactory.getLabel(id, change, depot);
		}

		String fromPath = tagEntry.getFromPath();
		long fromChange = tagEntry.getFromChange();
		int revision = query.findHeadRevision(fromPath, fromChange);
		TagConvert tag = new TagConvert(fromPath, revision);
		label.add(tag);

		labelMap.put(id, label);
	}

	/**
	 * Add CVS revisions to a STATIC label
	 * 
	 * @param entry
	 * @param change
	 * @throws Exception
	 */
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
