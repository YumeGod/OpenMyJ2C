package rip.jnic.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.util.ArrayList;

public class SafeClassWriter extends ClassWriter
{
    private final ClassMetadataReader classMetadataReader;
    
    public SafeClassWriter(final ClassMetadataReader classMetadataReader, final int flags) {
        super(flags);
        this.classMetadataReader = classMetadataReader;
    }
    
    public SafeClassWriter(final ClassReader classReader, final ClassMetadataReader classMetadataReader, final int flags) {
        super(classReader, flags);
        this.classMetadataReader = classMetadataReader;
    }
    
    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        ArrayList<String> superClasses1;
        ArrayList<String> superClasses2;
        int size;
        int i;
        for (superClasses1 = this.classMetadataReader.getSuperClasses(type1), superClasses2 = this.classMetadataReader.getSuperClasses(type2), size = Math.min(superClasses1.size(), superClasses2.size()), i = 0; i < size && superClasses1.get(i).equals(superClasses2.get(i)); ++i) {}
        if (i == 0) {
            return "java/lang/Object";
        }
        return superClasses1.get(i - 1);
    }
}
