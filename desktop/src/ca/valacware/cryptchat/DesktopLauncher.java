package ca.valacware.cryptchat;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;

public class DesktopLauncher implements NativeInterface{
	private static CryptChat cryptChat;
	private static Clipboard clipboard;

	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		config.width = 250;
		config.height = 350;
		config.resizable = true;
		config.foregroundFPS = 15;
		config.backgroundFPS = 4;
		cryptChat = new CryptChat(new DesktopLauncher());
		new LwjglApplication(cryptChat, config);
	}

	@Override
	public void setInputBox(String str) {
		cryptChat.textBox = str;
	}

	@Override
	public void setInputPassword() {

	}

	@Override
	public void setInputTextAutoComplete() {

	}

	@Override
	public void setInputText() {

	}

	@Override
	public void setInputNumeric() {

	}

	@Override
	public String copyClipboard() {
		try {
			return (String) clipboard.getData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException e) {
			return "";
		}
	}

	@Override
	public void shareText(String share) {
		StringSelection stringSelection = new StringSelection(share);
		clipboard.setContents(stringSelection, null);
	}

	@Override
	public File getFile(String file) {
		return new File(file);
	}

	@Override
	public boolean isMobile() {
		return false;
	}
}
