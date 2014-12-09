package com.perforce.common.asset;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.ConverterException;
import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.common.depot.DepotConvert;
import com.perforce.common.depot.DepotInterface;
import com.perforce.common.node.NodeAttributes;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.config.ScmType;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.parser.Content;

public class AssetWriter {

	private Logger logger = LoggerFactory.getLogger(AssetWriter.class);

	private ChangeAction act;
	private String dir;
	private String path;
	private DepotInterface depot;
	private int blockSize = 8192;

	/**
	 * Used to create Perforce archive files in Convert mode
	 * 
	 * @param d
	 * @param a
	 * @throws ConfigException
	 */
	public AssetWriter(DepotConvert d, ChangeAction a) throws ConfigException {
		act = a;
		depot = d;
		path(act);
	}

	/**
	 * Used to create local client side files in Import mode
	 * 
	 * @param filePath
	 */
	public AssetWriter(String filePath) {
		path = filePath;

		File f = new File(filePath);
		dir = f.getParent();
	}

	private void path(ChangeAction act) throws ConfigException {
		// generate path
		String nodePath = act.getPath();
		dir = depot.getRoot() + depot.getBase() + nodePath + ",d/";
		path = dir + "1." + act.getEndChange();

		// Convert to lower for C1 mode
		if ((Boolean) Config.get(CFG.P4_C1_MODE)) {
			dir = dir.toLowerCase();
			path = path.toLowerCase();
		}

		// Update stats
		Stats.inc(StatsType.archiveCount);
		// Main.updateNodeStats();
	}

	public void check(ChangeAction act) throws Exception {
		// generate path
		String nodePath = act.getLazyCopy().getPath();
		String dir = depot.getRoot() + depot.getBase() + nodePath + ",d/";
		String path = dir + "1." + act.getLazyCopy().getEndChange();

		// Convert to lower for C1 mode
		if ((Boolean) Config.get(CFG.P4_C1_MODE)) {
			dir = dir.toLowerCase();
			path = path.toLowerCase();
		}

		// Check lazy reference for compressed archive
		if (act.getLazyCopy().isCompressed()) {
			path = path + ".gz";
		}

		File file = new File(path);
		if (!file.exists()) {
			throw new ConverterException("Missing archive:\n\t"
					+ act.getAction().toString() + " - " + path + ")");
		}

		// Update stats
		Stats.inc(StatsType.branchActionCount);
		// Main.updateNodeStats();
	}

	private String open() throws Exception {
		// create directory if needed
		File directory = new File(dir);
		if (!directory.mkdirs()) {
			if (!directory.exists()) {
				throw new ConverterException("Cannot create diretory: "
						+ directory.getPath());
			}
		}
		return path;
	}

	public void write(Content content) throws Exception {
		AssetType type = content.getAssetType();
		switch (type) {
		case P4_ASSET:
			// use cached file if present
			if (Config.isImportMode()) {
				writeClientFile(content);
			} else {
				writeArchive(content);
			}
			break;
		case PROPERTY:
			writeProperty(content);
			break;
		case TMP_FILE:
			writeCache(content);
			break;
		default:
			throw new Exception("Unknown AssetType: " + type);
		}
	}

	private void writeCache(Content content) throws Exception {
		String path = open();
		TranslateContent translate = new TranslateContent(content, path);

		// Links may need to be deleted before update to content.
		Path linkPath = FileSystems.getDefault().getPath(path);
		linkPath.toFile().delete();

		translate.writeRAW();
	}

	/**
	 * Private internal method to format directory properties into a versioned
	 * file and write to disk
	 * 
	 * @param property
	 */
	private void writeProperty(Content content) throws Exception {
		NodeAttributes attributes = content.getAttributes();
		FileOutputStream out = new FileOutputStream(open());
		if (attributes != null)
			out.write(attributes.toString().getBytes());
		out.flush();
		out.close();
	}

	/**
	 * Internal method to write the conversion archive to disk using a seek
	 * position and length offset in the Subversion dump file.
	 * 
	 * @param content
	 * @throws Exception
	 */
	private void writeClientFile(Content content) throws Exception {
		String path = open();
		TranslateContent translate = new TranslateContent(content, path);

		// Links may need to be deleted before update to content.
		Path linkPath = FileSystems.getDefault().getPath(path);
		linkPath.toFile().delete();

		translate.writeClient();
	}

	/**
	 * Internal method to write the conversion archive to disk using a seek
	 * position and length offset in the Subversion dump file.
	 * 
	 * @param content
	 * @throws Exception
	 */
	private void writeArchive(Content content) throws Exception {
		String path = open();
		TranslateContent translate = new TranslateContent(content, path);
		translate.writeArchive();

		// compress archive file if +C used
		if (content.isCompressed()) {
			compressArchive();
		}
	}

	/**
	 * Compress archive file. Used if +C flag is set.
	 * 
	 * @throws Exception
	 */
	private void compressArchive() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Compressing archive: " + path);
		}

		FileOutputStream fout = new FileOutputStream(path + ".gz");
		GZIPOutputStream gout = new GZIPOutputStream(fout);
		FileInputStream fin = new FileInputStream(path);
		byte[] buf = new byte[blockSize];
		int len;
		while ((len = fin.read(buf)) > 0) {
			gout.write(buf, 0, len);
		}
		fin.close();

		// flush and close gzip file
		gout.finish();
		gout.close();

		// unlink original archive
		File file = new File(path);
		file.delete();
	}
}
