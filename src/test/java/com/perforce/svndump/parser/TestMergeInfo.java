package com.perforce.svndump.parser;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.perforce.svn.process.MergeInfo;
import com.perforce.svn.process.MergePoint;

public class TestMergeInfo {

	@Test
	public void case001() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("proj/foo:123,124,125-129\n");
		sb.append("proj/bar:123,124,125-129,pro1-j/baz:345-347,349\n");
		MergeInfo merge = new MergeInfo("trunk/foo", sb.toString());
		Collection<MergePoint> points = merge.getMergePoints();

		// path without '-' is parsed (8)
		Assert.assertEquals(8, points.size());
	}
}
