package com.perforce.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Usage {

	private static Logger logger = LoggerFactory.getLogger(Usage.class);

	public static void print() {
		logger.warn("No configuration file specified...\n");

		if (logger.isInfoEnabled()) {
			StringBuffer usage = new StringBuffer();
			usage.append("Usage:\n");
			usage.append("\tjava -jar p4convert-svn.jar <config file>\n\n");
			usage.append("\tjava -jar p4convert-svn.jar --config [ SVN | CVS ]\n");
			usage.append("\tjava -jar p4convert-svn.jar --version\n");
			usage.append("\tjava -jar p4convert-svn.jar --info [ dumpfile ]\n");
			usage.append("\tjava -jar p4convert-svn.jar --users [ dumpfile ]\n");
			usage.append("\tjava -jar p4convert-svn.jar --extract [ rev ].[ node ] [ dumpfile ]\n");
			logger.info(usage.toString());
		}
	}
}
