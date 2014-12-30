package com.perforce.cvs.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RcsFileFinder {

	private Logger logger = LoggerFactory.getLogger(RcsFileFinder.class);
	private int count = 0;

	List<File> files = new ArrayList<File>();

	public RcsFileFinder(String path) {
		findFiles(path);
	}

	private void findFiles(String path) {
		File base = new File(path);
		if (!base.exists()) {
			logger.warn("CVSROOT does not exist: " + path);
			return;
		}

		File[] list = base.listFiles();

		for (File f : list) {
			if (f.isDirectory()) {
				if (!"CVSROOT".equals(f.getName())) {
					findFiles(f.getAbsolutePath());
				}
			} else {
				if (f.getName().endsWith(",v")) {
					files.add(f);
					logger.debug("file: " + f.getAbsolutePath());
					count++;
					System.out.print("Found: " + count + "\r");
				}
			}
		}
	}

	public List<File> getFiles() {
		return files;
	}

}
