package com.perforce.common.journal;

import com.perforce.common.ConverterException;
import com.perforce.common.schema.JournalRecord;

public class BuildCounter {

	public static String toJournal(String key, String value)
			throws ConverterException {
		StringBuffer sb = new StringBuffer();
		JournalRecord dbCounters = new JournalRecord("rv", "db.counters", 1);
		dbCounters.addField("name", key);
		dbCounters.addField("value", value);
		sb.append(dbCounters.toJournalString() + "\n");

		return sb.toString();
	}
}
