package com.perforce.common.schema;

import java.util.ArrayList;
import java.util.List;

import com.perforce.common.ConverterException;

public class JournalRecord {

	static class Entry {
		public Attribute attrib;
		public Object value;

		public Entry(Attribute attrib, Object value) {
			this.attrib = attrib;
			this.value = value;
		}
	}

	private String action;
	private String table;
	private int version;
	private TableVersion schema;
	private List<Entry> fields = new ArrayList<Entry>();

	public JournalRecord(String action, String table, int version) {
		this.action = action;
		this.table = table;
		this.version = version;
		schema = Schema.GetTableVersion(table, version);
//		System.out.println(table);
//		System.out.println(schema);
	}

	public void addField(String key, Object value) throws ConverterException {
		int next = fields.size();
		Attribute attrib = schema.getAttribute(next);
		if (attrib.getName().equals(key)) {
			fields.add(new Entry(attrib, value));
		} else {
			throw new ConverterException("Journal field error(" + key + ")");
		}
	}

	public String toJournalString() {
		StringBuffer sb = new StringBuffer();
		sb.append("@" + action + "@ ");
		sb.append(version + " ");
		sb.append("@" + table + "@ ");
		
		for (Entry e : fields) {
			sb.append(e.attrib.getDomain().toJournalFormat(e.value));
			sb.append(" ");
		}
		return sb.toString();
	}

}
