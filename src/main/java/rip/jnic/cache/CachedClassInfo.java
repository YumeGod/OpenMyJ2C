package rip.jnic.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CachedClassInfo
{
    private final String clazz;
    private final String name;
    private final int id;
    private final String desc;
    private final boolean isStatic;
    private final List<CachedFieldInfo> cachedFields;
    private final List<CachedMethodInfo> cachedMethods;
    
    public CachedClassInfo(final String clazz, final String name, final String desc, final int id, final boolean isStatic) {
        this.cachedFields = new ArrayList<CachedFieldInfo>();
        this.cachedMethods = new ArrayList<CachedMethodInfo>();
        this.clazz = clazz;
        this.name = name;
        this.desc = desc;
        this.id = id;
        this.isStatic = isStatic;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final CachedClassInfo that = (CachedClassInfo)o;
        return this.isStatic == that.isStatic && this.clazz.equals(that.clazz) && this.name.equals(that.name) && this.desc.equals(that.desc);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.clazz, this.name, this.desc, this.isStatic);
    }
    
    public List<CachedFieldInfo> getCachedFields() {
        return this.cachedFields;
    }
    
    public List<CachedMethodInfo> getCachedMethods() {
        return this.cachedMethods;
    }
    
    public void addCachedField(final CachedFieldInfo cachedFieldInfo) {
        boolean contains = false;
        for (final CachedFieldInfo cachedField : this.cachedFields) {
            if (cachedField.equals(cachedFieldInfo)) {
                contains = true;
            }
        }
        if (!contains) {
            cachedFieldInfo.setId(this.cachedFields.size());
            this.cachedFields.add(cachedFieldInfo);
        }
    }
    
    public int getCachedFieldId(final CachedFieldInfo cachedFieldInfo) {
        for (final CachedFieldInfo cachedField : this.cachedFields) {
            if (cachedField.equals(cachedFieldInfo)) {
                return cachedField.getId();
            }
        }
        cachedFieldInfo.setId(this.cachedFields.size());
        this.cachedFields.add(cachedFieldInfo);
        return cachedFieldInfo.getId();
    }
    
    public int getCachedMethodId(final CachedMethodInfo cachedMethodInfo) {
        for (final CachedMethodInfo methodInfo : this.cachedMethods) {
            if (methodInfo.equals(cachedMethodInfo)) {
                return methodInfo.getId();
            }
        }
        cachedMethodInfo.setId(this.cachedMethods.size());
        this.cachedMethods.add(cachedMethodInfo);
        return cachedMethodInfo.getId();
    }
    
    public void addCachedMethod(final CachedMethodInfo cachedMethodInfo) {
        boolean contains = false;
        for (final CachedMethodInfo methodInfo : this.cachedMethods) {
            if (methodInfo.equals(cachedMethodInfo)) {
                contains = true;
            }
        }
        if (!contains) {
            cachedMethodInfo.setId(this.cachedMethods.size());
            this.cachedMethods.add(cachedMethodInfo);
        }
    }
    
    public int getId() {
        return this.id;
    }
    
    @Override
    public String toString() {
        return "CachedClassInfo{clazz='" + this.clazz + '\'' + ", name='" + this.name + '\'' + ", id=" + this.id + ", desc='" + this.desc + '\'' + ", isStatic=" + this.isStatic + ", cachedFields=" + this.cachedFields + ", cachedMethods=" + this.cachedMethods + '}';
    }
}
