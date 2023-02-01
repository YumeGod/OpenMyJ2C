package rip.jnic.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class HexUtil
{
    public static final Charset DEFAULT_CHARSET;
    private static final byte[] DIGITS_LOWER;
    private static final byte[] DIGITS_UPPER;
    
    public static byte[] encode(final byte[] data) {
        return encode(data, true);
    }
    
    public static byte[] encode(final byte[] data, final boolean toLowerCase) {
        return encode(data, toLowerCase ? HexUtil.DIGITS_LOWER : HexUtil.DIGITS_UPPER);
    }
    
    private static byte[] encode(final byte[] data, final byte[] digits) {
        final int len = data.length;
        final byte[] out = new byte[len << 1];
        int i = 0;
        int j = 0;
        while (i < len) {
            out[j++] = digits[(0xF0 & data[i]) >>> 4];
            out[j++] = digits[0xF & data[i]];
            ++i;
        }
        return out;
    }
    
    public static String encodeToString(final byte[] data, final boolean toLowerCase) {
        return new String(encode(data, toLowerCase), HexUtil.DEFAULT_CHARSET);
    }
    
    public static String encodeToString(final byte[] data) {
        return new String(encode(data), HexUtil.DEFAULT_CHARSET);
    }
    
    public static String encodeToString(final String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        return encodeToString(data.getBytes(HexUtil.DEFAULT_CHARSET));
    }
    
    public static byte[] decode(final String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        return decode(data.getBytes(HexUtil.DEFAULT_CHARSET));
    }
    
    public static String decodeToString(final byte[] data) {
        final byte[] decodeBytes = decode(data);
        return new String(decodeBytes, HexUtil.DEFAULT_CHARSET);
    }
    
    public static String decodeToString(final String data) {
        if (StringUtils.isBlank(data)) {
            return null;
        }
        return decodeToString(data.getBytes(HexUtil.DEFAULT_CHARSET));
    }
    
    public static byte[] decode(final byte[] data) {
        final int len = data.length;
        if ((len & 0x1) != 0x0) {
            throw new IllegalArgumentException("hexBinary needs to be even-length: " + len);
        }
        final byte[] out = new byte[len >> 1];
        int f;
        for (int i = 0, j = 0; j < len; ++j, f |= toDigit(data[j], j), ++j, out[i] = (byte)(f & 0xFF), ++i) {
            f = toDigit(data[j], j) << 4;
        }
        return out;
    }
    
    private static int toDigit(final byte b, final int index) {
        final int digit = Character.digit(b, 16);
        if (digit == -1) {
            throw new IllegalArgumentException("Illegal hexadecimal byte " + b + " at index " + index);
        }
        return digit;
    }
    
    static {
        DEFAULT_CHARSET = StandardCharsets.UTF_8;
        DIGITS_LOWER = new byte[] { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 97, 98, 99, 100, 101, 102 };
        DIGITS_UPPER = new byte[] { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70 };
    }
}
