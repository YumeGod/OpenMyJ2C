package rip.jnic.utils;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.List;

public class CryptoUtils
{
    public static String hex(final byte[] bytes) {
        final StringBuilder result = new StringBuilder();
        for (final byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    public static String hexWithBlockSize(final byte[] bytes, int blockSize) {
        final String hex = hex(bytes);
        blockSize *= 2;
        final List<String> result = new ArrayList<String>();
        for (int index = 0; index < hex.length(); index += blockSize) {
            result.add(hex.substring(index, Math.min(index + blockSize, hex.length())));
        }
        return result.toString();
    }
    
    public static byte[] getRandomNonce(final int numBytes) {
        final byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }
    
    public static SecretKey getAESKey(final int keysize) throws NoSuchAlgorithmException {
        final KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(keysize, SecureRandom.getInstanceStrong());
        return keyGen.generateKey();
    }
    
    public static SecretKey getAESKeyFromPassword(final char[] password, final byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        final KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
        final SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }
}
