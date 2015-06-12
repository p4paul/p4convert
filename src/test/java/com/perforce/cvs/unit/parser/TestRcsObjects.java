package com.perforce.cvs.unit.parser;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.cvs.parser.rcstypes.RcsObjectString;
import com.perforce.cvs.parser.rcstypes.RcsObjectTag;

public class TestRcsObjects {

	@Test
	public void case001() throws Exception {
		String str = "foo:1.2";
		RcsObjectTag id = new RcsObjectTag(str);
		String ids = id.toString();
		Assert.assertTrue(ids.contentEquals(str));
	}

	@Test
	public void case002() throws Exception {
		String str = "@test@";
		RcsObjectString msg = new RcsObjectString(str);
		Assert.assertTrue(msg.toString().contentEquals(str));
	}

	@Test
	public void case003() throws Exception {
		String str = "@@";
		RcsObjectString msg = new RcsObjectString(str);
		Assert.assertTrue(msg.getString().contentEquals(""));
	}

	@Test
	public void case004() throws Exception {
		String str = "@foo@@bar@";
		RcsObjectString msg = new RcsObjectString(str);
		Assert.assertTrue(msg.getString().contentEquals("foo@bar"));
	}

	@Test
	public void case005() throws Exception {
		RcsObjectNum a = new RcsObjectNum("1.7");
		List<Integer> l = new ArrayList<Integer>();
		l.add(1);
		l.add(7);
		RcsObjectNum b = new RcsObjectNum(l);
		Assert.assertTrue(a.equals(b));
	}
}
