package com.perforce.svndump.parser;

import java.text.Normalizer.Form;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;
import com.perforce.svn.parser.SubversionWriter;
import com.perforce.svn.process.SvnProcessChange;

public class TestSchema {

	private static Logger logger = LoggerFactory.getLogger(TestSchema.class);

	private final String outFile = "out.dump";
	private final String dumpPath = "test/com/perforce/integration/dumps/";
	private final String dumpFile = "repo.dump";

	@Before
	public void before() {
		// Setup default test configuration
		try {
			Config.setDefault();
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Verify output using command line diff with -B flag (Ignore changes whose
	 * lines are all blank)
	 * 
	 * @param args
	 * @throws ConverterException
	 */
	@Test
	public void case001() throws Exception {
		String dumpFile = dumpPath + "rename_case/repo.dump";
		test(dumpFile);
	}

	@Test
	public void case002() throws Exception {
		String dumpFile = dumpPath + "pending_del_br_del/repo.dump";
		test(dumpFile);
	}

	@Test
	public void case003() throws Exception {
		// utf8 chars in path
		String dumpFile;
		Form form = (Form) Config.get(CFG.P4_NORMALISATION);
		if (form.equals(Form.NFD))
			dumpFile = dumpPath + "utf8_path_nfd/repo.dump";
		else
			dumpFile = dumpPath + "utf8_path_nfc/repo.dump";
		test(dumpFile);
	}

	@Test
	public void case004() throws Exception {
		String dumpCase = "delta";

		logger.info("testcase: " + dumpCase);
		Config.setDefault();
		Config.set(CFG.P4_MODE, "CONVERT");
		Config.set(CFG.SVN_DUMPFILE, dumpPath + dumpCase + "/" + dumpFile);

		// Run test case
		try {
			SvnProcessChange process = new SvnProcessChange();
			process.runSingle();
		} catch (ConverterException e) {
			Assert.assertTrue("Aborted correctly", true);
			return;
		}
		Assert.assertTrue("Abort missed", false);
	}

	private void test(String dumpFile) throws Exception {
		RecordReader dump = new RecordReader(dumpFile);

		SubversionWriter out = new SubversionWriter(outFile, false);
		for (Record record : dump) {
			out.putRecord(record);
		}
		out.flush();
		out.close();

		// Diff dump files
		String cmd = "diff " + dumpFile + " " + outFile;
		Process runDiff = Runtime.getRuntime().exec(cmd);
		runDiff.waitFor();
		Assert.assertEquals(0, runDiff.exitValue());
	}
}
