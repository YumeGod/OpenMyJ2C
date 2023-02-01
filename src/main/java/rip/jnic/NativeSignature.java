// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic;

import java.util.Iterator;

public class NativeSignature {
    public static void getJNIDefineName(String name, StringBuilder sb) {
        Iterator iterator = ((Iterable)name.chars()::iterator).iterator();
        while (iterator.hasNext()) {
            int cp = (Integer)iterator.next();
            if (cp >= 97 && cp <= 122 || cp >= 65 && cp <= 90 || cp == 95 || cp >= 48 && cp <= 57) {
                sb.append((char)cp);
                continue;
            }
            sb.append('_');
        }
    }

    public static void getJNICompatibleName(String name, StringBuilder sb) {
        Iterator iterator = ((Iterable)name.chars()::iterator).iterator();
        block7: while (iterator.hasNext()) {
            int cp = (Integer)iterator.next();
            if (cp < 127) {
                switch (cp) {
                    case 46:
                    case 47: {
                        sb.append('_');
                        continue block7;
                    }
                    case 36: {
                        sb.append("_00024");
                        continue block7;
                    }
                    case 95: {
                        sb.append("_1");
                        continue block7;
                    }
                    case 59: {
                        sb.append("_2");
                        continue block7;
                    }
                    case 91: {
                        sb.append("_3");
                        continue block7;
                    }
                }
                sb.append((char)cp);
                continue;
            }
            sb.append("_0");
            String hexed = Integer.toHexString(cp);
            for (int i = 0; i < 4 - hexed.length(); ++i) {
                sb.append('0');
            }
            sb.append(hexed);
        }
    }

    public static String getJNICompatibleName(String name) {
        StringBuilder sb = new StringBuilder();
        NativeSignature.getJNICompatibleName(name, sb);
        return sb.toString();
    }
}
