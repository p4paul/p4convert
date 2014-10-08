package com.perforce.svn.prescan;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.process.ProcessUser;
import com.perforce.svn.change.ChangeParser;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.RecordReader;

public class ExtractUsers {

	private static Logger logger = LoggerFactory.getLogger(ExtractUsers.class);

	public static void store(String dumpFile, String filename) throws Exception {
		Map<String, String> users = new HashMap<String, String>();
		RecordReader recordReader = new RecordReader(dumpFile);

		if (logger.isInfoEnabled()) {
			logger.info("Searching for users...");
		}
		for (Record record : recordReader) {
			if (record.getType() == Record.Type.REVISION) {
				String user = ChangeParser.getSubversionUser(record);
				if (!users.containsKey(user)) {
					String clean = ProcessUser.filter(user);
					users.put(user, clean);
				}
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
		comment.append("# Original Subversion user names are listed on the left-hand-side\n");
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
