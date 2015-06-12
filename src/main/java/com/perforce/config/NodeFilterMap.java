package com.perforce.config;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeFilterMap {

	private Logger logger = LoggerFactory.getLogger(NodeFilterMap.class);

	private ArrayList<String> filter = new ArrayList<String>();
	private boolean loaded = false;
	private String filename;

	public NodeFilterMap(String file) throws Exception {
		filename = file;
	}

	public NodeFilterMap() {
	}

	/**
	 * Load filter file into ArrayList skipping lines starting with a '#'
	 * 
	 * @param file
	 * @throws Exception
	 */
	public boolean load() throws Exception {

		RandomAccessFile rf;
		try {
			rf = new RandomAccessFile(filename, "r");
			String line = null;

			while ((line = rf.readLine()) != null) {
				loaded = true;

				// ignore comments starting with '#' and add lines to table
				if (!line.startsWith("#")) {
					line = line.trim();
					if (!line.isEmpty()) {
						filter.add(line);
						if (logger.isInfoEnabled()) {
							logger.info(filename + ": " + line);
						}
					}
				}
			}
			rf.close();
			return loaded;
		} catch (FileNotFoundException e) {
			// no file, then return
			return false;
		}
	}

	public void store() throws Exception {
		// do not clobber existing file, unless it has been loaded
		if (!loaded) {
			logger.warn(filename + ": has not been loaded!");
			return;
		}

		FileOutputStream fs = new FileOutputStream(filename, false);
		BufferedOutputStream bs = new BufferedOutputStream(fs);
		DataOutputStream out = new DataOutputStream(bs);

		for (String e : filter) {
			out.writeBytes(e + "\n");
		}
		out.flush();
		out.close();
	}

	public boolean contains(String path) {
		for (String pattern : filter) {
			if (path.matches(pattern)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * add new elements to the list
	 * 
	 * @param e
	 */
	public void add(String e) {
		filter.add(e);
	}

	public boolean isEmpty() {
		return filter.isEmpty();
	}

	public void clean() {
		filter.clear();
		loaded = true;
	}

}
