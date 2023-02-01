package rip.jnic.special;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class ClInitSpecialMethodProcessor implements SpecialMethodProcessor
{
    @Override
    public String preProcess(final MethodContext context) {
        final String name = "$myj2cClinit";
        if (!Util.getFlag(context.clazz.access, 512)) {
            context.proxyMethod = new MethodNode(458752, 4362, name, context.method.desc, context.method.signature, new String[0]);
            context.clazz.methods.add(context.proxyMethod);
        }
        return name;
    }
    
    @Override
    public void postProcess(final MethodContext context) {
        final InsnList instructions = context.method.instructions;
        instructions.clear();
        instructions.add(new LdcInsnNode((Object)context.classIndex));
        instructions.add(new LdcInsnNode(Type.getObjectType(context.clazz.name)));
        instructions.add(new MethodInsnNode(184, context.obfuscator.getNativeDir() + "/Loader", "registerNativesForClass", "(ILjava/lang/Class;)V", false));
        instructions.add(new MethodInsnNode(184, context.clazz.name, "$myj2cLoader", "()V", false));
        if (Util.getFlag(context.clazz.access, 512)) {
            if (context.nativeMethod == null) {
                throw new RuntimeException("Native method not created?!");
            }
            instructions.add(new MethodInsnNode(184, context.obfuscator.getStaticClassProvider().getCurrentClassName(), context.nativeMethod.name, context.nativeMethod.desc, false));
        }
        else if (!context.obfuscator.getNoInitClassMap().containsKey(context.clazz.name)) {
            instructions.add(new MethodInsnNode(184, context.clazz.name, "$myj2cClinit", context.method.desc, false));
        }
        instructions.add(new InsnNode(177));
    }
}
