package rip.jnic.env;

import rip.jnic.utils.Base64Util;
import rip.jnic.utils.HexUtil;
import rip.jnic.utils.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class LicenseManager
{
    public static final String KEY_ALGORITHM = "RSA";
    private static final String AES = "AES";
    private static Map<String, String> cache;
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private static final String DEFAULT_URL_ENCODING = "UTF-8";
    
    public static String getValue(final String key) {
        if (LicenseManager.cache.get(key) == null) {
            String v = r3().get(key);
            v = ((v == null) ? "" : v);
            LicenseManager.cache.put(key, v);
            return v;
        }
        return LicenseManager.cache.get(key);
    }
    
    public static void printInfo(final String code) {
        final Map<String, String> licenseInfo = r3();
        final String sign = encodeHex(digest(code.getBytes(), "MD5", null, 88));
        if (!sign.equals(licenseInfo.get("sign"))) {
            System.out.println("您当前的版本为试用版 \n机器码:" + code);
            return;
        }
        if (licenseInfo.get("type") == null) {
            System.out.println("您当前的版本为试用版 \n机器码:" + code);
        }
        else {
            System.out.println(licenseInfo.get("message") + "\n机器码:" + code);
        }
    }

    
    private static Map<String, String> r3() {
        final Map<String, String> licenseMap = new HashMap<String, String>();
        try {
            final String updateCode = licenseMap.get("updateCode");
            licenseMap.remove("updateCode");
            final List<String> list = Arrays.asList((String[])licenseMap.values().toArray(new String[ 0 ]));
            Collections.sort(list, String::compareTo);
            if (! StringUtils.equals(encodeHex(digest(join(list.iterator(), "").getBytes(), "MD5", null, 88)), updateCode)) {
                throw new Exception("您当前的版本为未授权版本");
            }
        }
        catch (Exception e) {
            licenseMap.put("message", e.getMessage());
        }
        try {
            licenseMap.putAll(doValidProduct(licenseMap));
        }
        catch (Exception e) {
            licenseMap.put("message", e.getMessage());
        }
        return licenseMap;
    }
    
    public static String join(final Iterator<?> iterator, final String separator) {
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return "";
        }
        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            return Objects.toString(first, "");
        }
        final StringBuilder buf = new StringBuilder(256);
        if (first != null) {
            buf.append(first);
        }
        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }
    

    private static String decrypt(final String contentBase64, final String publicKeyBase64) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, IOException {
        final byte[] decode = decodeBase64(publicKeyBase64);
        final X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(decode);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final PublicKey publicKey = kf.generatePublic(x509EncodedKeySpec);
        final Cipher cipher = Cipher.getInstance(kf.getAlgorithm());
        cipher.init(2, publicKey);
        final byte[] bytes = decodeHex(new String(decodeBase64(contentBase64), StandardCharsets.UTF_8));
        final int inputLen = bytes.length;
        int offLen = 0;
        int i = 0;
        final int length = ((RSAPublicKey)publicKey).getModulus().bitLength() / 8;
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (inputLen - offLen > 0) {
            byte[] cache;
            if (inputLen - offLen > length) {
                cache = cipher.doFinal(bytes, offLen, length);
            }
            else {
                cache = cipher.doFinal(bytes, offLen, inputLen - offLen);
            }
            byteArrayOutputStream.write(cache);
            ++i;
            offLen = length * i;
        }
        final String data = byteArrayOutputStream.toString();
        byteArrayOutputStream.close();
        return decodeBase64String(data);
    }
    
    private static Map<String, String> doValidProduct(final Map<String, String> info) {
        final Map<String, String> map = new HashMap<String, String>();
        String type = "9";
        String version = "试用版";
        if (StringUtils.isNotEmpty(info.get("type"))) {
            type = info.get("type");
        }
        map.put("title", "试用版");
        map.put("type", type);
        boolean sameMachine = false;
        String key = null;
        if (key.equals(StringUtils.trim(info.get("code")))) {
            sameMachine = true;
        }
        if (!sameMachine) {
            throw new RuntimeException("您当前的版本为" + version);
        }
        final String sign = encodeHex(digest(info.get("code").getBytes(), "MD5", null, 88));
        if (!sign.equals(info.get("sign"))) {
            throw new RuntimeException("您当前的版本为" + version);
        }
        if (StringUtils.equals("1", type)) {
            version = "个人版";
        }
        else if (StringUtils.equals("2", type)) {
            version = "专业版";
        }
        final String expireDate = info.get("expireDate");
        final String authInfo = (info.get("name") == null) ? " " : ("授权用户:" + info.get("name") + " ");
        if (StringUtils.equals("-1", expireDate)) {
            map.put("title", version);
            map.put("message", "您当前的版本为" + version + "," + authInfo + "非常感谢您对我们产品的认可与支持！");
        }
        else {
            Date date = null;
            try {
                final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                date = simpleDateFormat.parse(expireDate);
            }
            catch (ParseException e2) {
                throw new RuntimeException("您当前的版本为" + version);
            }
            final long leftTime = date.getTime() - System.currentTimeMillis();
            final long leftDay = leftTime / 3600000L / 24L;
            if (leftDay <= 0L) {
                throw new RuntimeException("您的" + version + "许可，于" + formatDate(date, "yyyy年MM月dd日") + "已到期");
            }
            if (leftDay <= 7L) {
                map.put("message", "您当前的版本为" + version + "，" + authInfo + "许可到期时间为：" + formatDate(date, "yyyy年MM月dd日") + " 还剩最后" + leftDay + "天。");
            }
            else if (leftDay <= 60L) {
                map.put("message", "您当前的版本为" + version + "，" + authInfo + "许可到期时间为：" + formatDate(date, "yyyy年MM月dd日") + " 还剩余" + leftDay + "天。");
            }
            else {
                map.put("message", "您当前的版本为" + version + "，" + authInfo + "许可到期时间为：" + formatDate(date, "yyyy年MM月dd日") + "。");
            }
            map.put("title", version + "（剩余" + leftDay + "天）");
        }
        if (!StringUtils.equals("true", info.get("devlop"))) {
            return map;
        }
        return map;
    }
    
    public static String formatDate(final Date date, String pattern) {
        String formatDate = null;
        if (date != null) {
            if (StringUtils.isBlank(pattern)) {
                pattern = "yyyy-MM-dd";
            }
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            formatDate = simpleDateFormat.format(date);
        }
        return formatDate;
    }
    
    private static String encodeHex(final byte[] input) {
        return HexUtil.encodeToString(input);
    }
    
    private static byte[] decodeHex(final String input) {
        return HexUtil.decode(input.getBytes(StandardCharsets.UTF_8));
    }
    
    private static byte[] decodeBase64(final String input) {
        try {
            return Base64Util.decrypt(input);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private static String decodeBase64String(final String input) {
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
    
    private static byte[] decode(final byte[] input, final byte[] key, final byte[] iv) {
        return aes(input, key, iv, 2);
    }
    
    private static byte[] aes(final byte[] input, final byte[] key, final byte[] iv, final int mode) {
        try {
            final SecretKey secretKey = new SecretKeySpec(key, "AES");
            final IvParameterSpec ivSpec = new IvParameterSpec(iv);
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(mode, secretKey, ivSpec);
            return cipher.doFinal(input);
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static byte[] digest(final byte[] input, final String algorithm, final byte[] salt, final int iterations) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(algorithm);
            if (salt != null) {
                digest.update(salt);
            }
            byte[] result = digest.digest(input);
            for (int i = 1; i < iterations; ++i) {
                digest.reset();
                result = digest.digest(result);
            }
            return result;
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static int getLength(final Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }
    
    public static String v(final int i) {
        return encodeHex(digest(getValue("code").getBytes(), "MD5", null, i));
    }
    
    static {
        LicenseManager.cache = new HashMap<String, String>();
    }
}
