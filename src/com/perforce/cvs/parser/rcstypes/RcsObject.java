package com.perforce.cvs.parser.rcstypes;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.cvs.parser.RcsSchema;

public abstract class RcsObject {

	private Logger logger = LoggerFactory.getLogger(RcsObject.class);

	private Map<RcsSchema, Object> map = new HashMap<RcsSchema, Object>();

	public boolean isEmpty() {
		return map.isEmpty();
	}

	public boolean containsKey(RcsSchema key) {
		return map.containsKey(key);
	}

	public void add(RcsSchema key, String value) throws Exception {
		Class<?> type = key.getTag().getType();
		if (logger.isTraceEnabled()) {
			logger.trace("adding: " + key + " [" + value + "]  type:"
					+ type.getSimpleName());
		}

		Constructor<?> cons = type.getConstructor(String.class);
		Object item = cons.newInstance(value);
		insert(key, item);
	}

	public void add(RcsSchema key, RcsObjectBlock values) throws Exception {
		Class<?> type = key.getTag().getType();
		if (logger.isTraceEnabled()) {
			logger.trace("adding: " + key + " lines[" + values.size()
					+ "]  type:" + type.getSimpleName());
		}
		insert(key, values);
	}

	private static <T> boolean check(RcsSchema key, Class<T> type) {
		return key.getTag().getType() == type;
	}

	private <T> void insert(RcsSchema key, T item) {
		if (check(key, item.getClass())) {
			if (logger.isTraceEnabled()) {
				logger.trace("inserting: " + key.getName() + " "
						+ item.toString());
			}
			map.put(key, item);
		} else {
			logger.warn("unknown " + item.getClass().getName() + " option '"
					+ key.toString() + "'");
		}

	}

	public Object get(RcsSchema item) {
		if (map.containsKey(item)) {
			return map.get(item);
		}
		return null;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (RcsSchema key : map.keySet()) {
			sb.append("... " + key.getName());
			sb.append(" " + map.get(key));
			sb.append("\n");
		}
		return sb.toString();
	}

	public RcsObjectNum getID() {
		RcsObjectNum id = (RcsObjectNum) get(RcsSchema.ID);
		if (id != null && id.getVer() != null) {
			return id;
		} else {
			id = (RcsObjectNum) get(RcsSchema.HEAD);
			if (id != null && id.getVer() != null) {
				return id;
			}
		}
		return null;
	}
}