package rip.jnic;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

public class InterfaceStaticClassProvider
{
    private final String nativeDir;
    private final List<ClassNode> readyClasses;
    private ClassNode currentClass;
    private StringBuilder methods;
    
    public InterfaceStaticClassProvider(final String nativeDir) {
        this.nativeDir = nativeDir;
        this.readyClasses = new ArrayList<ClassNode>();
    }
    
    public void addMethod(final MethodNode method, final String source) {
        final ClassNode classNode = this.getCurrentClass();
        if (classNode.methods.size() > 16384) {
            throw new RuntimeException("too many static interface methods");
        }
        classNode.methods.add(method);
        this.methods.append(source);
    }
    
    public void newClass() {
        this.currentClass = null;
        this.methods = null;
    }
    
    public List<ClassNode> getReadyClasses() {
        return this.readyClasses;
    }
    
    public boolean isEmpty() {
        return this.currentClass == null;
    }
    
    public String getMethods() {
        return this.methods.toString();
    }
    
    public String getCurrentClassName() {
        return this.getCurrentClass().name;
    }
    
    private ClassNode getCurrentClass() {
        if (this.currentClass == null) {
            this.methods = new StringBuilder();
            this.currentClass = new ClassNode();
            this.currentClass.version = 52;
            this.currentClass.sourceFile = "synthetic";
            this.currentClass.access = 1;
            this.currentClass.superName = "java/lang/Object";
            this.currentClass.name = this.nativeDir + "/interfacestatic/Methods" + this.readyClasses.size();
            this.readyClasses.add(this.currentClass);
        }
        return this.currentClass;
    }
}
