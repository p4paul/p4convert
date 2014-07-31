package com.perforce.svn.parser;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.ContentType;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class Property {
	private static Logger logger = LoggerFactory.getLogger(Property.class);

	private Map<String, String> map = new LinkedHashMap<String, String>();

	public static Property readProperty(SubversionReader dump, long length)
			throws Exception {
		Property prop = new Property();

		String line = dump.getLine();
		long count = line.getBytes().length + 1;
		while ((!line.contains("PROPS-END")) && (count < length)) {
			String[] args;
			String key = "";
			String value = "";
			int keyLength = -1;
			int valueLength = -1;

			// Key
			args = line.split(" ");
			if (args[0].contains("K")) {
				keyLength = Integer.parseInt(args[1]);
				line = dump.getLine();
				count += line.getBytes().length + 1;
				if (line.getBytes().length == keyLength) {
					key = line;
				} else {
					StringBuffer sb = new StringBuffer();
					sb.append("length missmatch on 'key'");
					sb.append(keyLength + "/");
					sb.append(line.getBytes().length);
					throw new RuntimeException(sb.toString());
				}
			}

			// Value
			line = dump.getLine();
			count += line.getBytes().length + 1;
			args = line.split(" ");
			if (args[0].contains("V")) {
				valueLength = Integer.parseInt(args[1]);

				// Could be binary or more than one line
				byte[] bytes = dump.getBlob(valueLength);
				ContentType type = ContentType.UTF_8;
				line = getString(bytes, type);
				if (line == null) {
					type = (ContentType) Config.get(CFG.SVN_PROP_TYPE);
					line = getString(bytes, type);
				}
				if (line == null) {
					line = "<binary property>";
					if (type != ContentType.P4_BINARY && key.contains("svn:")) {
						StringBuffer sb = new StringBuffer();
						sb.append("Unable to parse prop value for key: ");
						sb.append(key);
						sb.append("\nTo avoid this warning set ");
						sb.append("com.p4convert.svn.propTextType ");
						sb.append("to your code page or 'BINARY'.");
						Stats.inc(StatsType.warningCount);
						logger.warn(sb.toString());
					}
				}
				value = line;
				count += bytes.length + 1;

				// read left over CR/LF
				dump.getLine();
			}

			// Store
			if ((keyLength >= 0) && (valueLength >= 0)) {
				prop.map.put(key, value);
			}
			line = dump.getLine();
			count += line.getBytes().length + 1;
		}

		// Distinguish between empty property and no property
		if (line.contains("PROPS-END")) {
			return prop;
		} else {
			return null;
		}
	}

	private static String getString(byte[] bytes, ContentType contentType) {
		try {
			Charset charset = Charset.forName(contentType.getName());
			CharsetDecoder decoder = charset.newDecoder();
			CharBuffer cbuf = decoder.decode(ByteBuffer.wrap(bytes));
			return cbuf.toString();
		} catch (CharacterCodingException e) {
			logger.debug("Property value not " + contentType.getName());
		} catch (UnsupportedCharsetException e) {
			logger.debug("Unsupported charset " + contentType.getName());
		}
		return null;
	}

	public String toString() {
		String line = null;
		StringBuffer sb = new StringBuffer();

		for (String p : map.keySet()) {
			sb.append("K " + p.getBytes().length + "\n" + p + "\n");
			sb.append("V " + map.get(p).getBytes().length + "\n" + map.get(p)
					+ "\n");
		}
		sb.append("PROPS-END\n");

		// Format string to OS normalisation type (NFD or NFC)
		try {
			Form form = (Form) Config.get(CFG.P4_NORMALISATION);
			line = Normalizer.normalize(sb.toString(), form);
		} catch (ConfigException e) {
			e.printStackTrace();
		}
		return line;
	}

	public long findLong(String key) {
		if (map.containsKey(key)) {
			// remove leading white space
			String value = map.get(key).replaceAll("^\\s+", "");
			return Long.parseLong(value);
		}
		return Long.MIN_VALUE;
	}

	public String findString(String key) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		return null;
	}

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public Set<String> getKeySet() {
		return map.keySet();
	}
}
