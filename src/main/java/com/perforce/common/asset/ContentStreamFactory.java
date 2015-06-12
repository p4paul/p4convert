package com.perforce.common.asset;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.config.ScmType;
import com.perforce.cvs.asset.CvsContentStream;
import com.perforce.cvs.asset.CvsContentStream2;
import com.perforce.svn.asset.SvnContentStream;
import com.perforce.svn.parser.Content;

public class ContentStreamFactory {

	public static ContentStream getContentStream(Content content)
			throws Exception {
		ScmType scm = (ScmType) Config.get(CFG.SCM_TYPE);
		switch (scm) {
		case SVN:
			return new SvnContentStream(content);
		case CVS:
			return cvsContentStream(content);
		default:
			return null;
		}
	}

	public static ContentStream scanContentStream(Content content, long len)
			throws Exception {
		ScmType type = (ScmType) Config.get(CFG.SCM_TYPE);
		switch (type) {
		case SVN:
			return new SvnContentStream(content, len);
		case CVS:
			return cvsContentStream(content);
		default:
			return null;
		}
	}

	private static ContentStream cvsContentStream(Content content) {
		ContentStream stream;
		AssetType type = content.getAssetType();
		switch (type) {
		case P4_ASSET:
		case PROPERTY:
			stream = new CvsContentStream2(content);
			break;
		case TMP_FILE:
			stream = new CvsContentStream(content);
			break;
		default:
			stream = null;
		}
		return stream;
	}
}
