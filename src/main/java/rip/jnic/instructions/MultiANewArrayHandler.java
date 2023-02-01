// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic.instructions;

import rip.jnic.MethodContext;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MultiANewArrayHandler
        extends GenericInstructionHandler<MultiANewArrayInsnNode> {
    @Override
    protected void process(MethodContext context, MultiANewArrayInsnNode node) {
        this.instructionName = null;
        context.output.append(this.genCode(context, node));
    }

    @Override
    public String insnToString(MethodContext context, MultiANewArrayInsnNode node) {
        return String.format("MULTIANEWARRAY %d %s", node.dims, node.desc);
    }

    @Override
    public int getNewStackPointer(MultiANewArrayInsnNode node, int currentStackPointer) {
        return currentStackPointer - node.dims + 1;
    }

    private String genCode(MethodContext context, MultiANewArrayInsnNode node) {
        String code = "{\n";
        int dimensions = node.dims;
        code = code + "jsize dim[] = " + String.format("{ %s };\n", IntStream.range(0, dimensions).mapToObj(i -> String.format("cstack%d.i", i)).collect(Collectors.joining(", ")));
        String desc = node.desc;
        desc = desc.substring(1);
        code = code + "cstack0.l = (*env)->NewObjectArray(env, dim[0], c_" + context.getCachedClasses().getId(desc) + "_(env)->clazz, NULL);\n";
        code = code + this.getSub(context, node, 0, dimensions);
        code = code + "};";
        return code;
    }

    private String getSub(MethodContext context, MultiANewArrayInsnNode node, int index, int max) {
        String code = "";
        if (index < max - 1) {
            String space = "";
            for (int i = 0; i < index + 1; ++i) {
                space = space + "    ";
            }
            int next = index + 1;
            code = code + space + "for(jsize d" + index + " = 0; d" + index + " < dim[" + index + "]; d" + index + "++) {\n";
            String desc = node.desc;
            for (int i = 0; i < next + 1; ++i) {
                desc = desc.substring(1);
            }
            String s = space;
            switch (Type.getType(desc).getSort()) {
                case 1: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewBooleanArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 2: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewCharArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 3: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewByteArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 4: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewShortArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 5: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewIntArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 6: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewFloatArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 7: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewLongArray(env, dim[" + next + "]);\n";
                    break;
                }
                case 8: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewDoubleArray(env, dim[" + next + "]);\n";
                    break;
                }
                default: {
                    code = code + s + "            cstack" + next + ".l = (*env)->NewObjectArray(env, dim[" + next + "], c_" + context.getCachedClasses().getId(desc) + "_(env)->clazz, NULL);\n";
                }
            }
            code = code + space + "            (*env)->SetObjectArrayElement(env, cstack" + index + ".l, d" + index + ", cstack" + next + ".l);\n";
            code = code + this.getSub(context, node, next, max);
            code = code + space + "}\n";
        }
        return code;
    }
}
