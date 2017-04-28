package ca.valacware.cryptchat;

/**
 * Created by Ted on 2017-01-08.
 */
interface PacketInterface {
	void packetStatus(int contactID, long packetID, byte status, Connection connection);
	void packetNewID(int contactID, long oldID, long newID);
	void packetText(int contactID, long packetID, byte[] msg, Connection connection);
	void packetLoginAgain(byte version);
	void packetRequestLogin(byte version, byte[] publicKey, Connection connection);
	void packetLogin(byte version, int userID, long pass, Connection connection);
	void packetContact(int contactID, boolean added, Connection connection);
	void packetContact(int contactID, byte[] publicKey);
	void packetAESKey(int contactID, byte[] aesKey, Connection connection);
}