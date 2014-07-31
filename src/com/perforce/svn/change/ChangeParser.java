package com.perforce.svn.change;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.config.UserMapping;
import com.perforce.svn.parser.Record;

public class ChangeParser {

	private static Logger logger = LoggerFactory.getLogger(ChangeParser.class);

	private static Date lastDate = new Date(0);

	public static void resetLastDate() {
		lastDate = new Date(0);
	}

	public static long getDateLong(Record record) throws Exception {
		return getDate(record).getTime() / 1000; // convert from ms to s
	}

	public static Date getDate(Record record) throws ParseException {
		Date date;
		String dateStr = record.findPropertyString("svn:date");

		if (dateStr == null) {
			date = lastDate;
		} else {
			// format date/time
			String format = "yyyy-MM-dd'T'HH:mm:ss";
			dateStr = dateStr.substring(0, 19);
			SimpleDateFormat dateFormat = new SimpleDateFormat(format);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			date = dateFormat.parse(dateStr);
			lastDate = date;
		}
		return date;
	}

	/**
	 * Returns the original user name used in Subversion
	 * 
	 * @param record
	 * @return
	 */
	public static String getSubversionUser(Record record) {
		String user = record.findPropertyString("svn:author");
		if (user == null) {
			user = "unknown";
		}
		return user;
	}

	public static String filterUser(String user) {
		// No choice here; any reserved characters used are purged.
		// Remove '@' and '#', but replace ' ' with '_'
		user = user.replace("@", "");
		user = user.replace("#", "");
		user = user.replace(" ", "_");
		user = user.replace("*", "");
		user = user.replace("%%", "");
		user = user.replace("...", "");

		if (user.isEmpty()) {
			user = "unknown";
		}

		return user;
	}

	/**
	 * Returns filtered user name used in Perforce. Filtering includes user map
	 * and reserved characters.
	 * 
	 * @param record
	 * @return
	 */
	public static String getUser(Record record) {
		String orig = getSubversionUser(record);

		// Rename users based on Mapping file
		String user = UserMapping.get(orig);

		// No choice here; any reserved characters used are purged.
		// Remove '@' and '#', but replace ' ' with '_'
		user = filterUser(user);

		if (logger.isTraceEnabled()) {
			logger.trace("username: " + orig + " => " + user);
		}
		return user;
	}

	public static int getSvnRevision(Record record) {
		return record.getSvnRevision();
	}

	public static String getDescription(Record record) throws ConfigException {
		return getLog(record, 0);
	}

	public static String getSummary(Record record) throws ConfigException {
		return getLog(record, 32);
	}

	private static String getLog(Record record, int len) throws ConfigException {
		String log = record.findPropertyString("svn:log");
		if (log == null) {
			return "empty";
		} else {
			if (log.isEmpty()) {
				log = "empty";
			}

			// Use the revision ID template to build log, where <description> is
			// substituted with the subversion log and <rev> with the subversion
			// revision.
			String template = (String) Config.get(CFG.P4_LOG_ID);
			if (template != null) {
				int rev = record.getSvnRevision();
				String newlog = template.replace("<rev>", "" + rev);
				log = newlog.replace("<description>", log);
			}

			if ((log.length() > len) && (len > 0)) {
				return log.substring(0, (len - 1));
			} else {
				return log;
			}
		}
	}
}
