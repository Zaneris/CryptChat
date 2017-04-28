package ca.valacware.cryptchat;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import static ca.valacware.cryptchat.CryptChat.*;

public class StreamOut extends Thread {
	static final ArrayList<Packet> buffer = new ArrayList<>();
	private static final Packet ping = new Packet();
	private static Packet toSend;
	private DataOutputStream streamOut;
	private StreamIn streamIn;
	static Packet login;
	final Object lock = new Object();
	boolean connected = false;
	private Socket socket;

	@Override
	public void run() {
		while(true) {
			killAll();
			try {
				socket = new Socket(CC_HOST, CC_PORT);
				streamOut = new DataOutputStream(socket.getOutputStream());
				streamIn = new StreamIn(socket);
				streamIn.start();
				synchronized (lock) {
					connected = true;
				}
				int keepAlive = 0;
				synchronized (buffer) {
					if(login!=null)
						buffer.add(0,login);
				}
				while (true) {
					synchronized (buffer) {
						if(!buffer.isEmpty())
							toSend = buffer.remove(0);
					}
					if(toSend!=null) {
						streamOut.write(toSend.packet);
						streamOut.flush();
						toSend = null;
						keepAlive = 0;
					} else if(keepAlive>=100) {
						streamOut.write(ping.packet);
						streamOut.flush();
						keepAlive = 0;
					}
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					keepAlive++;
				}
			} catch (IOException e) {
				synchronized (lock) {
					connected = false;
				}
				//e.printStackTrace();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private void killAll() {
		try {
			if(streamIn!=null) synchronized (streamIn.lock) {
				streamIn.endLife = true;
			}
			if(streamOut!=null) streamOut.close();
			if(socket!=null) socket.close();
		} catch (IOException e) {
			// Ignore
		}
	}
}
