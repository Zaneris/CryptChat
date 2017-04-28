package ca.valacware.cryptchat;

final class Packet {
	static final byte[] P_VERSION = {0x01};
	private static final byte[] B_TRUE 	= {1};
	private static final byte[] B_FALSE = {0};

	private static final byte P_PING	= 0x00;
	private static final byte P_STATUS  = 0x01;
	private static final byte P_CONTACT = 0x02;
	static final byte 		  P_TEXT 	= 0x03;
	private static final byte P_LOGIN	= 0x04;
	private static final byte P_AES_KEY = 0x05;

	static final byte[] STATUS_SENT		= {0x00};
	static final byte[] STATUS_DELIVERED= {0x01};
	static final byte[] STATUS_READ		= {0x02};

	byte[] packet;

	Packet(byte[] packet) {
		this.packet = packet;
	}

	private static byte[] createPacket(byte type, byte[]... bytes) {
		int i = 1;
		for(byte[] b:bytes)
			i += b.length;
		byte[] packet = new byte[i+2];
		byte[] size = CryptChat.shortToBytes(i);
		packet[0] = size[0];
		packet[1] = size[1];
		packet[2] = type;
		i = 3;
		for(byte[] b:bytes) {
			System.arraycopy(b,0,packet,i,b.length);
			i+=b.length;
		}
		return packet;
	}

	Packet() {
		packet = createPacket(P_PING);
	}

	static byte[] message(byte[] msg, int contactID, long msgID) {
		byte[] bDest = CryptChat.intToBytes(contactID);
		byte[] bID = CryptChat.longToBytes(msgID);

		return createPacket(P_TEXT, bDest, bID, msg);
	}

	static byte[] status(long msgID, int contactID, byte[] status) {
		byte[] bID = CryptChat.longToBytes(msgID);
		byte[] bDest = CryptChat.intToBytes(contactID);
		return createPacket(P_STATUS, bID, bDest, status);
	}

	static byte[] newID(long oldID, long newID, int contactID) {
		byte[] oID = CryptChat.longToBytes(oldID);
		byte[] bDest = CryptChat.intToBytes(contactID);
		byte[] nID = CryptChat.longToBytes(newID);

		return createPacket(P_STATUS, oID, bDest, nID);
	}

	static byte[] newUser(byte[] publicKey) {
		return createPacket(P_LOGIN, P_VERSION, publicKey);
	}

	static byte[] reconnect() {
		return createPacket(P_LOGIN, P_VERSION);
	}

	static byte[] login(int loginID, long loginPass) {
		byte[] logID = CryptChat.intToBytes(loginID);
		byte[] logPass = CryptChat.longToBytes(loginPass);
		return createPacket(P_LOGIN, P_VERSION, logPass, logID);
	}

	static byte[] contactStatus(int contactID, boolean added) {
		byte[] bContact = CryptChat.intToBytes(contactID);
		byte[] bAdded = (added? B_TRUE : B_FALSE);
		return createPacket(P_CONTACT, bContact, bAdded);
	}

	static byte[] contactKey(int contactID, byte[] key) {
		byte[] bContact = CryptChat.intToBytes(contactID);
		return createPacket(P_CONTACT, bContact, key);
	}

	static byte[] aesKey(int contactID, byte[] key) {
		byte[] bContact = CryptChat.intToBytes(contactID);
		return createPacket(P_AES_KEY, bContact, key);
	}

	static void action(PacketInterface packetInterface, byte[] packet) {
		action(packetInterface, packet, null);
	}

	static void action(PacketInterface packetInterface, byte[] packet, Connection connection) {
		if(packet==null || packet.length==0)
			return;
		int contactID;
		long packetID;
		switch (packet[0]) {
			case P_STATUS:
				if(packet.length!=14 && packet.length!=21)
					return;
				packetID = CryptChat.bytesToLong(packet,1);
				contactID = CryptChat.bytesToInt(packet,9);
				if(packet.length==14) {
					packetInterface.packetStatus(contactID, packetID, packet[13], connection);
				} else {
					long newID = CryptChat.bytesToLong(packet,13);
					packetInterface.packetNewID(contactID, packetID, newID);
				}
			case P_TEXT:
				if(packet.length<20)
					return;
				contactID = CryptChat.bytesToInt(packet,1);
				packetID = CryptChat.bytesToLong(packet,5);
				byte[] msg = new byte[packet.length-13];
				System.arraycopy(packet,13,msg,0,packet.length-13);
				packetInterface.packetText(contactID, packetID, msg, connection);
				break;
			case P_LOGIN:
				if(packet.length<2)
					return;
				byte version = packet[1];
				if(packet.length==2) {
					packetInterface.packetLoginAgain(version);
				} else if(packet.length==14) {
					long pass = CryptChat.bytesToLong(packet,2);
					int userID = CryptChat.bytesToInt(packet,10);
					packetInterface.packetLogin(version, userID, pass, connection);
				} else if (packet.length>14){
					byte[] publicKey = new byte[packet.length-2];
					System.arraycopy(packet,2,publicKey,0,packet.length-2);
					packetInterface.packetRequestLogin(version, publicKey, connection);
				}
				break;
			case P_CONTACT:
				if(packet.length<6)
					return;
				contactID = CryptChat.bytesToInt(packet,1);
				if(packet.length==6) {
					boolean added = packet[5]>0;
					packetInterface.packetContact(contactID, added, connection);
				} else {
					byte[] publicKey = new byte[packet.length-5];
					System.arraycopy(packet,5,publicKey,0,packet.length-5);
					packetInterface.packetContact(contactID, publicKey);
				}
				break;
			case P_AES_KEY:
				if(packet.length<6)
					return;
				contactID = CryptChat.bytesToInt(packet,1);
				byte[] aesKey = new byte[packet.length-5];
				System.arraycopy(packet,5,aesKey,0,packet.length-5);
				packetInterface.packetAESKey(contactID, aesKey, connection);
				break;
		}
	}
}