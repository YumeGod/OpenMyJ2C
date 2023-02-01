package rip.jnic.instructions;

import rip.jnic.MethodContext;
import org.objectweb.asm.tree.LabelNode;

public class LabelHandler extends GenericInstructionHandler<LabelNode>
{
    @Override
    public void accept(final MethodContext context, final LabelNode node) {
        context.method.tryCatchBlocks.stream().filter(x -> x.start.equals(node)).forEachOrdered(context.tryCatches::add);
        context.method.tryCatchBlocks.stream().filter(x -> x.end.equals(node)).forEachOrdered(context.tryCatches::remove);
        try {
            super.accept(context, node);
        }
        catch (UnsupportedOperationException ex) {}
        context.output.append(String.format("%s:;\n", context.getLabelPool().getName(node.getLabel())));
    }
    
    @Override
    public String insnToString(final MethodContext context, final LabelNode node) {
        return String.format("LABEL %s", context.getLabelPool().getName(node.getLabel()));
    }
    
    @Override
    public int getNewStackPointer(final LabelNode node, final int currentStackPointer) {
        return currentStackPointer;
    }
    
    @Override
    protected void process(final MethodContext context, final LabelNode node) {
        throw new UnsupportedOperationException("break at super.process()");
    }
}
