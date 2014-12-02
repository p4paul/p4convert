package com.perforce.common.label;

import java.util.List;

public interface LabelInterface {

	String getName();

	String getOwner();

	Long getDate();
	
	void setAutomatic(long automatic);
	
	String getAutomatic();

	String getDesc();

	List<TagConvert> getTags();

	void add(TagConvert tags) throws Exception;

	void submit() throws Exception;
}
