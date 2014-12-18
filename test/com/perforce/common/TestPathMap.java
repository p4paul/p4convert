package com.perforce.common;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.perforce.common.node.PathMapEntry;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class TestPathMap {

	@Before
	public void before() {
		PathMapTranslator.clear();
	}

	@Test
	public void testBasicMap() {
		PathMapEntry entry = new PathMapEntry("(main)/projFoo/(bar/.*)",
				"//depot/proj.FooBar/{1}/{2}");
		PathMapTranslator.add(entry);

		String path = "main/projFoo/bar/src/baz.txt";
		String base = "//depot/proj.FooBar/main/bar/src/baz.txt";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}

	@Test
	public void testMultiMap() {
		PathMapEntry entry1 = new PathMapEntry("(rel1)/projFoo/(bar/.*)",
				"//depot/proj.FooBar/{1}-{2}");
		PathMapTranslator.add(entry1);

		PathMapEntry entry2 = new PathMapEntry("(.*)/projFoo/(bar/.*)",
				"//depot/proj.FooBar/{1}/{2}");
		PathMapTranslator.add(entry2);

		String path = "main/projFoo/bar/src/baz.txt";
		String base = "//depot/proj.FooBar/main/bar/src/baz.txt";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}

	@Test
	public void testPartMap() {
		PathMapEntry entry = new PathMapEntry("RELEASE_(.*)/projFoo/(bar/.*)",
				"//depot/R{1}/{2}");
		PathMapTranslator.add(entry);

		String path = "RELEASE_1.0.2/projFoo/bar/src/baz.txt";
		String base = "//depot/R1.0.2/bar/src/baz.txt";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}
	
	@Test
	public void testRegexCharsInPath() {
		PathMapEntry entry = new PathMapEntry("RELEASE_(.*)/projFoo/(bar/.*)",
				"//depot/R{1}/{2}");
		PathMapTranslator.add(entry);

		String path = "RELEASE_1.*.2/projFoo/bar/(src)/baz.txt";
		String base = "//depot/R1.*.2/bar/(src)/baz.txt";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}

	@Test
	public void testDefautMap() throws ConfigException {
		Config.setDefault();
		Config.set(CFG.P4_DEPOT_SUB, "sub/");

		PathMapTranslator.setDefault();

		String path = "main/src/baz.txt";
		String base = "//import/sub/main/src/baz.txt";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}
	
	@Test
	public void testNullPath() throws ConfigException {
		Config.setDefault();
		Config.set(CFG.P4_DEPOT_SUB, "sub/");

		PathMapTranslator.setDefault();

		String path = null;
		String base = "//import/sub/";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}
	
	@Test
	public void testRegexPath() throws ConfigException {
		Config.setDefault();

		PathMapTranslator.setDefault();

		String path = "trunk/{1}/foo.txt";
		String base = "//import/trunk/{1}/foo.txt";
		Assert.assertEquals(base, PathMapTranslator.translate(path));
	}

}
