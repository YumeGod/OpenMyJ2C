package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.LdcInsnNode;

public class LdcHandler extends GenericInstructionHandler<LdcInsnNode>
{
    public static String getIntString(final int value) {
        return (value == Integer.MIN_VALUE) ? "(jint) 2147483648U" : String.valueOf(value);
    }
    
    public static String getLongValue(final long value) {
        return (value == Long.MIN_VALUE) ? "(jlong) 9223372036854775808ULL" : (String.valueOf(value) + "LL");
    }
    
    public static String getFloatValue(final float value) {
        if (Float.isNaN(value)) {
            return "NAN";
        }
        if (value == Float.POSITIVE_INFINITY) {
            return "HUGE_VALF";
        }
        if (value == Float.NEGATIVE_INFINITY) {
            return "-HUGE_VALF";
        }
        return value + "f";
    }
    
    public static String getDoubleValue(final double value) {
        if (Double.isNaN(value)) {
            return "NAN";
        }
        if (value == Double.POSITIVE_INFINITY) {
            return "HUGE_VAL";
        }
        if (value == Double.NEGATIVE_INFINITY) {
            return "-HUGE_VAL";
        }
        return String.valueOf(value);
    }
    
    @Override
    protected void process(final MethodContext context, final LdcInsnNode node) {
        final Object cst = node.cst;
        if (cst instanceof String) {
            if (node.cst.toString() != null && node.cst.toString().length() > 0) {
                this.instructionName += "_STRING";
                this.props.put("cst_ptr", Util.utf82unicode(node.cst.toString()));
                this.props.put("cst_length", "" + node.cst.toString().length());
            }
            else {
                this.instructionName += "_STRING_NULL";
            }
        }
        else if (cst instanceof Integer) {
            this.instructionName += "_INT";
            this.props.put("cst", getIntString((int)cst));
        }
        else if (cst instanceof Long) {
            this.instructionName += "_LONG";
            this.props.put("cst", getLongValue((long)cst));
        }
        else if (cst instanceof Float) {
            this.instructionName += "_FLOAT";
            this.props.put("cst", getFloatValue((float)node.cst));
        }
        else if (cst instanceof Double) {
            this.instructionName += "_DOUBLE";
            this.props.put("cst", getDoubleValue((double)node.cst));
        }
        else {
            if (!(cst instanceof Type)) {
                throw new UnsupportedOperationException();
            }
            this.instructionName += "_CLASS";
            this.props.put("class_ptr", "c_" + context.getCachedClasses().getId(node.cst.toString()) + "_");
            this.props.put("cst_ptr", context.getCachedClasses().getPointer(node.cst.toString()));
        }
    }
    
    @Override
    public String insnToString(final MethodContext context, final LdcInsnNode node) {
        return String.format("LDC %s", node.cst);
    }
    
    @Override
    public int getNewStackPointer(final LdcInsnNode node, final int currentStackPointer) {
        if (node.cst instanceof Double || node.cst instanceof Long) {
            return currentStackPointer + 2;
        }
        return currentStackPointer + 1;
    }
}
