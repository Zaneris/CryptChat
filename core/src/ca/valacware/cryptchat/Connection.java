package ca.valacware.cryptchat;

/**
 * Created by Ted on 2017-01-08.
 */
interface Connection {
	int getUserID();
	boolean sendPacket(byte[] packet);
	void setUserID(int userID, byte version);
	byte getPacketVersion();
	boolean loggedIn();
	void killAll();
}