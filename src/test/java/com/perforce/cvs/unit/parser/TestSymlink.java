package com.perforce.cvs.unit.parser;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.perforce.common.asset.AssetWriter;
import com.perforce.integration.SystemCaller;

public class TestSymlink {

	@Before
	@After
	public void cleanup() throws Exception {
		String tmp = "rm" + " -rf tmp";
		SystemCaller.exec(tmp, true, false);
	}

	@Test
	public void createDirOverLink() throws Exception {
		File base = new File("tmp");
		assertFalse(base.exists());
		base.mkdir();
		assertTrue(base.exists());

		File link = new File(base, "link");
		File target = new File(base, "null");
		Files.createSymbolicLink(link.toPath(), target.toPath());
		assertFalse(link.exists());

		String path = "tmp/link/dir1/dir2/";
		File directory = new File(path);
		assertFalse(directory.exists());

		AssetWriter writer = new AssetWriter(path + "foo.c");
		writer.open();
		assertTrue(directory.exists());
	}

	@Test
	public void createDirOverFile() throws Exception {
		File base = new File("tmp");
		assertFalse(base.exists());
		base.mkdir();
		assertTrue(base.exists());

		File file = new File(base, "file");
		file.createNewFile();
		assertTrue(file.exists());

		String path = "tmp/file/dir1/dir2/";
		File directory = new File(path);
		assertFalse(directory.exists());

		AssetWriter writer = new AssetWriter(path + "foo.c");
		writer.open();
		assertTrue(directory.exists());
	}
}
