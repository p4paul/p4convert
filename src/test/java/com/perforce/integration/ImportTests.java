package com.perforce.integration;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.Normalizer.Form;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ScmType;
import com.perforce.svn.change.ChangeParser;
import com.perforce.svn.process.SvnProcessChange;

public class ImportTests {

	private static Logger logger = LoggerFactory.getLogger(ImportTests.class);

	// Set fixed paths
	private final static String p4dVersion = "r15.1";
	private final static String p4dPath = "src/test/resources/";
	private final static String basePath = "src/test/java/com/perforce/integration/import/base/";
	private final static String dumpPath = "src/test/java/com/perforce/integration/dumps/";
	private final static String dumpFile = "repo.dump";
	private final static String journalFile = "jnl.sort";
	private final static String seedFile = "ckp.seed";
	private final static String seedLbr = "lbr.seed/";
	private final static String depotName = "import";

	// Globals
	private static String p4d;
	private static String cwd;
	private static String p4root;
	private static String p4ws;

	// Once at start of regression tests
	static {
		try {
			Config.setDefault();

			p4ws = System.getProperty("user.dir") + "/ws/";
			cwd = System.getProperty("user.dir") + "/";
			p4root = System.getProperty("user.dir") + "/p4_root/";

			String os = System.getProperty("os.name").toLowerCase();
			p4d = cwd + p4dPath + p4dVersion + "/";
			if (os.contains("win")) {
				p4d += "bin.ntx64/p4d.exe";
			}
			if (os.contains("mac")) {
				p4d += "bin.darwin90x86_64/p4d";
			}
			if (os.contains("nix") || os.contains("nux")) {
				p4d += "bin.linux26x86_64/p4d";
			}

			// Check environment
			checkEnvironment();
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
			Config.set(CFG.SCM_TYPE, ScmType.SVN);
			Config.set(CFG.P4_MODE, "IMPORT");
			Config.set(CFG.P4_USER, "svn-user");
			Config.set(CFG.P4_CLIENT, "svn-client");
			Config.set(CFG.P4_CLIENT_ROOT, p4ws);
			Config.set(CFG.P4_ROOT, p4root);
			Config.set(CFG.VERSION, "alpha/TestMode");

			String rsh = "rsh:" + p4d + " -r " + p4root + " -i";
			Config.set(CFG.P4_PORT, rsh);

			Config.set(CFG.SVN_PROP_NAME, ".svn.properties");
			Config.set(CFG.SVN_PROP_ENCODE, "ini");
			Config.set(CFG.SVN_PROP_ENABLED, true);
			Config.set(CFG.P4_END, 0L);

			Config.set(CFG.EXCLUDE_MAP, "test_exclude.map");
			Config.set(CFG.INCLUDE_MAP, "test_include.map");
			Config.set(CFG.ISSUE_MAP, "test_issue.map");
			Config.set(CFG.USER_MAP, "test_users.map");
			Config.set(CFG.TYPE_MAP, "test_types.map");
			Config.set(CFG.PATH_MAP, "test_path.map");

			// Set path translation map defaults
			PathMapTranslator.setDefault();
			
			// Reset TypeMap
			TypeMap.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void case001() throws Exception { // TEST
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "add-del-br-del-br";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "add-del-br-del-br_13.1";
			testCase(test);
		} else {
			String test = "add-del-br-del-br_10.2";
			testCase(test);
		}
	}

	@Test
	public void case002() throws Exception { // TEST
		String test = "bin_tags";
		testCase(test);
	}

	@Test
	public void case003() throws Exception { // TEST
		TypeMap.add("gif", "binary");
		String test = "binary_file";
		testCase(test);
	}

	@Test
	public void case004() throws Exception { // TEST
		String test = "branch-remove-add";
		testCase(test);
	}

	@Test
	public void case005() throws Exception {
		String test = "copy-and-modify";
		testCase(test);
	}

	@Test
	public void case006() throws Exception {
		String test = "copy-from-previous-version";
		testCase(test);
	}

	@Test
	public void case007() throws Exception {
		String test = "copy-from-previous-version-and-modify";
		testCase(test);
	}

	@Test
	public void case008() throws Exception {
		String test = "copy-parent-modify-prop";
		testCase(test);
	}

	@Test
	public void case009() throws Exception {
		String test = "del_integ";
		testCase(test);
	}

	@Test
	public void case010() throws Exception {
		String test = "dir_prop_AER";
		testCase(test);
	}

	@Test
	public void case011() throws Exception {
		String test = "dir_prop_change";
		testCase(test);
	}

	@Test
	public void case012() throws Exception {
		String test = "empty_edit";
		testCase(test);
	}

	@Test
	public void case013() throws Exception {
		String test = "exe_prop_set_del";
		testCase(test);
	}

	@Test
	public void case014() throws Exception {
		String test = "exe_prop_set_edit_del";
		testCase(test);
	}

	@Test
	public void case015() throws Exception {
		String test = "file-dir-file";
		testCase(test);
	}

	@Test
	public void case016() throws Exception {
		String test = "file_replace_with_dirty_edit";
		testCase(test);
	}

	@Test
	public void case017() throws Exception {
		String test = "file_with_prop";
		testCase(test);
	}

	@Test
	public void case018() throws Exception {
		String test = "integ_del";
		testCase(test);
	}

	@Test
	public void case019() throws Exception {
		String test = "key_set_and_branch";
		testCase(test);
	}

	@Test
	public void case020() throws Exception {
		String test = "line_endings";
		testCase(test);
	}

	@Test
	public void case021() throws Exception {
		String test = "modified-in-place";
		testCase(test);
	}

	@Test
	public void case022() throws Exception {
		String test = "no-author";
		testCase(test);
	}

	@Test
	public void case023() throws Exception {
		String test = "prop_set_and_branch";
		testCase(test);
	}

	@Test
	public void case024() throws Exception {
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			String test = "reserved_chars_win";
			testCase(test);
		} else {
			String test = "reserved_chars";
			testCase(test);
		}
	}

	@Test
	public void case025() throws Exception {
		String test = "sparce_branch";
		testCase(test);
	}

	@Test
	public void case026() throws Exception {
		String test = "sub_block_merge_replace";
		testCase(test);
	}

	@Test
	public void case027() throws Exception {
		String test = "symlink_file_and_dir";
		testCase(test);
	}

	@Test
	public void case028() throws Exception {
		String test = "tag-empty-trunk";
		testCase(test);
	}

	@Test
	public void case029() throws Exception {
		String test = "tag-trunk-with-dir";
		testCase(test);
	}

	@Test
	public void case030() throws Exception {
		String test = "tag-trunk-with-file";
		testCase(test);
	}

	@Test
	public void case031() throws Exception {
		String test = "tag-trunk-with-file2";
		testCase(test);
	}

	@Test
	public void case032() throws Exception {
		String test = "tag-with-dirty-edit";
		testCase(test);
	}

	@Test
	public void case033() throws Exception {
		String test = "tag-with-modified-file";
		testCase(test);
	}

	@Test
	public void case034() throws Exception {
		String test = "tag-with-prop";
		testCase(test);
	}

	@Test
	public void case035() throws Exception {
		Config.set(CFG.P4_CASE, CaseSensitivity.FIRST);
		Config.set(CFG.P4_C1_MODE, true);
		String test = "url-encoding-bug";
		testCase(test, 5);
	}

	@Test
	public void case036() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		String test = "utf16_files";
		testCase(test);
	}

	@Test
	public void case037() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		// Test non-unicode server behaviour
		String test = "utf8-bom";
		testCase(test);
	}

	@Test
	public void case038() throws Exception {
		Config.set(CFG.P4_TRANSLATE, true);
		Config.set(CFG.P4_UNICODE, false);
		// Test non-unicode server behaviour
		String test = "utf8_files";
		testCase(test);
	}

	@Test
	public void case039() throws Exception {
		String test = "null_path";
		testCase(test);
	}

	@Test
	public void case040() throws Exception {
		String test = "sym_link_and_branch";
		testCase(test);
	}

	@Test
	public void case041() throws Exception {
		String test = "sym_edit";
		testCase(test);
	}

	@Test
	public void case042() throws Exception {
		String test = "pending_del_br_del";
		testCase(test);
	}

	@Test
	public void case043() throws Exception {
		CaseSensitivity mode = (CaseSensitivity) Config.get(CFG.P4_CASE);

		String test;
		if (mode == CaseSensitivity.NONE) {
			// switch to broker on 4445 running without -C1 for Unix test
			Config.set(CFG.P4_C1_MODE, false);
			Config.set(CFG.P4_PORT, "localhost:4445");
			test = "rename_case";
		} else {
			test = "rename_none";
		}
		testCase(test);
	}

	@Test
	public void case044() throws Exception {
		String test = "winansi_non";
		testCase(test);
	}

	@Test
	public void case045() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
		Config.set(CFG.P4_CHARSET, "utf8");
		String test = "winansi_uni";
		testCase(test);
	}

	@Test
	public void case046() throws Exception {
		String test = "reserved_chars_br";
		testCase(test);
	}

	@Test
	public void case047() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
		Config.set(CFG.P4_CHARSET, "utf8");
		String test = "utf16_files";
		testCase(test);
	}

	@Test
	public void case048() throws Exception {
		String test = "large_files";
		testCase(test);
	}

	@Test
	public void case049() throws Exception {
		String test = "empty_add";
		testCase(test);
	}

	@Test
	public void case050() throws Exception {
		String test = "empty_prop";
		testCase(test);
	}

	@Test
	public void case051() throws Exception {
		String test;
		Form form = (Form) Config.get(CFG.P4_NORMALISATION);
		if (form.equals(Form.NFD))
			test = "utf8_path_nfd";
		else
			test = "utf8_path_nfc";
		testCase(test);
	}

	@Test
	public void case052() throws Exception {
		String test = "job055336";
		testCase(test);
	}

	@Test
	public void case053() throws Exception {
		String test = "unicode_props";
		testCase(test);
	}

	@Test
	public void case054() throws Exception {
		Config.set(CFG.P4_DEPOT_SUB, "sub/");

		// Update translation map as P4_DEPOT_SUB has changed
		PathMapTranslator.setDefault();

		String test = "subpath_del_br_del";
		testCase(test);
	}

	@Test
	public void case055() throws Exception {
		Config.set(CFG.P4_LINEEND, true);
		String test = "line_feed";
		testCase(test);
	}

	@Test
	public void case056() throws Exception {
		Config.set(CFG.P4_LINEEND, true);
		int ver = getServerVersion();
		String test = null;
		if (ver > 20131) {
			test = "dir_rollback";
		} else if (ver > 20102 && ver <= 20131) {
			test = "dir_rollback_13.1";
		} else {
			test = "dir_rollback_10.2";
		}
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case057() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		String test = "utf32_files_non";
		testCase(test);
	}

	@Test
	public void case058() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
		Config.set(CFG.P4_CHARSET, "utf32be-bom");
		String test = "utf32_files_uni";
		testCase(test);
	}

	@Test
	public void case059() throws Exception {
		Config.set(CFG.P4_PASSWD, "Password");
		String test = "usernames";
		String seed = dumpPath + test + "/";
		testCase(test, seed);
	}

	@Test
	public void case060() throws Exception {
		String test = "baseless_copy";
		testCase(test);
	}

	@Test
	public void case061() throws Exception {
		String test = "bigTextFile";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case062() throws Exception {
		int ver = getServerVersion();
		if (ver <= 20102) {
			String test = "empty_rollback";
			testCase(test);
		}
	}

	@Test
	public void case063() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
		Config.set(CFG.P4_CHARSET, "utf8");
		String test = "win1252";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case064() throws Exception {
		String test = "bad_encoding";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case065() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "merge_info";
		testCase(test);
	}

	@Test
	public void case066() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "n_source_merge";
		testCase(test);
	}

	@Test
	public void case067() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "all_integ";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "all_integ_13.1";
			testCase(test);
		} else {
			String test = "all_integ_10.2";
			testCase(test);
		}
	}

	@Test
	public void case068() throws Exception {
		String test = "hudge_file";
		testCase(test);
	}

	@Test
	public void case069() throws Exception {
		String test = "win1252_prop";
		testCase(test);
	}

	@Test
	public void case070() throws Exception {
		String test = "utf8-nobom";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case071() throws Exception {
		int ver = getServerVersion();
		if (ver > 20102) {
			String test = "sym_swap";
			testCase(test);
		} else {
			// XXX skip as this hangs p4-java
		}
	}

	@Test
	public void case072() throws Exception {
		String test = "empty_rev";
		testCase(test);
	}

	@Test
	public void case073() throws Exception {
		ChangeParser.resetLastDate();
		String test = "subfile_del_br";
		testCase(test);
	}

	@Test
	public void case074() throws Exception {
		Config.set(CFG.SVN_PROP_ENABLED, true);
		String test = "null_dir";
		testCase(test);
	}

	@Test
	public void case075() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "merge_levels";
		testCase(test);
	}

	@Test
	public void case076() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "merge_cherry";
		testCase(test);
	}

	@Test
	public void case077() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "merge_span";
		testCase(test);
	}

	@Test
	public void case078() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "merge_multi";
		testCase(test);
	}

	@Test
	public void case079() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "merge_copy";
		testCase(test);
	}

	@Test
	public void case080() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		Config.set(CFG.P4_START, 6L);
		String test = "merge_offset";
		String seed = dumpPath + test + "/";
		testCase(test, seed);
	}

	@Test
	public void case081() throws Exception {
		Config.set(CFG.SVN_PROP_ENABLED, true);
		String test = "del-add-w-dir";
		testCase(test);
	}

	@Test
	public void case082() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "copy_self";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "copy_self_13.1";
			testCase(test);
		} else {
			String test = "copy_self_10.2";
			testCase(test);
		}
	}

	@Test
	public void case083() throws Exception {
		String test = "del-add-not-same-rev";
		testCase(test);
	}

	@Test
	public void case084() throws Exception {
		String test = "replace";
		testCase(test);
	}

	@Test
	public void case085() throws Exception {
		String test = "replace-with-cp-and-contents";
		testCase(test);
	}

	@Test
	public void case086() throws Exception {
		String test = "symlink-target";
		testCase(test);
	}

	@Test
	public void case087() throws Exception {
		String test = "symlink-target-same-rev";
		testCase(test);
	}

	@Test
	public void case088() throws Exception {
		String test = "symlink-relative-target";
		testCase(test);
	}

	@Test
	public void case089() throws Exception {
		Config.set(CFG.P4_PASSWD, "Password");
		Config.set(CFG.P4_DEPOT_PATH, "depot");

		// Update translation map as P4_DEPOT_PATH has changed
		PathMapTranslator.setDefault();

		String test = "security";
		String seed = dumpPath + test + "/";
		testCase(test, seed);
	}

	@Test
	public void case090() throws Exception {
		String test = "symlink-to-file";
		testCase(test);
	}

	@Test
	public void case091() throws Exception {
		String test = "russian";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case092() throws Exception {
		String test = "symlink2file";
		testCase(test);
	}

	@Test
	public void case093() throws Exception {
		String test = "replace-file-with-dir";
		testCase(test);
	}

	@Test
	public void case094() throws Exception {
		String test = "trailing-cr";
		testCase(test);
	}

	@Test
	public void case095() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "file2symlink-revert";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "file2symlink-revert_13.1";
			testCase(test);
		} else {
			String test = "file2symlink-revert_10.2";
			testCase(test);
		}
	}

	@Test
	public void case096() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "file2symlink-revert-replace";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "file2symlink-revert-replace_13.1";
			testCase(test);
		} else {
			String test = "file2symlink-revert-replace_10.2";
			testCase(test);
		}
	}

	@Test
	public void case097() throws Exception {
		String test = "replace-file-with-dir-with-replace-action";
		testCase(test);
	}

	@Test
	public void case098() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "copy-self";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "copy-self_13.1";
			testCase(test);
		} else {
			String test = "copy-self_10.2";
			testCase(test);
		}
	}

	@Test
	public void case099() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "copy-self-dir";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "copy-self-dir_13.1";
			testCase(test);
		} else {
			String test = "copy-self-dir_10.2";
			testCase(test);
		}
	}

	@Test
	public void case100() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "file2symlink-revert-edit";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "file2symlink-revert-edit_13.1";
			testCase(test);
		} else {
			String test = "file2symlink-revert-edit_10.2";
			testCase(test);
		}
	}

	@Test
	public void case101() throws Exception {
		String test = "copy-replace-file";
		testCase(test);
	}

	@Test
	public void case102() throws Exception {
		int ver = getServerVersion();
		if (ver > 20131) {
			String test = "symlink-to-file-copy";
			testCase(test);
		} else if (ver > 20102 && ver <= 20131) {
			String test = "symlink-to-file-copy_13.1";
			testCase(test);
		} else {
			String test = "symlink-to-file-copy_10.2";
			testCase(test);
		}
	}

	@Test
	public void case103() throws Exception {
		Config.set(CFG.P4_LINEEND, false);
		String test = "extra_line_endings";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case104() throws Exception {
		Config.set(CFG.SVN_PROP_TYPE, ContentType.windows_1251);
		String test = "prop_winansi";
		testCase(test);
	}

	@Test
	public void case105() throws Exception {
		String test = "null_file";
		testCase(test);
	}

	@Test
	public void case106() throws Exception {
		Config.set(CFG.TYPE_MAP, "types.map");
		String test = "sub_conflict";
		testCase(test);
	}

	@Test
	public void case107() throws Exception {
		TypeMap.add("cpp", "text");
		String test = "fixdocs";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case108() throws Exception {
		String test = "copy-with-edits";
		testCase(test);
	}

	@Test
	public void case109() throws Exception {
		TypeMap.add("pl", "text");
		String test = "typemap_win_lineend";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case110() throws Exception {
		String test = "utf16_del";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case111() throws Exception {
		String test = "delete_twice";
		testCase(test);
	}

	@Test
	public void case112() throws Exception {
		String test = "replace_dir_with_copy";
		testCase(test);
	}

	@Test
	public void case113() throws Exception {
		String test = "replace_over_del";
		testCase(test);
	}

	@Test
	public void case114() throws Exception {
		String test = "symlink-exec";
		testCase(test);
	}

	@Test
	public void case115() throws Exception {
		String test = "symlink-missing";
		testCase(test, 1);
	}

	@Test
	public void case116() throws Exception {
		Config.set(CFG.P4_C1_MODE, true);
		Config.set(CFG.P4_CASE, CaseSensitivity.FIRST);
		String test = "rename_edit";
		testCase(test);
	}

	@Test
	public void case117() throws Exception {
		String test = "exe_prop_branch";
		testCase(test);
	}

	@Test
	public void case118() throws Exception {
		String test = "labels_basic";
		Config.set(CFG.SVN_LABELS, true);
		Config.set(CFG.SVN_LABEL_DEPTH, 2);
		Config.set(CFG.SVN_LABEL_FORMAT, "label:{depth}");

		String path = dumpPath + test + "/exclude.map";
		Config.set(CFG.EXCLUDE_MAP, path);

		testCase(test);
	}

	@Test
	public void case119() throws Exception {
		String test = "labels_branch-branch";
		Config.set(CFG.SVN_LABELS, true);
		Config.set(CFG.SVN_LABEL_DEPTH, 2);
		Config.set(CFG.SVN_LABEL_FORMAT, "label:{depth}");

		String path = dumpPath + test + "/exclude.map";
		Config.set(CFG.EXCLUDE_MAP, path);

		testCase(test, 0);
	}

	@Test
	public void case120() throws Exception {
		String test = "job115";
		testCase(test);
	}

	@Test
	public void case121() throws Exception {
		String test = "labels_delete";
		Config.set(CFG.SVN_LABELS, true);
		Config.set(CFG.SVN_LABEL_DEPTH, 2);
		Config.set(CFG.SVN_LABEL_FORMAT, "label:{depth}");

		String path = dumpPath + test + "/exclude.map";
		Config.set(CFG.EXCLUDE_MAP, path);

		// TODO testCase(test);
	}

	@Test
	public void case122() throws Exception {
		String test = "labels_tag_brFile";
		Config.set(CFG.SVN_MERGEINFO, true);
		Config.set(CFG.SVN_LABELS, true);
		Config.set(CFG.SVN_LABEL_DEPTH, 2);
		Config.set(CFG.SVN_LABEL_FORMAT, "label:{depth}");

		String path = dumpPath + test + "/exclude.map";
		Config.set(CFG.EXCLUDE_MAP, path);

		// TODO testCase(test);
	}

	@Test
	public void case123() throws Exception {
		Config.set(CFG.SVN_PROP_ENABLED, false);
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "branch_multi";
		testCase(test);
	}

	@Test
	public void case124() throws Exception {
		Config.set(CFG.P4_TRANSLATE, true);
		Config.set(CFG.P4_UNICODE, false);

		String test = "utf8_files_tr";
		testCase(test);
	}

	@Test
	public void case125() throws Exception {
		String test = "dots_path";
		testCase(test);
	}

	@Test
	public void case126() throws Exception {
		Config.set(CFG.SVN_MERGEINFO, true);
		String test = "copy-kdate";
		testCase(test);
	}

	/**
	 * Environment test
	 * 
	 * @throws Exception
	 */
	private static void checkEnvironment() throws Exception {
		// Test binary exists and report version
		int test = SystemCaller.exec(p4d + " -V", true, false);
		if (test != 0) {
			logger.info("Cannot find " + p4d + ", please check PATH!");
			logger.info("PATH = " + System.getenv("PATH"));
			System.exit(test);
		}
	}

	/**
	 * Run a "p4d -V" to extract server version, combine major and minor numbers
	 * by removing the "." and return as an int.
	 * 
	 * @return
	 * @throws Exception
	 */
	private static int getServerVersion() throws Exception {
		int version = 0;
		Process process = Runtime.getRuntime().exec(p4d + " -V");

		InputStreamReader stdout;
		stdout = new InputStreamReader(process.getInputStream());
		BufferedReader bout = new BufferedReader(stdout);

		String line;
		while ((line = bout.readLine()) != null) {
			if (line.startsWith("Rev. P4D")) {
				Pattern p = Pattern.compile("\\d{4}\\.\\d{1}");
				Matcher m = p.matcher(line);
				while (m.find()) {
					String found = m.group();
					found = found.replace(".", ""); // strip "."
					version = Integer.parseInt(found);
				}
			}
		}

		bout.close();
		return version;
	}

	/**
	 * Filter the journal file for selected tables then sort
	 * 
	 * @param p4Root
	 * @throws Exception
	 */
	private void filterJournal(String p4Root) throws Exception {
		String[] tables = { "@db.rev@", "@db.integed@", "@db.change@",
				"@db.desc@", "@db.label@" };

		// Filter results (grep and sort)
		String base = "grep '^@pv@' " + p4root + "checkpoint.1 | ";
		String file = p4Root + journalFile;
		for (String t : tables) {
			String grep = base + "grep '" + t + "' >> " + file;
			SystemCaller.exec(grep, true, false);
		}
	}

	/**
	 * Cleans the perforce server (db files and archives)
	 * 
	 * @throws Exception
	 */
	private void cleanPerforce() throws Exception {
		// Remove old server and workspace
		String rm = "rm" + " -rf " + p4root + " " + p4ws;
		SystemCaller.exec(rm, true, false);

		// Make p4 root
		new File(p4root).mkdir();
	}

	private void seedPerforce(String seed) throws Exception {
		String meta = cwd + seed + seedFile;
		String lbr = cwd + seed + seedLbr;
		String map = cwd + seed + seedLbr + "changeMap.txt";
		String ckp = p4d + " -C1 -r " + p4root + " -jr " + meta;
		SystemCaller.exec(ckp, true, false);
		String upd = p4d + " -C1 -r " + p4root + " -xu";
		SystemCaller.exec(upd, true, false);

		String cp = "cp -rfv " + lbr + " " + p4root + "import";
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
		testCase(dumpCase, null, 0);
	}

	private void testCase(String dumpCase, int warnings) {
		testCase(dumpCase, null, warnings);
	}

	private void testCase(String dumpCase, String seed) {
		testCase(dumpCase, seed, 0);
	}

	private void testCase(String dumpCase, String seed, int warnings) {
		try {
			// Select dump file for test case
			logger.info("testcase: " + dumpCase);
			String dumpFileName = dumpPath + dumpCase + "/" + dumpFile;
			Config.set(CFG.SVN_DUMPFILE, dumpFileName);

			// Paths and configurations
			String base = basePath + dumpCase + "/";

			// clean Perforce server
			cleanPerforce();

			// seed Perforce if checkpoint provided
			if (seed != null) {
				seedPerforce(seed);
			}

			// Switch to unicode in enabled
			if ((Boolean) Config.get(CFG.P4_UNICODE)) {
				String p4uni = p4d + " -r " + p4root + " -xi ";
				SystemCaller.exec(p4uni, true, false);
			}

			// Run test case
			SvnProcessChange process = new SvnProcessChange();
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
			Assert.assertEquals("Warnings:", warnings, warn);

		} catch (Throwable e) {
			e.printStackTrace();
			Assert.fail("Exception");
		}
	}
}
