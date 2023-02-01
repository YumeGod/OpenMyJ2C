package rip.jnic;

import rip.jnic.cache.ClassNodeCache;
import rip.jnic.cache.FieldNodeCache;
import rip.jnic.cache.MethodNodeCache;
import rip.jnic.cache.NodeCache;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import java.util.*;

public class MethodContext
{
    public NativeObfuscator obfuscator;
    public final MethodNode method;
    public final ClassNode clazz;
    public final int methodIndex;
    public final int classIndex;
    public final StringBuilder output;
    public final StringBuilder nativeMethods;
    public Type ret;
    public ArrayList<Type> argTypes;
    public int line;
    public List<Integer> stack;
    public List<Integer> locals;
    public Set<TryCatchBlockNode> tryCatches;
    public Map<CatchesBlock, String> catches;
    public MethodNode proxyMethod;
    public MethodNode nativeMethod;
    public int stackPointer;
    private final LabelPool labelPool;
    
    public MethodContext(final NativeObfuscator obfuscator, final MethodNode method, final int methodIndex, final ClassNode clazz, final int classIndex) {
        this.labelPool = new LabelPool();
        this.obfuscator = obfuscator;
        this.method = method;
        this.methodIndex = methodIndex;
        this.clazz = clazz;
        this.classIndex = classIndex;
        this.output = new StringBuilder();
        this.nativeMethods = new StringBuilder();
        this.line = -1;
        this.stack = new ArrayList<Integer>();
        this.locals = new ArrayList<Integer>();
        this.tryCatches = new HashSet<TryCatchBlockNode>();
        this.catches = new HashMap<CatchesBlock, String>();
    }
    
    public NodeCache<String> getCachedStrings() {
        return this.obfuscator.getCachedStrings();
    }
    
    public ClassNodeCache getCachedClasses() {
        return this.obfuscator.getCachedClasses();
    }
    
    public MethodNodeCache getCachedMethods() {
        return this.obfuscator.getCachedMethods();
    }
    
    public FieldNodeCache getCachedFields() {
        return this.obfuscator.getCachedFields();
    }
    
    public Snippets getSnippets() {
        return this.obfuscator.getSnippets();
    }
    
    public LabelPool getLabelPool() {
        return this.labelPool;
    }
}
