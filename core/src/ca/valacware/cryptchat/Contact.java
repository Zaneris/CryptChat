package ca.valacware.cryptchat;

import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.Key;
import java.security.PublicKey;
import java.util.*;

/**
 * Created by Ted on 2017-01-06.
 */
class Contact {
	private static DateFormat dateFormat;
	int contactID;
	private Key aesKey;
	TreeMap<Long, Message> chat;
	private List<String> history;
	long lastSent, lastDelivered, lastRead;
	private File statusFile;
	PublicKey publicKey;
	String nickname;

	String getName() {
		return nickname + ":" + contactID;
	}

	Contact(int contactID) {
		this.contactID = contactID;
		nickname = "";
		statusFile = CryptChat.system.getFile("data/" + contactID + "/status.ini");
		loadStatuses();
	}

	void initialize(byte[] bAesKey) {
		if(dateFormat==null)
			dateFormat = new DateFormat();
		aesKey = new SecretKeySpec(bAesKey, "AES");
		history = new ArrayList<>();
		chat = new TreeMap<>(Collections.<Long>reverseOrder());
		checkLogsAndLoadHistory();
		if(!statusFile.exists())
			saveStatuses();
	}

	private void checkLogsAndLoadHistory() {
		File logs = CryptChat.system.getFile("data/" + contactID);
		if(!logs.exists()) logs.mkdir();
		logs = CryptChat.system.getFile("data/" + contactID + "/logs");
		if(!logs.exists()) logs.mkdir();
		File[] files = logs.listFiles();
		if(files!=null) {
			for (File file : files) {
				if (file != null && file.isFile()) {
					history.add(file.getName());
				}
			}
		}
		Collections.sort(history, Collections.<String>reverseOrder());
		while(chat.size() < 50 && !history.isEmpty()) {
			loadNextFromHistory();
		}
	}

	void loadNextFromHistory() {
		File file = CryptChat.system.getFile("data/" + contactID + "/logs/" + history.remove(0));
		if (!file.exists()) return;
		FileBytesIn streamIn = null;
		try {
			streamIn = new FileBytesIn(file);
			while(streamIn.available()>0) {
					long id = streamIn.readLong();
					boolean client = streamIn.readBool();
					byte[] msg = new byte[streamIn.readShort()];
					streamIn.read(msg);
					chat.put(id, new Message(CryptChat.decryptToString(msg, aesKey), client));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(streamIn!=null)
				try {
					streamIn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	private void loadStatuses() {
		if(!statusFile.exists()) return;
		String[] data = CryptChat.decryptStringFromFile(statusFile).split("/");
		if(data.length<4) return;
		byte[] aesKey = CryptChat.base16ToBytes(data[0]);
		lastRead = Long.parseLong(data[1]);
		lastDelivered = Long.parseLong(data[2]);
		lastSent = Long.parseLong(data[3]);
		if(data.length>4)
			nickname = data[4];
		initialize(aesKey);
	}

	void updateStatus(long pID, boolean read) {
		Message m = chat.get(pID);
		if (m != null) {
			if(lastDelivered<pID)
				lastDelivered = pID;
			if(read) {
				if(lastRead<pID)
					lastRead = pID;
			}
			saveStatuses();
		}
	}

	void saveStatuses() {
		CryptChat.encryptStringToFile(CryptChat.bytesToBase16(aesKey.getEncoded(),false)
				+ "/" + lastRead
				+ "/" + lastDelivered
				+ "/" + lastSent
				+ "/" + nickname, statusFile);
	}

	void receiveMessage(long msgID, byte[] msg) {
		chat.put(msgID, new Message(CryptChat.decryptToString(msg, aesKey),false));
		saveMessage(msgID, msg, false);
		CryptChat.sendPacket(Packet.status(msgID, contactID, Packet.STATUS_READ));
	}

	private void saveMessage(long id, byte[] msg, boolean client) {
		Date d = new Date(id);
		String fileName = "data/" + contactID + "/logs/" + dateFormat.format(d) + ".dat";
		File file = CryptChat.system.getFile(fileName);
		FileBytesOut streamOut = null;
		try {
			streamOut = new FileBytesOut(file, true);
			streamOut.write(id);
			streamOut.write(client);
			streamOut.write((short)msg.length);
			streamOut.write(msg);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(streamOut!=null)
				try {
					streamOut.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	void sendMessage(String msg) {
		msg = msg.trim();
		if (msg.length() > 0) {
			long pID = System.currentTimeMillis();
			chat.put(pID, new Message(msg, true));
			byte[] eMsg = CryptChat.encryptString(msg, aesKey);
			// TODO - Save unsent messages.
			CryptChat.sendPacket(Packet.message(eMsg,contactID,pID));
		}
	}

	void updateID(long oldID, long newID) {
		Message m = chat.remove(oldID);
		if(m!=null) {
			chat.put(newID,m);
			if(newID>lastSent) {
				lastSent = newID;
				saveStatuses();
			}
			saveMessage(newID, CryptChat.encryptString(m.getMsg(), aesKey), true);
		}
	}
}
