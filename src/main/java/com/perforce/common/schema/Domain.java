package com.perforce.common.schema;

public enum Domain {
	INT {
		public String toJournalFormat(Object o) {
			return o.toString();
		}
	},
	KEY {
		public String toJournalFormat(Object o) {
			StringBuffer buf = new StringBuffer();
			buf.append("@");
			buf.append(o.toString().replaceAll("@", "@@"));
			buf.append("@");

			return buf.toString();
		}
	},
	TEXT {
		public String toJournalFormat(Object o) {
			StringBuffer buf = new StringBuffer();
			buf.append("@");
			buf.append(o.toString().replaceAll("@", "@@"));
			buf.append("@");

			return buf.toString();
		}
	},
	OCTET {
		public String toJournalFormat(Object o) {
			return o.toString();
		}
	},
	OCTETS {
		public String toJournalFormat(Object o) {
			return o.toString();
		}
	},
	DATE {
		public String toJournalFormat(Object o) {
			return o.toString();
		}
	},
	INT8 {
		public String toJournalFormat(Object o) {
			return o.toString();
		}
	},
	INT64 {
		public String toJournalFormat(Object o) {
			return o.toString();
		}
	};

	Domain() {

	}

	abstract public String toJournalFormat(Object o);
}