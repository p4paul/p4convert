package com.perforce.cvs.parser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.cvs.parser.rcstypes.RcsObject;
import com.perforce.cvs.parser.rcstypes.RcsObjectAdmin;
import com.perforce.cvs.parser.rcstypes.RcsObjectBlock;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;

public class RcsReader {

	private Logger logger = LoggerFactory.getLogger(RcsReader.class);

	private File rcsFile;
	private String rcsPath;
	private RcsObjectAdmin rcsAdmin;
	private RcsObject rcsDesc;
	private Map<String, RcsObjectDelta> rcsDeltas = new HashMap<String, RcsObjectDelta>();

	private CvsLineReader cvsLineReader;

	public RcsReader(File file) throws Exception {
		rcsFile = file;
		rcsPath = parseBasePath();
		rcsAdmin = new RcsObjectAdmin();
		cvsLineReader = new CvsLineReader(rcsFile.toString());

		parseRcsAdmin();

		RcsObjectDelta rcsObject = parseRcsDeltas();
		while (!rcsObject.isEmpty()) {

			if (rcsObject.containsKey(RcsSchema.DATE)) {
				rcsDeltas.put(rcsObject.getID().toString(), rcsObject);
			}

			if (rcsObject.containsKey(RcsSchema.DESC)) {
				rcsDesc = rcsObject;
			}

			// find log and add to rcsDeltas matching the same ID key
			if (rcsObject.containsKey(RcsSchema.LOG)) {
				String key = rcsObject.getID().toString();
				if (rcsDeltas.containsKey(key)) {
					RcsObjectDelta set = rcsDeltas.get(key);
					set.add(RcsSchema.LOG, rcsObject.getLog());
					rcsDeltas.put(key, set);
				}
			}

			if (rcsObject.containsKey(RcsSchema.TEXT)) {
				String key = rcsObject.getID().toString();
				if (rcsDeltas.containsKey(key)) {
					RcsObjectDelta set = rcsDeltas.get(key);
					set.add(RcsSchema.TEXT, rcsObject.getBlock());
					rcsDeltas.put(key, set);
				}
			}

			rcsObject = parseRcsDeltas();
		}
		cvsLineReader.close();
	}

	public RcsObjectAdmin getAdmin() {
		return rcsAdmin;
	}

	public ArrayList<RcsObjectNum> getIDs() {
		ArrayList<RcsObjectNum> list = new ArrayList<RcsObjectNum>();
		for (String key : rcsDeltas.keySet()) {
			list.add(new RcsObjectNum(key));
		}
		return list;
	}

	public RcsObjectDelta getDelta(RcsObjectNum id) {
		String key = id.toString();
		RcsObjectDelta delta = rcsDeltas.get(key);
		return delta;
	}

	public RcsObject getDesc() {
		return rcsDesc;
	}

	/**
	 * Returns a File object to the RCS ',v' file.
	 * 
	 * @return
	 */
	public File getRcsFile() {
		return rcsFile;
	}

	private void parseRcsAdmin() throws Exception {
		String line = getLine();
		StringBuffer sb = new StringBuffer();

		while (line != null) {
			// drop out on empty line
			if (line.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("end of admin");
				}
				return;
			}

			// add all phrases in line
			if (line.endsWith(";")) {
				sb.append(line);
				parsePhrase(sb.toString(), rcsAdmin);
				sb = new StringBuffer();
			} else {
				sb.append(line);
			}

			// get next line
			line = getLine();
		}
	}

	private String getLine() throws IOException {
		String line = cvsLineReader.getLine();
		return line;
	}

	private RcsObjectDelta parseRcsDeltas() throws Exception {
		RcsObjectDelta rcsObject = new RcsObjectDelta();

		// find and read delta number
		String line = getLine();
		StringBuffer sb = new StringBuffer();

		while (line != null) {
			// block might be delta e.g. 1.1
			if (line.contains(".")) {
				rcsObject.add(RcsSchema.ID, line);
				line = getLine();
				break;
			}

			// block might be a description e.g. desc
			if (line.startsWith("desc")) {
				String log = parseLog();
				rcsObject.add(RcsSchema.DESC, log);
				line = getLine();
				break;
			}
			line = getLine();
		}

		while (line != null) {
			// drop out on empty line (end of delta block)
			if (line.isEmpty()) {
				if (logger.isTraceEnabled()) {
					logger.trace("end of delta");
				}
				return rcsObject;
			}

			// add all phrases in line
			if (rcsObject.containsKey(RcsSchema.DATE)) {
				if (line.endsWith(";")) {
					sb.append(line);
					parsePhrase(sb.toString(), rcsObject);
					sb = new StringBuffer();
				} else {
					sb.append(line);
				}
			} else {
				parsePhrase(line, rcsObject);
			}

			// get next line
			line = getLine();
		}
		return rcsObject;
	}

	private void parsePhrase(String line, RcsObject rcs) throws Exception {

		// The "(?<=;)" is a cleaver (positive lookbehind) regex that leaves
		// the ';' in the string
		String[] phrases = line.split(";");
		for (String phrase : phrases) {

			// tidy up phrase
			phrase = phrase.trim();

			// split into key/value pairs
			String args[] = phrase.split("\\s+");

			// find key and detect if there is a value
			RcsSchema type = RcsSchema.parse(args[0]);

			// process value for key
			switch (type) {
			case LOG:
				String log = parseLog();
				rcs.add(type, log);
				break;

			case TEXT:
				RcsObjectBlock block = parseText();
				rcs.add(type, block);
				break;

			case SYMBOLS:
				rcs.add(type, phrase);
				break;

			case BRANCHES:
				StringBuffer sb = new StringBuffer();
				for (int i = 1; i < args.length; i++) {
					sb.append(args[i] + " ");
				}
				rcs.add(type, sb.toString());
				break;

			default:
				if (args.length > 1) {
					// parse remainder for values
					String r = args[1];
					r = r.trim();
					rcs.add(type, r);
				} else {
					rcs.add(type, "");
				}
				break;
			}
		}
	}

	private String parseLog() throws Exception {
		StringBuffer log = new StringBuffer();

		String line = getLine();
		if (!line.startsWith("@"))
			return null;

		// remove starting '@'
		line = line.substring(1);

		while (line != null) {
			// check for terminating '@'
			String end = line.replaceAll("@@", "");
			if (end.endsWith("@")) {
				break;
			} else {
				line = line.replaceAll("@@", "@");
				log.append(line);
				log.append("\n");
				line = getLine();
			}
		}
		return log.toString();
	}

	/**
	 * Check the buffer starts with an '@' and return a buffer less the starting
	 * '@', else null.
	 * 
	 * @param buf
	 * @return
	 */
	private ByteArrayOutputStream startAtpersand(ByteArrayOutputStream buf) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] b = buf.toByteArray();
		if (b[0] == '@') {
			out.write(b, 1, buf.size() - 1);
			return out;
		} else {
			return null;
		}
	}

	private ByteArrayOutputStream decodeAtpersand(ByteArrayOutputStream buf) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte last = '\0';
		for (byte b : buf.toByteArray()) {
			if (b == '@' && last == '@') {
				// don't write and invalidate last char to process @@@@ -> @@
				last = '\0';
			} else {
				out.write(b);
				last = b;
			}

		}
		return out;
	}

	private boolean endAtpersand(ByteArrayOutputStream buf)
			throws ConfigException {
		int size = buf.size();

		// exit early if less than 2 chars
		if (size < 2) {
			return false;
		}

		byte[] bytes = buf.toByteArray();

		// count '@' in line, if even then not end '@'
		int count = 0;
		for (byte b : bytes) {
			if (b == '@') {
				count++;
			}
		}
		if ((count % 2) != 0) {
			return true;
		}

		return false;
	}

	private RcsObjectBlock parseText() throws Exception {
		RcsObjectBlock lines = new RcsObjectBlock();

		// to help with debug
		int sum = 0;
		StringBuffer sb = new StringBuffer();

		// check and remove starting '@'
		ByteArrayOutputStream line = cvsLineReader.getData();
		line = startAtpersand(line);
		if (line == null)
			return null;

		while (line != null) {
			// replace '@@' with '@'
			ByteArrayOutputStream clean = new ByteArrayOutputStream();
			clean = decodeAtpersand(line);

			// exit if ending with '@\n'
			if (endAtpersand(line)) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				// copy buffer, but trim ending '@\n' chars
				out.write(clean.toByteArray(), 0, clean.size() - 2);
				if (out.size() > 0) {
					lines.add(out);
					if (logger.isTraceEnabled()) {
						sum += out.size();
						sb.append("parse:");
						sb.append(out.size());
						sb.append(":");
						sb.append(sum);
						sb.append(":END");
					}
				}
				break;
			} else {
				lines.add(clean);
				if (logger.isTraceEnabled()) {
					sum += clean.size();
					sb.append("parse:");
					sb.append(clean.size());
					sb.append(":");
					sb.append(sum);
					sb.append(" ");
				}
			}
			// get next line;
			line = cvsLineReader.getData();
		}
		if (logger.isTraceEnabled()) {
			logger.trace(sb.toString());
			logger.trace("total[" + lines.size() + "] " + sum);
		}
		return lines;
	}

	private String parseBasePath() throws Exception {
		String cvsroot = (String) Config.get(CFG.CVS_ROOT);
		String module = (String) Config.get(CFG.CVS_MODULE);

		String base = getRcsFile().getAbsolutePath();
		// remove CVSROOT from path
		if (base.startsWith(cvsroot)) {
			base = base.substring(cvsroot.length());
		}
		// remove MODULE from path
		if (base.startsWith(module)) {
			base = base.substring(module.length());
		}
		// remove leading '/'
		if (base.startsWith("/")) {
			base = base.substring(1);
		}
		// remove ',v' extension
		if (base.endsWith(",v")) {
			base = base.substring(0, base.lastIndexOf(",v"));
		}
		// remove attic from base path
		if (base.contains("Attic")) {
			int p = base.lastIndexOf("Attic");
			base = base.substring(0, p) + base.substring(p + 6);
		}
		return base;
	}

	public String getPath() {
		return rcsPath;
	}
}
