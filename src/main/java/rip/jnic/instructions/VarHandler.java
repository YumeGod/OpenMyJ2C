package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.tree.VarInsnNode;

public class VarHandler extends GenericInstructionHandler<VarInsnNode>
{
    @Override
    protected void process(final MethodContext context, final VarInsnNode node) {
        this.props.put("var", String.valueOf(node.var));
    }
    
    @Override
    public String insnToString(final MethodContext context, final VarInsnNode node) {
        return String.format("%s %d", Util.getOpcodeString(node.getOpcode()), node.var);
    }
    
    @Override
    public int getNewStackPointer(final VarInsnNode node, final int currentStackPointer) {
        switch (node.getOpcode()) {
            case 21:
            case 23:
            case 25: {
                return currentStackPointer + 1;
            }
            case 22:
            case 24: {
                return currentStackPointer + 2;
            }
            case 54:
            case 56:
            case 58: {
                return currentStackPointer - 1;
            }
            case 55:
            case 57: {
                return currentStackPointer - 2;
            }
            default: {
                throw new RuntimeException();
            }
        }
    }
}
