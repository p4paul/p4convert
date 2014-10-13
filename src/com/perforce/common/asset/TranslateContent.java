package com.perforce.common.asset;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.Stats;
import com.perforce.common.StatsType;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.svn.asset.SvnContentStream;
import com.perforce.svn.parser.Content;

public class TranslateContent {

	private Logger logger = LoggerFactory.getLogger(TranslateContent.class);

	private Content content;
	private String path;
	private final static int blockSize = 8192;

	public TranslateContent(Content content, String path) {
		this.content = content;
		this.path = path;
	}

	public void writeArchive() throws Exception {

		boolean unicode = (Boolean) Config.get(CFG.P4_UNICODE);
		switch (content.getType()) {
		case UNKNOWN:
		case P4_BINARY:
			writeRAW();
			break;

		case SYMLINK:
			writeLINK();
			break;

		case UTF_16LE:
		case UTF_16BE:
			// always translated to utf8, even for non-unicode servers
			writeUTF8(true, true);
			break;

		case P4_TEXT:
		case US_ASCII:
			if (content.getLength() > 0)
				writeUTF8(unicode, unicode);
			else
				writeRAW();
			break;

		// UTF8 and other code pages
		default:
			if (content.getType().getP4Type() == TranslateCharsetType.UTF8) {
				writeUTF8(unicode, unicode);
			} else {
				// unknown and unsupported charsets
				writeRAW();
			}
			break;
		}
	}

	public void writeClient() throws Exception {

		boolean unicode = (Boolean) Config.get(CFG.P4_UNICODE);
		// Encode (unicode mode)
		switch (content.getType()) {
		case UNKNOWN:
		case P4_BINARY:
			writeRAW();
			break;

		case SYMLINK:
			createLINK();
			break;

		case UTF_16LE:
		case UTF_16BE:
			if (unicode) {
				// don't translate utf16 -- keep BOM, but cleanup line-endings
				writeUTF8(false, false);
			} else {
				// always translated to utf8, even for non-unicode servers
				// (p4java bug)
				writeUTF8(true, true);
			}
			break;

		case UTF_32LE:
		case UTF_32BE:
			if (unicode) {
				// don't translate utf32 -- keep BOM, but cleanup line-endings
				writeUTF8(false, false);
			} else {
				// non-unicode servers store utf32 as binary
				writeRAW();
			}
			break;

		case P4_TEXT:
		case US_ASCII:
			if (content.getLength() > 0)
				writeUTF8(unicode, false);
			else
				// empty files are treated as text, but don't need any
				// translation. Write as RAW or get a decode exception.
				writeRAW();
			break;

		// UTF8 and other code pages
		default:
			if (content.getType().getP4Type() == TranslateCharsetType.UTF8) {
				writeUTF8(unicode, !unicode);
			} else {
				// unknown and unsupported charsets
				writeRAW();
			}
			break;
		}
	}

	/**
	 * Generate archive content for a symlink.
	 * 
	 * @return
	 * @throws Exception
	 */
	public static String getLinkSource(Content content) throws Exception {

		// set decoder for Subversion content
		Charset fromCharset = Charset.forName("UTF-8");
		CharsetDecoder decoder = fromCharset.newDecoder();

		// Open Input channels
		ContentStream in = ContentStreamFactory.getContentStream(content);
		ReadableByteChannel rbc = Channels.newChannel((InputStream) in);
		Reader reader = Channels.newReader(rbc, decoder, blockSize);
		BufferedReader bufferedReader = new BufferedReader(reader);

		// Read line as link source if starting with byte sequence 'link '
		byte[] linkId = new byte[] { 'l', 'i', 'n', 'k', ' ' };
		byte[] b = new byte[linkId.length];
		in.read(b);

		String source = null;
		if (Arrays.equals(b, linkId)) {
			// read a line
			source = bufferedReader.readLine();
		}

		// close stream
		in.close();
		return source;
	}

	/**
	 * write archive file for symlink.
	 * 
	 * @throws Exception
	 */
	private void writeLINK() throws Exception {

		String link = getLinkSource(content);
		if (link == null) {
			link = "_unset_";
			Stats.inc(StatsType.warningCount);
			logger.warn("Symlink target is null setting to " + link);
		}

		// set encoder for Perforce archive
		Charset toCharset = Charset.forName("UTF-8");
		CharsetEncoder encoder = toCharset.newEncoder();

		// Open Output channels
		FileOutputStream out = new FileOutputStream(path);
		FileChannel fileChannel = out.getChannel();

		// translate CharBuffer to encoded ByteBuffer
		link = link + '\n';
		char[] chars = link.toCharArray();
		CharBuffer cbuf = CharBuffer.wrap(chars);
		ByteBuffer bbuf = encoder.encode(cbuf);
		fileChannel.write(bbuf);

		// close streams
		out.close();
	}

	/**
	 * Create a symlink in the workspace.
	 * 
	 * @throws Exception
	 */
	private void createLINK() throws Exception {
		String target = getLinkSource(content);
		if (target == null) {
			target = "_unset_";
			Stats.inc(StatsType.warningCount);
			logger.warn("Symlink target is null setting to " + target);
		}

		if (logger.isDebugEnabled()) {
			logger.debug("symlink: " + path + " target: " + target);
		}
		Path linkPath = FileSystems.getDefault().getPath(path);
		Path targetPath = FileSystems.getDefault().getPath(target);
		Files.createSymbolicLink(linkPath, targetPath);
	}

	/**
	 * Takes the content and translates it from the detected encoding into utf8.
	 * Translation is performed line-by-line (converting lines to unix form
	 * '\n')
	 * 
	 * @param out
	 * @throws Exception
	 */
	private void writeUTF8(boolean unicode, boolean rmBOM) throws Exception {

		// Trap unsupported charsets and down grade to binary.
		ContentStream in = ContentStreamFactory.getContentStream(content);
		FileOutputStream out = new FileOutputStream(path);

		try {
			// Set decoder for Subversion content.
			ContentType type = content.getDetectedType();
			Charset fromCharset = Charset.forName(type.getName());
			CharsetDecoder decoder = fromCharset.newDecoder();

			// set encoder for Perforce archive
			CharsetEncoder encoder;
			if (unicode) {
				Charset toCharset = Charset.forName("UTF-8");
				encoder = toCharset.newEncoder();
			} else {
				encoder = fromCharset.newEncoder();
			}

			// Open Input channels
			ReadableByteChannel rbc = Channels.newChannel((InputStream) in);
			Reader reader = Channels.newReader(rbc, decoder, blockSize);
			BufferedContentReader br = new BufferedContentReader(reader);

			// For unicode servers remove the BOM
			if (rmBOM) {
				((SvnContentStream) in).removeBOM();
				logger.warn("Stuff to do here for CVS");
			}

			// Open Output channels
			FileChannel fileChannel = out.getChannel();

			// read line by line
			String line = br.readLine();
			while (line != null) {
				// translate CharBuffer to encoded ByteBuffer
				char[] chars = line.toCharArray();
				CharBuffer cbuf = CharBuffer.wrap(chars);
				ByteBuffer bbuf = encoder.encode(cbuf);
				fileChannel.write(bbuf);

				ByteBuffer byteCR = encoder.encode(CharBuffer.wrap("\r"));
				ByteBuffer byteLF = encoder.encode(CharBuffer.wrap("\n"));

				switch (br.getEOL()) {
				case WIN:
					if ((Boolean) Config.get(CFG.P4_LINEEND)) {
						// Convert to UNIX (default)
						fileChannel.write(byteLF);
					} else {
						fileChannel.write(byteCR);
						fileChannel.write(byteLF);
					}
					break;
				case UNIX:
					fileChannel.write(byteLF);
					break;
				case MAC:
					fileChannel.write(byteCR);
					break;
				default:
					break;
				}

				// get next line
				line = br.readLine();
			}
		} catch (UnsupportedCharsetException e) {
			logger.warn("Unsupported char set, storing file as-is");
			content.setType(ContentType.P4_TEXT);
			writeRAW();
		} catch (UnmappableCharacterException e) {
			logger.warn("Unmappable char set, storing file as-is");
			content.setType(ContentType.P4_TEXT);
			writeRAW();
		} catch (MalformedInputException e) {
			// re-attempt encoding with a guess of CP1252.
			// Windows (Western Europe code page) most commonly miss read
			if (content.getDetectedType() != ContentType.windows_1252) {
				if (logger.isDebugEnabled()) {
					logger.debug("Malformed chars, trying windows_1252");
				}
				content.setDetectedType(ContentType.windows_1252);
				writeUTF8(unicode, rmBOM);
			} else {
				logger.warn("Malformed chars, storing file as-is");
				content.setType(ContentType.P4_TEXT);
				writeRAW();
			}
		} finally {
			// close streams
			out.close();
			in.close();
		}
	}

	public void writeRAW() throws Exception {
		// Open Input channels
		ContentStream in = ContentStreamFactory.getContentStream(content);
		ReadableByteChannel rbc = Channels.newChannel((InputStream) in);

		// Open Output channels
		FileOutputStream out = new FileOutputStream(path);
		FileChannel fileChannel = out.getChannel();

		// read content as bytes and write
		ByteBuffer bbuf = ByteBuffer.allocate(blockSize);

		// help with debug
		int sum = 0;
		int c = 0;
		StringBuffer sb = new StringBuffer();

		while (rbc.read(bbuf) != -1) {
			bbuf.flip();
			int w = fileChannel.write(bbuf);
			if (logger.isTraceEnabled()) {
				sum += w;
				c++;
				sb.append("wrote[" + c + "]" + w + ":" + sum + " ");
			}
			bbuf.clear();
		}

		if (logger.isTraceEnabled()) {
			logger.trace(sb.toString());
			logger.trace("total[" + c + "] " + sum);
		}

		// close streams
		out.close();
		in.close();
	}
}
