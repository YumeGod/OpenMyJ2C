// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class BootstrapMethodsPool {
    private final String baseName;
    private final HashMap<String, Integer> namePool = new HashMap();
    private final HashMap<String, HashMap<String, BootstrapMethod>> methods = new HashMap();
    private final List<ClassNode> classes = new ArrayList<ClassNode>();

    public BootstrapMethodsPool(String baseName) {
        this.baseName = baseName;
    }

    public BootstrapMethod getMethod(String name, String desc, Consumer<MethodNode> creator) {
        ClassNode classNode = null;
        BootstrapMethod existingMethod = (BootstrapMethod)this.methods.computeIfAbsent(name, unused -> new HashMap()).get(desc);
        if (existingMethod != null) {
            return existingMethod;
        }
        MethodNode newMethod = new MethodNode(4169, name, desc, null, new String[0]);
        creator.accept(newMethod);
        ClassNode classNode2 = this.classes.size() == 0 ? null : (classNode = this.classes.get((int)0).methods.size() > 10000 ? null : this.classes.get(0));
        if (classNode == null) {
            classNode = new ClassNode(458752);
            classNode.version = 52;
            classNode.name = this.baseName + "/Bootstrap" + this.classes.size();
            this.classes.add(classNode);
        }
        classNode.methods.add(newMethod);
        BootstrapMethod bootstrapMethod = new BootstrapMethod(classNode, newMethod);
        this.methods.computeIfAbsent(name, unused -> new HashMap()).put(desc, bootstrapMethod);
        return bootstrapMethod;
    }

    public List<ClassNode> getClasses() {
        return this.classes;
    }

    public static class BootstrapMethod {
        private final ClassNode classNode;
        private final MethodNode methodNode;

        private BootstrapMethod(ClassNode classNode, MethodNode methodNode) {
            this.classNode = classNode;
            this.methodNode = methodNode;
        }

        public ClassNode getClassNode() {
            return this.classNode;
        }

        public MethodNode getMethodNode() {
            return this.methodNode;
        }
    }
}
