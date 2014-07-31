package com.perforce.config;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;

public class Config {

	private static Logger logger = LoggerFactory.getLogger(Config.class);

	private static Configuration config;

	public static void load(String file) throws ConfigException {
		String defaultSchema = (String) Config.get(CFG.SCHEMA);
		config = new Configuration(file);
		String schema = (String) get(CFG.SCHEMA);

		// Test configuration schema
		if (!schema.contentEquals(defaultSchema)) {
			throw new ConfigException("Schema mismatch - got: " + schema
					+ " required: " + defaultSchema);
		}

		// Test system case sensitivity
		CaseSensitivity caseSensitivity = (CaseSensitivity) get(CFG.P4_CASE);
		boolean modeCaseSensitive = (caseSensitivity == CaseSensitivity.NONE);
		if (isCaseSensitive() != modeCaseSensitive) {
			Stats.inc(StatsType.warningCount);
			logger.warn("System case sensitivity mismatch with configuration");
		}
	}

	public static void store(String file, ScmType type) throws ConfigException {
		config.store(file, type);
	}

	public static void setDefault() throws ConfigException {
		config = new Configuration();
	}

	public static Object get(CFG id) throws ConfigException {
		return config.get(id);
	}

	public static <T> void set(CFG id, T value) throws ConfigException {
		config.set(id, value);
	}

	public static String summary() {
		return config.summary();
	}

	/**
	 * Test if file system is case sensitive. Used to warn for configuration
	 * mismatch
	 * 
	 * @return
	 * @throws ConfigException
	 * @throws
	 */
	public static boolean isCaseSensitive() throws ConfigException {
		testWrite("tmp.txt");
		testWrite("TMP.txt");
		boolean test = testRead("tmp.txt");
		testRead("TMP.txt"); // delete twin if case sensitive
		return test;
	}

	private static void testWrite(String file) {
		try {
			FileOutputStream fs = new FileOutputStream(file, false);
			BufferedOutputStream bs = new BufferedOutputStream(fs);
			DataOutputStream out = new DataOutputStream(bs);
			out.writeBytes(file);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static boolean testRead(String file) {
		try {
			FileInputStream fs = new FileInputStream(file);
			BufferedInputStream bs = new BufferedInputStream(fs);
			DataInputStream in = new DataInputStream(bs);

			int len = file.getBytes().length;
			byte[] b = new byte[len];
			in.read(b, 0, len);
			in.close();
			new File(file).delete();

			String result = new String(b);
			return result.contentEquals(file);
		} catch (FileNotFoundException e) {
			// don't look for missing file...
			// (already deleted on case aware file systems)
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	public static boolean isImportMode() {
		String mode = config.getString(CFG.P4_MODE);
		return ("IMPORT".equalsIgnoreCase(mode));
	}
}
