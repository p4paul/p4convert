package com.perforce.svndump.prescan;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.parser.Record;
import com.perforce.svn.prescan.ExtractRecord;

public class TestExtractRecord {

	private static Logger logger = LoggerFactory
			.getLogger(TestExtractRecord.class);
	private final static String dumpPath = "test/com/perforce/integration/dumps/";
	private final static String dumpFile = "repo.dump";

	static {
		try {
			Config.setDefault();
		} catch (ConfigException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void case001() throws Exception {
		String test = "pending_del_br_del";
		String path = dumpPath + test + "/" + dumpFile;

		Record record = extract(path, 1, 2);

		String recordMD5 = record.getHeader().findString("Text-content-md5");
		String verifyMD5 = "f75b8179e4bbe7e2b4a074dcef62de95";
		Assert.assertEquals(verifyMD5, recordMD5);
	}

	private static Record extract(String path, int rev, int node)
			throws Exception {
		logger.info("Scanning: " + path);
		ExtractRecord extract = new ExtractRecord(path);
		Record record = extract.findNode(rev, node);
		return record;
	}
}
