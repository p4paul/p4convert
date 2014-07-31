package com.perforce.config;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserMapping {

	private static Logger logger = LoggerFactory.getLogger(UserMapping.class);

	private static boolean loaded = false;
	private static Map<String, String> map = new HashMap<String, String>();

	public static boolean load(String filename) throws Exception {
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
						String parts[] = line.split(",");
						map.put(parts[0].trim(), parts[1].trim());
					}
				}
			}
			rf.close();

			if (logger.isInfoEnabled()) {
				logger.info("Loaded user translation map '" + filename + "'; "
						+ map.size() + " user names remapped.\n");
			}
			return loaded;
		} catch (FileNotFoundException e) {
			// no file, then return
			return false;
		}
	}

	public static String get(String svnUser) {
		if (map.containsKey(svnUser))
			return map.get(svnUser);
		else
			return svnUser;
	}

}
