package rip.jnic.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarFile;

public class ClassMetadataReader
{
    private final List<JarFile> classPath;
    
    public ClassMetadataReader(final List<JarFile> classPath) {
        this.classPath = classPath;
    }
    
    public List<JarFile> getCp() {
        return Collections.unmodifiableList((List<? extends JarFile>)this.classPath);
    }
    
    public ClassMetadataReader() {
        this.classPath = new ArrayList<JarFile>();
    }
    
    public void acceptVisitor(final byte[] classData, final ClassVisitor visitor) {
        new ClassReader(classData).accept(visitor, 0);
    }
    
    public void acceptVisitor(final String className, final ClassVisitor visitor) throws IOException, ClassNotFoundException {
        this.acceptVisitor(this.getClassData(className), visitor);
    }
    
    private static byte[] read(final InputStream input) throws IOException {
        try (final ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[4096];
            for (int length = input.read(buffer); length >= 0; length = input.read(buffer)) {
                output.write(buffer, 0, length);
            }
            return output.toByteArray();
        }
    }
    
    public byte[] getClassData(final String className) throws IOException, ClassNotFoundException {
        for (final JarFile file : this.classPath) {
            if (file.getEntry(className + ".class") == null) {
                continue;
            }
            try (final InputStream in = file.getInputStream(file.getEntry(className + ".class"))) {
                return read(in);
            }
        }
        throw new ClassNotFoundException(className);
    }
    
    public String getSuperClass(final String type) {
        if (type.equals("java/lang/Object")) {
            return null;
        }
        try {
            return this.getSuperClassASM(type);
        }
        catch (IOException | ClassNotFoundException ex2) {
            return "java/lang/Object";
        }
    }
    
    protected String getSuperClassASM(final String type) throws IOException, ClassNotFoundException {
        final CheckSuperClassVisitor cv = new CheckSuperClassVisitor();
        this.acceptVisitor(type, cv);
        return cv.superClassName;
    }
    
    public ArrayList<String> getSuperClasses(String type) {
        final ArrayList<String> superclasses = new ArrayList<String>(1);
        superclasses.add(type);
        while ((type = this.getSuperClass(type)) != null) {
            superclasses.add(type);
        }
        Collections.reverse(superclasses);
        return superclasses;
    }
    
    public void close() {
        this.classPath.forEach(file -> {
            try {
                file.close();
            }
            catch (IOException ex) {}
        });
    }
    
    private static class CheckSuperClassVisitor extends ClassVisitor
    {
        String superClassName;
        
        public CheckSuperClassVisitor() {
            super(458752);
        }
        
        @Override
        public void visit(final int version, final int access, final String name, final String signature, final String superName, final String[] interfaces) {
            this.superClassName = superName;
        }
    }
}
