package com.perforce.svn.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.node.NodeHelper;
import com.perforce.svn.query.QueryInterface;

public class MergeInfo {

	private Logger logger = LoggerFactory.getLogger(MergeInfo.class);

	private String path;
	private Collection<MergePoint> mergePoints = new ArrayList<MergePoint>();

	public MergeInfo(String path, String info) {
		if (info != null && !info.isEmpty()) {
			this.path = path;
			if (logger.isTraceEnabled()) {
				logger.trace("MergeInfo: " + path);
				logger.trace(info);
			}
			parse(info);
		}
	}

	public MergeInfo(String path) {
		this.path = path;
	}

	private void parse(String info) {
		// (\\r?\\n)|,(?=[^,-]{0,1000}:)
		String[] mergeList = info.split("\\r?\\n");
		for (String m : mergeList) {

			String[] subList = m.split(",(?=[^,]+:)");
			for (String s : subList) {
				if (s.contains(":")) {
					parseMerge(s);
				}
			}
		}
	}

	private void parseMerge(String m) {
		// parse path from string
		String path = m.substring(0, m.indexOf(":"));
		if (path.startsWith("/")) {
			path = path.substring(1, path.length());
		}

		// Fetch merge revision ranges. Ranges follow the form:
		// <path>:<merge>,<ignore>,<ignore>...
		String remainder = m.substring(m.indexOf(":") + 1);
		String[] rangeStr = remainder.split(",");
		ArrayList<String> ranges;
		ranges = new ArrayList<String>(Arrays.asList(rangeStr));

		if (!ranges.isEmpty()) {
			// Find all merge ranges.
			for (String r : ranges) {
				MergeRange mergeRange = parseRange(r);

				// Parse issue, ignore all remaining ranges
				if (mergeRange == null) {
					break;
				}

				MergePoint mergePoint = new MergePoint(path, mergeRange);
				mergePoints.add(mergePoint);

				if (logger.isDebugEnabled()) {
					logger.debug("merge range: " + mergeRange.toString());
				}
			}
		}
	}

	/**
	 * Parse the range part of the mergeinfo string. Could contain a range
	 * separated by a '-' or just a single point.
	 * 
	 * Returns a range object;
	 * 
	 * @param info
	 * @return
	 */
	private MergeRange parseRange(String info) {
		MergeRange range = null;
		// test for non-inheritable mergeinfo denoted by *
		if (info.contains("*")) {
			info = info.substring(0, info.indexOf("*"));
		}

		// calculate the widest range
		try {
			if (info.contains("-")) {
				String[] arg = info.split("-");
				long start = Long.parseLong(arg[0]);
				long end = Long.parseLong(arg[1]);
				range = new MergeRange(start, end);
			} else {
				long point = Long.parseLong(info);
				range = new MergeRange(point, point);
			}
		} catch (NumberFormatException e) {
			logger.warn("Unable to read MergeInfo range: " + info);
		}
		return range;
	}

	public ArrayList<MergeSource> getMergeSources(String nodePath, QueryInterface query) throws Exception {
		ArrayList<MergeSource> fromList = new ArrayList<MergeSource>();

		for (MergePoint m : mergePoints) {
			String fromDir = m.getMergePath();
			if (fromDir != null) {
				String remap = NodeHelper.remap(path, fromDir, nodePath);
				if (remap != null) {
					MergeSource src = new MergeSource(remap, m.getRevStart(), m.getRevEnd());
					if (src.fetchNode(query)) {
						fromList.add(src);
					}
				}
			}
		}

		return fromList;
	}

	public MergeInfo removeLast(MergeInfo base) {
		if (base != null) {
			MergeInfo result = new MergeInfo(path);

			for (MergePoint m : mergePoints) {
				if (!base.getMergePoints().contains(m)) {
					result.addMergePoint(m);
				}
			}
			return result;
		}
		return this;
	}

	public boolean isEmpty() {
		if (mergePoints != null && !mergePoints.isEmpty())
			return false;
		return true;
	}

	public String getPath() {
		return path;
	}

	public Collection<MergePoint> getMergePoints() {
		return mergePoints;
	}

	public void addMergePoint(MergePoint m) {
		mergePoints.add(m);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (MergePoint m : mergePoints) {
			sb.append(m.toString() + ", ");
		}
		return sb.toString();
	}
}
