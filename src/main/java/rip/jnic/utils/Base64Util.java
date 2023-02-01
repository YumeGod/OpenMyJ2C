package rip.jnic.utils;

public class Base64Util
{
    private static char[] base64EncodeChars;
    private static byte[] base64DecodeChars;
    
    public static String encrypt(final byte[] data) {
        final StringBuffer sb = new StringBuffer();
        final int len = data.length;
        int i = 0;
        while (i < len) {
            final int b1 = data[i++] & 0xFF;
            if (i == len) {
                sb.append(Base64Util.base64EncodeChars[b1 >>> 2]);
                sb.append(Base64Util.base64EncodeChars[(b1 & 0x3) << 4]);
                sb.append("==");
                break;
            }
            final int b2 = data[i++] & 0xFF;
            if (i == len) {
                sb.append(Base64Util.base64EncodeChars[b1 >>> 2]);
                sb.append(Base64Util.base64EncodeChars[(b1 & 0x3) << 4 | (b2 & 0xF0) >>> 4]);
                sb.append(Base64Util.base64EncodeChars[(b2 & 0xF) << 2]);
                sb.append("=");
                break;
            }
            final int b3 = data[i++] & 0xFF;
            sb.append(Base64Util.base64EncodeChars[b1 >>> 2]);
            sb.append(Base64Util.base64EncodeChars[(b1 & 0x3) << 4 | (b2 & 0xF0) >>> 4]);
            sb.append(Base64Util.base64EncodeChars[(b2 & 0xF) << 2 | (b3 & 0xC0) >>> 6]);
            sb.append(Base64Util.base64EncodeChars[b3 & 0x3F]);
        }
        return sb.toString();
    }
    
    public static byte[] decrypt(final String str) throws Exception {
        final StringBuffer sb = new StringBuffer();
        final byte[] data = str.getBytes("US-ASCII");
        final int len = data.length;
        int i = 0;
        while (i < len) {
            int b1;
            do {
                b1 = Base64Util.base64DecodeChars[data[i++]];
            } while (i < len && b1 == -1);
            if (b1 == -1) {
                break;
            }
            int b2;
            do {
                b2 = Base64Util.base64DecodeChars[data[i++]];
            } while (i < len && b2 == -1);
            if (b2 == -1) {
                break;
            }
            sb.append((char)(b1 << 2 | (b2 & 0x30) >>> 4));
            int b3;
            do {
                b3 = data[i++];
                if (b3 == 61) {
                    return sb.toString().getBytes("iso8859-1");
                }
                b3 = Base64Util.base64DecodeChars[b3];
            } while (i < len && b3 == -1);
            if (b3 == -1) {
                break;
            }
            sb.append((char)((b2 & 0xF) << 4 | (b3 & 0x3C) >>> 2));
            int b4;
            do {
                b4 = data[i++];
                if (b4 == 61) {
                    return sb.toString().getBytes("iso8859-1");
                }
                b4 = Base64Util.base64DecodeChars[b4];
            } while (i < len && b4 == -1);
            if (b4 == -1) {
                break;
            }
            sb.append((char)((b3 & 0x3) << 6 | b4));
        }
        return sb.toString().getBytes("iso8859-1");
    }
    
    static {
        Base64Util.base64EncodeChars = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };
        Base64Util.base64DecodeChars = new byte[] { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };
    }
}
