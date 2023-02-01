// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.ZipOutputStream;

public class Util {
    private static final Map<Integer, String> OPCODE_NAME_MAP = new HashMap<Integer, String>();

    public static boolean getFlag(int value, int flag) {
        return (value & flag) > 0;
    }

    public static Map<String, String> createMap(Object ... parts) {
        HashMap<String, String> tokens = new HashMap<String, String>();
        for (int i = 0; i < parts.length; i += 2) {
            tokens.put(parts[i].toString(), parts[i + 1].toString());
        }
        return tokens;
    }

    private static String replaceTokens(String string, Map<String, String> tokens, String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(string);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(tokens.get(matcher.group(1))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String dynamicFormat(String string, Map<String, String> tokens) {
        String patternString = String.format("\\$(%s)", tokens.keySet().stream().map(Util::unicodify).collect(Collectors.joining("|")));
        return Util.replaceTokens(string, tokens, patternString);
    }

    public static String dynamicRawFormat(String string, Map<String, String> tokens) {
        if (tokens.isEmpty()) {
            return string;
        }
        String patternString = String.format("(%s)", tokens.keySet().stream().map(Util::unicodify).collect(Collectors.joining("|")));
        return Util.replaceTokens(string, tokens, patternString);
    }

    public static <T> Stream<T> reverse(Stream<T> input) {
        Object[] temp = input.toArray();
        return (Stream<T>) IntStream.range(0, temp.length).mapToObj(i -> temp[temp.length - i - 1]);
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public static String readResource(String filePath) {
        try (InputStream in = NativeObfuscator.class.getClassLoader().getResourceAsStream(filePath);){
            String string = Util.writeStreamToString(in);
            return string;
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void copyResource(String from, Path to) throws IOException {
        try (InputStream in = NativeObfuscator.class.getClassLoader().getResourceAsStream(from);){
            Objects.requireNonNull(in, "Can't copy resource " + from);
            Files.copy(in, to.resolve(Paths.get(from, new String[0]).getFileName()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String writeStreamToString(InputStream stream) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Util.transfer(stream, baos);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void writeEntry(JarFile f, ZipOutputStream out, JarEntry e) throws IOException {
        out.putNextEntry(new JarEntry(e.getName()));
        try (InputStream in = f.getInputStream(e);){
            Util.transfer(in, out);
        }
        out.closeEntry();
    }

    public static void writeEntry(ZipOutputStream out, String entryName, byte[] data) throws IOException {
        out.putNextEntry(new JarEntry(entryName));
        out.write(data, 0, data.length);
        out.closeEntry();
    }

    static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[4096];
        int r = in.read(buffer, 0, 4096);
        while (r != -1) {
            out.write(buffer, 0, r);
            r = in.read(buffer, 0, 4096);
        }
    }

    public static String escapeCppNameString(String value) {
        Matcher m = Pattern.compile("([^a-zA-Z_0-9])").matcher(value);
        StringBuffer sb = new StringBuffer(value.length());
        while (m.find()) {
            m.appendReplacement(sb, String.format("u%d", m.group(1).charAt(0)));
        }
        m.appendTail(sb);
        String output = sb.toString();
        if (output.length() > 0 && output.charAt(0) >= '0' && output.charAt(0) <= '9') {
            output = "_" + output;
        }
        return output;
    }

    public static String escapeCommentString(String value) {
        StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= ' ' && c <= '~') {
                result.append(c);
                continue;
            }
            result.append("\\u").append(String.format("%04x", c));
        }
        return result.toString();
    }

    private static String unicodify(String string) {
        StringBuilder result = new StringBuilder();
        for (char c : string.toCharArray()) {
            result.append("\\u").append(String.format("%04x", c));
        }
        return result.toString();
    }

    public static int byteArrayToInt(byte[] b) {
        if (b.length == 4) {
            return b[0] << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | b[3] & 0xFF;
        }
        if (b.length == 2) {
            return (b[0] & 0xFF) << 8 | b[1] & 0xFF;
        }
        return 0;
    }

    public static String getOpcodesString(int value, String prefix) {
        for (Field f : Opcodes.class.getFields()) {
            try {
                if (!f.getName().startsWith(prefix) || (Integer)f.get(null) != value) continue;
                return f.getName().substring(prefix.length());
            }
            catch (ReflectiveOperationException reflectiveOperationException) {
                // empty catch block
            }
        }
        return null;
    }

    public static String getOpcodeString(int opcode) {
        return OPCODE_NAME_MAP.getOrDefault(opcode, "UNKNOWN");
    }

    private static boolean isValidJavaIdentifier(String className) {
        if (className.length() == 0 || !Character.isJavaIdentifierStart(className.charAt(0))) {
            return false;
        }
        String name = className.substring(1);
        for (int i = 0; i < name.length(); ++i) {
            if (Character.isJavaIdentifierPart(name.charAt(i))) continue;
            return false;
        }
        return true;
    }

    public static boolean isValidJavaFullClassName(String fullName) {
        boolean flag;
        block9: {
            if (fullName.equals("")) {
                return false;
            }
            flag = true;
            try {
                if (!fullName.endsWith(".")) {
                    int index = fullName.indexOf(".");
                    if (index != -1) {
                        String[] str;
                        for (String name : str = fullName.split("\\.")) {
                            if (name.equals("")) {
                                flag = false;
                            } else {
                                if (Util.isValidJavaIdentifier(name)) continue;
                                flag = false;
                            }
                            break block9;
                        }
                        break block9;
                    }
                    if (!Util.isValidJavaIdentifier(fullName)) {
                        flag = false;
                    }
                    break block9;
                }
                flag = false;
            }
            catch (Exception ex) {
                flag = false;
                ex.printStackTrace();
            }
        }
        return flag;
    }

    public static String utf82unicode(String inStr) {
        String a = "";
        String str = inStr == null ? "" : inStr;
        for (int i = 0; i < str.length(); ++i) {
            StringBuffer sb = new StringBuffer();
            char c = str.charAt(i);
            int j = c >>> 8;
            String tmp = Integer.toHexString(j);
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
            j = c & 0xFF;
            tmp = Integer.toHexString(j);
            if (tmp.length() == 1) {
                sb.append("0");
            }
            sb.append(tmp);
            a = a + Integer.parseInt(sb.toString(), 16);
            if (i >= str.length() - 1) continue;
            a = a + ", ";
        }
        return a;
    }

    static {
        OPCODE_NAME_MAP.put(0, "NOP");
        OPCODE_NAME_MAP.put(1, "ACONST_NULL");
        OPCODE_NAME_MAP.put(2, "ICONST_M1");
        OPCODE_NAME_MAP.put(3, "ICONST_0");
        OPCODE_NAME_MAP.put(4, "ICONST_1");
        OPCODE_NAME_MAP.put(5, "ICONST_2");
        OPCODE_NAME_MAP.put(6, "ICONST_3");
        OPCODE_NAME_MAP.put(7, "ICONST_4");
        OPCODE_NAME_MAP.put(8, "ICONST_5");
        OPCODE_NAME_MAP.put(9, "LCONST_0");
        OPCODE_NAME_MAP.put(10, "LCONST_1");
        OPCODE_NAME_MAP.put(11, "FCONST_0");
        OPCODE_NAME_MAP.put(12, "FCONST_1");
        OPCODE_NAME_MAP.put(13, "FCONST_2");
        OPCODE_NAME_MAP.put(14, "DCONST_0");
        OPCODE_NAME_MAP.put(15, "DCONST_1");
        OPCODE_NAME_MAP.put(16, "BIPUSH");
        OPCODE_NAME_MAP.put(17, "SIPUSH");
        OPCODE_NAME_MAP.put(18, "LDC");
        OPCODE_NAME_MAP.put(21, "ILOAD");
        OPCODE_NAME_MAP.put(22, "LLOAD");
        OPCODE_NAME_MAP.put(23, "FLOAD");
        OPCODE_NAME_MAP.put(24, "DLOAD");
        OPCODE_NAME_MAP.put(25, "ALOAD");
        OPCODE_NAME_MAP.put(46, "IALOAD");
        OPCODE_NAME_MAP.put(47, "LALOAD");
        OPCODE_NAME_MAP.put(48, "FALOAD");
        OPCODE_NAME_MAP.put(49, "DALOAD");
        OPCODE_NAME_MAP.put(50, "AALOAD");
        OPCODE_NAME_MAP.put(51, "BALOAD");
        OPCODE_NAME_MAP.put(52, "CALOAD");
        OPCODE_NAME_MAP.put(53, "SALOAD");
        OPCODE_NAME_MAP.put(54, "ISTORE");
        OPCODE_NAME_MAP.put(55, "LSTORE");
        OPCODE_NAME_MAP.put(56, "FSTORE");
        OPCODE_NAME_MAP.put(57, "DSTORE");
        OPCODE_NAME_MAP.put(58, "ASTORE");
        OPCODE_NAME_MAP.put(79, "IASTORE");
        OPCODE_NAME_MAP.put(80, "LASTORE");
        OPCODE_NAME_MAP.put(81, "FASTORE");
        OPCODE_NAME_MAP.put(82, "DASTORE");
        OPCODE_NAME_MAP.put(83, "AASTORE");
        OPCODE_NAME_MAP.put(84, "BASTORE");
        OPCODE_NAME_MAP.put(85, "CASTORE");
        OPCODE_NAME_MAP.put(86, "SASTORE");
        OPCODE_NAME_MAP.put(87, "POP");
        OPCODE_NAME_MAP.put(88, "POP2");
        OPCODE_NAME_MAP.put(89, "DUP");
        OPCODE_NAME_MAP.put(90, "DUP_X1");
        OPCODE_NAME_MAP.put(91, "DUP_X2");
        OPCODE_NAME_MAP.put(92, "DUP2");
        OPCODE_NAME_MAP.put(93, "DUP2_X1");
        OPCODE_NAME_MAP.put(94, "DUP2_X2");
        OPCODE_NAME_MAP.put(95, "SWAP");
        OPCODE_NAME_MAP.put(96, "IADD");
        OPCODE_NAME_MAP.put(97, "LADD");
        OPCODE_NAME_MAP.put(98, "FADD");
        OPCODE_NAME_MAP.put(99, "DADD");
        OPCODE_NAME_MAP.put(100, "ISUB");
        OPCODE_NAME_MAP.put(101, "LSUB");
        OPCODE_NAME_MAP.put(102, "FSUB");
        OPCODE_NAME_MAP.put(103, "DSUB");
        OPCODE_NAME_MAP.put(104, "IMUL");
        OPCODE_NAME_MAP.put(105, "LMUL");
        OPCODE_NAME_MAP.put(106, "FMUL");
        OPCODE_NAME_MAP.put(107, "DMUL");
        OPCODE_NAME_MAP.put(108, "IDIV");
        OPCODE_NAME_MAP.put(109, "LDIV");
        OPCODE_NAME_MAP.put(110, "FDIV");
        OPCODE_NAME_MAP.put(111, "DDIV");
        OPCODE_NAME_MAP.put(112, "IREM");
        OPCODE_NAME_MAP.put(113, "LREM");
        OPCODE_NAME_MAP.put(114, "FREM");
        OPCODE_NAME_MAP.put(115, "DREM");
        OPCODE_NAME_MAP.put(116, "INEG");
        OPCODE_NAME_MAP.put(117, "LNEG");
        OPCODE_NAME_MAP.put(118, "FNEG");
        OPCODE_NAME_MAP.put(119, "DNEG");
        OPCODE_NAME_MAP.put(120, "ISHL");
        OPCODE_NAME_MAP.put(121, "LSHL");
        OPCODE_NAME_MAP.put(122, "ISHR");
        OPCODE_NAME_MAP.put(123, "LSHR");
        OPCODE_NAME_MAP.put(124, "IUSHR");
        OPCODE_NAME_MAP.put(125, "LUSHR");
        OPCODE_NAME_MAP.put(126, "IAND");
        OPCODE_NAME_MAP.put(127, "LAND");
        OPCODE_NAME_MAP.put(128, "IOR");
        OPCODE_NAME_MAP.put(129, "LOR");
        OPCODE_NAME_MAP.put(130, "IXOR");
        OPCODE_NAME_MAP.put(131, "LXOR");
        OPCODE_NAME_MAP.put(132, "IINC");
        OPCODE_NAME_MAP.put(133, "I2L");
        OPCODE_NAME_MAP.put(134, "I2F");
        OPCODE_NAME_MAP.put(135, "I2D");
        OPCODE_NAME_MAP.put(136, "L2I");
        OPCODE_NAME_MAP.put(137, "L2F");
        OPCODE_NAME_MAP.put(138, "L2D");
        OPCODE_NAME_MAP.put(139, "F2I");
        OPCODE_NAME_MAP.put(140, "F2L");
        OPCODE_NAME_MAP.put(141, "F2D");
        OPCODE_NAME_MAP.put(142, "D2I");
        OPCODE_NAME_MAP.put(143, "D2L");
        OPCODE_NAME_MAP.put(144, "D2F");
        OPCODE_NAME_MAP.put(145, "I2B");
        OPCODE_NAME_MAP.put(146, "I2C");
        OPCODE_NAME_MAP.put(147, "I2S");
        OPCODE_NAME_MAP.put(148, "LCMP");
        OPCODE_NAME_MAP.put(149, "FCMPL");
        OPCODE_NAME_MAP.put(150, "FCMPG");
        OPCODE_NAME_MAP.put(151, "DCMPL");
        OPCODE_NAME_MAP.put(152, "DCMPG");
        OPCODE_NAME_MAP.put(153, "IFEQ");
        OPCODE_NAME_MAP.put(154, "IFNE");
        OPCODE_NAME_MAP.put(155, "IFLT");
        OPCODE_NAME_MAP.put(156, "IFGE");
        OPCODE_NAME_MAP.put(157, "IFGT");
        OPCODE_NAME_MAP.put(158, "IFLE");
        OPCODE_NAME_MAP.put(159, "IF_ICMPEQ");
        OPCODE_NAME_MAP.put(160, "IF_ICMPNE");
        OPCODE_NAME_MAP.put(161, "IF_ICMPLT");
        OPCODE_NAME_MAP.put(162, "IF_ICMPGE");
        OPCODE_NAME_MAP.put(163, "IF_ICMPGT");
        OPCODE_NAME_MAP.put(164, "IF_ICMPLE");
        OPCODE_NAME_MAP.put(165, "IF_ACMPEQ");
        OPCODE_NAME_MAP.put(166, "IF_ACMPNE");
        OPCODE_NAME_MAP.put(167, "GOTO");
        OPCODE_NAME_MAP.put(168, "JSR");
        OPCODE_NAME_MAP.put(169, "RET");
        OPCODE_NAME_MAP.put(170, "TABLESWITCH");
        OPCODE_NAME_MAP.put(171, "LOOKUPSWITCH");
        OPCODE_NAME_MAP.put(172, "IRETURN");
        OPCODE_NAME_MAP.put(173, "LRETURN");
        OPCODE_NAME_MAP.put(174, "FRETURN");
        OPCODE_NAME_MAP.put(175, "DRETURN");
        OPCODE_NAME_MAP.put(176, "ARETURN");
        OPCODE_NAME_MAP.put(177, "RETURN");
        OPCODE_NAME_MAP.put(178, "GETSTATIC");
        OPCODE_NAME_MAP.put(179, "PUTSTATIC");
        OPCODE_NAME_MAP.put(180, "GETFIELD");
        OPCODE_NAME_MAP.put(181, "PUTFIELD");
        OPCODE_NAME_MAP.put(182, "INVOKEVIRTUAL");
        OPCODE_NAME_MAP.put(183, "INVOKESPECIAL");
        OPCODE_NAME_MAP.put(184, "INVOKESTATIC");
        OPCODE_NAME_MAP.put(185, "INVOKEINTERFACE");
        OPCODE_NAME_MAP.put(186, "INVOKEDYNAMIC");
        OPCODE_NAME_MAP.put(187, "NEW");
        OPCODE_NAME_MAP.put(188, "NEWARRAY");
        OPCODE_NAME_MAP.put(189, "ANEWARRAY");
        OPCODE_NAME_MAP.put(190, "ARRAYLENGTH");
        OPCODE_NAME_MAP.put(191, "ATHROW");
        OPCODE_NAME_MAP.put(192, "CHECKCAST");
        OPCODE_NAME_MAP.put(193, "INSTANCEOF");
        OPCODE_NAME_MAP.put(194, "MONITORENTER");
        OPCODE_NAME_MAP.put(195, "MONITOREXIT");
        OPCODE_NAME_MAP.put(197, "MULTIANEWARRAY");
        OPCODE_NAME_MAP.put(198, "IFNULL");
        OPCODE_NAME_MAP.put(199, "IFNONNULL");
    }
}
