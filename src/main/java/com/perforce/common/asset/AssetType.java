package com.perforce.common.asset;

public enum AssetType {

	P4_ASSET, // translated, and stored in full (client or archive file)
	PROPERTY, // SVN property file stored as text in Perforce
	TMP_FILE // written in UTF8, and stored in full
}
