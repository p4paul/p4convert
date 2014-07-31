package com.perforce.svn;

import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.ExitCode;
import com.perforce.common.Usage;
import com.perforce.common.asset.ContentType;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ScmType;
import com.perforce.config.Version;
import com.perforce.cvs.process.CvsProcessChange;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.SubversionWriter;
import com.perforce.svn.prescan.ExtractRecord;
import com.perforce.svn.prescan.ExtractUsers;
import com.perforce.svn.prescan.LastRevision;
import com.perforce.svn.prescan.UsageParser;
import com.perforce.svn.process.SvnProcessChange;

public class Main {

	private static Logger logger = LoggerFactory.getLogger(Main.class);

	public static String defaultFile = "default.cfg";

	/**
	 * MAIN
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		ExitCode code = processArgs(args);
		System.exit(code.value());
	}

	/**
	 * Process arguments method (TODO improve argument parsing) Access is
	 * 'public' to allow calls by test cases.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static ExitCode processArgs(String[] args) throws Exception {

		// Find version from manifest
		Version ver = new Version();

		// Load default configuration and add version
		Config.setDefault();
		Config.set(CFG.VERSION, ver.getVersion());

		// Load configuration (may update logger as well)
		if (args.length == 1) {
			String arg = args[0];
			if (arg.contentEquals("--version")) {
				System.out.println(ver.getVersion() + "\n");
				return ExitCode.OK;
			} else {
				Config.load(arg);
			}
		}

		else if (args.length == 2) {
			String arg = args[0];
			String opt1 = args[1];
			if (arg.contentEquals("--users")) {
				Config.set(CFG.SVN_PROP_TYPE, ContentType.P4_BINARY);
				String mapFile = (String) Config.get(CFG.USER_MAP);
				ExtractUsers.store(opt1, mapFile);
				System.exit(ExitCode.OK.value());
			}

			else if ("--info".equals(arg)) {
				Config.set(CFG.SVN_PROP_TYPE, ContentType.P4_BINARY);
				prescanStats(opt1);
				return ExitCode.OK;
			}

			else if ("--config".equals(arg)) {
				Config.store(defaultFile, ScmType.parse(opt1));
				return ExitCode.OK;
			}

			else {
				Usage.print();
				return ExitCode.USAGE;
			}
		}

		// check for extraction
		else if (args.length == 3) {
			String arg = args[0];
			String nodepoint = args[1];
			String dumpFile = args[2];
			if ("--extract".equals(arg)) {
				extractNode(dumpFile, nodepoint);
				return ExitCode.OK;
			} else {
				Usage.print();
				return ExitCode.USAGE;
			}
		}

		else {
			Usage.print();
			return ExitCode.USAGE;
		}

		// Start conversion
		ExecutorService executor = Executors.newFixedThreadPool(1);

		Callable<Integer> callable = null;
		switch ((ScmType) Config.get(CFG.SCM_TYPE)) {
		case SVN:
			callable = new SvnProcessChange();
			break;
		case CVS:
			callable = new CvsProcessChange();
			break;
		default:
			logger.error("SCM type not specified in config ().");
			System.exit(ExitCode.USAGE.value());
		}
		Future<Integer> submit = executor.submit(callable);

		ExitCode exit = ExitCode.parse(submit.get());
		return exit;
	}

	private static void extractNode(String dumpFile, String nodepoint)
			throws Exception {
		ExtractRecord extract = new ExtractRecord(dumpFile);
		String[] splits = nodepoint.split("\\.");
		if (splits.length == 2) {
			// Parse rev and node values
			int rev = Integer.parseInt(splits[0]);
			int node = Integer.parseInt(splits[1]);

			// Find the record
			List<Record> records = extract.findNode(rev, 0, node);

			// Store and show results
			if (records != null) {
				String filename = "node." + nodepoint + ".dump";
				SubversionWriter out = new SubversionWriter(filename, false);
				for (Record r : records) {
					out.putRecord(r);
					if (logger.isInfoEnabled()) {
						logger.info(r.toString());
					}
				}
				out.flush();
				out.close();
			} else {
				logger.warn("record not found.\n");
			}
		} else {
			logger.warn("syntax error '" + nodepoint + "'.\n");
		}
	}

	private static void prescanStats(String dumpFile) throws Exception {
		Config.set(CFG.SVN_DUMPFILE, dumpFile);
		if (logger.isInfoEnabled()) {
			logger.info("Scanning: " + dumpFile);
		}

		// Revisions
		LastRevision rev = new LastRevision(dumpFile);
		String revLastString = rev.find();
		rev.close();

		if (revLastString == null) {
			String err = "Cannot find last revision in dumpfile";
			logger.error(err);
			throw new ConverterException(err);
		}

		if (logger.isInfoEnabled()) {
			long revLast = Long.parseLong(revLastString);
			logger.info("   subversion revisions: \t" + revLast);

			// Run usage analysis
			UsageParser usage = new UsageParser(dumpFile, revLast);

			int pathLength = usage.getPathLength();
			logger.info("   longest subversion path: \t" + pathLength);

			long emptyNodes = usage.getEmptyNodes();
			logger.info("   empty nodes: \t\t" + emptyNodes);

			long revs = usage.getTree().toCount();
			logger.info("   file revision count: \t" + revs);

			float mem = (float) (revs * 256.0 / 1024 / 1024 / 1024);

			StringBuilder sb = new StringBuilder();
			Formatter formatter = new Formatter(sb, Locale.UK);
			formatter.format("   estimated memory: \t\t%.2f GBytes", mem);
			formatter.close();
			logger.info(sb.toString());

			if (pathLength > 216) {
				logger.warn("Long paths, check overall pathlength to Perforce depot!");
			}
		}
	}
}
