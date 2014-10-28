package com.perforce.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Usage {

	private static Logger logger = LoggerFactory.getLogger(Usage.class);

	public static void print() {
		logger.warn("No option or configuration file specified...\n");

		if (logger.isInfoEnabled()) {
			StringBuffer usage = new StringBuffer();
			usage.append("Usage:\n");
			usage.append("\tjava -jar p4convert.jar [ config file ]\n");
			usage.append("\nOptions:\n");
			usage.append("\tjava -jar p4convert.jar --config [ SVN | CVS ]\n");
			usage.append("\tjava -jar p4convert.jar --version\n");
			usage.append("\nSubversion specific options:\n");
			usage.append("\tjava -jar p4convert.jar --info [ dumpfile ]\n");
			usage.append("\tjava -jar p4convert.jar --users [ dumpfile ]\n");
			usage.append("\tjava -jar p4convert.jar --extract [ rev ].[ node ] [ dumpfile ]\n");
			logger.info(usage.toString());
		}
	}
}
