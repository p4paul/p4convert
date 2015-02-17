package com.perforce.svn.parser;

import java.util.Iterator;

import com.perforce.common.parser.LineReader;

public class SubversionReader extends LineReader implements Iterator<Record> {

	private Record currentValue;
	private int currentChangeNumber = 0;
	private int currentNodeNumber = 0;
	private boolean subBlock = false;

	// Constructor
	public SubversionReader(String path) {
		open(path);
	}

	@Override
	public Record next() {
		RecordStateTrace.update(currentValue);
		return currentValue;
	}

	@Override
	public boolean hasNext() {

		String line = new String();
		int readlines = 0;
		do {
			line = getLine();
			if (line == null) {
				return false;
			}
			readlines++;
		} while (line.isEmpty());

		String[] args = line.split(": ");
		String token = args[0];

		NodeEntry entry = NodeEntry.parse(token);

		switch (entry) {
		case REVISION:
			try {
				Revision revision = new Revision(line, this);
				currentChangeNumber = (int) revision
						.findHeaderLong("Revision-number");
				revision.setChangeNumber(currentChangeNumber);
				currentNodeNumber = 0;
				currentValue = revision;
				subBlock = false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;

		case NODE:
		case TEXT:
		case PROP:
		case CONTENT:
			try {
				if (subBlock)
					subBlock = false;
				if (readlines == 1)
					subBlock = true;

				Node node = new Node(line, this);
				node.setSvnRevision(currentChangeNumber);
				node.setNodeNumber(currentNodeNumber++);

				if (node.isSubBlock())
					node.setSubBlock(false);
				if (readlines == 1)
					node.setSubBlock(true);

				currentValue = node;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return true;

		default:
			try {
				Schema schema = new Schema(line, this);
				currentValue = schema;
			} catch (ParserException e) {
				e.printStackTrace();
			}
			return true;
		}
	}

	@Override
	public void remove() {
	}
}
