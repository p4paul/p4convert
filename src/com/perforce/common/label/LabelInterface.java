package com.perforce.common.label;

import java.util.ArrayList;
import java.util.List;

public interface LabelInterface {

	public void setFrom(String fromPath, long fromRev);

	public String getFromPath();

	public long getFromRev();

	String getName();

	String getOwner();

	Long getDate();

	void setAutomatic(boolean auto);
	
	boolean isAutomatic();

	String getAutomatic();

	String getDesc();

	List<TagConvert> getTags();

	void add(TagConvert tags) throws Exception;

	void addView(String view) throws Exception;

	public ArrayList<String> getView();

	void submit() throws Exception;
}
