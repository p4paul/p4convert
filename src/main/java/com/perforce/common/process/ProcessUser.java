package com.perforce.common.process;

public class ProcessUser {

	/**
	 * Filter reserved characters from the username. Empty usernames are
	 * populated with 'unknown'.
	 * 
	 * Remove '@', '*', '%', '...' and '#', but replace ' ' with '_'
	 * 
	 * @param user
	 * @return
	 */
	public static String filter(String user) {
		user = user.replace("@", "");
		user = user.replace("#", "");
		user = user.replace(" ", "_");
		user = user.replace("*", "");
		user = user.replace("%%", "");
		user = user.replace("...", "");

		if (user.isEmpty()) {
			user = "unknown";
		}

		return user;
	}
}
