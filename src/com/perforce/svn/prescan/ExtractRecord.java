package com.perforce.svn.prescan;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.Record.Type;
import com.perforce.svn.parser.RecordReader;

public class ExtractRecord {

	private Logger logger = LoggerFactory.getLogger(ExtractRecord.class);

	private String dumpFile;

	public ExtractRecord(String path) throws Exception {
		dumpFile = path;
	}

	public Record findRecord(int rev) {
		RecordReader recordReader = new RecordReader(dumpFile);

		if (logger.isInfoEnabled()) {
			logger.info("searching for record: " + rev + "...");
		}

		for (Record r : recordReader) {
			if ((r.getSvnRevision()) == rev && (r.getType() == Type.REVISION)) {
				return r;
			}
		}
		return null;
	}

	public Record findNode(int rev, int node) {
		RecordReader recordReader = new RecordReader(dumpFile);
		boolean gotRecord = false;

		if (logger.isInfoEnabled()) {
			logger.info("searching for node: " + rev + "." + node + "...");
		}

		for (Record record : recordReader) {

			switch (record.getType()) {

			case REVISION:
				if (record.getSvnRevision() == rev) {
					gotRecord = true;
				}
				break;

			case NODE:
				if (gotRecord) {
					if (record.getNodeNumber() == node) {
						return record;
					}
				}
				break;

			default:
				break;
			}
		}
		return null;
	}

	public List<Record> findNode(int rev, int start, int end) {
		RecordReader recordReader = new RecordReader(dumpFile);
		boolean gotRecord = false;
		List<Record> records = new ArrayList<Record>();

		if (logger.isInfoEnabled()) {
			logger.info("searching for nodes: " + rev + "." + start + "-" + end
					+ "...");
		}

		for (Record record : recordReader) {

			switch (record.getType()) {

			case REVISION:
				if (record.getSvnRevision() == rev) {
					gotRecord = true;
					records.add(record);
				}
				break;

			case NODE:
				if (gotRecord) {
					int n = record.getNodeNumber();
					if ((n >= start) && (n <= end)) {
						records.add(record);
					}
					if (n >= end) {
						return records;
					}
				}
				break;

			default:
				break;
			}
		}
		return null;
	}
}
