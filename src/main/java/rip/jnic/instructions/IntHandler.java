package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.tree.IntInsnNode;

public class IntHandler extends GenericInstructionHandler<IntInsnNode>
{
    @Override
    protected void process(final MethodContext context, final IntInsnNode node) {
        this.props.put("operand", String.valueOf(node.operand));
        if (node.getOpcode() == 188) {
            this.instructionName = this.instructionName + "_" + node.operand;
        }
    }
    
    @Override
    public String insnToString(final MethodContext context, final IntInsnNode node) {
        return String.format("%s %d", Util.getOpcodeString(node.getOpcode()), node.operand);
    }
    
    @Override
    public int getNewStackPointer(final IntInsnNode node, final int currentStackPointer) {
        switch (node.getOpcode()) {
            case 16:
            case 17: {
                return currentStackPointer + 1;
            }
            case 188: {
                return currentStackPointer;
            }
            default: {
                throw new RuntimeException();
            }
        }
    }
}
