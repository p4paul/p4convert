package com.perforce.common.client;

import com.perforce.p4java.client.IClient;
import com.perforce.p4java.server.IOptionsServer;

public class Connection {

	private String user;
	private IOptionsServer iserver;
	private IClient iclient;
	
	
	public Connection(String user, IOptionsServer iserver, IClient iclient) {
		this.user = user;
		this.iserver = iserver;
		this.iclient = iclient;
	}

	public String getUser() {
		return user;
	}
	
	public IOptionsServer getIserver() {
		return iserver;
	}

	public IClient getIclient() {
		return iclient;
	}
}
