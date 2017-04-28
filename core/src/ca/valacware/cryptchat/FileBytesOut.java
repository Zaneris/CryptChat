package ca.valacware.cryptchat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileBytesOut extends FileOutputStream {

	FileBytesOut(File file, boolean append) throws FileNotFoundException {
		super(file, append);
	}

	void write(long x) throws IOException {
		this.write(CryptChat.longToBytes(x));
	}

	@Override
	public void write(int x) throws IOException {
		this.write(CryptChat.intToBytes(x));
	}

	void write(short x) throws IOException {
		this.write(CryptChat.shortToBytes(x));
	}

	void write(boolean bool) throws IOException {
		this.write(new byte[]{(byte)(bool?1:0)});
	}
}