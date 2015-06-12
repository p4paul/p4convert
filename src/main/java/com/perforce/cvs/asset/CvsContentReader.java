package com.perforce.cvs.asset;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.AssetWriter;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.cvs.parser.RcsDeltaAction;
import com.perforce.cvs.parser.RcsReader;
import com.perforce.cvs.parser.rcstypes.RcsObjectBlock;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.cvs.parser.rcstypes.RcsObjectNumList;
import com.perforce.svn.parser.Content;

public class CvsContentReader {

	private static Logger logger = LoggerFactory
			.getLogger(CvsContentReader.class);

	private RcsReader rcsDelta;
	private RcsObjectNum rcsHEAD;
	private RcsObjectBlock rcsBlock;

	public CvsContentReader(RcsReader rcs) {
		this.rcsDelta = rcs;
		this.rcsHEAD = rcs.getAdmin().getID();
		RcsObjectDelta deltaHEAD = rcs.getDelta(rcsHEAD);
		this.rcsBlock = deltaHEAD.getBlock();
	}

	/**
	 * Writes out all RCS text revisions in full. Stores the text content in a
	 * temporary directory, using the RCS id as a file name.
	 * 
	 * @param id
	 * @throws Exception
	 */
	public void cacheContent() throws Exception {
		cacheContent(rcsHEAD, rcsHEAD, this.rcsBlock);
	}

	/**
	 * [RECURSIVE]
	 * 
	 * @param id
	 * @throws Exception
	 */
	private void cacheContent(RcsObjectNum id, RcsObjectNum last,
			RcsObjectBlock fullBlock) throws Exception {
		boolean parseError = false;
		RcsObjectBlock lastBlock = new RcsObjectBlock(fullBlock);
		do {
			if (logger.isDebugEnabled()) {
				logger.debug("processing: " + last + " > " + id);
			}
			RcsObjectDelta delta = rcsDelta.getDelta(id);

			// build tmp path from rev id and base path
			String tmp = (String) Config.get(CFG.CVS_TMPDIR);
			String base = rcsDelta.getPath();
			String path = tmp + "/" + base + "/" + id;

			// undelta text; skip HEAD as it is already in full text
			try {
				if (parseError) {
					throw new Exception("Exception due to previous RCS errors.");
				}

				if (!rcsHEAD.equals(id)) {
					RcsObjectBlock blockDelta = delta.getBlock();
					if (blockDelta != null) {
						lastBlock = undelta(lastBlock, blockDelta);
					} else {
						logger.warn("No data block: " + base + " "
								+ delta.getID());
						Stats.inc(StatsType.warningCount);
					}
				}

				// write blockFull to tmp file
				AssetWriter asset = new AssetWriter(path);
				Content content = new Content(lastBlock);
				asset.write(content);
			} catch (Exception e) {
				logger.warn("RCS parse error on: " + base + " " + delta.getID());
				Stats.inc(StatsType.warningCount);

				// write blockFull to tmp file
				File dummy = new File(path);
				new FileOutputStream(dummy).close();
				parseError = true;
			}

			// recurse on branches
			RcsObjectNumList tags = delta.getBranches();
			if (!tags.isEmpty()) {
				for (RcsObjectNum tag : tags.getList()) {
					cacheContent(tag, id, lastBlock);
				}
			}

			last = id;
			id = delta.getNext();
		} while (id != null);
	}

	private List<RcsDeltaAction> parse(RcsObjectBlock delta) throws Exception {

		Iterator<ByteArrayOutputStream> lines = delta.iterator();

		// exit early if nothing to process
		if (!lines.hasNext()) {
			return new ArrayList<RcsDeltaAction>();
		}

		ByteArrayOutputStream line = lines.next();
		RcsDeltaAction action = new RcsDeltaAction(line);
		switch (action.getAction()) {
		case ADD:
		case DELETE:
			return parseDeltas(delta);

		case TEXT:
			return new ArrayList<RcsDeltaAction>();

		default:
			StringBuffer sb = new StringBuffer();
			sb.append("unknown type: " + action.getAction());
			logger.error(sb.toString());
			throw new Exception(sb.toString());
		}
	}

	/**
	 * Reads only the delta commands and skips over the correct number of text
	 * lines.
	 * 
	 * @param lines
	 * @return
	 * @throws Exception
	 */
	private List<RcsDeltaAction> parseDeltas(RcsObjectBlock delta)
			throws Exception {

		Iterator<ByteArrayOutputStream> lines = delta.iterator();
		List<RcsDeltaAction> list = new ArrayList<RcsDeltaAction>();

		while (lines.hasNext()) {
			ByteArrayOutputStream line = lines.next();
			RcsDeltaAction action = new RcsDeltaAction(line);
			switch (action.getAction()) {
			case ADD:
				list.add(action);
				for (int i = 0; i < action.getLength(); i++) {
					// read lines and add to action
					if (lines.hasNext()) {
						line = lines.next();
						action.addLine(line);
					}
				}
				break;
			case DELETE:
				list.add(action);
				break;

			default:
				StringBuffer sb = new StringBuffer();
				sb.append("unmatched line: " + line);
				logger.error(sb.toString());
				throw new Exception(sb.toString());
			}
		}
		return list;
	}

	/**
	 * Rebuilds the delta given the full file
	 * 
	 * @param full
	 * @param delta
	 * @return
	 * @throws Exception
	 */
	private RcsObjectBlock undelta(RcsObjectBlock full, RcsObjectBlock delta)
			throws Exception {

		List<RcsDeltaAction> list = parse(delta);

		// If full is empty and no parsed deltas, then return the delta
		if (full.isEmpty() && list.isEmpty()) {
			return delta;
		}

		// Apply deltas in reverse order to preserve index references
		Collections.reverse(list);
		for (RcsDeltaAction d : list) {
			if (logger.isTraceEnabled()) {
				logger.trace("... " + d);
			}

			switch (d.getAction()) {
			case ADD:
				full.insert(d.getLine(), d.getBlock());
				break;

			case DELETE:
				full.remove(d.getLine(), d.getLength());
				break;

			default:
				StringBuffer sb = new StringBuffer();
				sb.append("unknown type: " + d.getAction());
				logger.error(sb.toString());
				throw new Exception(sb.toString());
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace("result: " + full);
		}
		return full;
	}
}
