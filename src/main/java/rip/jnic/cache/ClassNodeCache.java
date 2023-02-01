package rip.jnic.cache;

import java.util.HashMap;
import java.util.Map;

public class ClassNodeCache
{
    private final String pointerPattern;
    private final Map<String, CachedClassInfo> cache;
    
    public ClassNodeCache(final String pointerPattern) {
        this.pointerPattern = pointerPattern;
        this.cache = new HashMap<String, CachedClassInfo>();
    }
    
    public String getPointer(final String key) {
        return String.format(this.pointerPattern, this.getId(key));
    }
    
    public int getId(String clazz) {
        if (clazz.endsWith(";") && !clazz.startsWith("[")) {
            if (clazz.startsWith("L")) {
                clazz = clazz.substring(1);
            }
            clazz = clazz.replace(";", "");
        }
        if (clazz.startsWith("native/magic/1/linkcallsite/obfuscator")) {
            return 0;
        }
        if (!this.cache.containsKey(clazz)) {
            final CachedClassInfo classInfo = new CachedClassInfo(clazz, clazz, "", this.cache.size(), false);
            this.cache.put(clazz, classInfo);
        }
        return this.cache.get(clazz).getId();
    }
    
    public CachedClassInfo getClass(String clazz) {
        if (clazz.endsWith(";") && !clazz.startsWith("[")) {
            if (clazz.startsWith("L")) {
                clazz = clazz.substring(1);
            }
            clazz = clazz.replace(";", "");
        }
        if (clazz.startsWith("native/magic/1/linkcallsite/obfuscator")) {
            return null;
        }
        if (!this.cache.containsKey(clazz)) {
            final CachedClassInfo classInfo = new CachedClassInfo(clazz, clazz, "", this.cache.size(), false);
            this.cache.put(clazz, classInfo);
        }
        return this.cache.get(clazz);
    }
    
    public int size() {
        return this.cache.size();
    }
    
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }
    
    public Map<String, CachedClassInfo> getCache() {
        return this.cache;
    }
    
    public void clear() {
        this.cache.clear();
    }
}
