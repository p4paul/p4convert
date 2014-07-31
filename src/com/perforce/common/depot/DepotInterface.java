package com.perforce.common.depot;

import com.perforce.config.ConfigException;

public interface DepotInterface {

	public String getName();
	
	public String getBase();

	public String getRoot() throws ConfigException;

	public String getUser();

	public String getClient();

	public long getDefaultDate();

	public String getDefaultClientRoot() throws Exception;
}
