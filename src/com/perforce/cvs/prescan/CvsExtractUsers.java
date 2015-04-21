package com.perforce.cvs.prescan;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.cvs.parser.RcsFileFinder;
import com.perforce.cvs.parser.RcsReader;
import com.perforce.cvs.parser.rcstypes.RcsObjectDelta;
import com.perforce.cvs.parser.rcstypes.RcsObjectNum;
import com.perforce.cvs.process.CvsProcessChange;
import com.perforce.svn.prescan.Progress;

public class CvsExtractUsers {

	private static Logger logger = LoggerFactory.getLogger(CvsExtractUsers.class);

	public static void store(String cvsroot, String filename) throws Exception {
		Map<String, String> users = new HashMap<String, String>();

		RcsFileFinder rcsFiles = CvsProcessChange.find(cvsroot);

		Progress progress = new Progress(rcsFiles.getFiles().size());
		int count = 0;
		for (File file : rcsFiles.getFiles()) {
			try {
				RcsReader rcs = new RcsReader(file, false);
				ArrayList<RcsObjectNum> deltas = rcs.getIDs();
				for (RcsObjectNum id : deltas) {
					RcsObjectDelta revision = rcs.getDelta(id);
					String user = revision.getAuthor();
					users.put(user, user);
				}
				progress.update(++count);
			} catch (Exception e) {
				logger.warn("Unable to process file: " + file.getAbsolutePath());
				Stats.inc(StatsType.warningCount);
				e.printStackTrace();
			}
		}
		
		if (logger.isInfoEnabled()) {
			int u = users.size();
			logger.info("Found " + u + " user(s).");
		}
		FileOutputStream fs = new FileOutputStream(filename, false);
		BufferedOutputStream bs = new BufferedOutputStream(fs);
		DataOutputStream out = new DataOutputStream(bs);

		StringBuffer comment = new StringBuffer();
		comment.append("# User translation map. (" + filename + ")\n");
		comment.append("# Original CVS user names are listed on the left-hand-side\n");
		comment.append("# with the translation on the right-hand-side.\n");
		comment.append("# Note: Perforce restricts the use of characters '@', '#', '*', '%' and '...'\n");
		out.writeBytes(comment.toString());
		for (String user : users.keySet()) {
			out.writeBytes(user + ", " + users.get(user) + "\n");
		}
		out.flush();
		out.close();
	}
}
