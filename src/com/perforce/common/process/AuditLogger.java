package com.perforce.common.process;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class AuditLogger {

	private static Logger logger = LoggerFactory.getLogger(AuditLogger.class);

	private static String auditFile;
	private static DataOutputStream out;

	static {
		try {
			auditFile = (String) Config.get(CFG.AUDIT_FILE);

			// add file handler
			FileOutputStream fs = new FileOutputStream(auditFile, false);
			BufferedOutputStream bs = new BufferedOutputStream(fs);
			out = new DataOutputStream(bs);

			// add header
			out.writeBytes("# <SCM path>, <SCM id>, <P4 change>, <MD5 sum>\n");
			out.flush();

		} catch (ConfigException e) {
			logger.error("Cannot get Configuration", e);
			throw new RuntimeException(e);
		} catch (Exception e) {
			logger.error("Cannot write to file: " + auditFile, e);
			throw new RuntimeException(e);
		}
	}

	public static void log(String path, int rev, long cng, String md5)
			throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append(path + ", ");
		sb.append(rev + ", ");
		sb.append(cng + ", ");
		sb.append(md5 + "\n");

		out.writeBytes(sb.toString());
		out.flush();
	}
}
