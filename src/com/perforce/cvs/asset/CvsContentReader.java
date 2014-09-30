package com.perforce.cvs.asset;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	public RcsObjectBlock xxxgetContent(RcsObjectNum rcs) throws Exception {
		RcsObjectDelta deltaNow = rcsDelta.getDelta(rcsHEAD);
		RcsObjectBlock blockFull = deltaNow.getBlock();
		while (deltaNow.getNext() != null && !rcs.equals(rcsHEAD)) {
			RcsObjectNum rcsNEXT = deltaNow.getNext();
			RcsObjectDelta deltaNext = rcsDelta.getDelta(rcsNEXT);
			RcsObjectBlock blockNext = deltaNext.getBlock();

			if (logger.isTraceEnabled()) {
				logger.trace("undelta rev: " + rcsNEXT);
			}
			blockFull = undelta(blockFull, blockNext);

			if (rcsNEXT.equals(rcs)) {
				break;
			} else {
				deltaNow = deltaNext;
			}
		}
		return blockFull;
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
		RcsObjectBlock lastBlock = new RcsObjectBlock(fullBlock);
		do {
			if (logger.isTraceEnabled()) {
				logger.trace("processing: " + last + " > " + id);
			}
			RcsObjectDelta delta = rcsDelta.getDelta(id);

			// undelta text; skip HEAD as it is already in full text
			if (!rcsHEAD.equals(id)) {
				RcsObjectBlock blockDelta = delta.getBlock();
				lastBlock = undelta(lastBlock, blockDelta);
			}

			// build tmp path from rev id and base path
			String tmp = (String) Config.get(CFG.CVS_TMPDIR);
			String base = rcsDelta.getPath();
			String path = tmp + "/" + base + "/" + id;

			// write blockFull to tmp file
			AssetWriter asset = new AssetWriter(path);
			Content content = new Content(lastBlock);
			asset.write(content);

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

		List<RcsDeltaAction> list = new ArrayList<RcsDeltaAction>();

		RcsDeltaAction last = null;
		for (ByteArrayOutputStream d : delta) {
			RcsDeltaAction act = new RcsDeltaAction(d);
			switch (act.getAction()) {
			case ADD:
			case DELETE:
				list.add(act);
				last = act;
				break;

			case TEXT:
				last.addLine(d);
				break;

			default:
				StringBuffer sb = new StringBuffer();
				sb.append("unknown type: " + act.getAction());
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

		// Apply deltas in reverse order to preserve index references
		Collections.reverse(list);
		for (RcsDeltaAction d : list) {
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

		// full.clean();

		if (logger.isTraceEnabled()) {
			logger.trace("result: " + full);
		}
		return full;
	}
}
