package com.perforce.svndump.prescan;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.Main;
import com.perforce.common.ExitCode;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ScmType;

public class TestUsage {
	private final static String dumpPath = "test/com/perforce/integration/dumps/";
	private final static String dumpFile = "repo.dump";

	@Test
	public void test_MainFlagUsage() throws Exception {
		File file = new File("default.cfg");
		file.delete();

		// generate default configuration file
		String args[] = { "--type=SVN", "--default" };
		ExitCode code = Main.processArgs(args);

		boolean test = (!file.isDirectory() && file.canRead());
		Assert.assertEquals(true, test);
		Assert.assertEquals(ExitCode.OK, code);
	}

	@Test
	public void test_MainFlagInfo() throws Exception {
		// pass --info to scan SVN dump file
		String testcase = dumpPath + "add-del-br-del-br/" + dumpFile;
		String args[] = { "--type=SVN", "--info", "--repo=" + testcase };
		ExitCode code = Main.processArgs(args);

		Assert.assertEquals(ExitCode.OK, code);
	}

	@Test
	public void test_MainFlagRun() throws Exception {

		File config = new File("test.cfg");
		Config.setDefault();
		Config.set(CFG.SCM_TYPE, ScmType.SVN);
		Config.set(CFG.P4_MODE, "CONVERT");
		Config.set(CFG.P4_ROOT, "./p4_root/");
		Config.set(CFG.SVN_DUMPFILE, dumpPath + "add-del-br-del-br/" + dumpFile);

		Config.store(config.getName(), ScmType.SVN);

		// pass config file to run conversion
		String args[] = { "--config=" + config.getName() };
		ExitCode code = Main.processArgs(args);

		// clean up test file
		config.delete();
		Assert.assertEquals(ExitCode.OK, code);
	}

	@Test
	public void test_MainFlagRun_EXCEPTION() throws Exception {

		File config = new File("test.cfg");
		Config.setDefault();
		Config.set(CFG.SCM_TYPE, ScmType.SVN);
		Config.set(CFG.P4_MODE, "IMPORT");
		Config.set(CFG.P4_PORT, "");
		Config.set(CFG.P4_ROOT, "");
		Config.set(CFG.P4_CLIENT_ROOT, "");
		Config.set(CFG.SVN_DUMPFILE, dumpPath + "add-del-br-del-br/" + dumpFile);

		Config.store(config.getName(), ScmType.SVN);

		// pass config file to run conversion
		String args[] = { "--config=" + config.getName() };
		ExitCode code = Main.processArgs(args);

		// clean up test file
		config.delete();
		Assert.assertEquals(ExitCode.EXCEPTION, code);
	}
}
