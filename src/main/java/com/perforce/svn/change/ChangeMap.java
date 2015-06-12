package com.perforce.svn.change;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeMap {

	private static Logger logger = LoggerFactory.getLogger(ChangeMap.class);

	private static Boolean loaded = false;
	private static TreeMap<Integer, Long> mapOfRevisions = new TreeMap<Integer, Long>();
	private static TreeMap<Long, Integer> mapOfChanges = new TreeMap<Long, Integer>();

	public static void add(long svnRevision, long p4Change) {
		mapOfRevisions.put((int) svnRevision, p4Change);
		mapOfChanges.put(p4Change, (int) svnRevision);
	}

	public static long getChange(int revision) {
		if (mapOfRevisions.containsKey(revision)) {
			return mapOfRevisions.get(revision);
		}
		Integer change = mapOfRevisions.floorKey(revision);
		if (change == null) {
			return 0;
		}
		return change;
	}

	public static int getRevision(long change) {
		if (mapOfChanges.containsKey(change)) {
			return mapOfChanges.get(change);
		}
		Long rev = mapOfChanges.floorKey(change);
		if (rev == null) {
			return 0;
		}
		// CAUTION if long is too big
		return (int) (rev + 0);
	}

	public static void store(String file) throws Exception {

		// do not clobber existing file, unless it has been loaded
		if (!loaded)
			return;

		FileOutputStream fs = new FileOutputStream(file, false);
		BufferedOutputStream bs = new BufferedOutputStream(fs);
		DataOutputStream out = new DataOutputStream(bs);

		out.writeBytes("# <Change>, <SVN revsion>\n");
		for (long change : mapOfChanges.keySet()) {
			String map = change + ", " + getRevision(change) + "\n";
			out.writeBytes(map);
		}
		out.flush();
		out.close();
	}

	public static void load(String file) throws Exception {

		RandomAccessFile rf;

		loaded = true;
		try {
			rf = new RandomAccessFile(file, "r");
			String line = null;

			if (logger.isInfoEnabled()) {
				logger.info("loading ChangeMap: \t" + file);
			}

			while ((line = rf.readLine()) != null) {
				// ignore comments starting with '#' and add lines to table
				if (!line.startsWith("#")) {
					try {
						String[] tokens = line.split(",");
						long p4Change = Long.valueOf(tokens[0].trim());
						long svnRevision = Long.valueOf(tokens[1].trim());
						add(svnRevision, p4Change);
					} catch (NumberFormatException e) {
						logger.warn("Cannot process line in changeMap: " + line);
					}
				}
			}

			rf.close();
		} catch (FileNotFoundException e) {
			// no file, then return
			return;
		}
	}
}
