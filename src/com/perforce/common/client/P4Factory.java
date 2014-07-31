package com.perforce.common.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.p4java.core.file.FileAction;
import com.perforce.p4java.core.file.FileSpecOpStatus;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.svn.history.ChangeAction.Action;

public class P4Factory {

	private static Logger logger = LoggerFactory.getLogger(P4Factory.class);

	private static final Map<String, String> P4FileTypes;
	static {
		Map<String, String> map = new HashMap<String, String>();
		map.put("ctempobj", "Sw");
		map.put("ctext", "C");
		map.put("cxtext", "Cx");
		map.put("ktext", "k");
		map.put("kxtext", "kx");
		map.put("ltext", "F");
		map.put("tempobj", "FSw");
		map.put("ubinary", "F");
		map.put("uresource", "F");
		map.put("uxbinary", "Fx");
		map.put("xbinary", "x");
		map.put("xltext", "Fx");
		map.put("xtempobj", "Swx");
		map.put("xtext", "x");
		map.put("xunicode", "x");
		map.put("xutf16", "x");
		P4FileTypes = Collections.unmodifiableMap(map);
	}

	/**
	 * Validates returned FileSpec from operation. Will suppress warning
	 * messages in the ignore string.
	 * 
	 * @param fileSpecs
	 * @param ignore
	 * @throws ConverterException
	 */
	public static void validateFileSpecs(List<IFileSpec> fileSpecs,
			String... ignore) throws ConverterException {
		for (IFileSpec fileSpec : fileSpecs) {
			if (fileSpec.getOpStatus() != FileSpecOpStatus.VALID) {
				String msg = fileSpec.getStatusMessage();
				
				// superfluous p4java message
				boolean unknownMsg = true;
				ArrayList<String> ignoreList = new ArrayList<String>();
				ignoreList.add("file does not exist");
				ignoreList.addAll(Arrays.asList(ignore));
				for (String istring : ignoreList) {
					if (msg.contains(istring)) {
						if(logger.isTraceEnabled()) {
							logger.trace(msg);
						}
						// its a known message
						unknownMsg = false;
					}	
				}
						
				// check and report unknown message
				if(unknownMsg) {
					Stats.inc(StatsType.warningCount);
					logger.warn("p4java: " + msg);
				}
			}
		}
	}
	
	/**
	 * Look for a message in the returned FileSpec from operation.
	 * 
	 * @param fileSpecs
	 * @param ignore
	 * @return
	 * @throws ConverterException
	 */
	public static boolean trapFileSpecs(List<IFileSpec> fileSpecs,
			String trap) throws ConverterException {
		for (IFileSpec fileSpec : fileSpecs) {
			if (fileSpec.getOpStatus() != FileSpecOpStatus.VALID) {
				String msg = fileSpec.getStatusMessage();
				if( msg.contains(trap) ) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Translates P4Java FileActions to internal query Actions
	 * 
	 * @param fileAction
	 * @return
	 * @throws ConverterException
	 */
	public static Action p4javaToQueryAction(FileAction fileAction)
			throws ConverterException {
		Action action = null;

		if (fileAction == null)
			return null;

		switch (fileAction) {
		case ADD:
			action = Action.ADD;
			break;
		case EDIT:
			action = Action.EDIT;
			break;
		case DELETE:
			action = Action.REMOVE;
			break;
		case BRANCH:
		case INTEGRATE:
			action = Action.BRANCH;
			break;
		default:
			throw new ConverterException("Unknown FileAction: "
					+ fileAction.name());
		}
		return action;
	}

	/**
	 * Translates P4Java file types to internal ContentType
	 * 
	 * @param headType
	 * @return
	 */
	public static ContentType p4javaToContentType(String headType) {
		ContentType type = ContentType.UNKNOWN;
		
		if(headType == null)
			return ContentType.UNKNOWN;
		
		String base = null;
		String[] parts;
		if (headType.contains("+")) {
			parts = headType.split("\\+");
			base = parts[0];
		} else {
			base = headType;
		}

		if (base.contains("text"))
			type = ContentType.P4_TEXT;
		if (base.contains("binary"))
			type = ContentType.P4_BINARY;
		if (base.contains("unicode"))
			type = ContentType.P4_UNICODE;
		if (base.contains("utf16"))
			type = ContentType.P4_UTF16;
		if (base.contains("symlink"))
			type = ContentType.SYMLINK;

		return type;
	}

	/**
	 * Translates P4Java file modification bits to internal ContentProperty
	 * bits.
	 * 
	 * @param headType
	 * @return
	 */
	public static List<ContentProperty> p4javaToContentProperty(String headType) {
		
		List<ContentProperty> props = new ArrayList<ContentProperty>();
		if(headType == null)
			return props;
		
		// Fetch modification bits
		String modBits = "";
		if (headType.contains("+")) {
			String[] parts = headType.split("\\+");
			modBits = parts[1];
		} else {
			if (P4FileTypes.containsKey(headType)) {
				modBits = P4FileTypes.get(headType);
			}
		}

		// Build array of property bits
		for (char c : modBits.toCharArray()) {
			if (c == 'x')
				props.add(ContentProperty.EXECUTE);
			if (c == 'k')
				props.add(ContentProperty.KEYWORD);
			if (c == 'l')
				props.add(ContentProperty.LOCK);
			if (c == 'm')
				props.add(ContentProperty.MODTIME);
			if (c == 'w')
				props.add(ContentProperty.WRITABLE);
		}
		return props;
	}
}
