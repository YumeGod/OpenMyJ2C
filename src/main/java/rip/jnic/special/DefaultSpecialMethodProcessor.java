package rip.jnic.special;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class DefaultSpecialMethodProcessor implements SpecialMethodProcessor
{
    @Override
    public String preProcess(final MethodContext context) {
        context.proxyMethod = context.method;
        final MethodNode method = context.method;
        method.access |= 0x100;
        return "native_" + context.method.name + context.methodIndex;
    }
    
    @Override
    public void postProcess(final MethodContext context) {
        context.method.instructions.clear();
        if (Util.getFlag(context.clazz.access, 512)) {
            final InsnList list = new InsnList();
            int localVarsPosition = 0;
            for (final Type arg : context.argTypes) {
                list.add(new VarInsnNode(arg.getOpcode(21), localVarsPosition));
                localVarsPosition += arg.getSize();
            }
            if (context.nativeMethod == null) {
                throw new RuntimeException("Native method not created?!");
            }
            list.add(new MethodInsnNode(184, context.obfuscator.getStaticClassProvider().getCurrentClassName(), context.nativeMethod.name, context.nativeMethod.desc, false));
            list.add(new InsnNode(Type.getReturnType(context.method.desc).getOpcode(172)));
            context.method.instructions = list;
        }
    }
}
