package ca.valacware.cryptchat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Ted on 2017-01-09.
 */
class FileBytesIn extends FileInputStream {
	private static final byte[] byteLong  = new byte[8];
	private static final byte[] byteInt	  = new byte[4];
	private static final byte[] byteShort = new byte[2];

	FileBytesIn(File file) throws FileNotFoundException {
		super(file);
	}

	long readLong() throws IOException {
		read(byteLong);
		return CryptChat.bytesToLong(byteLong);
	}

	int readInt() throws IOException {
		read(byteInt);
		return CryptChat.bytesToInt(byteInt);
	}

	short readShort() throws IOException {
		read(byteShort);
		return CryptChat.bytesToShort(byteShort);
	}

	boolean readBool() throws IOException {
		return read()>0;
	}
}
