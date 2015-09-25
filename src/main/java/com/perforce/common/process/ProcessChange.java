package com.perforce.common.process;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.ExitCode;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.UserMapping;
import com.perforce.config.Version;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.change.ChangeMap;

public abstract class ProcessChange implements Callable<Integer> {

	private Logger logger = LoggerFactory.getLogger(ProcessChange.class);

	protected long revStart;
	protected long revEnd;
	protected boolean isLabels;
	protected ProcessLabel processLabel;

	private ChangeInterface currentChange = null;
	private boolean stop = false;
	private boolean clean = true;
	private ExitCode runState = ExitCode.OK;

	/**
	 * This method is extended by the specific implementation of ProcessChange
	 * 
	 * @throws Exception
	 */
	protected void processChange() throws Exception {
		logger.error("common.ProcessChange.processChange() should be extended");
		throw new RuntimeException();
	}

	/**
	 * Core run method for conversions
	 * 
	 */
	@Override
	public Integer call() throws Exception {
		try {
			// register exception hook
			Runtime.getRuntime().addShutdownHook(new CleanShutdown());

			// Initialise common environment
			processInit();

			// Call processChange implementation
			processChange();

			// log summary
			if (logger.isInfoEnabled()) {
				String summary = Stats.summary(currentChange.getChange());
				logger.info(summary);
			}

			// check for warnings
			long warn = Stats.getLong(StatsType.warningCount);
			if (warn > 0) {
				runState = ExitCode.WARNING;
			}

		} catch (Throwable e) {
			// catch any remaining throws
			logger.error("Caught exception on exit", e);
			runState = ExitCode.EXCEPTION;
		} finally {
			// save state and shutdown cleanly
			saveState();
			clean = true;
		}

		return runState.value();
	}

	/**
	 * Runs conversion as single threaded (blocking) Used for UI and test cases
	 * 
	 * @throws Throwable
	 */
	public void runSingle() throws Exception {
		// Initialise common environment
		processInit();

		// Call processChange implementation
		processChange();

		// save state and shutdown cleanly
		saveState();
	}

	private void processInit() throws Exception {
		// Setup stats counters
		Stats.setDefault();

		// Log version of jar file
		Version ver = new Version();
		logger.info("jar build version: \t" + ver.getVersion());

		// Check JRE for symlink support
		if (Config.isImportMode()) {
			String javaVer = System.getProperty("java.version");
			logger.info("java.version:\t\t" + javaVer);
			if (!javaVer.startsWith("1.7")) {
				throw new RuntimeException("JRE 1.7.x required for Import mode");
			}
		}

		// Set unicode handling for p4-java
		System.setProperty("com.perforce.p4java.defaultCharset", "UTF-8");

		// Initialise changeMap tables from existing file
		String changeMapFile = (String) Config.get(CFG.CHANGE_MAP);
		ChangeMap.load(changeMapFile);

		// Initialise user mapping, if required
		String userMapFile = (String) Config.get(CFG.USER_MAP);
		UserMapping.load(userMapFile);

		// Initialise type map, if required
		String typeMapFile = (String) Config.get(CFG.TYPE_MAP);
		TypeMap.load(typeMapFile);

		// Initialise path map, if required
		String pathMapFile = (String) Config.get(CFG.PATH_MAP);
		boolean isPathMap = PathMapTranslator.load(pathMapFile);

		// else, use default path translation map
		if (!isPathMap) {
			PathMapTranslator.setDefault();
		}

		// Report operation mode
		if (Config.isImportMode()) {
			logger.info("conversion mode: \tIMPORT (front-door)\n");
		} else {
			logger.info("conversion mode: \tCONVERT (back-door)\n");
		}
	}

	/**
	 * Submit current pending change or delete if empty changelist
	 * 
	 * @throws Exception
	 */
	public void submit() throws Exception {

		boolean skip = (Boolean) Config.get(CFG.P4_SKIP_EMPTY);

		// Submit or delete the current pending change.
		if (currentChange != null) {
			// update stats
			Stats.addUser(currentChange.getUser());

			// check if current change has revisions
			int revCount = currentChange.getNumberOfRevisions();
			long change = currentChange.getChange();
			if (skip && revCount == 0) {
				currentChange.delete();
			} else {
				long c = currentChange.submit();
				change = (c > change) ? c : change;
			}

			// logging details
			Stats.inc(StatsType.currentRevision);
			if (logger.isInfoEnabled()) {
				StringBuffer log = new StringBuffer();
				log.append("mapping: r" + currentChange.getSvnRevision());
				log.append(" => @" + change + "\n");
				logger.info(log.toString());
			}

			// map change to subversion revision
			ChangeMap.add(currentChange.getSvnRevision(), change);
		}

		// Submit any labels
		if (isLabels && processLabel != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Submitting labels:");
				logger.trace(processLabel.toString());
			}
			processLabel.submit();
		}
	}

	/**
	 * Closes the journal and flushes to disk, typically at the end of
	 * conversion
	 * 
	 * @throws Exception
	 */
	public void close() throws Exception {
		currentChange.close();
	}

	/**
	 * Private method to save thread state on exit.
	 * 
	 */
	private void saveState() {
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Saving changeMap...");
			}
			ChangeMap.store((String) Config.get(CFG.CHANGE_MAP));
		} catch (Exception e) {
			logger.error("Unable to saving changeMap", e);
		}
	}

	/**
	 * Private shutdown hook to exit cleanly after exception.
	 * 
	 */
	private class CleanShutdown extends Thread {
		public void run() {
			stop = true;
			runState = ExitCode.SHUTDOWN;
			if (logger.isInfoEnabled()) {
				logger.info("Caught EXIT shutting down ...");
			}
			while (!clean) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.error("InterruptedException", e);
				}
			}
		}
	}

	public boolean isStop() {
		return stop;
	}

	public void setClean(boolean clean) {
		this.clean = clean;
	}

	public ChangeInterface getCurrentChange() {
		return currentChange;
	}

	public void setCurrentChange(ChangeInterface currentChange) {
		this.currentChange = currentChange;
	}

	protected void setChangeRange(long revLast) throws Exception {
		// Test start and end revisions
		if (revStart > revLast || revEnd > revLast) {
			String err = "Specified revision range exceeds last revision";
			logger.error(err);
			throw new ConverterException(err);
		}

		// Auto set end revision
		if (revLast > 0 && revEnd == 0) {
			Config.set(CFG.P4_END, revLast);
			revEnd = revLast;
		}

		// Test start vs end range
		if (revStart > revEnd) {
			String err = "Specified start revision exceeds end revision";
			logger.error(err);
			throw new ConverterException(err);
		}

		// Log import range
		if (logger.isInfoEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append("importing revs: \t");
			sb.append(revStart + " to " + revEnd);
			sb.append(" out of " + revLast);
			logger.info(sb.toString());
		}
	}
}
