package rip.jnic;

import rip.jnic.nativeobfuscator.Native;
import rip.jnic.nativeobfuscator.NotNative;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;

public class ClassMethodFilter
{
    private static final String NATIVE_ANNOTATION_DESC;
    private static final String NOT_NATIVE_ANNOTATION_DESC;
    private final List<String> blackList;
    private final List<String> whiteList;
    private final boolean useAnnotations;
    private static final AntPathMatcher pathMatcher;
    
    public ClassMethodFilter(final List<String> blackList, final List<String> whiteList, final boolean useAnnotations) {
        this.blackList = blackList;
        this.whiteList = whiteList;
        this.useAnnotations = useAnnotations;
    }
    
    public boolean shouldProcess(final ClassNode classNode) {
        if ((classNode.access & 0x200) != 0x0) {
            return false;
        }
        if (!Util.isValidJavaFullClassName(classNode.name.replaceAll("/", "."))) {
            return false;
        }
        if (this.blackList != null) {
            for (final String black : this.blackList) {
                if (!black.contains("#") && ClassMethodFilter.pathMatcher.matchStart(black, classNode.name)) {
                    return false;
                }
            }
        }
        boolean toMethod = false;
        if (this.whiteList != null && this.whiteList.size() > 0) {
            for (final String white : this.whiteList) {
                if (!white.contains("#")) {
                    if (ClassMethodFilter.pathMatcher.matchStart(white, classNode.name)) {
                        return true;
                    }
                    continue;
                }
                else {
                    final String whiteClass = white.split("#")[0];
                    if (!ClassMethodFilter.pathMatcher.matchStart(whiteClass, classNode.name)) {
                        continue;
                    }
                    toMethod = true;
                }
            }
            if (!toMethod) {
                return false;
            }
        }
        return (this.useAnnotations && classNode.invisibleAnnotations != null && classNode.invisibleAnnotations.stream().anyMatch(annotationNode -> annotationNode.desc.equals(ClassMethodFilter.NATIVE_ANNOTATION_DESC))) || classNode.methods.stream().anyMatch(methodNode -> this.shouldProcess(classNode, methodNode));
    }
    
    public boolean shouldProcess(final ClassNode classNode, final MethodNode methodNode) {
        if (this.blackList != null) {
            for (final String black : this.blackList) {
                if (black.contains("#")) {
                    final String blackClass = black.split("#")[0];
                    if (!ClassMethodFilter.pathMatcher.matchStart(blackClass, classNode.name)) {
                        continue;
                    }
                    final String blackMethod = black.split("#")[1];
                    if (blackMethod.contains("!")) {
                        if (ClassMethodFilter.pathMatcher.matchStart(blackMethod, methodNode.name + '!' + methodNode.desc)) {
                            return false;
                        }
                        continue;
                    }
                    else {
                        if (ClassMethodFilter.pathMatcher.matchStart(blackMethod, methodNode.name)) {
                            return false;
                        }
                        continue;
                    }
                }
            }
        }
        if (this.whiteList != null && this.whiteList.size() > 0) {
            boolean bl = false;
            for (final String white : this.whiteList) {
                if (white.contains("#")) {
                    bl = true;
                    final String whiteClass = white.split("#")[0];
                    if (!ClassMethodFilter.pathMatcher.matchStart(whiteClass, classNode.name)) {
                        continue;
                    }
                    final String whiteMethod = white.split("#")[1];
                    if (whiteMethod.contains("!")) {
                        if (ClassMethodFilter.pathMatcher.matchStart(whiteMethod, methodNode.name + '!' + methodNode.desc)) {
                            return true;
                        }
                        continue;
                    }
                    else {
                        if (ClassMethodFilter.pathMatcher.matchStart(whiteMethod, methodNode.name)) {
                            return true;
                        }
                        continue;
                    }
                }
            }
            if (bl) {
                return false;
            }
        }
        if (this.useAnnotations) {
            final boolean classIsMarked = classNode.invisibleAnnotations != null && classNode.invisibleAnnotations.stream().anyMatch(annotationNode -> annotationNode.desc.equals(ClassMethodFilter.NATIVE_ANNOTATION_DESC));
            return (methodNode.invisibleAnnotations != null && methodNode.invisibleAnnotations.stream().anyMatch(annotationNode -> annotationNode.desc.equals(ClassMethodFilter.NATIVE_ANNOTATION_DESC))) || (classIsMarked && (methodNode.invisibleAnnotations == null || methodNode.invisibleAnnotations.stream().noneMatch(annotationNode -> annotationNode.desc.equals(ClassMethodFilter.NOT_NATIVE_ANNOTATION_DESC))));
        }
        return true;
    }
    
    public static void cleanAnnotations(final ClassNode classNode) {
        if (classNode.invisibleAnnotations != null) {
            classNode.invisibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(ClassMethodFilter.NATIVE_ANNOTATION_DESC));
        }
        classNode.methods.stream().filter(methodNode -> methodNode.invisibleAnnotations != null).forEach(methodNode -> methodNode.invisibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(ClassMethodFilter.NATIVE_ANNOTATION_DESC) || annotationNode.desc.equals(ClassMethodFilter.NOT_NATIVE_ANNOTATION_DESC)));
    }
    
    static {
        NATIVE_ANNOTATION_DESC = Type.getDescriptor(Native.class);
        NOT_NATIVE_ANNOTATION_DESC = Type.getDescriptor(NotNative.class);
        pathMatcher = new AntPathMatcher();
    }
}
