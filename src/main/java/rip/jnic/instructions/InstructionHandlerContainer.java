package rip.jnic.instructions;

import rip.jnic.MethodContext;
import org.objectweb.asm.tree.AbstractInsnNode;

public class InstructionHandlerContainer<T extends AbstractInsnNode>
{
    private final InstructionTypeHandler<T> handler;
    private final Class<T> clazz;
    
    public InstructionHandlerContainer(final InstructionTypeHandler<T> handler, final Class<T> clazz) {
        this.handler = handler;
        this.clazz = clazz;
    }
    
    public void accept(final MethodContext context, final AbstractInsnNode node) {
        this.handler.accept(context, this.clazz.cast(node));
    }
    
    public String insnToString(final MethodContext context, final AbstractInsnNode node) {
        return this.handler.insnToString(context, this.clazz.cast(node));
    }
    
    public int getNewStackPointer(final AbstractInsnNode node, final int stackPointer) {
        return this.handler.getNewStackPointer(this.clazz.cast(node), stackPointer);
    }
}
