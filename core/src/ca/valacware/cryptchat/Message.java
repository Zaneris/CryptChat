package ca.valacware.cryptchat;

import java.io.Serializable;

public class Message implements Serializable {
	private String msg;
	private boolean client;

	public Message(String msg, boolean client) {
		this.msg = msg;
		this.client = client;
	}

	boolean isClient() {
		return client;
	}

	String getMsg() {
		return msg;
	}
}
