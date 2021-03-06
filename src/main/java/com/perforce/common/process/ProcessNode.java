package com.perforce.common.process;

import java.net.URLDecoder;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.asset.TypeMap;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.node.Action;
import com.perforce.config.ConfigException;
import com.perforce.svn.change.ChangeInterface;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.history.RevisionTree.NodeType;
import com.perforce.svn.parser.Content;
import com.perforce.svn.process.MergeSource;
import com.perforce.svn.query.QueryInterface;

public abstract class ProcessNode {

	private Logger logger = LoggerFactory.getLogger(ProcessNode.class);

	private ChangeAction lastAction;
	private DepotInterface depot;
	private QueryInterface query;

	protected ChangeInterface changelist;

	public ProcessNode(DepotInterface depot) throws Exception {
		this.depot = depot;
		this.query = ProcessFactory.getQuery(depot);
	}

	/**
	 * Public static method to process node actions from the parsed Subversion
	 * Dumpfile
	 * 
	 * @throws Exception
	 */
	public void process() throws Exception {
		// find node type
		NodeType nodeType = getNodeType();

		switch (nodeType) {
		case FILE:
			processFile();
			break;

		case DIR:
			processDir();
			break;

		default:
			throw new ConverterException("Node-type(" + nodeType + ")");
		}
	}

	protected void processFile() throws Exception {
		logger.error("common.ProcessNode.processFile() should be extended");
		throw new Exception();
	}

	protected void processDir() throws Exception {
		logger.error("common.ProcessNode.processDir() should be extended");
		throw new Exception();
	}

	protected NodeType getNodeType() throws Exception {
		logger.error("common.ProcessNode.getNodeType() should be extended");
		throw new Exception();
	}

	protected List<ContentProperty> getContentProp() throws Exception {
		logger.error("common.ProcessNode.getContentProp() should be extended");
		throw new Exception();
	}

	protected void setContentProp(String path, Content content) throws Exception {
		// check typemap for properties
		List<ContentProperty> typemapProps;
		typemapProps = TypeMap.getContentProperty(path);

		// check for file properties
		List<ContentProperty> contentProps = getContentProp();

		// add typemap and file properties
		contentProps.addAll(typemapProps);

		// Set properties or use previous
		if (content.isBlob() && contentProps.isEmpty()) {
			ChangeAction last = getLastAction(path);
			if (last != null) {
				content.setProps(last.getProps());
			}
		} else {
			content.setProps(contentProps);
		}
	}

	protected ChangeAction getLastAction(String path) throws Exception {
		if (lastAction == null) {
			// find last action using path and Perforce change number
			long change = changelist.getChange();
			ChangeAction last = query.findLastAction(path, change);
			lastAction = last;
		}
		return lastAction;
	}

	/**
	 * Console output for current node action
	 * 
	 * @param action
	 * @param nodeType
	 * @param path
	 * @param content
	 * @param subBlock
	 */
	protected void verbose(long change, int id, Action action, NodeType nodeType, String path, Content content,
			boolean subBlock) {
		if (logger.isInfoEnabled()) {
			StringBuffer sb = new StringBuffer();
			sb.append(change + "." + id + " ");
			if (subBlock)
				sb.append("SUB:");
			sb.append(action.toString().charAt(0));
			sb.append(":" + nodeType.toString().charAt(0));
			sb.append(" - " + path);
			if (content != null) {
				ContentType t = content.getType();
				sb.append(" (" + t.getName());

				if (content.getProps() != null) {
					if (content.getProps().isEmpty() == false) {
						sb.append("+");
						for (ContentProperty p : content.getProps()) {
							sb.append(p.toString());
						}
					}
				}
				sb.append(")");
			}

			logger.info(sb.toString());
		}
	}

	/**
	 * Calculate merge action based on content
	 * 
	 * @param from
	 * @param content
	 * @param action
	 */
	protected void processMergeCredit(MergeSource from, Content content, Action action) {

		String fromMD5 = null;
		ChangeAction fromNode = from.getFromNode();
		if (fromNode != null) {
			fromMD5 = fromNode.getMd5();
		}

		switch (action) {

		case MERGE:
			if (content.isBlob()) {
				String resultMD5 = content.getMd5();
				if (resultMD5.equals(fromMD5)) {
					from.setMergeAction(Action.MERGE_COPY);
				} else {
					from.setMergeAction(Action.MERGE_EDIT);
				}
			} else {
				from.setMergeAction(Action.MERGE_IGNORE);
			}
			break;
		case BRANCH:
			if (content.isBlob()) {
				from.setMergeAction(Action.MERGE_EDIT);
			} else {
				from.setMergeAction(Action.BRANCH);
			}
			break;
		default:
			from.setMergeAction(action);
		}
	}

	/**
	 * Remove strange chars from path such as: "%, @, # and *" replacing them
	 * with the URL coded version
	 * 
	 * @param path
	 * @return
	 * @throws ConfigException
	 */
	protected String formatPath(String path) throws ConfigException {
		if (path != null && !path.isEmpty()) {

			// Manage URL encoded paths
			if (path.contains("%")) {
				try {
					// if it decodes OK, don't replace '%'
					path = URLDecoder.decode(path, "UTF-8");
				} catch (Exception e) {
				}
				// must replace before other '@' and '#' symbols
				path = path.replace("%", "%25");
			}

			path = path.replace("@", "%40");
			path = path.replace("#", "%23");

			// job057825: Windows OS can't support '*' in filename
			if (path.contains("*")) {
				String os = System.getProperty("os.name").toLowerCase();
				if (os.contains("win")) {
					StringBuffer sb = new StringBuffer();
					sb.append("Windows does not support asterisk in filenames, ");
					sb.append("replacing '*' with tag '_ASTERISK_'.");
					logger.warn(sb.toString());
					path = path.replace("*", "_ASTERISK_");
				} else {
					path = path.replace("*", "%2A");
				}
			}

			if (path.contains("...")) {
				StringBuffer sb = new StringBuffer();
				sb.append("Perforce does not allow '...' in filenames, ");
				sb.append("replacing '...' with tag '_ELLIPSIS_'.");
				logger.warn(sb.toString());
				path = path.replace("...", "_ELLIPSIS_");
			}

			path = getQuery().getPath(path);
		}
		return path;
	}

	protected QueryInterface getQuery() {
		return query;
	}

	public DepotInterface getDepot() {
		return depot;
	}
}
