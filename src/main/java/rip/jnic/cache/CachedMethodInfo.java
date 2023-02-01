package rip.jnic.cache;

import java.util.Objects;

public class CachedMethodInfo
{
    private final String clazz;
    private final String name;
    private final String desc;
    private final boolean isStatic;
    private int id;
    
    public CachedMethodInfo(final String clazz, final String name, final String desc, final boolean isStatic) {
        this.clazz = clazz;
        this.name = name;
        this.desc = desc;
        this.isStatic = isStatic;
        this.id = -1;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final CachedMethodInfo that = (CachedMethodInfo)o;
        return this.isStatic == that.isStatic && this.clazz.equals(that.clazz) && this.name.equals(that.name) && this.desc.equals(that.desc);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.clazz, this.name, this.desc, this.isStatic);
    }
    
    public String getClazz() {
        return this.clazz;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDesc() {
        return this.desc;
    }
    
    public boolean isStatic() {
        return this.isStatic;
    }
    
    public int getId() {
        return this.id;
    }
    
    public void setId(final int id) {
        this.id = id;
    }
    
    @Override
    public String toString() {
        return "CachedMethodInfo{clazz='" + this.clazz + '\'' + ", name='" + this.name + '\'' + ", desc='" + this.desc + '\'' + ", isStatic=" + this.isStatic + '}';
    }
}
