package com.perforce.svn.parser;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import com.perforce.svn.asset.SvnContentReader;
import com.perforce.svn.parser.Record.Type;

public class SubversionWriter {

	private OutputStream bs = null;
	private OutputStreamWriter out = null;
	private boolean hide = false;

	// Constructor
	public SubversionWriter(String filename, boolean hide) throws Exception {
		bs = new BufferedOutputStream(new FileOutputStream(filename));
		out = new OutputStreamWriter(bs, "UTF8");
		this.hide = hide;
	}

	public void putRecord(Record record) throws IOException {

		if (!record.isSubBlock()) {
			if (record.getType() != Type.SCHEMA) {
				if (record.getSvnRevision() != 0) {
					out.write("\n");
				}
			}
		}
		out.write(record.getHeader().toString());
		if (record.getProperty() != null) {
			out.write(record.getProperty().toString());

			if (record.getProperty().isEmpty()
					&& (!record.getHeader().hasKey("Text-content-length")))
				out.write("\n");
		}
		Content content = record.getContent();
		if (content != null && content.isBlob()) {
			SvnContentReader in = new SvnContentReader(content);
			byte[] block = in.nextBlock();
			while (block != null) {
				if (!hide) {
					out.write(new String(block));
				} else {
					out.write("[block:" + block.length + "] ");
				}
				block = in.nextBlock();
			}
			out.write("\n");
			in.close();
		}
		if (record.isSubBlock()) {
			out.write("\n\n");
		}
	}

	public void seperator(String msg) throws IOException {
		out.write("---- " + msg + " ------------------------------------\n");
		out.flush();
	}

	public void flush() throws IOException {
		out.write("\n");
		out.flush();
	}

	public void close() throws IOException {
		out.close();
	}
}
