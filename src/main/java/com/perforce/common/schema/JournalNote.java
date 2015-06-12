package com.perforce.common.schema;

import java.util.ArrayList;
import java.util.List;

import com.perforce.common.ConverterException;
import com.perforce.common.schema.JournalRecord.Entry;

public class JournalNote {

	private String action;
	private NoteType type;
	private int version;
	private TableVersion schema;

	private List<Entry> fields = new ArrayList<Entry>();

	public enum NoteType {
		CKP_HEADER(0), 
		JNL_HEADER(2);

		final int id;

		NoteType(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}
	
	
	public enum FlagType {
		JC(0), 
		JD(1),
		JDS(2);

		final int id;

		FlagType(int id) {
			this.id = id;
		}

		public int getValue() {
			return id;
		}
	}


	public enum RunMode {
		C0(0x0001),
		C1(0x0002),
		C2(0x0004),
		UNICODE(0x0008);

		final int flag;

		RunMode(int f) {
			this.flag = f;
		}

		public int getValue() {
			return flag;
		}
	}

	public JournalNote(String a, NoteType t, int v) {
		action = a;
		type = t;
		version = v;
		schema = Schema.GetTableVersion(type.name(), version);
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
		sb.append(type.getId() + " ");

		for (Entry e : fields) {
			sb.append(e.attrib.getDomain().toJournalFormat(e.value));
			sb.append(" ");
		}
		return sb.toString();
	}
}
