package com.perforce.cvs.parser.rcstypes;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RcsObjectBlock implements Iterable<ByteArrayOutputStream> {

	private Logger logger = LoggerFactory.getLogger(RcsObjectBlock.class);
	private ArrayList<ByteArrayOutputStream> lines = new ArrayList<ByteArrayOutputStream>();

	public RcsObjectBlock() {
	}

	public RcsObjectBlock(RcsObjectBlock block) {
		lines.addAll(block.getLines());
	}

	public int size() {
		return lines.size();
	}

	public void add(ByteArrayOutputStream line) {
		lines.add(line);
	}

	public void insert(int line, RcsObjectBlock block) {
		lines.addAll(line, block.getLines());
	}

	public void remove(int line, int count) {
		while (count > 0) {
			lines.remove(line - 1);
			count--;
		}
	}

	public void clean() {
		ArrayList<ByteArrayOutputStream> clean = new ArrayList<ByteArrayOutputStream>();
		for (ByteArrayOutputStream line : lines) {
			if (line != null) {
				clean.add(line);
			}
		}
		lines = clean;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("\n");
		int sum = 0;
		int count = 0;
		for (ByteArrayOutputStream line : lines) {
			if (line != null) {
				if(logger.isTraceEnabled()) {
					sb.append("\t" + count + ": ");
					sb.append(line);
					count++;
				}
				sum += line.size();
			}
		}
		sb.append("total["  + lines.size() + "] " + sum + "\n");
		return sb.toString();
	}

	public ArrayList<ByteArrayOutputStream> getLines() {
		return lines;
	}

	@Override
	public Iterator<ByteArrayOutputStream> iterator() {
		return lines.iterator();
	}
}
