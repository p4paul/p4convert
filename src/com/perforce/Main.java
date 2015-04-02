package com.perforce;

import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.ExitCode;
import com.perforce.common.asset.ContentType;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ScmType;
import com.perforce.config.Version;
import com.perforce.cvs.prescan.CvsExtractUsers;
import com.perforce.cvs.process.CvsProcessChange;
import com.perforce.svn.parser.Record;
import com.perforce.svn.parser.SubversionWriter;
import com.perforce.svn.prescan.ExcludeParser;
import com.perforce.svn.prescan.ExtractRecord;
import com.perforce.svn.prescan.LastRevision;
import com.perforce.svn.prescan.SvnExtractUsers;
import com.perforce.svn.prescan.UsageParser;
import com.perforce.svn.process.SvnProcessChange;
import com.perforce.svn.tag.TagParser;

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
		// Process arguments
		ExitCode exit = processArgs(args);
		System.exit(exit.value());
	}

	public static ExitCode processArgs(String[] args) throws Exception {
		// Configure arguments
		CommandLineParser parser = new GnuParser();
		Options options = new Options();
		options.addOption("v", "version", false, "Version string");
		options.addOption("c", "config", true, "Use configuration file");
		options.addOption("t", "type", true, "SCM type (CVS | SVN)");
		options.addOption("d", "default", false,
				"Generate a configuration file");
		options.addOption("r", "repo", true, "Repository file/path");
		options.addOption(null, "tree", true,
				"(with --info), display tree to specified depth");
		options.addOption(null, "tags", true, "find tags to specified depth");
		options.addOption("i", "info", false, "Report on repository usage");
		options.addOption("u", "users", false, "List repository users");
		options.addOption("e", "extract", true, "Extract a revision");
		options.addOption("S", "start", true,
				"Start revision, for incremental (SVN)");
		options.addOption("E", "end", true,
				"End revision, for incremental (SVN)");

		// Process arguments
		ExitCode exit = ExitCode.USAGE;
		try {
			CommandLine line = parser.parse(options, args);
			exit = processOptions(line);
		} catch (UnrecognizedOptionException e) {
			logger.warn(e.getLocalizedMessage() + "\n");
		}

		if (exit.equals(ExitCode.USAGE)) {
			StringBuffer sb = new StringBuffer();
			sb.append("\nExample: standard usage.\n");
			sb.append("\t java -jar p4convert.jar --config=myFile.cfg\n\n");
			sb.append("Example: generate a CVS configuration file.\n");
			sb.append("\t java -jar p4convert.jar --type=CVS --default\n\n");
			sb.append("Example: report Subversion repository usage.\n");
			sb.append("\t java -jar p4convert.jar --type=SVN --repo=/path/to/repo.dump --info\n\n");

			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar p4convert.jar", options);
			logger.info(sb.toString());
		}
		return exit;
	}

	private static ExitCode processOptions(CommandLine line) throws Exception {
		ScmType scmType;
		String repoPath;
		String configFile;
		int treeDepth = 0;

		// Find version from manifest
		Version ver = new Version();

		// Load default configuration and add version
		Config.setDefault();
		Config.set(CFG.VERSION, ver.getVersion());

		// --version
		if (line.hasOption("version")) {
			System.out.println(ver.getVersion() + "\n");
			return ExitCode.OK;
		}

		// --config=VALUE
		if (line.hasOption("config")) {
			configFile = line.getOptionValue("config");
			Config.load(configFile);
			return startConversion(line);
		}

		// --type=VALUE (REQUIRED for all the following options)
		if (line.hasOption("type")) {
			scmType = ScmType.parse(line.getOptionValue("type"));
			Config.set(CFG.SCM_TYPE, scmType);
		} else {
			return ExitCode.USAGE;
		}

		// --default
		if (line.hasOption("default")) {
			Config.store(defaultFile, scmType);
			return ExitCode.OK;
		}

		// --repo=VALUE (REQUIRED for all the following options)
		if (line.hasOption("repo")) {
			repoPath = line.getOptionValue("repo");
			switch (scmType) {
			case SVN:
				Config.set(CFG.SVN_DUMPFILE, repoPath);
				setEndRevision(repoPath);
				break;
			case CVS:
				Config.set(CFG.CVS_ROOT, repoPath);
				break;
			default:
			}
		} else {
			return ExitCode.USAGE;
		}

		// --tree=VALUE
		if (line.hasOption("tree")) {
			treeDepth = Integer.parseInt(line.getOptionValue("tree"));
		}

		// --info
		if (line.hasOption("info")) {
			switch (scmType) {
			case SVN:
				Config.set(CFG.SVN_PROP_TYPE, ContentType.P4_BINARY);
				prescanStats(repoPath, treeDepth);
				return ExitCode.OK;
			default:
				return ExitCode.USAGE;
			}
		}

		// --tags=VALUE
		if (line.hasOption("tags")) {
			int depth = Integer.parseInt(line.getOptionValue("tags"));
			switch (scmType) {
			case SVN:
				Config.set(CFG.SVN_PROP_TYPE, ContentType.P4_BINARY);
				prescanTags(repoPath, depth);
				return ExitCode.OK;
			default:
				return ExitCode.USAGE;
			}
		}

		// --users
		if (line.hasOption("users")) {
			String mapFile = (String) Config.get(CFG.USER_MAP);
			switch (scmType) {
			case SVN:
				Config.set(CFG.SVN_PROP_TYPE, ContentType.P4_BINARY);
				SvnExtractUsers.store(repoPath, mapFile);
				return ExitCode.OK;
			case CVS:
				CvsExtractUsers.store(repoPath, mapFile);
				return ExitCode.OK;
			default:
				return ExitCode.USAGE;
			}
		}

		// --extract=VALUE
		if (line.hasOption("extract")) {
			String node = line.getOptionValue("extract");
			switch (scmType) {
			case SVN:
				extractNode(repoPath, node);
				return ExitCode.OK;
			default:
				return ExitCode.USAGE;
			}
		}

		return ExitCode.USAGE;
	}

	private static ExitCode startConversion(CommandLine line) throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(1);

		Callable<Integer> callable = null;
		switch ((ScmType) Config.get(CFG.SCM_TYPE)) {
		case SVN:
			// override if --repo flag is used
			if (line.hasOption("repo")) {
				String repoPath = line.getOptionValue("repo");
				logger.debug("... flag: --repo=" + repoPath);
				Config.set(CFG.SVN_DUMPFILE, repoPath);
			}

			// override if --start flag is used
			if (line.hasOption("start")) {
				long start = Long.parseLong(line.getOptionValue("start"));
				logger.debug("... flag: --start=" + start);
				Config.set(CFG.SVN_START, start);
			}

			// override if --end flag is used
			if (line.hasOption("end")) {
				long end = Long.parseLong(line.getOptionValue("end"));
				logger.debug("... flag: --end=" + end);
				Config.set(CFG.SVN_END, end);
			}

			callable = new SvnProcessChange();
			break;

		case CVS:
			// override if --repo flag is used
			if (line.hasOption("repo")) {
				String repoPath = line.getOptionValue("repo");
				logger.debug("... flag: --repo=" + repoPath);
				Config.set(CFG.CVS_ROOT, repoPath);
			}

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

	private static void setEndRevision(String dumpFile) throws Exception {
		// Revisions
		LastRevision rev = new LastRevision(dumpFile);
		String revLastString = rev.find();
		rev.close();

		if (revLastString == null) {
			String err = "Cannot find last revision in dumpfile";
			logger.error(err);
			throw new ConverterException(err);
		}

		// Run usage analysis
		long revLast = Long.parseLong(revLastString);
		Config.set(CFG.SVN_END, revLast);
	}

	private static void prescanTags(String dumpFile, int depth)
			throws Exception {

		Config.set(CFG.SVN_LABEL_DEPTH, depth);
		Config.set(CFG.SVN_LABEL_FORMAT, "");

		ExcludeParser.preload("tags/.*");

		TagParser.parse(dumpFile);
		logger.info(TagParser.toLog());
	}

	private static void prescanStats(String dumpFile, int depth)
			throws Exception {

		String line = "--------------------------------------------------------------------------------";

		// Run usage analysis
		UsageParser usage = new UsageParser(dumpFile);
		long revLast = (long) Config.get(CFG.SVN_END);
		int pathLength = usage.getPathLength();
		long emptyNodes = usage.getEmptyNodes();
		long revs = usage.getTree().toCount();
		float mem = (float) (revs * 256.0 / 1024 / 1024 / 1024);

		// Build report
		StringBuffer sb = new StringBuffer();
		sb.append(line + "\n");
		sb.append("\t\t\t\t" + "Information Summary" + "\n");
		sb.append(line + "\n\n");
		sb.append("\tSubversion revisions: \t" + revLast + "\n");
		sb.append("\tLongest path: \t\t" + pathLength + "\n");
		sb.append("\tEmpty nodes: \t\t" + emptyNodes + "\n");
		sb.append("\tFile revision count: \t" + revs + "\n");

		StringBuilder memsb = new StringBuilder();
		Formatter formatter = new Formatter(memsb, Locale.UK);
		formatter.format("\tEstimated memory: \t%.2f GBytes", mem);
		formatter.close();
		sb.append(memsb + "\n\n");
		sb.append(line + "\n");

		// Build tree (if required)
		if (depth > 0) {
			Config.set(CFG.SVN_LABEL_DEPTH, depth);
			sb.append("\t\t\t\t" + "Tree Structure" + "\n");
			sb.append(line + "\n");
			sb.append(usage.getTree().toString());
			sb.append(line + "\n\n");
		}

		logger.info(sb.toString());

		if (pathLength > 216) {
			logger.warn("Long paths, check overall pathlength to Perforce depot!");
		}
	}
}
