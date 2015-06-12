package com.perforce.common.journal;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;

public class JournalWriter {

	private OutputStream bs = null;
	private OutputStreamWriter out = null;

	// Constructor
	public JournalWriter(String filename) {
		try {
			bs = new BufferedOutputStream(new FileOutputStream(filename));
			out = new OutputStreamWriter(bs, "UTF8");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public void write(String journal) {
		try {
			Form form = (Form) Config.get(CFG.P4_NORMALISATION);
			journal = Normalizer.normalize(journal, form);
			out.write(journal);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ConfigException e) {
			e.printStackTrace();
		}
	}

	public void write(ArrayList<String> journals) {
		for (String journal : journals) {
			write(journal);
		}
	}

	public void flush() {
		try {
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
