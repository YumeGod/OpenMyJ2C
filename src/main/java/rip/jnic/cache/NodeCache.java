package rip.jnic.cache;

import java.util.HashMap;
import java.util.Map;

public class NodeCache<T>
{
    private final String pointerPattern;
    private final Map<T, Integer> cache;
    
    public NodeCache(final String pointerPattern) {
        this.pointerPattern = pointerPattern;
        this.cache = new HashMap<T, Integer>();
    }
    
    public String getPointer(final T key) {
        return String.format(this.pointerPattern, this.getId(key));
    }
    
    public int getId(final T key) {
        if (!this.cache.containsKey(key)) {
            this.cache.put(key, this.cache.size());
        }
        return this.cache.get(key);
    }
    
    public int size() {
        return this.cache.size();
    }
    
    public boolean isEmpty() {
        return this.cache.isEmpty();
    }
    
    public Map<T, Integer> getCache() {
        return this.cache;
    }
    
    public void clear() {
        this.cache.clear();
    }
}
