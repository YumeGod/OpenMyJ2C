package rip.jnic;

import rip.jnic.utils.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AntPathMatcher
{
    public static final String DEFAULT_PATH_SEPARATOR = "/";
    private static final int CACHE_TURNOFF_THRESHOLD = 65536;
    private static final Pattern VARIABLE_PATTERN;
    private static final char[] WILDCARD_CHARS;
    private String pathSeparator;
    private PathSeparatorPatternCache pathSeparatorPatternCache;
    private boolean caseSensitive;
    private boolean trimTokens;
    private volatile Boolean cachePatterns;
    private final Map<String, String[]> tokenizedPatternCache;
    final Map<String, AntPathStringMatcher> stringMatcherCache;
    
    public AntPathMatcher() {
        this.caseSensitive = true;
        this.trimTokens = false;
        this.tokenizedPatternCache = new ConcurrentHashMap<String, String[]>(256);
        this.stringMatcherCache = new ConcurrentHashMap<String, AntPathStringMatcher>(256);
        this.pathSeparator = "/";
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache("/");
    }
    
    public AntPathMatcher(final String pathSeparator) {
        this.caseSensitive = true;
        this.trimTokens = false;
        this.tokenizedPatternCache = new ConcurrentHashMap<String, String[]>(256);
        this.stringMatcherCache = new ConcurrentHashMap<String, AntPathStringMatcher>(256);
        this.pathSeparator = pathSeparator;
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(pathSeparator);
    }
    
    public void setPathSeparator(final String pathSeparator) {
        this.pathSeparator = ((pathSeparator != null) ? pathSeparator : "/");
        this.pathSeparatorPatternCache = new PathSeparatorPatternCache(this.pathSeparator);
    }
    
    public void setCaseSensitive(final boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public void setTrimTokens(final boolean trimTokens) {
        this.trimTokens = trimTokens;
    }
    
    public void setCachePatterns(final boolean cachePatterns) {
        this.cachePatterns = cachePatterns;
    }
    
    private void deactivatePatternCache() {
        this.cachePatterns = false;
        this.tokenizedPatternCache.clear();
        this.stringMatcherCache.clear();
    }
    
    public boolean isPattern(final String path) {
        if (path == null) {
            return false;
        }
        boolean uriVar = false;
        for (int i = 0; i < path.length(); ++i) {
            final char c = path.charAt(i);
            if (c == '*' || c == '?') {
                return true;
            }
            if (c == '{') {
                uriVar = true;
            }
            else if (c == '}' && uriVar) {
                return true;
            }
        }
        return false;
    }
    
    public boolean match(final String pattern, final String path) {
        return this.doMatch(pattern, path, true, null);
    }
    
    public boolean matchStart(final String pattern, final String path) {
        return this.doMatch(pattern, path, false, null);
    }
    
    protected boolean doMatch(final String pattern, final String path, final boolean fullMatch, final Map<String, String> uriTemplateVariables) {
        if (path == null || path.startsWith(this.pathSeparator) != pattern.startsWith(this.pathSeparator)) {
            return false;
        }
        final String[] pattDirs = this.tokenizePattern(pattern);
        if (fullMatch && this.caseSensitive && !this.isPotentialMatch(path, pattDirs)) {
            return false;
        }
        final String[] pathDirs = this.tokenizePath(path);
        int pattIdxStart;
        int pattIdxEnd;
        int pathIdxStart;
        int pathIdxEnd;
        for (pattIdxStart = 0, pattIdxEnd = pattDirs.length - 1, pathIdxStart = 0, pathIdxEnd = pathDirs.length - 1; pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd; ++pattIdxStart, ++pathIdxStart) {
            final String pattDir = pattDirs[pattIdxStart];
            if ("**".equals(pattDir)) {
                break;
            }
            if (!this.matchStrings(pattDir, pathDirs[pathIdxStart], uriTemplateVariables)) {
                return false;
            }
        }
        if (pathIdxStart > pathIdxEnd) {
            if (pattIdxStart > pattIdxEnd) {
                return pattern.endsWith(this.pathSeparator) == path.endsWith(this.pathSeparator);
            }
            if (!fullMatch) {
                return true;
            }
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(this.pathSeparator)) {
                return true;
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; ++i) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }
        else {
            if (pattIdxStart > pattIdxEnd) {
                return false;
            }
            if (!fullMatch && "**".equals(pattDirs[pattIdxStart])) {
                return true;
            }
            while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                final String pattDir = pattDirs[pattIdxEnd];
                if (pattDir.equals("**")) {
                    break;
                }
                if (!this.matchStrings(pattDir, pathDirs[pathIdxEnd], uriTemplateVariables)) {
                    return false;
                }
                --pattIdxEnd;
                --pathIdxEnd;
            }
            if (pathIdxStart > pathIdxEnd) {
                for (int i = pattIdxStart; i <= pattIdxEnd; ++i) {
                    if (!pattDirs[i].equals("**")) {
                        return false;
                    }
                }
                return true;
            }
            while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
                int patIdxTmp = -1;
                for (int j = pattIdxStart + 1; j <= pattIdxEnd; ++j) {
                    if (pattDirs[j].equals("**")) {
                        patIdxTmp = j;
                        break;
                    }
                }
                if (patIdxTmp == pattIdxStart + 1) {
                    ++pattIdxStart;
                }
                else {
                    final int patLength = patIdxTmp - pattIdxStart - 1;
                    final int strLength = pathIdxEnd - pathIdxStart + 1;
                    int foundIdx = -1;
                    int k = 0;
                Label_0480:
                    while (k <= strLength - patLength) {
                        for (int l = 0; l < patLength; ++l) {
                            final String subPat = pattDirs[pattIdxStart + l + 1];
                            final String subStr = pathDirs[pathIdxStart + k + l];
                            if (!this.matchStrings(subPat, subStr, uriTemplateVariables)) {
                                ++k;
                                continue Label_0480;
                            }
                        }
                        foundIdx = pathIdxStart + k;
                        break;
                    }
                    if (foundIdx == -1) {
                        return false;
                    }
                    pattIdxStart = patIdxTmp;
                    pathIdxStart = foundIdx + patLength;
                }
            }
            for (int i = pattIdxStart; i <= pattIdxEnd; ++i) {
                if (!pattDirs[i].equals("**")) {
                    return false;
                }
            }
            return true;
        }
    }
    
    private boolean isPotentialMatch(final String path, final String[] pattDirs) {
        if (!this.trimTokens) {
            int pos = 0;
            for (final String pattDir : pattDirs) {
                int skipped = this.skipSeparator(path, pos, this.pathSeparator);
                pos += skipped;
                skipped = this.skipSegment(path, pos, pattDir);
                if (skipped < pattDir.length()) {
                    return skipped > 0 || (pattDir.length() > 0 && this.isWildcardChar(pattDir.charAt(0)));
                }
                pos += skipped;
            }
        }
        return true;
    }
    
    private int skipSegment(final String path, final int pos, final String prefix) {
        int skipped = 0;
        for (int i = 0; i < prefix.length(); ++i) {
            final char c = prefix.charAt(i);
            if (this.isWildcardChar(c)) {
                return skipped;
            }
            final int currPos = pos + skipped;
            if (currPos >= path.length()) {
                return 0;
            }
            if (c == path.charAt(currPos)) {
                ++skipped;
            }
        }
        return skipped;
    }
    
    private int skipSeparator(final String path, final int pos, final String separator) {
        int skipped;
        for (skipped = 0; path.startsWith(separator, pos + skipped); skipped += separator.length()) {}
        return skipped;
    }
    
    private boolean isWildcardChar(final char c) {
        for (final char candidate : AntPathMatcher.WILDCARD_CHARS) {
            if (c == candidate) {
                return true;
            }
        }
        return false;
    }
    
    protected String[] tokenizePattern(final String pattern) {
        String[] tokenized = null;
        final Boolean cachePatterns = this.cachePatterns;
        if (cachePatterns == null || cachePatterns) {
            tokenized = this.tokenizedPatternCache.get(pattern);
        }
        if (tokenized == null) {
            tokenized = this.tokenizePath(pattern);
            if (cachePatterns == null && this.tokenizedPatternCache.size() >= 65536) {
                this.deactivatePatternCache();
                return tokenized;
            }
            if (cachePatterns == null || cachePatterns) {
                this.tokenizedPatternCache.put(pattern, tokenized);
            }
        }
        return tokenized;
    }
    
    protected String[] tokenizePath(final String path) {
        return StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
    }
    
    private boolean matchStrings(final String pattern, final String str, final Map<String, String> uriTemplateVariables) {
        return this.getStringMatcher(pattern).matchStrings(str, uriTemplateVariables);
    }
    
    protected AntPathStringMatcher getStringMatcher(final String pattern) {
        AntPathStringMatcher matcher = null;
        final Boolean cachePatterns = this.cachePatterns;
        if (cachePatterns == null || cachePatterns) {
            matcher = this.stringMatcherCache.get(pattern);
        }
        if (matcher == null) {
            matcher = new AntPathStringMatcher(pattern, this.caseSensitive);
            if (cachePatterns == null && this.stringMatcherCache.size() >= 65536) {
                this.deactivatePatternCache();
                return matcher;
            }
            if (cachePatterns == null || cachePatterns) {
                this.stringMatcherCache.put(pattern, matcher);
            }
        }
        return matcher;
    }
    
    public String extractPathWithinPattern(final String pattern, final String path) {
        final String[] patternParts = StringUtils.tokenizeToStringArray(pattern, this.pathSeparator, this.trimTokens, true);
        final String[] pathParts = StringUtils.tokenizeToStringArray(path, this.pathSeparator, this.trimTokens, true);
        final StringBuilder builder = new StringBuilder();
        boolean pathStarted = false;
        for (int segment = 0; segment < patternParts.length; ++segment) {
            final String patternPart = patternParts[segment];
            if (patternPart.indexOf(42) > -1 || patternPart.indexOf(63) > -1) {
                while (segment < pathParts.length) {
                    if (pathStarted || (segment == 0 && !pattern.startsWith(this.pathSeparator))) {
                        builder.append(this.pathSeparator);
                    }
                    builder.append(pathParts[segment]);
                    pathStarted = true;
                    ++segment;
                }
            }
        }
        return builder.toString();
    }
    
    public Map<String, String> extractUriTemplateVariables(final String pattern, final String path) {
        final Map<String, String> variables = new LinkedHashMap<String, String>();
        final boolean result = this.doMatch(pattern, path, true, variables);
        if (!result) {
            throw new IllegalStateException("Pattern \"" + pattern + "\" is not a match for \"" + path + "\"");
        }
        return variables;
    }
    
    public String combine(final String pattern1, final String pattern2) {
        if (!StringUtils.hasText(pattern1) && !StringUtils.hasText(pattern2)) {
            return "";
        }
        if (!StringUtils.hasText(pattern1)) {
            return pattern2;
        }
        if (!StringUtils.hasText(pattern2)) {
            return pattern1;
        }
        final boolean pattern1ContainsUriVar = pattern1.indexOf(123) != -1;
        if (!pattern1.equals(pattern2) && !pattern1ContainsUriVar && this.match(pattern1, pattern2)) {
            return pattern2;
        }
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnWildCard())) {
            return this.concat(pattern1.substring(0, pattern1.length() - 2), pattern2);
        }
        if (pattern1.endsWith(this.pathSeparatorPatternCache.getEndsOnDoubleWildCard())) {
            return this.concat(pattern1, pattern2);
        }
        final int starDotPos1 = pattern1.indexOf("*.");
        if (pattern1ContainsUriVar || starDotPos1 == -1 || this.pathSeparator.equals(".")) {
            return this.concat(pattern1, pattern2);
        }
        final String ext1 = pattern1.substring(starDotPos1 + 1);
        final int dotPos2 = pattern2.indexOf(46);
        final String file2 = (dotPos2 == -1) ? pattern2 : pattern2.substring(0, dotPos2);
        final String ext2 = (dotPos2 == -1) ? "" : pattern2.substring(dotPos2);
        final boolean ext1All = ext1.equals(".*") || ext1.isEmpty();
        final boolean ext2All = ext2.equals(".*") || ext2.isEmpty();
        if (!ext1All && !ext2All) {
            throw new IllegalArgumentException("Cannot combine patterns: " + pattern1 + " vs " + pattern2);
        }
        final String ext3 = ext1All ? ext2 : ext1;
        return file2 + ext3;
    }
    
    private String concat(final String path1, final String path2) {
        final boolean path1EndsWithSeparator = path1.endsWith(this.pathSeparator);
        final boolean path2StartsWithSeparator = path2.startsWith(this.pathSeparator);
        if (path1EndsWithSeparator && path2StartsWithSeparator) {
            return path1 + path2.substring(1);
        }
        if (path1EndsWithSeparator || path2StartsWithSeparator) {
            return path1 + path2;
        }
        return path1 + this.pathSeparator + path2;
    }
    
    public Comparator<String> getPatternComparator(final String path) {
        return new AntPatternComparator(path);
    }
    
    static {
        VARIABLE_PATTERN = Pattern.compile("\\{[^/]+?\\}");
        WILDCARD_CHARS = new char[] { '*', '?', '{' };
    }
    
    protected static class AntPathStringMatcher
    {
        private static final Pattern GLOB_PATTERN;
        private static final String DEFAULT_VARIABLE_PATTERN = "((?s).*)";
        private final String rawPattern;
        private final boolean caseSensitive;
        private final boolean exactMatch;
        private final Pattern pattern;
        private final List<String> variableNames;
        
        public AntPathStringMatcher(final String pattern) {
            this(pattern, true);
        }
        
        public AntPathStringMatcher(final String pattern, final boolean caseSensitive) {
            this.variableNames = new ArrayList<String>();
            this.rawPattern = pattern;
            this.caseSensitive = caseSensitive;
            final StringBuilder patternBuilder = new StringBuilder();
            final Matcher matcher = AntPathStringMatcher.GLOB_PATTERN.matcher(pattern);
            int end = 0;
            while (matcher.find()) {
                patternBuilder.append(this.quote(pattern, end, matcher.start()));
                final String match = matcher.group();
                if ("?".equals(match)) {
                    patternBuilder.append('.');
                }
                else if ("*".equals(match)) {
                    patternBuilder.append(".*");
                }
                else if (match.startsWith("{") && match.endsWith("}")) {
                    final int colonIdx = match.indexOf(58);
                    if (colonIdx == -1) {
                        patternBuilder.append("((?s).*)");
                        this.variableNames.add(matcher.group(1));
                    }
                    else {
                        final String variablePattern = match.substring(colonIdx + 1, match.length() - 1);
                        patternBuilder.append('(');
                        patternBuilder.append(variablePattern);
                        patternBuilder.append(')');
                        final String variableName = match.substring(1, colonIdx);
                        this.variableNames.add(variableName);
                    }
                }
                end = matcher.end();
            }
            if (end == 0) {
                this.exactMatch = true;
                this.pattern = null;
            }
            else {
                this.exactMatch = false;
                patternBuilder.append(this.quote(pattern, end, pattern.length()));
                this.pattern = (this.caseSensitive ? Pattern.compile(patternBuilder.toString()) : Pattern.compile(patternBuilder.toString(), 2));
            }
        }
        
        private String quote(final String s, final int start, final int end) {
            if (start == end) {
                return "";
            }
            return Pattern.quote(s.substring(start, end));
        }
        
        public boolean matchStrings(final String str, final Map<String, String> uriTemplateVariables) {
            if (this.exactMatch) {
                return this.caseSensitive ? this.rawPattern.equals(str) : this.rawPattern.equalsIgnoreCase(str);
            }
            if (this.pattern != null) {
                final Matcher matcher = this.pattern.matcher(str);
                if (matcher.matches()) {
                    if (uriTemplateVariables != null) {
                        if (this.variableNames.size() != matcher.groupCount()) {
                            throw new IllegalArgumentException("The number of capturing groups in the pattern segment " + this.pattern + " does not match the number of URI template variables it defines, which can occur if capturing groups are used in a URI template regex. Use non-capturing groups instead.");
                        }
                        for (int i = 1; i <= matcher.groupCount(); ++i) {
                            final String name = this.variableNames.get(i - 1);
                            if (name.startsWith("*")) {
                                throw new IllegalArgumentException("Capturing patterns (" + name + ") are not supported by the AntPathMatcher. Use the PathPatternParser instead.");
                            }
                            final String value = matcher.group(i);
                            uriTemplateVariables.put(name, value);
                        }
                    }
                    return true;
                }
            }
            return false;
        }
        
        static {
            GLOB_PATTERN = Pattern.compile("\\?|\\*|\\{((?:\\{[^/]+?\\}|[^/{}]|\\\\[{}])+?)\\}");
        }
    }
    
    protected static class AntPatternComparator implements Comparator<String>
    {
        private final String path;
        
        public AntPatternComparator(final String path) {
            this.path = path;
        }
        
        @Override
        public int compare(final String pattern1, final String pattern2) {
            final PatternInfo info1 = new PatternInfo(pattern1);
            final PatternInfo info2 = new PatternInfo(pattern2);
            if (info1.isLeastSpecific() && info2.isLeastSpecific()) {
                return 0;
            }
            if (info1.isLeastSpecific()) {
                return 1;
            }
            if (info2.isLeastSpecific()) {
                return -1;
            }
            final boolean pattern1EqualsPath = pattern1.equals(this.path);
            final boolean pattern2EqualsPath = pattern2.equals(this.path);
            if (pattern1EqualsPath && pattern2EqualsPath) {
                return 0;
            }
            if (pattern1EqualsPath) {
                return -1;
            }
            if (pattern2EqualsPath) {
                return 1;
            }
            if (info1.isPrefixPattern() && info2.isPrefixPattern()) {
                return info2.getLength() - info1.getLength();
            }
            if (info1.isPrefixPattern() && info2.getDoubleWildcards() == 0) {
                return 1;
            }
            if (info2.isPrefixPattern() && info1.getDoubleWildcards() == 0) {
                return -1;
            }
            if (info1.getTotalCount() != info2.getTotalCount()) {
                return info1.getTotalCount() - info2.getTotalCount();
            }
            if (info1.getLength() != info2.getLength()) {
                return info2.getLength() - info1.getLength();
            }
            if (info1.getSingleWildcards() < info2.getSingleWildcards()) {
                return -1;
            }
            if (info2.getSingleWildcards() < info1.getSingleWildcards()) {
                return 1;
            }
            if (info1.getUriVars() < info2.getUriVars()) {
                return -1;
            }
            if (info2.getUriVars() < info1.getUriVars()) {
                return 1;
            }
            return 0;
        }
        
        private static class PatternInfo
        {
            private final String pattern;
            private int uriVars;
            private int singleWildcards;
            private int doubleWildcards;
            private boolean catchAllPattern;
            private boolean prefixPattern;
            private Integer length;
            
            public PatternInfo(final String pattern) {
                this.pattern = pattern;
                if (this.pattern != null) {
                    this.initCounters();
                    this.catchAllPattern = this.pattern.equals("/**");
                    this.prefixPattern = (!this.catchAllPattern && this.pattern.endsWith("/**"));
                }
                if (this.uriVars == 0) {
                    this.length = ((this.pattern != null) ? this.pattern.length() : 0);
                }
            }
            
            protected void initCounters() {
                int pos = 0;
                if (this.pattern != null) {
                    while (pos < this.pattern.length()) {
                        if (this.pattern.charAt(pos) == '{') {
                            ++this.uriVars;
                            ++pos;
                        }
                        else if (this.pattern.charAt(pos) == '*') {
                            if (pos + 1 < this.pattern.length() && this.pattern.charAt(pos + 1) == '*') {
                                ++this.doubleWildcards;
                                pos += 2;
                            }
                            else if (pos > 0 && !this.pattern.substring(pos - 1).equals(".*")) {
                                ++this.singleWildcards;
                                ++pos;
                            }
                            else {
                                ++pos;
                            }
                        }
                        else {
                            ++pos;
                        }
                    }
                }
            }
            
            public int getUriVars() {
                return this.uriVars;
            }
            
            public int getSingleWildcards() {
                return this.singleWildcards;
            }
            
            public int getDoubleWildcards() {
                return this.doubleWildcards;
            }
            
            public boolean isLeastSpecific() {
                return this.pattern == null || this.catchAllPattern;
            }
            
            public boolean isPrefixPattern() {
                return this.prefixPattern;
            }
            
            public int getTotalCount() {
                return this.uriVars + this.singleWildcards + 2 * this.doubleWildcards;
            }
            
            public int getLength() {
                if (this.length == null) {
                    this.length = ((this.pattern != null) ? AntPathMatcher.VARIABLE_PATTERN.matcher(this.pattern).replaceAll("#").length() : 0);
                }
                return this.length;
            }
        }
    }
    
    private static class PathSeparatorPatternCache
    {
        private final String endsOnWildCard;
        private final String endsOnDoubleWildCard;
        
        public PathSeparatorPatternCache(final String pathSeparator) {
            this.endsOnWildCard = pathSeparator + "*";
            this.endsOnDoubleWildCard = pathSeparator + "**";
        }
        
        public String getEndsOnWildCard() {
            return this.endsOnWildCard;
        }
        
        public String getEndsOnDoubleWildCard() {
            return this.endsOnDoubleWildCard;
        }
    }
}
