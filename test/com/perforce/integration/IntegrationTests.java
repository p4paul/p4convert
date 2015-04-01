package com.perforce.integration;

import static org.junit.Assert.fail;

import java.io.File;
import java.text.Normalizer.Form;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.label.LabelHistory;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.config.Normaliser;
import com.perforce.config.ScmType;
import com.perforce.svn.change.ChangeParser;
import com.perforce.svn.process.SvnProcessChange;

public class IntegrationTests {

	private static Logger logger = LoggerFactory
			.getLogger(IntegrationTests.class);

	private final String dumpPath = "test/com/perforce/integration/dumps/";
	private final String dumpFile = "repo.dump";
	private final String journalFile = "jnl.0";

	@Before
	public void before() {
		// Setup default test configuration
		try {
			Config.setDefault();
			Config.set(CFG.P4_MODE, "CONVERT");
			Config.set(CFG.SCM_TYPE, ScmType.SVN);
			Config.set(CFG.P4_USER, "svn-user");
			Config.set(CFG.P4_CLIENT, "svn-client");
			Config.set(CFG.P4_ROOT, "./p4_root/");
			Config.set(CFG.TEST, true);

			Config.set(CFG.VERSION, "alpha/TestMode");
			Config.set(CFG.SVN_PROP_NAME, ".svn.properties");
			Config.set(CFG.SVN_PROP_ENCODE, "ini");
			Config.set(CFG.SVN_PROP_ENABLED, true);
			Config.set(CFG.SVN_END, 0L);
			Config.set(CFG.P4_CLIENT_ROOT, "/ws");

			Config.set(CFG.EXCLUDE_MAP, "test_exclude.map");
			Config.set(CFG.INCLUDE_MAP, "test_include.map");
			Config.set(CFG.ISSUE_MAP, "test_issue.map");
			Config.set(CFG.USER_MAP, "test_users.map");
			Config.set(CFG.TYPE_MAP, "test_types.map");
			Config.set(CFG.PATH_MAP, "test_path.map");

			// Set path translation map defaults
			PathMapTranslator.setDefault();

			// Clear LabelHistory Map
			LabelHistory.clear();
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void case001() throws Exception {
		String test = "add-del-br-del-br";
		testCase(test);
	}

	@Test
	public void case002() throws Exception {
		String test = "bin_tags";
		testCase(test);
	}

	@Test
	public void case003() throws Exception {
		TypeMap.add("gif", "binary");
		String test = "binary_file";
		testCase(test);
	}

	@Test
	public void case004() throws Exception {
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
		Config.set(CFG.P4_C1_MODE, true);
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

		String test = "utf8-bom";
		testCase(test);
	}

	@Test
	public void case038() throws Exception {
		Config.set(CFG.P4_TRANSLATE, false);
		Config.set(CFG.P4_UNICODE, false);

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
			test = "rename_case";
		} else {
			test = "rename_none";
		}
		testCase(test);
	}

	@Test
	public void case044() throws Exception {
		Config.set(CFG.P4_UNICODE, false);
		String test = "winansi_non";
		testCase(test);
	}

	@Test
	public void case045() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
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
		String test = "utf16_files_uni";
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
		else {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				test = "utf8_path_nfc_win";
			} else {
				test = "utf8_path_nfc";
			}
		}
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
		// tested using: 'p4 -Cutf32be-bom -Qutf8' as file is BE
		Config.set(CFG.P4_UNICODE, true);
		String test = "utf32_files_uni";
		testCase(test);
	}

	@Test
	public void case059() throws Exception {
		// Force encoding to NFC (only interested in metadata for this test)
		Config.set(CFG.P4_NORMALISATION, Normaliser.NFC);
		String test = "usernames";
		testCase(test);
	}

	@Test
	public void case060() throws Exception {
		Config.set(CFG.P4_DOWNGRADE, true);
		String test = "baseless_copy";
		testCase(test);
	}

	@Test
	public void case061() throws Exception {
		Config.set(CFG.P4_C1_MODE, true);
		String test = "bigTextFile";
		// PROPRIAROTY DATA -- not released
		// testCase(test);
	}

	@Test
	public void case062() throws Exception {
		String test = "empty_rollback";
		testCase(test);
	}

	@Test
	public void case063() throws Exception {
		Config.set(CFG.P4_UNICODE, true);
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
		String test = "all_integ";
		testCase(test);
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
		String test = "sym_swap";
		testCase(test);
	}

	@Test
	public void case072() throws Exception {
		ChangeParser.resetLastDate();
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
		Config.set(CFG.P4_OFFSET, 1000L);
		String test = "merge_offset";
		testCase(test);
	}

	@Test
	public void case081() throws Exception {
		Config.set(CFG.SVN_PROP_ENABLED, true);
		String test = "del-add-w-dir";
		testCase(test);
	}

	@Test
	public void case082() throws Exception {
		String test = "copy_self";
		testCase(test);
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
		Config.set(CFG.P4_OFFSET, 1000L);
		Config.set(CFG.P4_DEPOT_PATH, "depot");

		// Update translation map as P4_DEPOT_PATH has changed
		PathMapTranslator.setDefault();

		// This test largely ineffective in Convert mode
		String test = "security";
		testCase(test);
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
		String test = "file2symlink-revert";
		testCase(test);
	}

	@Test
	public void case096() throws Exception {
		String test = "file2symlink-revert-replace";
		testCase(test);
	}

	@Test
	public void case097() throws Exception {
		String test = "replace-file-with-dir-with-replace-action";
		testCase(test);
	}

	@Test
	public void case098() throws Exception {
		String test = "copy-self";
		testCase(test);
	}

	@Test
	public void case099() throws Exception {
		String test = "copy-self-dir";
		testCase(test);
	}

	@Test
	public void case100() throws Exception {
		String test = "file2symlink-revert-edit";
		testCase(test);
	}

	@Test
	public void case101() throws Exception {
		String test = "copy-replace-file";
		testCase(test);
	}

	@Test
	public void case102() throws Exception {
		String test = "symlink-to-file-copy";
		testCase(test);
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
		Config.set(CFG.P4_NORMALISATION, Normaliser.NFD);
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

		testCase(test);
	}

	@Test
	public void case120() throws Exception {
		Config.set(CFG.P4_C1_MODE, true);
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

	private void testCase(String dumpCase) {
		testCase(dumpCase, 0);
	}

	private void testCase(String dumpCase, int warnings) {
		try {
			logger.info("testcase: " + dumpCase);

			Config.set(CFG.SVN_DUMPFILE, dumpPath + dumpCase + "/" + dumpFile);

			String depotName = (String) Config.get(CFG.P4_DEPOT_PATH);
			String basePath = dumpPath + dumpCase + "/";
			String p4Root = (String) Config.get(CFG.P4_ROOT);

			// Remove old server
			String rm = "rm" + " -rf " + p4Root;
			SystemCaller.exec(rm, true, false);

			// Run test case
			SvnProcessChange process = new SvnProcessChange();
			process.runSingle();

			// Diff archive files
			String arcTest = p4Root + depotName;
			String arcBase = basePath + depotName;
			new File(arcTest).mkdir(); // some tests have no archive files
			new File(arcBase).mkdir();

			String cmd = "diff -r " + arcTest + " " + arcBase;
			int arch = SystemCaller.exec(cmd, true, false);
			Assert.assertEquals("Archive:", 0, arch);

			// Diff metadata
			String jnlTest = "p4_root/" + journalFile;
			String jnlBase = dumpPath + dumpCase + "/" + journalFile;
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
			fail("Exception");
		}
	}
}
