package ca.valacware.cryptchat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Created by Ted on 2017-01-13.
 */
class RSAKeyGen implements Runnable {
	@Override
	public void run() {
		KeyPairGenerator keyPairGen;
		try {
			keyPairGen = KeyPairGenerator.getInstance("RSA");
			keyPairGen.initialize(4096,new SecureRandom());
			KeyPair keyPair = keyPairGen.generateKeyPair();
			synchronized (CryptChat.keyLock) {
				CryptChat.rsaKey = keyPair;
			}
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
