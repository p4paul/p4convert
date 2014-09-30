package com.perforce.cvs.parser;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.perforce.cvs.parser.rcstypes.RcsObjectBlock;

public class RcsDeltaAction {

	private RcsDeltaType action = RcsDeltaType.TEXT;
	private int line;
	private int length;
	private RcsObjectBlock block = new RcsObjectBlock();

	public RcsDeltaAction(ByteArrayOutputStream buf) {
		//TODO may need to use a Charset for translation?
		String str = buf.toString();

		Pattern r = Pattern.compile("^(a|d)(\\d+) (\\d+)");
		Matcher m = r.matcher(str);
		if (m.find()) {
			// set action
			if (m.group(1).contains("a"))
				action = RcsDeltaType.ADD;
			if (m.group(1).contains("d"))
				action = RcsDeltaType.DELETE;

			// set line and length
			line = Integer.parseInt(m.group(2));
			length = Integer.parseInt(m.group(3));
		}
	}

	public void addLine(ByteArrayOutputStream line) {
		block.add(line);
	}

	public RcsObjectBlock getBlock() {
		return block;
	}

	public RcsDeltaType getAction() {
		return action;
	}

	public int getLine() {
		return line;
	}

	public int getLength() {
		return length;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(action + ":" + line + ":" + length);
		return sb.toString();
	}
}
