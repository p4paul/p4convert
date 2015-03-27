package com.perforce.cvs.integration;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer.Form;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ScmType;
import com.perforce.cvs.process.CvsProcessChange;
import com.perforce.integration.SystemCaller;

public class CvsIntegrationTests {

	private static Logger logger = LoggerFactory
			.getLogger(CvsIntegrationTests.class);

	// Set fixed paths
	private final static String basePath = "test/com/perforce/cvs/integration/integ/base/";
	private final static String cvsRootPath = "/test/com/perforce/cvs/integration/dumps/";
	private final String journalFile = "jnl.0";
	private final static String depotName = "import";

	// Globals
	private static String cwd;

	// Once at start of regression tests
	static {
		try {
			Config.setDefault();

			cwd = System.getProperty("user.dir");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Before
	public void before() {
		// Setup default test configuration
		try {
			Config.setDefault();
			Config.set(CFG.TEST, true);
			Config.set(CFG.SCM_TYPE, ScmType.CVS);
			Config.set(CFG.P4_MODE, "CONVERT");
			Config.set(CFG.P4_USER, "svn-user");
			Config.set(CFG.P4_CLIENT, "svn-client");
			Config.set(CFG.P4_PORT, "localhost:4444");
			Config.set(CFG.P4_ROOT, "./p4_root/");

			Config.set(CFG.VERSION, "alpha/TestMode");
			Config.set(CFG.P4_CLIENT_ROOT, "/ws");

			Config.set(CFG.EXCLUDE_MAP, "test_exclude.map");
			Config.set(CFG.INCLUDE_MAP, "test_include.map");
			Config.set(CFG.ISSUE_MAP, "test_issue.map");
			Config.set(CFG.USER_MAP, "test_users.map");
			Config.set(CFG.TYPE_MAP, "test_types.map");
			Config.set(CFG.PATH_MAP, "test_path.map");

			// Set path translation map defaults
			PathMapTranslator.setDefault();
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
		// PROPRIAROTY DATA -- not released
		// testCase("CVScluster01");
	}

	@Test
	public void case005() throws Exception {
		Config.set(CFG.CVS_MODULE, "rcs-deltas");
		Config.set(CFG.CVS_LABELS, true);
		// PROPRIAROTY DATA -- not released
		// testCase("CVScluster01");
	}

	@Test
	public void case006() throws Exception { // TEST
		Config.set(CFG.P4_C1_MODE, true);
		Config.set(CFG.CVS_MODULE, "reserved_chars");
		testCase("CVScluster01");
	}

	@Test
	public void case007() throws Exception { // TEST
		Config.set(CFG.P4_C1_MODE, true);
		Config.set(CFG.CVS_MODULE, "merge-rev");
		testCase("CVScluster01");
	}

	@Test
	public void case008() throws Exception {
		Config.set(CFG.P4_OFFSET, 100L);
		Config.set(CFG.CVS_MODULE, "add-edit-del-offset");
		testCase("CVScluster01");
	}

	@Test
	public void case009() throws Exception {
		Config.set(CFG.CVS_MODULE, "label-r1");
		Config.set(CFG.CVS_LABELS, true);
		// PROPRIAROTY DATA -- not released
		// testCase("CVScluster01");
	}

	@Test
	public void case010() throws Exception {
		Config.set(CFG.CVS_MODULE, "empty-rev");
		testCase("CVScluster01");
	}

	@Test
	public void case011() throws Exception {
		Config.set(CFG.CVS_MODULE, "empty-edit");
		testCase("CVScluster01");
	}

	/*
	 * CVS doesn't support symbolic links
	 * 
	 * @Test public void case012() throws Exception { Config.set(CFG.CVS_MODULE,
	 * "symlink"); testCase("CVScluster01"); }
	 * 
	 * @Test public void case013() throws Exception { Config.set(CFG.CVS_MODULE,
	 * "symlink-target"); testCase("CVScluster01"); }
	 */

	@Test
	public void case014() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "utf8-bom");
		testCase("CVScluster01");
	}

	@Test
	public void case015() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "utf8-nobom");
		testCase("CVScluster01");
	}

	@Test
	public void case016() throws Exception {
		Form form = (Form) Config.get(CFG.P4_NORMALISATION);
		if (form.equals(Form.NFD))
			Config.set(CFG.CVS_MODULE, "utf8_path_nfd");
		else
			Config.set(CFG.CVS_MODULE, "utf8_path_nfc");

		// TODO Config.set(CFG.CVS_MODULE, "utf8_path_nfc_win");
		testCase("CVScluster01");
	}

	@Test
	public void case019() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "win1251");
		testCase("CVScluster01");
	}

	@Test
	public void case020() throws Exception {
		Config.set(CFG.CVS_MODULE, "win_line");
		testCase("CVScluster01");
	}

	@Test
	public void case021() throws Exception {
		Config.set(CFG.CVS_MODULE, "nix_line");
		testCase("CVScluster01");
	}

	@Test
	public void case022() throws Exception {
		Config.set(CFG.CVS_MODULE, "mac_line");
		testCase("CVScluster01");
	}

	@Test
	public void case023() throws Exception {
		Config.set(CFG.CVS_MODULE, "huge_file");
		// PROPRIAROTY DATA -- not released
		// testCase("CVScluster01");
	}

	@Test
	public void case024() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "utf16_le");
		testCase("CVScluster01");
	}

	@Test
	public void case025() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "utf16_le_bom");
		testCase("CVScluster01");
	}

	@Test
	public void case026() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "utf16_be");
		testCase("CVScluster01");
	}

	@Test
	public void case027() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "utf16_be_bom");
		testCase("CVScluster01");
	}

	/*
	 * TODO merge operations
	 * 
	 * @Test public void case028() throws Exception { Config.set(CFG.CVS_MODULE,
	 * "merge-copy"); testCase("CVScluster01"); }
	 * 
	 * @Test public void case029() throws Exception { Config.set(CFG.CVS_MODULE,
	 * "merge-edit"); testCase("CVScluster01"); }
	 */

	@Test
	public void case030() throws Exception {
		Config.set(CFG.CVS_MODULE, "label_rev");
		Config.set(CFG.CVS_LABELS, true);
		testCase("CVScluster01");
	}

	@Test
	public void case031() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "bad_encoding");
		// PROPRIAROTY DATA -- not released
		// testCase("CVScluster01");
	}

	@Test
	public void case032() throws Exception {
		Config.set(CFG.CVS_MODULE, "binary-file");
		testCase("CVScluster01");
	}

	@Test
	public void case033() throws Exception {
		Config.set(CFG.CVS_MODULE, "no-author");
		testCase("CVScluster01");
	}

	@Test
	public void case034() throws Exception {
		Config.set(CFG.CVS_MODULE, "revert");
		testCase("CVScluster01");
	}

	@Test
	public void case035() throws Exception {
		Config.set(CFG.P4_C1_MODE, true);
		Config.set(CFG.CVS_MODULE, "branch");
		testCase("CVScluster01");
	}

	@Test
	public void case036() throws Exception {
		Config.set(CFG.CVS_MODULE, "username_umlaut");
		// PROPRIAROTY DATA -- not released
		// testCase("CVScluster01");
	}

	@Test
	public void case037() throws Exception {
		Config.set(CFG.CVS_MODULE, "username_rev_chars");
		testCase("CVScluster01");
	}

	@Test
	public void case038() throws Exception {
		Config.set(CFG.CVS_MODULE, "username_comma");
		testCase("CVScluster01");
	}

	@Test
	public void case039() throws Exception {
		Config.set(CFG.CVS_MODULE, "revert-from-prev-version-and-modify");
		testCase("CVScluster01");
	}

	@Test
	public void case040() throws Exception {
		Config.set(CFG.CVS_MODULE, "exec-file");
		testCase("CVScluster01");
	}

	@Test
	public void case041() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		Config.set(CFG.CVS_MODULE, "win1252_non");
		testCase("CVScluster01");
	}

	@Test
	public void case042() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
		Config.set(CFG.CVS_MODULE, "win1252");
		testCase("CVScluster01");
	}

	@Test
	public void case043() throws Exception {
		Config.set(CFG.CVS_MODULE, "null_path");
		testCase("CVScluster01");
	}

	@Test
	public void case044() throws Exception {
		Config.set(CFG.CVS_MODULE, "replace_dir_with_copy");
		testCase("CVScluster01");
	}

	@Test
	public void case045() throws Exception {
		CaseSensitivity mode = (CaseSensitivity) Config.get(CFG.P4_CASE);
		if (mode == CaseSensitivity.NONE) {
			Config.set(CFG.CVS_MODULE, "rename_case");
		} else {
			Config.set(CFG.CVS_MODULE, "rename_none");
		}
		testCase("CVScluster01");
	}

	@Test
	public void case046() throws Exception {
		Config.set(CFG.CVS_MODULE, "replace_file");
		testCase("CVScluster01");
	}

	@Test
	public void case047() throws Exception {
		Config.set(CFG.CVS_MODULE, "dead-branch");
		testCase("CVScluster01");
	}

	@Test
	public void case048() throws Exception {
		Config.set(CFG.CVS_MODULE, "end-file");
		testCase("CVScluster01");
	}

	@Test
	public void case049() throws Exception {
		Config.set(CFG.CVS_MODULE, "parse-comment");
		testCase("CVScluster01");
	}

	@Test
	public void case050() throws Exception {
		Config.set(CFG.CVS_MODULE, "branch-branch");
		testCase("CVScluster01");
	}

	@Test
	public void case051() throws Exception {
		Config.set(CFG.P4_C1_MODE, true);
		Config.set(CFG.CVS_LABELS, true);
		Config.set(CFG.CVS_MODULE, "br-trunk-del");
		testCase("CVScluster01");
	}

	@Test
	public void case052() throws Exception {
		Form form = (Form) Config.get(CFG.P4_NORMALISATION);
		if (form.equals(Form.NFC)) {
			Config.set(CFG.CVS_MODULE, "uri_path_nfc");
		} else {
			Config.set(CFG.CVS_MODULE, "uri_path");
		}

		String uri = "%E3%82%B9%E3%83%91%E3%83%A0_%E6%A4%9C%E7%96%AB_%2812%E6%9C%88_31_1969_04_00_%E5%8D%88%E5%BE%8C_%E3%81%8B%E3%82%89_12%E6%9C%88_31_1969_04_00_%E5%8D%88%E5%BE%8C_PST%29_add.txt,v";
		String dir = cwd + cvsRootPath + "CVScluster01" + "/uri_path/src";
		Path src = FileSystems.getDefault().getPath(dir, "short,v");
		Path dst = FileSystems.getDefault().getPath(dir, uri);
		Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);

		String testCase = (String) Config.get(CFG.CVS_MODULE);
		runTest("CVScluster01");

		String base = basePath + testCase + "/";
		diffMetadata(base, 5);
	}

	@Test
	public void case053() throws Exception {
		Config.set(CFG.CVS_MODULE, "symbols");
		testCase("CVScluster01");
	}

	@Test
	public void case054() throws Exception {
		Config.set(CFG.CVS_MODULE, "parse_text");
		testCase("CVScluster01");
	}

	@Test
	public void case055() throws Exception {
		Config.set(CFG.CVS_MODULE, "parse_dead");
		testCase("CVScluster01");
	}

	@Test
	public void case056() throws Exception {
		Config.set(CFG.P4_DEPOT_SUB, "sub/");
		Config.set(CFG.CVS_MODULE, "subpath_basic");
		testCase("CVScluster01");
	}

	@Test
	public void case057() throws Exception {
		Config.set(CFG.CVS_LABELS, true);
		Config.set(CFG.P4_DEPOT_SUB, "sub/foo/");
		Config.set(CFG.CVS_MODULE, "subpath_label");
		testCase("CVScluster01");
	}

	@Test
	public void case058() throws Exception {
		Config.set(CFG.CVS_LABELS, true);
		Config.set(CFG.CVS_MODULE, "label-view");
		testCase("CVScluster01");
	}
	
	@Test
	public void case059() throws Exception {
		Config.set(CFG.CVS_MODULE, "no-main");
		testCase("CVScluster01");
	}
	
	@Test
	public void case060() throws Exception {
		Config.set(CFG.CVS_LABELS, true);
		Config.set(CFG.CVS_MODULE, "no-main-label");
		testCase("CVScluster01");
	}

	private void testCase(String cvsCluster) {
		try {
			String testCase = (String) Config.get(CFG.CVS_MODULE);
			runTest(cvsCluster);

			String base = basePath + testCase + "/";
			diffArchive(base);
			diffMetadata(base, 0);
		} catch (Throwable e) {
			e.printStackTrace();
			fail("Exception");
		}
	}

	private void runTest(String cvsCluster) throws Exception {
		String p4_root = (String) Config.get(CFG.P4_ROOT);

		// Select dump file for test case
		String testCase = (String) Config.get(CFG.CVS_MODULE);
		logger.info("testcase: " + testCase);
		String cvsRootTest = cwd + cvsRootPath + cvsCluster + "/";
		Config.set(CFG.CVS_ROOT, cvsRootTest);

		// Remove old server
		String rm = "rm" + " -rf " + p4_root;
		SystemCaller.exec(rm, true, false);

		// Remove temp dir
		String tmp = "rm" + " -rf tmp";
		SystemCaller.exec(tmp, true, false);

		// Run test case
		CvsProcessChange process = new CvsProcessChange();
		process.runSingle();
	}

	private void diffArchive(String base) throws Exception {
		String p4_root = (String) Config.get(CFG.P4_ROOT);

		String arcTest = p4_root + depotName;
		String arcBase = base + depotName;
		new File(arcTest).mkdir(); // some tests have no archive files
		new File(arcBase).mkdir();

		String cmd = "diff -r " + arcTest + " " + arcBase;
		int arch = SystemCaller.exec(cmd, true, false);
		Assert.assertEquals("Archive:", 0, arch);
	}

	private void diffMetadata(String base, long warn) throws Exception {

		String jnlTest = "p4_root/" + journalFile;
		String jnlBase = base + journalFile;
		String sortTest = "<(sort " + jnlTest + ")";
		String sortBase = "<(sort " + jnlBase + ")";

		String p4m = "diff " + sortTest + " " + sortBase;
		int meta = SystemCaller.exec(p4m, true, false);
		Assert.assertEquals("Metadata:", 0, meta);

		// check warning count
		long count = Stats.getLong(StatsType.warningCount);
		Assert.assertEquals("Warnings:", warn, count);
	}
}
