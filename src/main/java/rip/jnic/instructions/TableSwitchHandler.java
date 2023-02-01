package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.TableSwitchInsnNode;

public class TableSwitchHandler extends GenericInstructionHandler<TableSwitchInsnNode>
{
    @Override
    protected void process(final MethodContext context, final TableSwitchInsnNode node) {
        final StringBuilder output = context.output;
        output.append(getStart(context)).append("\n    ");
        for (int i = 0; i < node.labels.size(); ++i) {
            output.append(String.format("    %s\n    ", getPart(context, node.min + i, node.labels.get(i).getLabel())));
        }
        output.append(String.format("    %s\n    ", getDefault(context, node.dflt.getLabel())));
        this.instructionName = "TABLESWITCH_END";
    }
    
    private static String getStart(final MethodContext context) {
        return context.getSnippets().getSnippet("TABLESWITCH_START", Util.createMap("stackindexm1", String.valueOf(context.stackPointer - 1)));
    }
    
    private static String getPart(final MethodContext context, final int index, final Label label) {
        return context.getSnippets().getSnippet("TABLESWITCH_PART", Util.createMap("index", index, "label", context.getLabelPool().getName(label)));
    }
    
    private static String getDefault(final MethodContext context, final Label label) {
        return context.getSnippets().getSnippet("TABLESWITCH_DEFAULT", Util.createMap("label", context.getLabelPool().getName(label)));
    }
    
    @Override
    public String insnToString(final MethodContext context, final TableSwitchInsnNode node) {
        return "TABLESWITCH";
    }
    
    @Override
    public int getNewStackPointer(final TableSwitchInsnNode node, final int currentStackPointer) {
        return currentStackPointer - 1;
    }
}
