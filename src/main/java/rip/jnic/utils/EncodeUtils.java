package rip.jnic.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

public class EncodeUtils
{
    public static final String UTF_8 = "UTF-8";
    private static final char[] BASE62;
    
    public static String encodeHex(final byte[] input) {
        return HexUtil.encodeToString(input);
    }
    
    public static byte[] decodeHex(final String input) {
        return HexUtil.decode(input.getBytes(StandardCharsets.UTF_8));
    }
    
    public static String encodeBase64(final byte[] input) {
        return Base64Util.encrypt(input);
    }
    
    public static String encodeBase64(final String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        try {
            return Base64Util.encrypt(input.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e) {
            return "";
        }
    }
    
    public static byte[] decodeBase64(final String input) {
        try {
            return Base64Util.decrypt(input);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String decodeBase64String(final String input) {
        if (StringUtils.isBlank(input)) {
            return "";
        }
        try {
            return new String(Base64Util.decrypt(input), "UTF-8");
        }
        catch (Exception e) {
            return "";
        }
    }
    
    public static String encodeBase62(final byte[] input) {
        final char[] chars = new char[input.length];
        for (int i = 0; i < input.length; ++i) {
            chars[i] = EncodeUtils.BASE62[(input[i] & 0xFF) % EncodeUtils.BASE62.length];
        }
        return new String(chars);
    }
    
    static {
        BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    }
}
