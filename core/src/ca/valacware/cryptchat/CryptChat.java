package ca.valacware.cryptchat;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntSet;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

import static ca.valacware.cryptchat.Reversed.reversed;

public class CryptChat extends ApplicationAdapter implements InputProcessor, PacketInterface {
	//region Constants
	static final int CC_PORT = 443;
	static final String CC_HOST = "74.208.120.68";

	private static final Color CC_COLOR_0_SENDING 		= new Color(0.9f, 0.0f, 0.0f, 1.0f);
	private static final Color CC_COLOR_1_SENT 			= new Color(0.9f, 0.9f, 0.0f, 1.0f);
	private static final Color CC_COLOR_2_DELIVERED 	= new Color(0.6f, 0.9f, 0.0f, 1.0f);
	private static final Color CC_COLOR_3_READ 			= new Color(0.0f, 0.9f, 0.0f, 1.0f);
	private static final Color CC_COLOR_OTHER 			= new Color(0.0f, .75f, 0.9f, 1.0f);
	private static final Color CC_COLOR_BUBBLE_OTHER 	= new Color(.15f, .15f, .15f, 1.0f);
	private static final Color CC_COLOR_BUBBLE_CLIENT 	= new Color(0.2f, 0.2f, 0.2f, 1.0f);
	private static final Color CC_COLOR_LINE 			= new Color(0.0f, 0.9f, 0.0f, 1.0f);
	private static final float CC_PADDING = 9f;
	private static final float CC_BUBBLE_RADIUS = 10f;

	private static final ByteBuffer bufferLong = ByteBuffer.allocate(8);
	private static final ByteBuffer bufferInt = ByteBuffer.allocate(4);
	private static final ByteBuffer bufferShort = ByteBuffer.allocate(2);

	private static final byte CC_STATE_AWAITING_USER_ID = 0x00;
	private static final byte CC_STATE_NEW_PASSPHRASE 	= 0x01;
	private static final byte CC_STATE_NEW_PASSPHRASE2 	= 0x02;
	private static final byte CC_STATE_ENTER_PASSPHRASE	= 0x03;
	private static final byte CC_STATE_CONTACTS			= 0x04;
	private static final byte CC_STATE_CONTACT_ID		= 0x05;
	private static final byte CC_STATE_CONTACT_KEY		= 0x06;
	private static final byte CC_STATE_CHAT				= 0x07;
	private static final byte CC_STATE_INFO				= 0x08;
	private static final byte CC_STATE_NICKNAME			= 0x09;
	private static final byte CC_STATE_RSA				= 0x0A;

	private static final String CC_STRING_USER_NEW 				= "Requesting new contact # ...";
	private static final String CC_STRING_USER_ID 				= "Your contact # is ";
	private static final String CC_STRING_PASSPHRASE_NEW 		= "Enter a passphrase for your account.";
	private static final String CC_STRING_PASSPHRASE_CONFIRM 	= "Re-enter passphrase to confirm.";
	private static final String CC_STRING_PASSPHRASE_ENTER 		= "Enter passphrase.";
	private static final String CC_STRING_FIRST_CONTACT			= "Add your first contact.";
	private static final String CC_STRING_CONTACT_ID			= "Enter their contact #.";
	private static final String CC_STRING_CONTACT_KEY			= "Enter a 256-bit AES encryption key.";
	private static final String CC_STRING_NICKNAME				= "Enter a nickname for contact # ";
	private static final String CC_STRING_WELCOME				= "Welcome to CryptChat!";
	private static final String CC_STRING_RSA					= "Generating 4096-bit RSA encryption key pair, please wait ...";

	private static final String CC_ERROR_PASSPHRASE_NO_MATCH 	= "Passphrase did not match, try again.";
	private static final String CC_ERROR_PASSPHRASE_INVALID 	= "Invalid passphrase, try again.";
	private static final String CC_ERROR_KEY_INVALID 			= "Invalid key, must be 256 bits (32 bytes, 64 characters in hex 0-9 A-F).";
	private static final String CC_ERROR_ID_INVALID 			= "Not a valid Contact #.";
	private static final String CC_ERROR_RSA					= "Still generating key pair.";
	private static final String CC_ERROR_CONTACT_NO_EXIST		= "No contact exists with # ";
	//endregion
	//region Primitives
	private float height, width, halfHeight, halfWidth;
	private float pad, halfPad, doublePad;
	private float radius;
	private int backCounter;
	private float minHeight;
	//endregion
	//region Objects
	private static ByteBuffer byteBuffer;
	private BitmapFont fontText;
	private SpriteBatch batch;
	private Shapes shapes;
	private GlyphLayout glyph;
	static final Object textLock = new Object();
	static final Object keyLock = new Object();
	String textBox = "";
	private StreamOut streamOut;
	private User user;
	private String passphrase;
	private byte state;
	private List<String> info;
	private HashMap<Integer, Contact> contacts;
	private Contact activeContact;
	private KeyGenerator keyGen;
	static NativeInterface system;
	private static Key aesKey;
	static KeyPair rsaKey;
	private PrivateKey privateKey;
	private static Cipher aesCipher;
	private static Cipher rsaCipher;
	private static SecureRandom secureRandom;
	private static KeyFactory keyFactory;
	private static IvParameterSpec iv;
	private static byte[] byteIV;
	private IntSet keysDown;
	private boolean resetPaste;
	//endregion

	public CryptChat(NativeInterface nativeInterface) {
		system = nativeInterface;
	}

	@Override
	public void create() {
		Gdx.input.setInputProcessor(this);
		float scale = Gdx.graphics.getDensity();

		shapes = new Shapes();
		batch = new SpriteBatch();
		glyph = new GlyphLayout();
		info = new ArrayList<>();
		keysDown = new IntSet();
		resetPaste = true;
		pad = CC_PADDING * scale;
		halfPad = pad / 2f;
		doublePad = pad * 2f;
		radius = CC_BUBBLE_RADIUS * scale;

		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
				Gdx.files.internal("fonts/arial.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();

		if (system.isMobile()) {
			parameter.size = MathUtils.round(16f * scale);
		} else {
			parameter.size = MathUtils.round(22f * scale);
			Gdx.graphics.setWindowedMode(
					MathUtils.round(500f * scale),
					MathUtils.round(700f * scale));
		}

		fontText = generator.generateFont(parameter);
		generator.dispose();
		updateSizes();
		updateCam();

		contacts = new HashMap<>();
		secureRandom = new SecureRandom();
		byteIV = new byte[16];
		try {
			aesCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256,secureRandom);
			keyFactory = KeyFactory.getInstance("RSA");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			e.printStackTrace();
		}
		streamOut = new StreamOut();
		streamOut.start();
		checkUserData();
	}

	//region Encryption
	private void genKey(String strPass) {
		byte[] key = new byte[32];
		byte[] bPass = strPass.getBytes();
		byte len = (byte)bPass.length;
		for(int i = 0; i < 32; i++) {
			if(i<bPass.length)
				key[i] = bPass[i];
			else
				key[i] = ++len;
		}
		aesKey = new SecretKeySpec(key, "AES");
		passphrase = "";
	}

	private byte[] rsaEncrypt(byte[] aesKey, PublicKey publicKey) {
		try {
			rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			return rsaCipher.doFinal(aesKey);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	private byte[] rsaDecrypt(byte[] encoded) {
		try {
			rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
			return rsaCipher.doFinal(encoded);
		} catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	static byte[] encryptString(String str, Key key) {
		try {
			return encryptBytes(str.getBytes("UTF-8"), key);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	static byte[] encryptBytes(byte[] bytes, Key key) {
		secureRandom.nextBytes(byteIV);
		iv = new IvParameterSpec(byteIV);
		try {
			aesCipher.init(Cipher.ENCRYPT_MODE, key, iv);
			bytes = aesCipher.doFinal(bytes);
			byte[] encrypted = Arrays.copyOf(byteIV,bytes.length+16);
			System.arraycopy(bytes, 0, encrypted, 16, bytes.length);
			return encrypted;
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| BadPaddingException | IllegalBlockSizeException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	static void encryptStringToFile(String str, File file) {
		FileOutputStream outStream = null;
		try {
			outStream = new FileOutputStream(file, false);
			outStream.write(encryptString(str, aesKey));
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(outStream!=null) {
				try {
					outStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	static String decryptToString(byte[] bytes, Key key) {
		try {
			return new String(decryptToBytes(bytes, key), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}

	static byte[] decryptToBytes(byte[] bytes, Key key) {
		System.arraycopy(bytes, 0, byteIV, 0, 16);
		bytes = Arrays.copyOfRange(bytes, 16, bytes.length);
		iv = new IvParameterSpec(byteIV);
		try {
			aesCipher.init(Cipher.DECRYPT_MODE, key, iv);
			return aesCipher.doFinal(bytes);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException
				| BadPaddingException | IllegalBlockSizeException e) {
			e.printStackTrace();
			return new byte[0];
		}
	}

	private void saveKey() {
		File file = system.getFile("data/key.dat");
		FileBytesOut streamOut = null;
		byte[] encoded = encryptBytes(privateKey.getEncoded(), aesKey);
		try {
			streamOut = new FileBytesOut(file, false);
			streamOut.write(encoded);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (streamOut != null)
				try {
					streamOut.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	private void loadKey() {
		File file = system.getFile("data/key.dat");
		FileBytesIn streamIn = null;
		byte[] key = null;
		try {
			streamIn = new FileBytesIn(file);
			key = new byte[streamIn.available()];
			streamIn.read(key);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (streamIn != null)
				try {
					streamIn.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
		if(key!=null) {
			key = decryptToBytes(key, aesKey);
			privateKey = getPrivateKey(key);
		}
	}

	static String decryptStringFromFile(File file) {
		FileInputStream inputStream = null;
		byte[] bFile = new byte[(int) file.length()];
		try
		{
			inputStream = new FileInputStream(file);
			inputStream.read(bFile);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if(inputStream!=null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return decryptToString(bFile, aesKey);
	}

	private static PublicKey getPublicKey(byte[] encoded) {
		try {
			return keyFactory.generatePublic(new X509EncodedKeySpec(encoded));
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static PrivateKey getPrivateKey(byte[] encoded) {
		try {
			return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encoded));
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}
	//endregion

	private void checkUserData() {
		File data = system.getFile("data");
		if(!data.exists()) data.mkdir();
		data = system.getFile("data/user.dat");
		if(!data.exists()) {
			setState(CC_STATE_RSA);
		} else {
			setState(CC_STATE_ENTER_PASSPHRASE);
		}
	}

	private void saveUserData() {
		String data = "CryptChat/" + user.userID + "/" + user.login;
		encryptStringToFile(data, system.getFile("data/user.dat"));
	}

	private boolean loadUserData() {
		String[] data = decryptStringFromFile(system.getFile("data/user.dat")).split("/");
		if(data[0].matches("CryptChat")) {
			user = new User(Integer.parseInt(data[1]), Long.parseLong(data[2]));
		} else return false;
		return true;
	}

	private boolean loadContacts() {
		File[] files = system.getFile("data").listFiles();
		if (files != null) {
			for(File contact:files) {
				if(contact.isDirectory()) {
					Contact toAdd = new Contact(Integer.parseInt(contact.getName()));
					if(activeContact==null)
						activeContact = toAdd;
					contacts.put(toAdd.contactID, toAdd);
				}
			}
		}
		return activeContact != null;
	}

	private void setState(byte state) {
		this.state = state;
		switch (state) {
			case CC_STATE_RSA:
				info.add(CC_STRING_WELCOME);
				info.add(CC_STRING_RSA);
				new Thread(new RSAKeyGen()).start();
				break;
			case CC_STATE_AWAITING_USER_ID:
				info.add(CC_STRING_USER_NEW);
				break;
			case CC_STATE_NEW_PASSPHRASE:
				info.add(CC_STRING_PASSPHRASE_NEW);
				system.setInputPassword();
				break;
			case CC_STATE_NEW_PASSPHRASE2:
				info.add(CC_STRING_PASSPHRASE_CONFIRM);
				break;
			case CC_STATE_ENTER_PASSPHRASE:
				info.add(CC_STRING_PASSPHRASE_ENTER);
				system.setInputPassword();
				break;
			case CC_STATE_CHAT:
				if(activeContact!=null) {
					info.clear();
					system.setInputTextAutoComplete();
				}
				break;
			case CC_STATE_CONTACTS:
				// TODO - Contact display.
				break;
			case CC_STATE_CONTACT_ID:
				if(activeContact==null)
					info.add(CC_STRING_FIRST_CONTACT + "\n" + CC_STRING_CONTACT_ID);
				else
					info.add(CC_STRING_CONTACT_ID);
				system.setInputNumeric();
				break;
			case CC_STATE_CONTACT_KEY:
				info.add(CC_STRING_CONTACT_KEY);
				system.setInputText();
				break;
			case CC_STATE_INFO:
				system.setInputText();
				break;
			case CC_STATE_NICKNAME:
				info.add(CC_STRING_NICKNAME + activeContact.contactID);
				system.setInputTextAutoComplete();
				break;
		}
	}

	private boolean connected() {
		if (streamOut == null)
			return false;
		synchronized (streamOut.lock) {
			return streamOut.connected;
		}
	}

	private void textEntered(String msg) {
		if(state==CC_STATE_RSA) {
			info.add(CC_ERROR_RSA);
			return;
		}
		msg = msg.trim();
		if(msg.isEmpty())
			return;
		if(msg.startsWith("/")) {
			String[] cmd = msg.split(" ");
			boolean match = false;
			switch (cmd[0]) {
				case "/add":
					setState(CC_STATE_CONTACT_ID);
					match = true;
					break;
				case "/list":
					setState(CC_STATE_INFO);
					String s = "Contacts: ";
					for(Integer i:contacts.keySet()) {
						s += i + " ";
					}
					info.add(s.trim());
					match = true;
					break;
				case "/chat":
					setState(CC_STATE_CHAT);
					match = true;
					break;
				case "/switch":
					if(cmd.length>1) {
						int x = Integer.parseInt(cmd[1]);
						Contact contact = contacts.get(x);
						if(contact != null) {
							activeContact = contact;
							setState(CC_STATE_CHAT);
						}
					}
					match = true;
					break;
			}
			if(match) return;
		}
		switch (state) {
			case CC_STATE_CHAT:
				activeContact.sendMessage(msg);
				break;
			case CC_STATE_NEW_PASSPHRASE:
				passphrase = msg;
				setState(CC_STATE_NEW_PASSPHRASE2);
				break;
			case CC_STATE_NEW_PASSPHRASE2:
				if(passphrase.matches(msg)) {
					genKey(msg);
					saveUserData();
					saveKey();
					setState(CC_STATE_CONTACT_ID);
				} else {
					info.add(CC_ERROR_PASSPHRASE_NO_MATCH);
					setState(CC_STATE_NEW_PASSPHRASE);
				}
				break;
			case CC_STATE_ENTER_PASSPHRASE:
				genKey(msg);
				if(loadUserData()) {
					loadKey();
					synchronized (StreamOut.buffer) {
						StreamOut.login = new Packet(Packet.login(user.userID,user.login));
					}
					sendPacket(Packet.login(user.userID,user.login));
					if(loadContacts()) {
						setState(CC_STATE_CHAT);
					} else {
						setState(CC_STATE_CONTACT_ID);
					}
				} else {
					info.add(CC_ERROR_PASSPHRASE_INVALID);
					setState(CC_STATE_ENTER_PASSPHRASE);
				}
				break;
			case CC_STATE_CONTACT_ID:
				try {
					int newContact = Integer.parseInt(msg);
					activeContact = new Contact(newContact);
					sendPacket(Packet.contactStatus(newContact,true));
				} catch(NumberFormatException e) {
					info.add(CC_ERROR_ID_INVALID);
					setState(CC_STATE_CONTACT_ID);
				}
				break;
			case CC_STATE_CONTACT_KEY:
				msg = msg.replaceAll("\\s+","");
				msg = msg.toUpperCase();
				boolean valid = false;
				if(msg.length()==64) {
					for (char c : msg.toCharArray()) {
						valid = false;
						for (char x : hexArray) {
							if (x == c) {
								valid = true;
								break;
							}
						}
						if(!valid) break;
					}
				}
				if(!valid) {
					info.add(CC_ERROR_KEY_INVALID);
					setState(CC_STATE_CONTACT_KEY);
					system.setInputBox(bytesToBase16(keyGen.generateKey().getEncoded(), true));
				} else {
					byte[] aeskey = base16ToBytes(msg);
					activeContact.initialize(aeskey);
					contacts.put(activeContact.contactID, activeContact);
					sendPacket(Packet.aesKey(activeContact.contactID, rsaEncrypt(aeskey, activeContact.publicKey)));
					setState(CC_STATE_NICKNAME);
				}
				break;
			case CC_STATE_NICKNAME:
				msg = msg.replaceAll("[/]", "");
				activeContact.nickname = msg;
				activeContact.saveStatuses();
				setState(CC_STATE_CHAT);
				break;
		}
	}

	//region Graphics
	@Override
	public void render() {
		if(state==CC_STATE_RSA) {
			synchronized (keyLock) {
				if(rsaKey!=null) {
					setState(CC_STATE_AWAITING_USER_ID);
					sendPacket(Packet.newUser(rsaKey.getPublic().getEncoded()));
					privateKey = rsaKey.getPrivate();
				}
			}
		}
		long time = System.currentTimeMillis();

		synchronized (StreamIn.buffer) {
			for (Packet p : StreamIn.buffer)
				Packet.action(this, p.packet);
			StreamIn.buffer.clear();
		}

		Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		if(system.isMobile()) {
			synchronized (textLock) {
				if(!textBox.isEmpty()) {
					textEntered(textBox);
					textBox = "";
				}
			}
			drawBubbleText(-halfHeight);
		} else {
			manageInput();
			drawBubbleText(drawChatBox(time));
		}
		drawConnectionStatus(time);
	}

	private float drawChatBox(long time) {
		batch.begin();
		glyph.setText(fontText,
				(time % 1000 < 500 ? textBox + "|" : textBox),
				CC_COLOR_3_READ, width - pad * 2f, Align.left, true);
		fontText.draw(batch, glyph,
				-halfWidth + pad,
				-halfHeight + pad + (minHeight > glyph.height ? minHeight : glyph.height));
		batch.end();
		return -halfHeight + (minHeight > glyph.height ? minHeight : glyph.height) + pad;
	}

	private void drawConnectionStatus(long time) {
		if(connected()) {
			shapes.begin(ShapeRenderer.ShapeType.Filled);
			shapes.setColor(CC_COLOR_3_READ);
			shapes.circle(halfWidth - doublePad, halfHeight - doublePad, pad);
			shapes.end();
		} else if (time%2000 < 1000) {
			shapes.begin(ShapeRenderer.ShapeType.Filled);
			shapes.setColor(CC_COLOR_0_SENDING);
			shapes.circle(halfWidth - doublePad, halfHeight - doublePad, pad);
			shapes.end();
		}
	}

	private void drawBubbleText(float start) {
		boolean bubble = true;
		float position;
		do {
			position = start + pad + halfPad;
			if (bubble) {
				shapes.begin(ShapeRenderer.ShapeType.Filled);
				shapes.setColor(CC_COLOR_LINE);
				shapes.rectLine(-halfWidth, start + pad, halfWidth, start + pad, halfPad / 3f);
			} else batch.begin();
			position += halfPad;
			if(state == CC_STATE_CHAT) {
				for (Map.Entry<Long, Message> entry : activeContact.chat.entrySet()) {
					Message m = entry.getValue();
					setGlyphAuto(entry.getKey(), m);
					if (m.isClient()) {
						if (bubble) drawBubbleRight(position, CC_COLOR_BUBBLE_CLIENT);
						else drawTextRight(position);
					} else {
						if (bubble) drawBubbleLeft(position, CC_COLOR_BUBBLE_OTHER);
						else drawTextLeft(position);
					}
					position += glyph.height + doublePad + halfPad;
					if (position > halfHeight)
						break;
				}
			} else {
				for(String s:reversed(info)) {
					setGlyph(s, CC_COLOR_1_SENT);
					if (bubble) drawBubbleLeft(position, CC_COLOR_BUBBLE_OTHER);
					else drawTextLeft(position);
					position += glyph.height + doublePad + halfPad;
				}
			}
			if (bubble) shapes.end();
			else batch.end();
			bubble = !bubble;
		} while (!bubble);
	}

	private void drawBubbleLeft(float position, Color color) {
		shapes.setColor(color);
		shapes.roundedRect(
				-halfWidth + pad, position,
				glyph.width + doublePad, glyph.height + doublePad, radius);
	}

	private void drawBubbleRight(float position, Color color) {
		shapes.setColor(color);
		shapes.roundedRect(
				halfWidth - glyph.width - doublePad - pad, position,
				glyph.width + doublePad, glyph.height + doublePad, radius);
	}

	private void drawTextLeft(float position) {
		fontText.draw(
				batch, glyph,
				-halfWidth + pad * 2f,
				position + glyph.height + pad);
	}

	private void drawTextRight(float position) {
		fontText.draw(
				batch, glyph,
				halfWidth - glyph.width - doublePad,
				position + glyph.height + pad);
	}

	@Override
	public void resize(int width, int height) {
		updateSizes();
		updateCam();
	}

	private void updateCam() {
		OrthographicCamera cam = new OrthographicCamera(width, height);
		shapes.setProjectionMatrix(cam.combined);
		batch.setProjectionMatrix(cam.combined);
	}

	private void updateSizes() {
		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();
		halfWidth = width / 2f;
		halfHeight = height / 2f;
		glyph.setText(fontText, "|", CC_COLOR_3_READ, width, Align.left, false);
		minHeight = glyph.height;
	}

	private void setGlyph(String text, Color color) {
		glyph.setText(fontText, text, color, width*0.8f, Align.left, true);
	}

	private void setGlyphAuto(long id, Message m) {
		Color color = CC_COLOR_OTHER;
		if (m.isClient()) {
			if(id<=activeContact.lastRead) color = CC_COLOR_3_READ;
			else if(id<=activeContact.lastDelivered) color = CC_COLOR_2_DELIVERED;
			else if(id<=activeContact.lastSent) color = CC_COLOR_1_SENT;
			else color = CC_COLOR_0_SENDING;
		}
		glyph.setText(fontText, m.getMsg(), color, width*0.8f, Align.left, true);
	}
	//endregion
	//region Input
	private void manageInput() {
		if (backCounter > 0) {
			if (backCounter > 10) {
				if (textBox.length() > 0) {
					textBox = textBox.substring(0, textBox.length() - 1);
				} else backCounter = 0;
			} else backCounter++;
		}
	}

	@Override
	public boolean keyUp(int keycode) {
		switch (keycode) {
			case Input.Keys.BACKSPACE:
				backCounter = 0;
				break;
			case Input.Keys.V:
				resetPaste = true;
				break;
		}
		keysDown.remove(keycode);
		return false;
	}

	@Override
	public boolean keyDown(int keycode) {
		switch (keycode) {
			case Input.Keys.BACKSPACE:
				backCounter = 1;
				if (textBox.length() > 0)
					textBox = textBox.substring(0, textBox.length() - 1);
				break;
			case Input.Keys.ENTER:
				textEntered(textBox);
				textBox = "";
				backCounter = 0;
				break;
		}
		keysDown.add(keycode);
		if(resetPaste && keysDown.contains(Input.Keys.CONTROL_LEFT) && keysDown.contains(Input.Keys.V)) {
			textBox += system.copyClipboard();
		}
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		if (!Character.isIdentifierIgnorable(character))
			textBox += character;
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
	//endregion
	//region Byte Tools
	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();
	static String bytesToBase16(byte[] bytes, boolean withLines) {
		if(bytes==null || bytes.length==0)
			return "";
		char[] hexChars;
		if(withLines) {
			hexChars = new char[bytes.length * 2 + ((bytes.length - 1) / 16)];
			for (int j = 0; j < bytes.length; j++) {
				int v = bytes[j] & 0xFF;
				hexChars[j * 2 + j / 16] = hexArray[v >>> 4];
				hexChars[j * 2 + j / 16 + 1] = hexArray[v & 0x0F];
				if (((j + 1) < bytes.length) && (j + 1) % 16 == 0) hexChars[j * 2 + j / 16 + 2] = '\n';
			}
		} else {
			hexChars = new char[bytes.length * 2];
			for (int j = 0; j < bytes.length; j++) {
				int v = bytes[j] & 0xFF;
				hexChars[j * 2] = hexArray[v >>> 4];
				hexChars[j * 2 + 1] = hexArray[v & 0x0F];
			}
		}
		return new String(hexChars);
	}

	static byte[] base16ToBytes(String s) {
		s = s.replaceAll("\\s+","");
		s = s.toUpperCase();
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	static String bytesToBase64(byte[] bytes) {
		return new String(Base64Coder.encode(bytes));
	}

	static byte[] base64ToBytes(String str) {
		str = "This is unique! Yeah!";
		str = str.replaceAll("\\s+","/");
		str = str.replaceAll("[@_!#$%&]", "+");
		System.out.println(str);
		int j = str.length()%4;
		if(j==3) str = str + "=";
		else if(j==2) str = str + "==";
		else str = str + "+==";
		return Base64Coder.decode(str);
	}

	static byte[] longToBytes(long x) {
		byteBuffer = ByteBuffer.allocate(8);
		byteBuffer.putLong(0, x);
		return byteBuffer.array();
	}

	static byte[] intToBytes(int x) {
		byteBuffer = ByteBuffer.allocate(4);
		byteBuffer.putInt(0, x);
		return byteBuffer.array();
	}

	static byte[] shortToBytes(int x) {
		return shortToBytes((short)x);
	}

	static byte[] shortToBytes(short x) {
		byteBuffer = ByteBuffer.allocate(2);
		byteBuffer.putShort(0, x);
		return byteBuffer.array();
	}

	static long bytesToLong(byte[] bytes) {
		return bytesToLong(bytes,0);
	}

	static long bytesToLong(byte[] bytes, int offset) {
		bufferLong.clear();
		bufferLong.put(bytes, offset, 8);
		bufferLong.flip();
		return bufferLong.getLong();
	}

	static int bytesToInt(byte[] bytes) {
		return bytesToInt(bytes, 0);
	}

	static int bytesToInt(byte[] bytes, int offset) {
		bufferInt.clear();
		bufferInt.put(bytes, offset, 4);
		bufferInt.flip();
		return bufferInt.getInt();
	}

	static short bytesToShort(byte[] bytes) {
		return bytesToShort(bytes, 0);
	}

	static short bytesToShort(byte[] bytes, int offset) {
		bufferShort.clear();
		bufferShort.put(bytes, offset, 2);
		bufferShort.flip();
		return bufferShort.getShort();
	}
	//endregion
	//region Packets
	static void sendPacket(byte[] packet) {
		synchronized (StreamOut.buffer) {
			StreamOut.buffer.add(new Packet(packet));
		}
	}

	@Override
	public void packetStatus(int contactID, long packetID, byte status, Connection connection) {
		Contact contact = contacts.get(contactID);
		if(contact!=null) {
			contact.updateStatus(packetID, status==Packet.STATUS_READ[0]);
		}
	}

	@Override
	public void packetNewID(int contactID, long oldID, long newID) {
		Contact contact = contacts.get(contactID);
		if(contact!=null) {
			contact.updateID(oldID, newID);
		}
	}

	@Override
	public void packetText(int contactID, long packetID, byte[] msg, Connection connection) {
		Contact contact = contacts.get(contactID);
		if(contact!=null) {
			contact.receiveMessage(packetID, msg);
		}
	}

	@Override
	public void packetLoginAgain(byte version) {
		if(user!=null) sendPacket(Packet.login(user.userID,user.login));
	}

	@Override
	public void packetLogin(byte version, int userID, long pass, Connection connection) {
		if(state==CC_STATE_AWAITING_USER_ID) {
			user = new User(userID, pass);
			info.add(CC_STRING_USER_ID + userID);
			setState(CC_STATE_NEW_PASSPHRASE);
			synchronized (StreamOut.buffer) {
				StreamOut.login = new Packet(Packet.login(userID,pass));
			}
		}
	}

	@Override
	public void packetRequestLogin(byte version, byte[] publicKey, Connection connection) {
		// Server Only
	}

	@Override
	public void packetContact(int contactID, boolean added, Connection connection) {
		if(state==CC_STATE_CONTACT_ID && activeContact!=null && activeContact.contactID==contactID) {
			info.add(CC_ERROR_CONTACT_NO_EXIST + contactID);
			activeContact = null;
			setState(CC_STATE_CONTACT_ID);
		}
	}

	@Override
	public void packetContact(int contactID, byte[] publicKey) {
		if(state==CC_STATE_CONTACT_ID && activeContact!=null && activeContact.contactID==contactID) {
			info.clear();
			setState(CC_STATE_CONTACT_KEY);
			system.setInputBox(bytesToBase16(keyGen.generateKey().getEncoded(), true));
			activeContact.publicKey = getPublicKey(publicKey);
		}
	}

	@Override
	public void packetAESKey(int contactID, byte[] aesKey, Connection connection) {
		if(!contacts.containsKey(contactID)) {
			Contact newContact = new Contact(contactID);
			newContact.nickname = Integer.toString(contactID);
			System.out.println(aesKey.length);
			newContact.initialize(rsaDecrypt(aesKey));
			contacts.put(contactID, newContact);
			if(activeContact==null) {
				activeContact = newContact;
				setState(CC_STATE_CHAT);
			}
		}
	}
	//endregion
}