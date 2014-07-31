package com.perforce.common.journal;

import java.util.ArrayList;

import com.perforce.common.asset.ContentProperty;
import com.perforce.common.asset.TranslateCharsetType;
import com.perforce.config.ConfigException;
import com.perforce.svn.history.ChangeAction;
import com.perforce.svn.process.MergeSource;

public class FileRevision {

	private ArrayList<MergeSource> from;
	private ChangeAction to;

	public FileRevision(ChangeAction target, ArrayList<MergeSource> source) {
		from = source;
		to = target;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(to.toString() + "\n");
		if (from != null) {
			sb.append("\t\t(from: " + from.toString() + ")\n");
			sb.append("\t\t(lazy: " + to.getLazyCopy().toString() + ")\n");
		}

		return sb.toString();
	}

	public ArrayList<MergeSource> getFrom() {
		return from;
	}

	public ChangeAction getTo() {
		return to;
	}

	public String getMd5() {
		TranslateCharsetType p4type = to.getType().getP4Type();
		switch (p4type) {
		case BINARY:
		case UNKNOWN:
			return to.getMd5();
		default:
			return Digest.null_MD5;
		}
	}

	public int getTypeValue() throws ConfigException {
		int typeValue = 0;
		TranslateCharsetType p4type = to.getType().getP4Type();
		typeValue = p4type.getValue() | ContentProperty.getSum(to.getProps());

		// add +C flag if compressed
		if (to.getLazyCopy().isCompressed()) {
			typeValue |= ContentProperty.COMPRESS.getValue();
		}

		return typeValue;
	}
}
