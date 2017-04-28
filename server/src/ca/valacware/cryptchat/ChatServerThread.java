package ca.valacware.cryptchat;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ChatServerThread extends Thread implements Connection {
	private ChatServer server;
	private Socket socket;
	private DataInputStream streamIn;
	private DataOutputStream streamOut;
	private boolean loggedIn;
	private int userID;
	private byte version;

	ChatServerThread(ChatServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		loggedIn = false;
	}

	@Override
	public void killAll() {
		if(socket!=null) try {
			socket.close();
		} catch (IOException ignored) {}
		if(streamIn!=null) try {
			streamIn.close();
		} catch (IOException ignored) {}
		if(streamOut!=null) try {
			streamOut.close();
		} catch (IOException ignored) {}
		socket=null;
		streamIn=null;
		streamOut=null;
		loggedIn=false;
		if(userID>0) {
			server.removeClient(userID);
			System.out.println("UserID: " + userID + " has logged out.");
		}
		System.out.println("Client Disconnected!");
	}

	@Override
	public void run() {
		try {
			streamIn = new DataInputStream(socket.getInputStream());
			streamOut = new DataOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			killAll();
			return;
		}
		System.out.println("Client Connected: " + socket.getRemoteSocketAddress().toString());

		byte[] bytes = new byte[2];
		ByteBuffer byteBuf = ByteBuffer.allocate(2);
		short size;
		while (true) {
			try {
				bytes[0] = streamIn.readByte();
				bytes[1] = streamIn.readByte();
				size = bytesToShort(byteBuf,bytes);
				byte[] b = new byte[size];
				streamIn.readFully(b);
				server.handlePacket(b, this);
			} catch (IOException e) {
				killAll();
				return;
			}
		}
	}

	private short bytesToShort(ByteBuffer buffer, byte[] bytes) {
		buffer.clear();
		buffer.put(bytes, 0, 2);
		buffer.flip();
		return buffer.getShort();
	}

	@Override
	public int getUserID() {
		return userID;
	}

	@Override
	public boolean sendPacket(byte[] packet) {
		try {
			streamOut.write(packet);
			streamOut.flush();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	@Override
	public void setUserID(int userID, byte version) {
		this.userID = userID;
		this.version = version;
		loggedIn = true;
		System.out.println("UserID: " + userID + " has logged in.");
	}

	@Override
	public byte getPacketVersion() {
		return version;
	}

	@Override
	public boolean loggedIn() {
		return loggedIn;
	}
}