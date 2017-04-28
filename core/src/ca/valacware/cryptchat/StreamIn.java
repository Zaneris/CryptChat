package ca.valacware.cryptchat;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class StreamIn extends Thread {
	static final ArrayList<Packet> buffer = new ArrayList<>();
	private DataInputStream streamIn;
	final Object lock = new Object();
	boolean endLife = false;

	StreamIn(Socket socket) throws IOException {
		streamIn = new DataInputStream(socket.getInputStream());
	}

	private void killAll() {
		try {
			if(streamIn!=null) streamIn.close();
		} catch (IOException e) {
			// Ignore
		}
	}

	@Override
	public void run() {
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
				synchronized (buffer) {
					buffer.add(new Packet(b));
				}
			} catch (IOException e) {
				killAll();
				return;
			}
			synchronized (lock) {
				if(endLife) {
					killAll();
					return;
				}
			}
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private short bytesToShort(ByteBuffer buffer, byte[] bytes) {
		buffer.clear();
		buffer.put(bytes, 0, 2);
		buffer.flip();
		return buffer.getShort();
	}
}
