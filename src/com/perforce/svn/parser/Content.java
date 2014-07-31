package com.perforce.svn.parser;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.asset.AssetType;
import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.ContentType;
import com.perforce.common.journal.Digest;
import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.cvs.RevisionEntry;
import com.perforce.cvs.parser.rcstypes.RcsObjectBlock;
import com.perforce.svn.node.NodeAttributes;

public class Content {

	private static Logger logger = LoggerFactory.getLogger(Content.class);

	private AssetType asset;
	private boolean blob = false;
	private boolean compressed = false;

	private NodeAttributes attributes;
	private long position;
	private long length;
	private String fileName;
	private String md5 = Digest.null_MD5;
	private ContentType type = ContentType.UNKNOWN;
	private ContentType detected = ContentType.UNKNOWN;
	private List<ContentProperty> props = new ArrayList<ContentProperty>();

	private RcsObjectBlock block;

	public List<ContentProperty> getProps() {
		return props;
	}

	public void setProps(List<ContentProperty> propList) {
		props.clear();
		for (ContentProperty p : propList) {
			if (type == ContentType.SYMLINK && p == ContentProperty.EXECUTE) {
				logger.info("Ignoring +x modifer; disallowed combination (executable symlinks)");
			} else {
				props.add(p);
			}
		}
	}

	/**
	 * Once use to write temporary content for RCS expansion cache
	 * 
	 * @param block
	 */
	public Content(RcsObjectBlock block) {
		this.length = -1;
		this.asset = AssetType.TMP_FILE;
		this.blob = true;
		this.block = block;
	}

	/**
	 * Used to retrieve temporary content for RCS files.
	 * 
	 * @param rev
	 */
	public Content(RevisionEntry rev) {
		this.length = -1;
		fileName = rev.getTmpFile();
		if (fileName == null) {
			this.blob = false;
		} else {
			this.asset = AssetType.P4_ASSET;
			this.blob = true;
			this.fileName = rev.getTmpFile();
		}
	}

	public Content(SubversionReader dump, long contentLength) throws Exception {

		this.length = contentLength;
		this.position = dump.getFilePointer();
		this.fileName = dump.getFileName();
		this.asset = AssetType.P4_ASSET;
		this.blob = true;

		// compress large content > 10MB
		long large = (Long) Config.get(CFG.P4_LARGE_FILE);
		if (getLength() > large) {
			this.compressed = true;
		}

		// Skip over archive blob
		dump.seek(this.position + this.length);
	}

	public Content() {
		// TODO Auto-generated constructor stub
	}

	public AssetType getAssetType() {
		return this.asset;
	}

	public boolean isBlob() {
		return blob;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("<");
		sb.append(type.toString());
		sb.append(":");
		sb.append(length);
		sb.append("> MD5:");
		sb.append(md5);
		sb.append("\n");
		return (sb.toString());
	}

	public long getLength() {
		return length;
	}

	public ContentType getType() {
		return type;
	}

	/**
	 * Returns the original detected content type. Should not change when
	 * content is down graded for non-unicode support.
	 * 
	 * @return
	 */
	public ContentType getDetectedType() {
		return detected;
	}

	public long getPosition() {
		return position;
	}

	public void setType(ContentType t) {
		type = t;
	}

	/**
	 * Set when content type is detected (this should not change as the content
	 * does not change). setType() many change type, but not detected type; this
	 * is needed for down grade support for non-unicode servers.
	 * 
	 * @param detected
	 */
	public void setDetectedType(ContentType detected) {
		this.detected = detected;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMd5() {
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public NodeAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(NodeAttributes a) throws ParserException {
		attributes = a;
		blob = true;
		type = ContentType.P4_TEXT;
		if (logger.isTraceEnabled()) {
			logger.trace("setAttributes: " + attributes);
		}
		if (asset == AssetType.P4_ASSET)
			throw new ParserException("Content already set");
		asset = AssetType.PROPERTY;
	}

	public boolean isCompressed() {
		return compressed;
	}

	public RcsObjectBlock getBlock() {
		return block;
	}
}
