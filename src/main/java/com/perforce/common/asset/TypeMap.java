package com.perforce.common.asset;

import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.client.P4Factory;

public class TypeMap {

	private static Logger logger = LoggerFactory.getLogger(TypeMap.class);
	private static SortedMap<String, String> map = new TreeMap<String, String>();

	public static boolean load(String filename) throws Exception {

		RandomAccessFile rf;
		try {
			rf = new RandomAccessFile(filename, "r");
			String line = null;

			if (logger.isInfoEnabled()) {
				logger.info("loading TypeMap: \t" + filename);
			}

			while ((line = rf.readLine()) != null) {
				// ignore comments starting with '#' and add lines to table
				if (!line.startsWith("#")) {
					line = line.trim();
					if (!line.isEmpty()) {
						if (line.contains("//....")) {
							String[] parts = line.split("\\s+");
							String type = parts[0];
							String ext = parts[1].replaceFirst("//....", "");
							map.put(ext, type);
							if (logger.isTraceEnabled()) {
								logger.trace("... map: " + ext + "\t => "
										+ type);
							}
						} else {
							if (logger.isTraceEnabled()) {
								logger.trace("ignoring line: " + line);
							}
						}
					}
				}
			}
			rf.close();
			return true;
		} catch (FileNotFoundException e) {
			// no file, then return
			return false;
		}
	}

	public static List<ContentProperty> getContentProperty(String svnPath) {
		int pos = svnPath.lastIndexOf(".");
		String extention = svnPath.substring(pos + 1);
		String typemap = map.get(extention);

		List<ContentProperty> prop;
		prop = P4Factory.p4javaToContentProperty(typemap);

		if (logger.isTraceEnabled()) {
			logger.trace("getContentProperty: " + typemap + " "
					+ prop.toString());
		}
		return prop;
	}

	public static ContentType getContentType(String svnPath) {
		int pos = svnPath.lastIndexOf(".");
		String extention = svnPath.substring(pos + 1);
		String typemap = map.get(extention);

		ContentType type;
		type = P4Factory.p4javaToContentType(typemap);

		if (logger.isTraceEnabled()) {
			logger.trace("getContentType: " + extention + " " + type.toString());
		}
		return type;
	}
	
	/**
	 * Test injection method.
	 * @param ext
	 * @param type
	 */
	public static void add(String ext, String type) {
		map.put(ext, type);
	}
	
	/**
	 * Test cleanup method.
	 */
	public static void clear() {
		map.clear();
	}
}
