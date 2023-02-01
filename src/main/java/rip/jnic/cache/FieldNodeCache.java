package rip.jnic.cache;

import java.util.HashMap;
import java.util.Map;

public class FieldNodeCache
{
    private final String pointerPattern;
    private final Map<CachedFieldInfo, Integer> cache;
    private ClassNodeCache classNodeCache;
    
    public FieldNodeCache(final String pointerPattern, final ClassNodeCache classNodeCache) {
        this.pointerPattern = pointerPattern;
        this.classNodeCache = classNodeCache;
        this.cache = new HashMap<CachedFieldInfo, Integer>();
    }
    
    public String getPointer(final CachedFieldInfo fieldInfo) {
        return String.format(this.pointerPattern, this.getId(fieldInfo));
    }
    
    public int getId(final CachedFieldInfo fieldInfo) {
        if (!this.cache.containsKey(fieldInfo)) {
            final CachedClassInfo classInfo = this.classNodeCache.getClass(fieldInfo.getClazz());
            classInfo.addCachedField(fieldInfo);
            this.cache.put(fieldInfo, this.cache.size());
        }
        return this.cache.get(fieldInfo);
    }
    
    public int size() {
        return this.cache.size();
    }
    
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }
    
    public Map<CachedFieldInfo, Integer> getCache() {
        return this.cache;
    }
    
    public void clear() {
        this.cache.clear();
    }
}
