package com.perforce.cvs.integration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ScmType;
import com.perforce.cvs.process.CvsProcessChange;
import com.perforce.integration.SystemCaller;

public class CvsImportTests {
	private static Logger logger = LoggerFactory
			.getLogger(CvsImportTests.class);

	// Fetch system properties
	private final static String p4broker = "p4broker";
	private final static String p4d = "p4d";
	private final static String p4 = "p4";

	// Set fixed paths
	private final static String basePath = "test/com/perforce/cvs/integration/import/base/";
	private final static String cvsRootPath = "test/com/perforce/cvs/integration/dumps/";
	private final static String journalFile = "jnl.sort";
	private final static String seedFile = "ckp.seed";
	private final static String seedLbr = "lbr.seed/";
	private final static String depotName = "import";

	// Globals
	private static String cwd;
	private static String p4root;
	private static String p4user;
	private static String p4ws;

	// Once at start of regression tests
	static {
		try {
			Config.setDefault();

			p4ws = System.getProperty("user.dir") + "/ws/";
			cwd = System.getProperty("user.dir");
			p4root = "p4_root/";
			p4user = (String) Config.get(CFG.P4_USER);

			// Check environment
			checkEnvironment();

			// Kill brokers
			logger.info("Setting up p4brokers...");
			SystemCaller.killAll("p4broker");

			// Start broker (used for all tests)
			startBroker(cwd + "/" + p4root, "localhost:4444", "-C1 -vserver=1");
			startBroker(cwd + "/" + p4root, "localhost:4445", "-vserver=1");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	final void ImportTest() throws Exception {
		SystemCaller.killAll("p4broker");
	}

	@Before
	public void before() {
		// Setup default test configuration
		try {
			Config.setDefault();
			Config.set(CFG.TEST, true);
			Config.set(CFG.SCM_TYPE, ScmType.CVS);
			Config.set(CFG.P4_MODE, "IMPORT");
			Config.set(CFG.P4_PORT, "localhost:4444");
			Config.set(CFG.P4_CLIENT_ROOT, p4ws);
			Config.set(CFG.P4_ROOT, p4root);
			Config.set(CFG.VERSION, "alpha/TestMode");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void case001() throws Exception {
		Config.set(CFG.CVS_MODULE, "add-edit-del");
		testCase("CVScluster01");
	}

	@Test
	public void case002() throws Exception {
		Config.set(CFG.CVS_MODULE, "edit-textblock");
		testCase("CVScluster01");
	}

	@Test
	public void case003() throws Exception {
		Config.set(CFG.CVS_MODULE, "empty-full");
		testCase("CVScluster01");
	}

	@Test
	public void case004() throws Exception {
		Config.set(CFG.CVS_MODULE, "large-text");
		testCase("CVScluster01");
	}

	@Test
	public void case005() throws Exception {
		Config.set(CFG.CVS_MODULE, "rcs-deltas");
		testCase("CVScluster01");
	}

	@Test
	public void case006() throws Exception {
		Config.set(CFG.CVS_MODULE, "reserved_chars");
		testCase("CVScluster01");
	}

	@Test
	public void case007() throws Exception {
		Config.set(CFG.CVS_MODULE, "merge-rev");
		testCase("CVScluster01");
	}

	@Test
	public void case008() throws Exception {
		String test = "add-edit-del-offset";
		Config.set(CFG.CVS_MODULE, test);
		String seed = cvsRootPath + "CVScluster01" + "/" + test + "/";
		testCase("CVScluster01", seed);
	}

	@Test
	public void case009() throws Exception {
		Config.set(CFG.CVS_MODULE, "label-r1");
		// TODO testCase("CVScluster01");
	}

	@Test
	public void case032() throws Exception {
		Config.set(CFG.CVS_MODULE, "binary-file");
		testCase("CVScluster01");
	}

	// keyword expansion
	// single revision

	/**
	 * Environment test
	 * 
	 * @throws Exception
	 */
	private static void checkEnvironment() throws Exception {

		String[] p4bins = { p4, p4d, p4broker };

		// Test binary exists and report version
		for (String bin : p4bins) {
			int test = SystemCaller.exec(bin + " -V", true, false);
			if (test != 0) {
				logger.info("Cannot find " + bin + ", please check PATH!");
				logger.info("PATH = " + System.getenv("PATH"));
				System.exit(test);
			}
		}
	}

	/**
	 * Filter the journal file for selected tables then sort
	 * 
	 * @param p4Root
	 * @throws Exception
	 */
	private void filterJournal(String p4Root) throws Exception {
		String[] tables = { "@db.rev@", "@db.integed@", "@db.change@",
				"@db.desc@" };

		// Filter results (grep and sort)
		String base = "grep '^@pv@' " + p4root + "checkpoint.1 | ";
		String file = p4Root + journalFile;
		for (String t : tables) {
			String grep = base + "grep '" + t + "' >> " + file;
			SystemCaller.exec(grep, true, false);
		}
	}

	/**
	 * Start broker for Perforce connections Listen: <p4_port> Server: RSH
	 * <p4_root>
	 * 
	 * @throws Exception
	 */
	private static void startBroker(String p4root, String p4port, String flags)
			throws Exception {

		Map<String, String> broker = new LinkedHashMap<String, String>();
		String brokerConfig = "broker." + p4port + ".cfg";
		brokerConfig = brokerConfig.replace(":", ".");
		String rsh = "rsh:" + p4d + " -r " + p4root;
		rsh += " -Llog " + flags + " -i";

		// Build broker configuration file
		broker.put("target", "\"" + rsh + "\"");
		broker.put("listen", p4port);
		broker.put("directory", p4root);
		broker.put("logfile", "broker.log");
		broker.put("debug-level", "server=1");
		broker.put("admin-name", "svn-admin");
		broker.put("admin-phone", "svn-phone");
		broker.put("admin-email", "svn@email");
		broker.put("redirection", "selective");

		BufferedWriter out = new BufferedWriter(new FileWriter(brokerConfig));
		for (String key : broker.keySet()) {
			out.write(key + "=" + broker.get(key) + ";\n");
		}
		out.flush();
		out.close();

		String p4b = p4broker + " -c " + cwd + "/" + brokerConfig + " -d";
		SystemCaller.exec(p4b, false, false);
	}

	/**
	 * Cleans the perforce server (db files and archives)
	 * 
	 * @throws Exception
	 */
	private void cleanPerforce() throws Exception {
		// Stop (flush) old perforce instance
		String p4_port = (String) Config.get(CFG.P4_PORT);
		String stop = p4 + " -u " + p4user + " -p " + p4_port + " admin stop";
		SystemCaller.exec(stop, true, false);

		// Remove old server and workspace
		Thread.sleep(1000);
		String rm = "rm" + " -rf " + p4root + "/* " + p4ws;
		SystemCaller.exec(rm, true, false);

		// Make p4 root
		new File(p4root).mkdir();
	}

	private void seedPerforce(String seed) throws Exception {
		String meta = cwd + "/" + seed + seedFile;
		String lbr = cwd + "/" + seed + seedLbr;
		String map = cwd + "/" + seed + seedLbr + "changeMap.txt";
		String ckp = p4d + " -C1 -r " + p4root + " -jr " + meta;
		SystemCaller.exec(ckp, true, false);
		String upd = p4d + " -C1 -r " + p4root + " -xu";
		SystemCaller.exec(upd, true, false);

		String cp = "cp -rfv " + lbr + " " + cwd + "/" + p4root + "import";
		SystemCaller.exec(cp, true, false);

		String cpMap = "cp -f " + map + " " + cwd;
		SystemCaller.exec(cpMap, true, false);
	}

	/**
	 * Run testcase and validate result
	 * 
	 * @param dumpCase
	 */
	private void testCase(String dumpCase) {
		testCase(dumpCase, null);
	}

	private void testCase(String cvsCluster, String seed) {
		try {
			String p4_port = (String) Config.get(CFG.P4_PORT);

			// Select dump file for test case
			String testCase = (String) Config.get(CFG.CVS_MODULE);
			logger.info("testcase: " + testCase);
			String cvsRootTest = cwd + "/" + cvsRootPath + cvsCluster + "/";
			Config.set(CFG.CVS_ROOT, cvsRootTest);

			// Paths and configurations
			String base = basePath + testCase + "/";

			// clean Perforce server
			cleanPerforce();

			// seed Perforce if checkpoint provided
			if (seed != null) {
				seedPerforce(seed);
			}

			// Switch to unicode in enabled
			if ((Boolean) Config.get(CFG.P4_UNICODE)) {
				String p4uni = p4 + " -u " + p4user + " -p " + p4_port
						+ " counter -f unicode 1";
				SystemCaller.exec(p4uni, true, false);
			}

			// Run test case
			CvsProcessChange process = new CvsProcessChange();
			process.runSingle();

			// Checkpoint
			String p4c = p4d + " -r " + p4root + " -jc";
			SystemCaller.exec(p4c, true, false);

			// Diff archive files
			String arcTest = p4root + depotName;
			String arcBase = base + depotName;
			new File(arcTest).mkdir(); // some tests have no archive files
			new File(arcBase).mkdir();

			String cmd = "diff -r " + arcTest + " " + arcBase;
			int arch = SystemCaller.exec(cmd, true, false);
			Assert.assertEquals("Archive:", 0, arch);

			// Filter and sort journal
			filterJournal(p4root);

			// Diff metadata
			String jnlTest = p4root + journalFile;
			String jnlBase = base + journalFile;
			String sortTest = "<(sort " + jnlTest + ")";
			String sortBase = "<(sort " + jnlBase + ")";

			String p4m = "diff " + sortTest + " " + sortBase;
			int meta = SystemCaller.exec(p4m, true, false);
			Assert.assertEquals("Metadata:", 0, meta);

			// check warning count
			long warn = Stats.getLong(StatsType.warningCount);
			Assert.assertEquals("Warnings:", 0, warn);

		} catch (Throwable e) {
			e.printStackTrace();
			Assert.fail("Exception");
		}
	}
}
