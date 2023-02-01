package rip.jnic.cache;

import java.util.HashMap;
import java.util.Map;

public class MethodNodeCache
{
    private final String pointerPattern;
    private final Map<CachedMethodInfo, Integer> cache;
    private ClassNodeCache classNodeCache;
    
    public MethodNodeCache(final String pointerPattern, final ClassNodeCache classNodeCache) {
        this.pointerPattern = pointerPattern;
        this.classNodeCache = classNodeCache;
        this.cache = new HashMap<CachedMethodInfo, Integer>();
    }
    
    public String getPointer(final CachedMethodInfo methodInfo) {
        return String.format(this.pointerPattern, this.getId(methodInfo));
    }
    
    public int getId(final CachedMethodInfo methodInfo) {
        if (!this.cache.containsKey(methodInfo)) {
            final CachedClassInfo classInfo = this.classNodeCache.getClass(methodInfo.getClazz());
            classInfo.addCachedMethod(methodInfo);
            this.cache.put(methodInfo, this.cache.size());
        }
        return this.cache.get(methodInfo);
    }
    
    public int size() {
        return this.cache.size();
    }
    
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }
    
    public Map<CachedMethodInfo, Integer> getCache() {
        return this.cache;
    }
    
    public void clear() {
        this.cache.clear();
    }
}
