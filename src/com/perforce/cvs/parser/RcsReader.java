package com.perforce.cvs.parser;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
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

	private RandomAccessFile rf;

	public RcsReader(File file) throws Exception {
		rcsFile = file;
		rcsPath = parseBasePath();
		rcsAdmin = new RcsObjectAdmin();

		// TODO Charset charset = Charset.forName("US-ASCII");
		// br = Files.newBufferedReader(file.toPath(), charset);
		rf = new RandomAccessFile(file.toString(), "r");

		parseRcsAdmin();

		RcsObjectDelta rcsObject = parseRcsDeltas();
		while (!rcsObject.isEmpty()) {

			if (rcsObject.containsKey(RcsSchema.DATE)) {
				rcsDeltas.put(rcsObject.getID().toString(), rcsObject);
			}

			if (rcsObject.containsKey(RcsSchema.DESC)) {
				rcsDesc = rcsObject;
			}

			// -----------------------------------------------------------------------------------
			// TODO Split this here to read only metadata and skip content as
			// binary
			// content can't be read as lines/strings
			// read other fields.
			// -----------------------------------------------------------------------------------

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
		rf.close();
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
		String line = rf.readLine();
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

	private RcsObjectBlock parseText() throws Exception {
		RcsObjectBlock lines = new RcsObjectBlock();

		long position = rf.getFilePointer();
		long length = 0;

		String line = getLine();
		if (!line.startsWith("@"))
			return null;

		// remove starting '@'
		line = line.substring(1);

		while (line != null) {
			// end check for terminating '@'
			String end = line.replaceAll("@@", "");

			// exit if ending with '@'
			if (end.endsWith("@")) {
				// add line to lines
				line = line.replaceAll("@@", "@");
				line = line.substring(0, line.length() - 1);
				if (!"".equals(line)) {
					lines.add(line);
				}
				line = getLine();
				length = rf.getFilePointer() - position;
				break;
			} else {
				// add line to lines
				line = line.replaceAll("@@", "@");
				lines.add(line + "\n");
				line = getLine();
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("pos:" + position + " len:" + length);
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
