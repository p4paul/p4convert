package com.perforce.svn.parser;

import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.LinkedHashMap;
import java.util.Map;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class Header {

	private Map<String, String> map = new LinkedHashMap<String, String>();

	public static Header readHeader(String line, SubversionReader dump)
			throws ParserException {
		Header header = new Header();

		while (!line.isEmpty()) {
			if (line.contains(":")) {
				int pos = line.indexOf(":");
				String key = line.substring(0, pos);
				// offset by 2 for colon and space ': '
				String value = line.substring(pos + 2);	
				header.map.put(key, value);
				line = dump.getLine();
			} else {
				throw new ParserException(
						"Cannot find ':' seperator in string\n...(" + line
								+ ")");
			}
		}
		return header;
	}

	public String toString() {
		String line = null;
		StringBuffer sb = new StringBuffer();

		for (String key : map.keySet()) {
			sb.append(key + ": ");
			sb.append(map.get(key) + "\n");
		}
		sb.append("\n");

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
			String value = map.get(key);
			if (value != null) {
				value = value.trim();
				return Long.parseLong(value);
			}
		}
		return Long.MIN_VALUE;
	}

	public String findString(String key) {
		if (map.containsKey(key)) {
			String value = map.get(key);
			if (value != null)
				value = value.trim();
			return value;
		}
		return null;
	}

	public boolean hasKey(String key) {
		return map.containsKey(key);
	}
}
