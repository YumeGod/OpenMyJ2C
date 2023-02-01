package rip.jnic.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

public class StringUtils
{
    private static final String[] EMPTY_STRING_ARRAY;
    
    public static String[] tokenizeToStringArray(final String str, final String delimiters, final boolean trimTokens, final boolean ignoreEmptyTokens) {
        if (str == null) {
            return StringUtils.EMPTY_STRING_ARRAY;
        }
        final StringTokenizer st = new StringTokenizer(str, delimiters);
        final List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return toStringArray(tokens);
    }
    
    public static boolean hasText(final CharSequence str) {
        return str != null && str.length() > 0 && containsText(str);
    }
    
    private static boolean containsText(final CharSequence str) {
        for (int strLen = str.length(), i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
    
    public static String[] toStringArray(final Collection<String> collection) {
        return collection.isEmpty() ? StringUtils.EMPTY_STRING_ARRAY : collection.toArray(StringUtils.EMPTY_STRING_ARRAY);
    }
    
    public static boolean equals(final CharSequence cs1, final CharSequence cs2) {
        if (cs1 == cs2) {
            return true;
        }
        if (cs1 == null || cs2 == null) {
            return false;
        }
        if (cs1.length() != cs2.length()) {
            return false;
        }
        if (cs1 instanceof String && cs2 instanceof String) {
            return cs1.equals(cs2);
        }
        for (int length = cs1.length(), i = 0; i < length; ++i) {
            if (cs1.charAt(i) != cs2.charAt(i)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean contains(final CharSequence seq, final CharSequence searchSeq) {
        return seq != null && searchSeq != null && indexOf(seq, searchSeq, 0) >= 0;
    }
    
    static int indexOf(final CharSequence cs, final CharSequence searchChar, final int start) {
        if (cs instanceof String) {
            return ((String)cs).indexOf(searchChar.toString(), start);
        }
        if (cs instanceof StringBuilder) {
            return ((StringBuilder)cs).indexOf(searchChar.toString(), start);
        }
        return (cs instanceof StringBuffer) ? ((StringBuffer)cs).indexOf(searchChar.toString(), start) : cs.toString().indexOf(searchChar.toString(), start);
    }
    
    public static boolean isBlank(final CharSequence cs) {
        final int strLen = length(cs);
        if (strLen == 0) {
            return true;
        }
        for (int i = 0; i < strLen; ++i) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }
    
    public static int length(final CharSequence cs) {
        return (cs == null) ? 0 : cs.length();
    }
    
    public static String trim(final String str) {
        return (str == null) ? null : str.trim();
    }
    
    public static boolean isNotEmpty(final CharSequence cs) {
        return !isEmpty(cs);
    }
    
    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }
    
    public static String substringBetween(final String str, final String open, final String close) {
        if (!allNotNull(str, open, close)) {
            return null;
        }
        final int start = str.indexOf(open);
        if (start != -1) {
            final int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }
    
    public static boolean allNotNull(final String... values) {
        if (values == null) {
            return false;
        }
        for (final String val : values) {
            if (val == null) {
                return false;
            }
        }
        return true;
    }
    
    public static String replace(final String text, final String searchString, final String replacement) {
        return replace(text, searchString, replacement, -1);
    }
    
    public static String replace(final String text, final String searchString, final String replacement, final int max) {
        return replace(text, searchString, replacement, max, false);
    }
    
    private static String replace(final String text, String searchString, final String replacement, int max, final boolean ignoreCase) {
        if (isEmpty(text) || isEmpty(searchString) || replacement == null || max == 0) {
            return text;
        }
        if (ignoreCase) {
            searchString = searchString.toLowerCase();
        }
        int start = 0;
        int end = ignoreCase ? indexOfIgnoreCase(text, searchString, start) : indexOf(text, searchString, start);
        if (end == -1) {
            return text;
        }
        final int replLength = searchString.length();
        int increase = Math.max(replacement.length() - replLength, 0);
        increase *= ((max < 0) ? 16 : Math.min(max, 64));
        final StringBuilder buf = new StringBuilder(text.length() + increase);
        while (end != -1) {
            buf.append(text, start, end).append(replacement);
            start = end + replLength;
            if (--max == 0) {
                break;
            }
            end = (ignoreCase ? indexOfIgnoreCase(text, searchString, start) : indexOf(text, searchString, start));
        }
        buf.append(text, start, text.length());
        return buf.toString();
    }
    
    public static int indexOfIgnoreCase(final CharSequence str, final CharSequence searchStr) {
        return indexOfIgnoreCase(str, searchStr, 0);
    }
    
    public static int indexOfIgnoreCase(final CharSequence str, final CharSequence searchStr, int startPos) {
        if (str == null || searchStr == null) {
            return -1;
        }
        if (startPos < 0) {
            startPos = 0;
        }
        final int endLimit = str.length() - searchStr.length() + 1;
        if (startPos > endLimit) {
            return -1;
        }
        if (searchStr.length() == 0) {
            return startPos;
        }
        for (int i = startPos; i < endLimit; ++i) {
            if (regionMatches(str, true, i, searchStr, 0, searchStr.length())) {
                return i;
            }
        }
        return -1;
    }
    
    static boolean regionMatches(final CharSequence cs, final boolean ignoreCase, final int thisStart, final CharSequence substring, final int start, final int length) {
        if (cs instanceof String && substring instanceof String) {
            return ((String)cs).regionMatches(ignoreCase, thisStart, (String)substring, start, length);
        }
        int index1 = thisStart;
        int index2 = start;
        int tmpLen = length;
        final int srcLen = cs.length() - thisStart;
        final int otherLen = substring.length() - start;
        if (thisStart < 0 || start < 0 || length < 0) {
            return false;
        }
        if (srcLen >= length && otherLen >= length) {
            while (tmpLen-- > 0) {
                final char c1 = cs.charAt(index1++);
                final char c2 = substring.charAt(index2++);
                if (c1 != c2) {
                    if (!ignoreCase) {
                        return false;
                    }
                    final char u1 = Character.toUpperCase(c1);
                    final char u2 = Character.toUpperCase(c2);
                    if (u1 != u2 && Character.toLowerCase(u1) != Character.toLowerCase(u2)) {
                        return false;
                    }
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    
    static {
        EMPTY_STRING_ARRAY = new String[0];
    }
}
