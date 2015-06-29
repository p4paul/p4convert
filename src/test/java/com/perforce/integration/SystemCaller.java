package com.perforce.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SystemCaller {

	private final static int CROP = 120;
	private static Logger logger = LoggerFactory.getLogger(SystemCaller.class);

	public static int killAll(String args) throws Exception {
		String cmd;
		String os = System.getProperty("os.name").toLowerCase();
		if (os.contains("win")) {
			cmd = "taskkill /F /IM ";
			args = args + ".exe";
		} else {
			cmd = "killall ";
		}

		return exec(cmd + args, true, false);
	}

	public static int exec(String args, boolean wait, boolean local)
			throws Exception {

		// add wrapper and args to build command
		String[] command = null;
		String os = System.getProperty("os.name").toLowerCase();
		if (local) {
			if (os.contains("win")) {
				args = args.replace('\\', '/');
				command = new String[] { args };
			} else {
				command = new String[] { args };
			}
		} else {
			if (os.contains("win")) {
				args = args.replace('\\', '/');
				command = new String[] { "bash", "-c", args };
			} else {
				command = new String[] { "bash", "-c", args };
			}
		}

		// log command
		StringBuffer sb = new StringBuffer();
		for (String a : command) {
			sb.append(a + " ");
		}
		logger.info("running: " + sb.toString());

		// run command
		Process process = null;
		try {
			process = Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			logger.warn("Failed to run...");
			return -1;
		}

		// if blocking, then wait until done (flushing io)
		if (wait) {
			boolean error = false;
			InputStreamReader stdout;
			InputStreamReader stderr;
			stdout = new InputStreamReader(process.getInputStream());
			stderr = new InputStreamReader(process.getErrorStream());
			BufferedReader bout = new BufferedReader(stdout);
			BufferedReader berr = new BufferedReader(stderr);

			String line;
			while ((line = bout.readLine()) != null) {
				logger.debug(line);
			}
			while ((line = berr.readLine()) != null) {
				logger.debug(line);
				error = true;
			}
			stdout.close();
			stderr.close();

			process.waitFor();
			if (error)
				return 1;
			return process.exitValue();
		} else {
			return 0;
		}
	}
}
