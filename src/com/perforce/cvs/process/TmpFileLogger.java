package com.perforce.cvs.process;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class TmpFileLogger {

	private static Logger logger = LoggerFactory.getLogger(TmpFileLogger.class);

	private static File auditFile;
	private static DataOutputStream out;

	static {
		try {
			String tmpDir = (String) Config.get(CFG.CVS_TMPDIR);
			auditFile = new File(tmpDir, "tmpFile.log");

			File d = new File(tmpDir);
			if (!d.mkdirs()) {
				if (!d.exists()) {
					logger.error("Cannot create directory: " + d.getPath());
					throw new RuntimeException();
				}
			}
		} catch (ConfigException e) {
			logger.error("Cannot get Configuration", e);
			throw new RuntimeException(e);
		}

		try {
			// add file handler
			FileOutputStream fs = new FileOutputStream(auditFile, false);
			BufferedOutputStream bs = new BufferedOutputStream(fs);
			out = new DataOutputStream(bs);

			// add header
			out.writeBytes("# RCS file with expanded deltas\n");
			out.flush();
		} catch (Exception e) {
			logger.error("Cannot write to file: " + auditFile, e);
			throw new RuntimeException(e);
		}
	}

	public static void logRcsFile(File file) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("\nRCS file: ");
		sb.append(file);
		sb.append("\n");

		out.writeBytes(sb.toString());
		out.flush();
	}

	public static void logTmpFile(String file) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("... ");
		sb.append(file);
		sb.append("\n");

		out.writeBytes(sb.toString());
		out.flush();
	}
}
