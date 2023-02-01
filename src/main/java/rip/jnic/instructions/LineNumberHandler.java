package rip.jnic.instructions;

import rip.jnic.MethodContext;
import org.objectweb.asm.tree.LineNumberNode;

public class LineNumberHandler implements InstructionTypeHandler<LineNumberNode>
{
    @Override
    public void accept(final MethodContext context, final LineNumberNode node) {
        context.line = node.line;
    }
    
    @Override
    public String insnToString(final MethodContext context, final LineNumberNode node) {
        return String.format("Line %d", node.line);
    }
    
    @Override
    public int getNewStackPointer(final LineNumberNode node, final int currentStackPointer) {
        return currentStackPointer;
    }
}
