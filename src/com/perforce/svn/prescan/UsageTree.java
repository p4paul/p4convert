package com.perforce.svn.prescan;

import java.util.HashMap;
import java.util.Map;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.svn.history.Action;

public class UsageTree {

	public enum UsageType {
		FILE, DIR, UNKNOWN
	}

	private String name;
	private String path;
	private long count;
	private long headCount;

	private UsageTree parent;
	private Map<String, UsageTree> children = new HashMap<String, UsageTree>();

	public UsageTree(String name) {
		this.name = name;
	}

	public UsageTree add(String toPath, String fromPath, UsageType nodeType,
			Action action) {

		UsageTree node = null;

		// [FILE]
		if (nodeType == UsageType.FILE) {
			String dirPath = "";
			String[] bits = toPath.split("/");
			dirPath = bits[0];
			for (int i = 1; i < (bits.length - 1); i++) {
				dirPath += "/" + bits[i];
			}

			node = create(dirPath, nodeType);
			node.setHeadCount(1);
			node.setCount(1);
		}

		// [DIR]
		else {
			node = create(toPath, nodeType);

			if (action == Action.REMOVE) {
				long files = node.getHeadCount();
				node.clearHeadCount();
				node.setCount(files);
			} else if (action == Action.BRANCH) {
				UsageTree fromNode = this.getNode(fromPath);
				long files = 0;
				if (fromNode != null) {
					files = fromNode.getHeadCount();
				}
				node.setHeadCount(files);
				node.setCount(files);
			}
		}

		return node;
	}

	// create node
	private UsageTree create(String path, UsageType nodeType) {
		UsageTree node = this;

		// does node already exist?
		UsageTree nodeFind = this.getNode(path);
		if (nodeFind != null) {
			node = nodeFind;
		} else {
			// create nodes to link whole path
			for (String pathBit : path.split("/")) {
				if (!pathBit.isEmpty()) {
					UsageTree nodeBit = node.getNode(pathBit);
					if (nodeBit == null) {
						String nameBit = pathBit;

						nodeBit = new UsageTree(nameBit);
						nodeBit.setParent(node);
						node.getChildren().put(pathBit, nodeBit);
					}
					node = nodeBit;
				}
			}
			node.setPath(path);
		}

		// Count file revision actions
		if (nodeType == UsageType.FILE) {
			node.setCount(1);
		}

		return node;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		// File nodes store paths (but not folders)
		if (path != null)
			return path;

		// Build path from all, but last parent
		else {
			String join = name;
			UsageTree next = getParent();
			while (next.getParent() != null) {
				join = next.getName() + "/" + join;
				next = next.getParent();
			}
			return join;
		}
	}

	public void setPath(String path) {
		this.path = path;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long files) {
		this.count += files;
	}

	public long getHeadCount() {
		long total = headCount;

		if (children != null) {
			for (UsageTree node : children.values()) {
				total += node.getHeadCount();
			}
		}
		return total;
	}

	public void clearHeadCount() {
		headCount = 0;

		if (children != null) {
			for (UsageTree node : children.values()) {
				node.clearHeadCount();
			}
		}
	}

	public void setHeadCount(long files) {
		this.headCount += files;
		if (this.headCount < 0) {
			this.headCount = 0;
		}
	}

	public UsageTree getParent() {
		return parent;
	}

	public void setParent(UsageTree parent) {
		this.parent = parent;
	}

	public Map<String, UsageTree> getChildren() {
		return children;
	}

	public void setChildren(Map<String, UsageTree> children) {
		this.children = children;
	}

	/**
	 * Returns the node point in the tree based on the given path
	 */
	public UsageTree getNode(String path) {
		UsageTree node = this;
		if (path == null)
			return node;

		for (String pathBit : path.split("/")) {
			node = node.getChildren().get(pathBit);
			if (node == null)
				break;
		}
		return node;
	}

	/**
	 * toString and recursive toString method to indent and draw a tree of
	 * actions
	 */
	@Override
	public String toString() {
		return toString(0, new StringBuffer()).toString();
	}

	private String toString(int indent, StringBuffer sb) {
		String spaces = "";
		if (indent > 0) {
			spaces = String.format("%" + indent + "s", "");
		}
		sb.append("\t" + spaces + "+ " + name);
		sb.append("\n");

		int depth = 0;
		try {
			depth = (int) Config.get(CFG.SVN_LABEL_DEPTH);
		} catch (ConfigException e) {
		}

		if (children != null && indent < depth) {
			for (UsageTree node : children.values()) {
				node.toString(indent + 1, sb);
			}
		}
		return sb.toString();
	}

	public long toCount() {
		long total = getCount();

		if (children != null) {
			for (UsageTree node : children.values()) {
				total += node.toCount();
			}
		}
		return total;
	}
}
