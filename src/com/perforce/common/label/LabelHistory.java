package com.perforce.common.label;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.svn.process.MergeSource;
import com.perforce.svn.tag.TagEntry;
import com.perforce.svn.tag.TagParser;

public class LabelHistory {

	private static Logger logger = LoggerFactory.getLogger(LabelHistory.class);

	private static HashMap<String, LabelInterface> map = new HashMap<String, LabelInterface>();

	public static void add(LabelInterface label) throws ConverterException {
		String name = label.getName();
		if (!map.containsKey(name)) {
			map.put(name, label);
		} else {
			String err = "Label already submitted: " + name;
			throw new ConverterException(err);
		}

		if (logger.isDebugEnabled()) {
			String mode = (label.isAutomatic()) ? "AUTOMATIC" : "STATIC";
			logger.debug(mode + ": " + label.getFromPath() + "@"
					+ label.getFromRev());
		}
	}

	public static MergeSource find(String path, long svnRev) {
		if (!TagParser.isLabel(path)) {
			return null;
		}

		TagEntry tagEntry = TagParser.getLabel(path);
		String name = tagEntry.getId();
		if (map.containsKey(name)) {
			LabelInterface label = map.get(name);
			String fromPath = label.getFromPath();
			long fromRev = label.getFromRev();

			MergeSource from = new MergeSource(fromPath, 1, fromRev);
			return from;
		}
		return null;
	}

	public static void clear() {
		map = new HashMap<String, LabelInterface>();
	}
}
