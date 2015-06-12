package com.perforce.common.client;

import java.io.File;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.perforce.config.CFG;
import com.perforce.config.Config;
import com.perforce.p4java.PropertyDefs;
import com.perforce.p4java.client.IClient;
import com.perforce.p4java.client.IClientSummary.ClientLineEnd;
import com.perforce.p4java.core.IDepot;
import com.perforce.p4java.core.IDepot.DepotType;
import com.perforce.p4java.core.IUserSummary;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.client.ClientOptions;
import com.perforce.p4java.impl.generic.client.ClientView;
import com.perforce.p4java.impl.generic.client.ClientView.ClientViewMapping;
import com.perforce.p4java.impl.generic.core.Depot;
import com.perforce.p4java.impl.generic.core.User;
import com.perforce.p4java.impl.mapbased.client.Client;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;

public class ConnectionFactory {

	private static Logger logger = LoggerFactory
			.getLogger(ConnectionFactory.class);

	private static String superUser;
	private static IOptionsServer iserver = null;
	private static IClient iclient = null;
	private static boolean init = false;

	// single shared connection (might change to pool)
	private static Connection connection;

	public static Connection getConnection() throws Exception {
		init();
		iclient = assignIClient(iclient, superUser);
		iserver.setUserName(superUser);
		iserver.setCurrentClient(iclient);

		connection = new Connection(superUser, iserver, iclient);
		return connection;
	}

	private static void init() throws Exception {
		if (!init) {
			// get new server connection, or use existing
			iserver = createIOptionsServer();

			// set super user and login if password set
			superUser = (String) Config.get(CFG.P4_USER);
			String passwd = (String) Config.get(CFG.P4_PASSWD);
			if (!passwd.isEmpty()) {
				iserver.setUserName(superUser);
				iserver.login(passwd);
			} else {
				createUser(superUser);
			}

			// create import depot, or use existing
			createDepot(iserver);

			// create reusable client
			iclient = createIClient(superUser);

			init = true;
		}
	}

	private static void createUser(String user) throws Exception {
		// check if user exists (use 'p4 users' as login is not needed)
		boolean newUser = true;
		List<IUserSummary> users = iserver.getUsers(null, 0);
		for (IUserSummary u : users) {
			if (u.getLoginName() != null && u.getLoginName().equals(user)) {
				newUser = false;
			}
		}

		// create if new user, else set user
		if (newUser) {
			User implUser = new User();
			implUser.setLoginName(user);
			implUser.setFullName(user);
			if (logger.isTraceEnabled()) {
				logger.trace("Creating user: " + user);
			}
			iserver.createUser(implUser, true);
		} else {
			iserver.setUserName(user);
		}
	}

	public static void close() throws Exception {
		iserver.disconnect();
		iserver = null;
		iclient = null;
		init = false;
	}

	/**
	 * Create initial IOptionsServer from configuration
	 * 
	 * @param port
	 * @return
	 * @throws Exception
	 */
	private static IOptionsServer createIOptionsServer() throws Exception {

		IOptionsServer newIServer = iserver;

		if (newIServer == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("creating IOptionsServer...");
			}
			Properties props = System.getProperties();

			// Identify ourselves in server log files.
			props.put(PropertyDefs.PROG_NAME_KEY, "p4convert");
			props.put(PropertyDefs.PROG_VERSION_KEY, Config.get(CFG.VERSION));

			// Set up socket pooling to use a single socket
			props.put(RpcPropertyDefs.RPC_SOCKET_POOL_SIZE_NICK, "1");

			// Allow p4 admin commands.
			props.put(RpcPropertyDefs.RPC_RELAX_CMD_NAME_CHECKS_NICK, "true");

			// disable timeout for slow servers / large db lock times
			props.put(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK, "0");

			// Get a server connection
			String port = (String) Config.get(CFG.P4_PORT);
			String serverUri;
			if (port.startsWith("ssl:")) {
				serverUri = "p4javassl://" + port.substring(4, port.length());
			} else {
				serverUri = "p4java://" + port;
			}
			newIServer = ServerFactory.getOptionsServer(serverUri, props, null);
			try {
				newIServer.connect();
			} catch (ConnectionException e) {
				StringBuffer sb = new StringBuffer();
				sb.append("Unable to connect to server at ");
				sb.append(port);
				sb.append(", please check configuration.\n");
				if ((Boolean) Config.get(CFG.P4_UNICODE)) {
					sb.append("Unicode mode is enable; did you create the server with '-xi'?");
				}
				logger.error(sb.toString());
			}

			// set charset for unicode mode
			if ((Boolean) Config.get(CFG.P4_UNICODE)) {
				newIServer.setCharsetName((String) Config.get(CFG.P4_CHARSET));
			}
		}
		return newIServer;
	}

	/**
	 * Create import depot if needed
	 * 
	 * @param ios
	 * @throws Exception
	 */
	private static void createDepot(IOptionsServer ios) throws Exception {

		// Set super user and get depot name
		ios.setUserName(superUser);
		String depot = (String) Config.get(CFG.P4_DEPOT_PATH);

		// Find depot (must be created)
		IDepot idepot = null;
		List<IDepot> depotList = ios.getDepots();
		for (IDepot d : depotList) {
			if (d.getName().contains(depot)) {
				idepot = d;
			}
		}
		if (idepot == null) {
			Depot implDepot = new Depot();
			implDepot.setName(depot);
			implDepot.setDepotType(DepotType.LOCAL);
			implDepot.setMap(depot + "/...");

			try {
				ios.createDepot(implDepot);
				// refresh depot (workaround for job057913)
				job057913();
			} catch (P4JavaException e) {
				StringBuffer sb = new StringBuffer();
				sb.append("Conversion user '");
				sb.append(superUser);
				sb.append("' does not have permission to create depot '");
				sb.append(depot);
				sb.append("'; either create\nthe depot or grant super access.");
				logger.error(sb.toString());
				throw e;
			}
		}
	}

	/**
	 * The iserver object does not refresh after creating a new depot on 10.2
	 * and older Perforce servers see job057913 for details.
	 * 
	 * @throws Exception
	 */
	private static void job057913() throws Exception {
		iserver.disconnect();
		iserver = null;
		iserver = createIOptionsServer();
	}

	/**
	 * Create default client from Configuration
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private static IClient createIClient(String user) throws Exception {

		IClient newIClient = iclient;

		if (newIClient == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("creating IClient...");
			}

			String depot = (String) Config.get(CFG.P4_DEPOT_PATH);
			String client = (String) Config.get(CFG.P4_CLIENT);
			String root = (String) Config.get(CFG.P4_CLIENT_ROOT);
			File file = new File(root);
			root = file.getCanonicalPath() + "/";

			iserver.setUserName(user);
			newIClient = iserver.getClient(client);
			if (newIClient == null) {
				Client implClient = new Client();
				implClient.setName(client);
				iserver.createClient(implClient);
				newIClient = iserver.getClient(client);
			}

			// Set client options
			ClientOptions options = new ClientOptions();
			options.setAllWrite(true);
			options.setRmdir(true);
			options.setClobber(true);
			newIClient.setOptions(options);

			newIClient.setLineEnd(ClientLineEnd.UNIX);
			newIClient.setRoot(root);

			// Set client view
			ClientView clientView = new ClientView();
			String lhs = "//" + depot + "/...";
			String rhs = "//" + client + "/...";
			ClientViewMapping clientViewMapping = new ClientViewMapping(0, lhs,
					rhs);
			clientView.addEntry(clientViewMapping);
			newIClient.setClientView(clientView);
		}

		return newIClient;
	}

	/**
	 * Re-assign client to new user
	 * 
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private static IClient assignIClient(IClient iClient, String user)
			throws Exception {
		if (logger.isTraceEnabled()) {
			logger.trace("assigning IClient to " + user);
		}
		iserver.setUserName(superUser);
		iClient.setOwnerName(user);
		iClient.update();
		return iClient;
	}
}
