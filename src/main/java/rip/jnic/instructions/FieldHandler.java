package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import rip.jnic.cache.CachedClassInfo;
import rip.jnic.cache.CachedFieldInfo;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;

import java.util.List;

public class FieldHandler extends GenericInstructionHandler<FieldInsnNode>
{
    @Override
    protected void process(final MethodContext context, final FieldInsnNode node) {
        final boolean isStatic = node.getOpcode() == 178 || node.getOpcode() == 179;
        final CachedFieldInfo info = new CachedFieldInfo(node.owner, node.name, node.desc, isStatic);
        this.instructionName = this.instructionName + "_" + Type.getType(node.desc).getSort();
        this.props.put("class_ptr", "c_" + context.getCachedClasses().getId(node.owner) + "_");
        final CachedClassInfo classInfo = context.getCachedClasses().getCache().get(node.owner);
        final List<CachedFieldInfo> cachedFields = classInfo.getCachedFields();
        for (int i = 0; i < cachedFields.size(); ++i) {
            final CachedFieldInfo fieldNode = cachedFields.get(i);
            if (fieldNode.getName().equals(node.name)) {
                this.props.put("field_id", "id_" + i);
            }
        }
        if (this.props.get("field_id") == null) {
            cachedFields.add(info);
            this.props.put("field_id", "id_" + (cachedFields.size() - 1));
        }
    }
    
    @Override
    public String insnToString(final MethodContext context, final FieldInsnNode node) {
        return String.format("%s %s.%s %s", Util.getOpcodeString(node.getOpcode()), node.owner, node.name, node.desc);
    }
    
    @Override
    public int getNewStackPointer(final FieldInsnNode node, int currentStackPointer) {
        if (node.getOpcode() == 180 || node.getOpcode() == 181) {
            --currentStackPointer;
        }
        if (node.getOpcode() == 178 || node.getOpcode() == 180) {
            currentStackPointer += Type.getType(node.desc).getSize();
        }
        if (node.getOpcode() == 179 || node.getOpcode() == 181) {
            currentStackPointer -= Type.getType(node.desc).getSize();
        }
        return currentStackPointer;
    }
}
