package rip.jnic.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtils
{
    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int SALT_LENGTH_BYTE = 16;
    private static final Charset UTF_8;
    
    public static byte[] encrypt(final byte[] pText, final String password) throws Exception {
        final byte[] salt = CryptoUtils.getRandomNonce(16);
        final byte[] iv = CryptoUtils.getRandomNonce(12);
        final SecretKey aesKeyFromPassword = CryptoUtils.getAESKeyFromPassword(password.toCharArray(), salt);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(1, aesKeyFromPassword, new GCMParameterSpec(128, iv));
        final byte[] cipherText = cipher.doFinal(pText);
        final byte[] cipherTextWithIvSalt = ByteBuffer.allocate(iv.length + salt.length + cipherText.length).put(iv).put(salt).put(cipherText).array();
        return cipherTextWithIvSalt;
    }
    
    private static byte[] decrypt(final byte[] cText, final String password) throws Exception {
        final ByteBuffer bb = ByteBuffer.wrap(cText);
        final byte[] iv = new byte[12];
        bb.get(iv);
        final byte[] salt = new byte[16];
        bb.get(salt);
        final byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);
        final SecretKey aesKeyFromPassword = CryptoUtils.getAESKeyFromPassword(password.toCharArray(), salt);
        final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(2, aesKeyFromPassword, new GCMParameterSpec(128, iv));
        final byte[] plainText = cipher.doFinal(cipherText);
        return plainText;
    }
    
    public static void encryptFile(final Path fromFile, final Path toFile, final String password) throws Exception {
        final byte[] fileContent = Files.readAllBytes(fromFile);
        final byte[] encryptedText = encrypt(fileContent, password);
        Files.write(toFile, encryptedText, new OpenOption[0]);
    }
    
    public static byte[] decryptFile(final String fromEncryptedFile, final String password) throws Exception {
        final byte[] fileContent = Files.readAllBytes(Paths.get(fromEncryptedFile, new String[0]));
        return decrypt(fileContent, password);
    }
    
    public static void decryptToFile(final Path fromFile, final Path toFile, final String password) throws Exception {
        final byte[] fileContent = Files.readAllBytes(fromFile);
        Files.write(toFile, decrypt(fileContent, password), new OpenOption[0]);
    }
    
    public static void clearDirectory(final String path) {
        final File directory = new File(path);
        if (!directory.exists()) {
            return;
        }
        final Path basePath = directory.toPath();
        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        catch (Exception e) {
            System.out.println("删除" + path + "失败,请先手动删除后再运行本程序!");
        }
    }
    
    static {
        UTF_8 = StandardCharsets.UTF_8;
    }
}
