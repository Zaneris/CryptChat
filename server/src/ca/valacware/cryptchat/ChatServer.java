package ca.valacware.cryptchat;

import java.io.*;
import java.net.ServerSocket;
import java.util.*;

import static ca.valacware.cryptchat.CryptChat.*;

public class ChatServer implements PacketInterface {
	public static void main (String[] arg) {
		new ChatServer(CC_PORT);
	}

	private ServerSocket server;
	private final HashMap<Integer,Connection> clients;
	private final HashMap<Integer,User> users;
	private final HashMap<Integer,List<Packet>> packetCache;
	private int nextID = 54;
	private Random rand;
	private File data;

	private ChatServer(int port) {

		clients 	= new HashMap<>();
		users		= new HashMap<>();
		packetCache = new HashMap<>();
		rand 		= new Random();
		data		= new File("data/users.dat");

		loadUsers();

		try {
			System.out.println("Starting CryptChat Server on Port: " + port + " ...");
			server = new ServerSocket(port);
			System.out.println("Server Started: " + server);
		} catch (IOException ioe) {
			System.out.println(ioe);
		}

		ChatServerThread thread;
		while (true) {
			try {
				thread = new ChatServerThread(this,server.accept());
				thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void loadUsers() {
		File file = new File("data");
		if(!file.exists()) {
			file.mkdir();
			return;
		}
		if (!data.exists()) return;
		FileBytesIn userStream = null;
		FileBytesIn contactStream = null;
		try {
			userStream = new FileBytesIn(data);
			while(userStream.available()>0) {
				int id = userStream.readInt();
				long pass = userStream.readLong();
				User user = new User(id, pass);
				if(id>nextID) nextID=id;
				file = new File("data/" + id + ".key");
				if(file.exists()) {
					contactStream = new FileBytesIn(file);
					byte[] key = new byte[contactStream.available()];
					contactStream.read(key);
					user.publicKey = key;
				}
				file = new File("data/" + id + ".blk");
				if(file.exists()) {
					contactStream = new FileBytesIn(file);
					while(contactStream.available()>0) {
						 int contact = contactStream.readInt();
						 user.blocked.add(contact);
					}
					contactStream.close();
				}
				if(user.publicKey!=null) {
					users.put(user.userID, user);
					System.out.println("UserID: " + id + " loaded with " + user.blocked.size() + " blocked contact(s).");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(userStream!=null) {
				try {
					userStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(contactStream!=null) {
				try {
					contactStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void saveUser(int userId, long pass, byte[] publicKey) {
		FileBytesOut streamOut = null;
		try {
			streamOut = new FileBytesOut(data, true);
			streamOut.write(userId);
			streamOut.write(pass);
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
		File file = new File("data/" + userId + ".key");
		try {
			streamOut = new FileBytesOut(file, false);
			streamOut.write(publicKey);
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

	private void saveBlocked(int userId, int contactID) {
		File file = new File("data/" + userId + ".con");
		FileBytesOut streamOut = null;
		try {
			streamOut = new FileBytesOut(file, true);
			streamOut.write(contactID);
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

	synchronized void handlePacket(byte[] packet, Connection connection) {
		Packet.action(this, packet, connection);
	}

	synchronized void removeClient(int id) {
		clients.remove(id);
	}

	private void sendPacket(int userID, byte[] packet, Connection thread) {
		if(thread==null)
			thread = clients.get(userID);
		if(thread==null) {
			List<Packet> cache = packetCache.computeIfAbsent(userID, k -> new ArrayList<>());
			cache.add(new Packet(packet));
		} else {
			if(!thread.sendPacket(packet)) {
				thread.killAll();
				sendPacket(userID, packet, null);
			} else {
				checkIfTextDelivered(packet, userID);
			}
		}
	}

	private void checkIfTextDelivered(byte[] packet, int contactID) {
		if(packet[2]==Packet.P_TEXT && packet.length>19) {
			int userID = CryptChat.bytesToInt(packet,3);
			long packetID = CryptChat.bytesToLong(packet,7);
			sendPacket(userID, Packet.status(packetID,contactID,Packet.STATUS_DELIVERED),null);
		}
	}

	@Override
	public void packetStatus(int contactID, long packetID, byte status, Connection connection) {
		if(!connection.loggedIn())
			return;
		if(status==Packet.STATUS_READ[0]) {
			sendPacket(contactID,Packet.status(packetID,connection.getUserID(),Packet.STATUS_READ),null);
		}
	}

	@Override
	public void packetNewID(int contactID, long oldID, long newID) {
		// Only used by client.
	}

	@Override
	public void packetText(int contactID, long packetID, byte[] msg, Connection connection) {
		if(!connection.loggedIn())
			return;
		User user = users.get(contactID);
		if(user==null || user.blocked.contains(connection.getUserID()))
			return;
		int userID = connection.getUserID();
		long newID = System.currentTimeMillis();
		sendPacket(userID, Packet.newID(packetID,newID,contactID),connection);
		sendPacket(contactID, Packet.message(msg,userID,newID),null);
	}

	@Override
	public void packetLoginAgain(byte version) {

	}

	@Override
	public void packetRequestLogin(byte version, byte[] publicKey, Connection connection) {
		if(publicKey.length>100 && publicKey.length<800) {
			long pass = rand.nextLong();
			if (connection.sendPacket(Packet.login(++nextID, pass))) {
				connection.setUserID(nextID, version);
				User user = new User(nextID, pass);
				user.publicKey = publicKey;
				users.put(nextID, user);
				clients.put(nextID, connection);
				saveUser(nextID, pass, publicKey);
				if (nextID == 55)
					nextID = 15;
				if (nextID == 16)
					nextID = 1000;
			}
		}
	}

	@Override
	public void packetLogin(byte version, int userID, long pass, Connection connection) {
		if(connection==null || connection.loggedIn())
			return;
		User user = users.get(userID);
		if(user==null || user.login!=pass) return;
		connection.setUserID(userID, version);
		clients.put(userID, connection);
		List<Packet> cache = packetCache.get(userID);
		if(cache!=null) {
			while(!cache.isEmpty() && connection.loggedIn()) {
				sendPacket(userID, cache.remove(0).packet, connection);
			}
			if(cache.isEmpty()) {
				packetCache.remove(userID);
			}
		}
	}

	@Override
	public void packetContact(int contactID, boolean added, Connection connection) {
		if(!connection.loggedIn())
			return;
		if(added) {
			User contact = users.get(contactID);
			if(contact==null || contact.publicKey==null || contact.blocked.contains(connection.getUserID()))
				sendPacket(connection.getUserID(), Packet.contactStatus(contactID, false), connection);
			else
				sendPacket(connection.getUserID(), Packet.contactKey(contactID, contact.publicKey), connection);
		} else {
			users.get(connection.getUserID()).blocked.add(contactID);
			saveBlocked(connection.getUserID(), contactID);
		}
	}

	@Override
	public void packetContact(int contactID, byte[] publicKey) {
		// Client Only
	}

	@Override
	public void packetAESKey(int contactID, byte[] aesKey, Connection connection) {
		if(!connection.loggedIn())
			return;
		User user = users.get(contactID);
		if(user==null || user.blocked.contains(connection.getUserID()))
			return;
		sendPacket(contactID, Packet.aesKey(connection.getUserID(), aesKey), null);
	}
}