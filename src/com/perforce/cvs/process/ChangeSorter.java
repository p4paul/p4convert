package com.perforce.cvs.process;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.perforce.cvs.RevisionSorter;

public class ChangeSorter {

	private static Logger logger = LoggerFactory.getLogger(ChangeSorter.class);

	private CvsChangeList changes;

	/**
	 * Build CVS Changelists from RCS revisions
	 * 
	 * @param revSort
	 * @throws Exception
	 */
	public void build(RevisionSorter revSort) throws Exception {
		logger.info("Sorting revision into changes:");

		changes = new CvsChangeList(revSort);

		logger.info("... found " + changes.size() + " changes\n");
	}

	/**
	 * Load CVS change lists from file
	 * 
	 * @param filename
	 * @throws Exception
	 */
	public void load(String filename) throws Exception {
		logger.info("Loading changes: " + filename);

		GsonBuilder gbuilder = new GsonBuilder();
		gbuilder.setPrettyPrinting();
		Gson gson = gbuilder.create();

		FileReader fr = new FileReader(filename);
		BufferedReader br = new BufferedReader(fr);
		changes = gson.fromJson(br, CvsChangeList.class);
		br.close();

		logger.info("... found " + changes.size() + " changes\n");
	}

	/**
	 * Save CVS change lists to file in JSON
	 * 
	 * @param filename
	 * @throws Exception
	 */
	public void store(String filename) throws Exception {
		logger.info("Saving changes to file: " + filename);

		GsonBuilder gbuilder = new GsonBuilder();
		gbuilder.setPrettyPrinting();
		Gson gson = gbuilder.create();

		String json = gson.toJson(changes);

		FileWriter fw = new FileWriter(filename);
		fw.write(json);
		fw.close();

		logger.info("... done\n");
	}

	public List<CvsChange> getChanges() {
		return changes.getChanges();
	}

}
