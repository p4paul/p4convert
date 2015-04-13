package com.perforce.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ExitCode;
import com.perforce.common.asset.ContentType;

public class Configuration {

	private Logger logger = LoggerFactory.getLogger(Configuration.class);
	private Properties cfg = new Properties();

	public Configuration() throws ConfigException {
		super();
		setHidden();
		setDefault();
	}

	public Configuration(String filename) throws ConfigException {
		super();
		setHidden();
		load(filename);
	}

	/**
	 * Loads a default set of configurations
	 * 
	 * @throws Exception
	 */
	private void setDefault() throws ConfigException {
		String pwd = System.getProperty("user.dir") + File.separator;

		// General Configuration:
		Version ver = new Version();
		set(CFG.TEST, false);
		set(CFG.VERSION, ver.getVersion());
		set(CFG.SCHEMA, "5." + CFG.values().length);
		set(CFG.SCM_TYPE, ScmType.P4);

		// General audit and logging
		set(CFG.AUDIT_ENABLED, true);
		set(CFG.AUDIT_FILE, "audit.log");
		set(CFG.CHANGE_MAP, "changeMap.txt");

		// Perforce connection
		set(CFG.P4_MODE, "IMPORT");
		set(CFG.P4_ROOT, pwd + "p4_root" + File.separator);
		set(CFG.P4_PORT, "localhost:4444");
		set(CFG.P4_USER, "p4-user");
		set(CFG.P4_PASSWD, "");
		set(CFG.P4_CLIENT, "p4-client");
		set(CFG.P4_CLIENT_ROOT, pwd + "ws" + File.separator);
		set(CFG.P4_UNICODE, false);
		set(CFG.P4_TRANSLATE, true);
		set(CFG.P4_CHARSET, "<none>");
		set(CFG.P4_DEPOT_PATH, "import");
		set(CFG.P4_DEPOT_SUB, File.separator);
		set(CFG.P4_JNL_PREFIX, "jnl.");
		set(CFG.P4_JNL_INDEX, 0);
		set(CFG.P4_LOG_ID, "<description>");
		set(CFG.P4_OFFSET, 0L);
		set(CFG.P4_LINEEND, true);
		set(CFG.P4_SKIP_EMPTY, false);
		set(CFG.P4_DOWNGRADE, false);

		// Check system for case sensitivity
		if (Config.isCaseSensitive()) {
			set(CFG.P4_CASE, CaseSensitivity.NONE);
			set(CFG.P4_C1_MODE, false);
		} else {
			set(CFG.P4_CASE, CaseSensitivity.FIRST);
			set(CFG.P4_C1_MODE, true);
		}

		// Subversion specific modes
		set(CFG.SVN_DUMPFILE, "<unset>");
		set(CFG.SVN_START, 1L);
		set(CFG.SVN_END, 0L);
		set(CFG.SVN_PROP_NAME, ".svn.properties");
		set(CFG.SVN_PROP_ENCODE, "ini");
		set(CFG.SVN_PROP_ENABLED, false);
		set(CFG.SVN_PROP_TYPE, ContentType.UNKNOWN);
		set(CFG.SVN_DIR_NAME, ".svn.empty");
		set(CFG.SVN_DIR_ENABLED, false);
		set(CFG.SVN_KEEP_KEYWORD, true);
		set(CFG.SVN_MERGEINFO, false);
		set(CFG.SVN_LABELS, false);
		set(CFG.SVN_LABEL_DEPTH, 2);
		set(CFG.SVN_LABEL_FORMAT, "svn_label:{depth}");

		// CVS specific modes
		set(CFG.CVS_ROOT, "<unset>");
		set(CFG.CVS_MODULE, "");
		set(CFG.CVS_WINDOW, 20 * 1000L); // milliseconds
		set(CFG.CVS_TMPDIR, "tmp");
		set(CFG.CVS_LABELS, false);
		set(CFG.CVS_LABEL_FORMAT, "{symbol}");

		// UTF8 path normalisation
		String os = System.getProperty("os.name").toLowerCase();

		if (os.contains("win"))
			set(CFG.P4_NORMALISATION, Normaliser.NFC);
		else if (os.contains("mac"))
			set(CFG.P4_NORMALISATION, Normaliser.NFD);
		else if (os.contains("nix"))
			set(CFG.P4_NORMALISATION, Normaliser.NFC);
		else if (os.contains("nux"))
			set(CFG.P4_NORMALISATION, Normaliser.NFC);
		else if (os.contains("sun"))
			set(CFG.P4_NORMALISATION, Normaliser.NFC);
		else
			set(CFG.P4_NORMALISATION, Normaliser.NFC);
	}

	public void setHidden() throws ConfigException {
		// Set hidden properties
		set(CFG.EXCLUDE_MAP, "exclude.map");
		set(CFG.INCLUDE_MAP, "include.map");
		set(CFG.ISSUE_MAP, "issue.map");
		set(CFG.USER_MAP, "users.map");
		set(CFG.TYPE_MAP, "types.map");
		set(CFG.PATH_MAP, "path.map");
		set(CFG.P4_LARGE_FILE, 10L * 1024L * 1024L);
		set(CFG.CVS_MAXLINE, 1024L * 1024L * 1024L);
	}

	private static <T> boolean check(CFG id, Class<T> type) {
		if (id.getType() == type) {
			return true;
		}
		return false;
	}

	public <T> void set(CFG id, T value) throws ConfigException {
		if (check(id, value.getClass())) {
			cfg.setProperty(id.toString(), String.valueOf(value));
		} else {
			throw new ConfigException("unknown " + value.getClass().getName()
					+ " option '" + id.toString() + "'");
		}
	}

	public String getString(CFG id) {
		return cfg.getProperty(id.toString());
	}

	public Object get(CFG id) throws ConfigException {
		String key = id.toString();

		String value = cfg.getProperty(key);
		if (value == null) {
			StringBuffer sb = new StringBuffer();
			sb.append("Cannot find '" + key + "' in the configuration file.\n");
			sb.append("Please check or regnerate your configuration.\n");
			throw new ConfigException(sb.toString());
		}

		Class<?> type = id.getType();

		try {
			if (type.isEnum()) {
				Method method = type.getMethod("parse", String.class);
				return method.invoke(null, value);
			}
			try {
				Constructor<?> cons = type.getConstructor(String.class);
				return cons.newInstance(value);
			} catch (NoSuchMethodException e) {
				Method method = type.getMethod("parse", String.class);
				return method.invoke(null, value);
			}
		} catch (Exception e) {
			throw new ConfigException("unknown type in option '"
					+ type.toString() + "'", e);
		}
	}

	public String summary() {
		StringBuffer sb = new StringBuffer();
		sb.append("Configuration settings:\n");
		for (Map.Entry<Object, Object> c : cfg.entrySet()) {
			String key = (String) c.getKey();
			String value = (String) c.getValue();
			sb.append("   " + key + ": " + value + "\n");
		}
		return sb.toString();
	}

	private void load(String file) throws ConfigException {
		BufferedReader in = null;
		try {
			// Load User configuration options from file
			in = new BufferedReader(new FileReader(file));
			cfg.load(in);
			in.close();
		} catch (IOException e) {
			logger.error("Unable to load configuration file '" + file + "'\n",
					e);
			System.exit(ExitCode.USAGE.value());
		}
	}

	public void store(String file, ScmType type) throws ConfigException {
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(file));

			Date d = new Date();
			out.write("# P4Convert configuration (" + d.toString()
					+ ")\n\n");
			out.write("# Please note that all paths should be absolute and must end with a path\n");
			out.write("# delimiter e.g. '/' or '\\\\'.  For example:\n");
			out.write("#   com.p4convert.p4.clientRoot=C:\\\\perforce\\\\client_ws\\\\ \n");
			out.write("#   com.p4convert.p4.clientRoot=/Users/perforce/client_ws/ \n");

			switch (type) {
			case SVN:
				set(CFG.SCM_TYPE, ScmType.SVN);

				out.write("\n# Core converter settings\n");
				storeGroup(out, "com.p4convert.core.");

				out.write("\n# Subversion import options\n");
				storeGroup(out, "com.p4convert.svn.");
				break;

			case CVS:
				set(CFG.SCM_TYPE, ScmType.CVS);

				out.write("\n# Core converter settings\n");
				storeGroup(out, "com.p4convert.core.");

				out.write("\n# CVS import options\n");
				storeGroup(out, "com.p4convert.cvs.");
				break;

			default:
				out.close();
				throw new ConfigException("SCM type not recognised.");
			}

			out.write("\n# Perforce Environment\n");
			storeGroup(out, "com.p4convert.p4.");

			out.write("\n# Logging options\n");
			storeGroup(out, "com.p4convert.log.");

			out.close();
		} catch (IOException e) {
			throw new ConfigException("Cannot store configuration file", e);
		}
	}

	private void storeGroup(BufferedWriter out, String group)
			throws IOException {

		List<String> keys = new ArrayList<String>(cfg.stringPropertyNames());
		Collections.sort(keys);

		for (String key : keys) {
			String value = (String) cfg.get(key);
			if (key.startsWith(group))
				out.write(key + "=" + value + "\n");
		}
	}

	public Collection<Entry<Object, Object>> getEntrySet() {
		return cfg.entrySet();
	}

}
