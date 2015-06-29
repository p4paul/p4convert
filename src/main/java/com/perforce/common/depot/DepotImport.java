package com.perforce.common.depot;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.common.client.Connection;
import com.perforce.common.client.ConnectionFactory;
import com.perforce.common.node.PathMapTranslator;
import com.perforce.config.CFG;
import com.perforce.config.CaseSensitivity;
import com.perforce.config.Config;
import com.perforce.config.ConfigException;
import com.perforce.p4java.server.IOptionsServer;

public class DepotImport implements DepotInterface {

	private Logger logger = LoggerFactory.getLogger(DepotImport.class);

	private String depotName;
	private String depotSub;
	private String depotPath;
	private String user;
	private String client;
	private String defaultClientRoot;
	private CaseSensitivity caseMode;
	private IOptionsServer iserver;

	public DepotImport(String name, CaseSensitivity mode) throws Exception {
		depotName = name;

		depotSub = (String) Config.get(CFG.P4_DEPOT_SUB);
		if (!depotSub.endsWith("/")) {
			depotSub = new String(depotSub + "/");
		}
		if (!depotSub.startsWith("/")) {
			depotSub = new String("/" + depotSub);
		}

		depotPath = (String) Config.get(CFG.P4_ROOT);
		if (!depotPath.endsWith("/")) {
			depotPath = new String(depotPath + "/");
		}

		caseMode = mode;
		user = (String) Config.get(CFG.P4_USER);
		client = (String) Config.get(CFG.P4_CLIENT);
		defaultClientRoot = (String) Config.get(CFG.P4_CLIENT_ROOT);
		if (!defaultClientRoot.endsWith("/")
				&& !defaultClientRoot.endsWith("\\")) {
			defaultClientRoot = new String(defaultClientRoot + "/");
			logger.info("Adding missing delimiter '/' or '\\' to end of Client path");
			Config.set(CFG.P4_CLIENT_ROOT, defaultClientRoot);
		}

		// Initial connection
		Connection p4 = ConnectionFactory.getConnection();
		iserver = p4.getIserver();
	}

	@Override
	public String getClient() {
		return client;
	}

	@Override
	public long getDefaultDate() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getUser() {
		// Not used (satisfy interface)
		return user;
	}

	@Override
	public String getName() {
		return depotName;
	}

	@Override
	public String getPath(String scmPath) {
		String p4Path = PathMapTranslator.translate(scmPath);
		return p4Path;
	}

	@Override
	public String getRoot() throws ConfigException {
		return depotPath;
	}

	public IOptionsServer getIServer() {
		return iserver;
	}

	@Override
	public String getDefaultClientRoot() throws Exception {
		File file = new File(defaultClientRoot);
		return file.getCanonicalPath() + "/";
	}
	
	public CaseSensitivity getCaseMode() {
		return caseMode;
	}

	public int getServerVersion() {
		return iserver.getServerVersionNumber();
	}
}
