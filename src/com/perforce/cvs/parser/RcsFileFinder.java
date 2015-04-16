package com.perforce.cvs.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RcsFileFinder {

	private Logger logger = LoggerFactory.getLogger(RcsFileFinder.class);
	private int count = 0;

	List<File> files = new ArrayList<File>();

	public RcsFileFinder(String path) {
		findFiles(new File(path));
	}

	private void findFiles(File base) {
		if (!base.exists()) {
			logger.warn("CVSROOT does not exist: " + base);
			return;
		}
		
		Path dir = base.toPath();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {

			Iterator<Path> iter = stream.iterator();
			while (iter.hasNext()) {
				Path path = iter.next();

				if (path.toFile().isDirectory()) {
					if (!"CVSROOT".equals(path.getFileName().toString())) {
						findFiles(path.toFile());
					}
				} else {
					if (path.toString().endsWith(",v")) {
						File file = verifyFile(path);
						if (logger.isDebugEnabled()) {
							logger.debug("file: " + file);
						}
						files.add(file);

						count++;
						System.out.print("Found: " + count + "\r");
					}
				}
			}
		} catch (IOException e) {
			logger.error("Unable to list files: ", e);
		}
	}

	private File verifyFile(Path path) {
		File file = path.toFile();
		try {
			RandomAccessFile rf = new RandomAccessFile(file, "r");
			try {
				rf.close();
			} catch (IOException e) {
				logger.error("Unable to close file: ", file);
			}
		} catch (FileNotFoundException e) {
			String uriStr = path.toUri().toString();
			uriStr = uriStr.replaceFirst("file://", "");

			// re-encode path with a guess of CP1252, or fall back to URI.
			// Windows (Western Europe code page) most commonly miss read!
			try {
				uriStr = URLDecoder.decode(uriStr, "windows-1252");
				logger.debug("... decoding path with: windows-1252");
			} catch (UnsupportedEncodingException e2) {
				logger.warn("Unknown charset, encoding as URI");
			}

			file = new File(uriStr);
			try {
				logger.info("... renaming: " + file);
				Files.move(path, file.toPath(), StandardCopyOption.ATOMIC_MOVE);
			} catch (IOException e1) {
				logger.error("Unable to rename file: " + file);
			}
		}
		return file;
	}

	public List<File> getFiles() {
		return files;
	}

}
