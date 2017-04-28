package ca.valacware.cryptchat;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ted on 2017-01-06.
 */
class User {
	int userID;
	long login;
	byte[] publicKey;
	Set<Integer> blocked;

	User(int userID, long login) {
		this.userID = userID;
		this.login = login;
		blocked = new HashSet<>();
	}
}
