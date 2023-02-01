// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic.instructions;

import rip.jnic.CatchesBlock;
import rip.jnic.MethodContext;
import rip.jnic.MethodProcessor;
import rip.jnic.Util;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public abstract class GenericInstructionHandler<T extends AbstractInsnNode>
        implements InstructionTypeHandler<T> {
    protected Map<String, String> props;
    protected String instructionName;
    protected String trimmedTryCatchBlock;

    @Override
    public void accept(MethodContext context, T node) {
        this.props = new HashMap<String, String>();
        ArrayList<TryCatchBlockNode> tryCatchBlockNodeList = new ArrayList<TryCatchBlockNode>();
        for (TryCatchBlockNode tryCatchBlock : context.method.tryCatchBlocks) {
            if (!context.tryCatches.contains(tryCatchBlock) || !tryCatchBlockNodeList.stream().noneMatch(tryCatchBlockNode -> Objects.equals(tryCatchBlockNode.type, tryCatchBlock.type))) continue;
            tryCatchBlockNodeList.add(tryCatchBlock);
        }
        this.instructionName = MethodProcessor.INSTRUCTIONS.getOrDefault(((AbstractInsnNode)node).getOpcode(), "NOTFOUND");
        this.props.put("line", String.valueOf(context.line));
        StringBuilder tryCatch = new StringBuilder("\n");
        if (tryCatchBlockNodeList.size() > 0) {
            String tryCatchLabelName = context.catches.computeIfAbsent(new CatchesBlock(tryCatchBlockNodeList.stream().map(item -> new CatchesBlock.CatchBlock(item.type, item.handler)).collect(Collectors.toList())), key -> String.format("L_CATCH_%d", context.catches.size()));
            tryCatch.append("if ((*env)->ExceptionCheck(env)) { \n");
            tryCatch.append("   cstack0.l = (*env)->ExceptionOccurred(env);\n");
            for (TryCatchBlockNode tryCatchBlockNode2 : tryCatchBlockNodeList) {
                if (tryCatchBlockNode2.type == null) continue;
                tryCatch.append("   if ((*env)->IsInstanceOf(env, cstack0.l, c_" + context.obfuscator.getCachedClasses().getId(tryCatchBlockNode2.type) + "_(env)->clazz)) {\n");
                tryCatch.append("       (*env)->ExceptionClear(env);\n");
                tryCatch.append("       goto ").append(tryCatchLabelName).append(";\n  }\n");
            }
            tryCatch.append("       (*env)->ExceptionClear(env);\n");
            tryCatch.append("       goto ").append(tryCatchLabelName).append(";\n");
            tryCatch.append("}\n");
        } else if ("void".equals(MethodProcessor.CPP_TYPES[context.ret.getSort()])) {
            tryCatch.append(context.getSnippets().getSnippet("TRYCATCH_VOID", Util.createMap(new Object[0])));
        } else {
            String type = "";
            switch (context.ret.getSort()) {
                case 9:
                case 10: {
                    type = "l";
                    break;
                }
                case 1: {
                    type = "z";
                    break;
                }
                case 3: {
                    type = "b";
                    break;
                }
                case 2: {
                    type = "c";
                    break;
                }
                case 8: {
                    type = "d";
                    break;
                }
                case 6: {
                    type = "f";
                    break;
                }
                case 5: {
                    type = "i";
                    break;
                }
                case 7: {
                    type = "j";
                    break;
                }
                case 4: {
                    type = "s";
                    break;
                }
                default: {
                    type = "l";
                }
            }
            tryCatch.append(context.getSnippets().getSnippet("TRYCATCH_EMPTY", Util.createMap("rettype", type)));
        }
        this.props.put("trycatchhandler", tryCatch.toString());
        this.props.put("rettype", MethodProcessor.CPP_TYPES[context.ret.getSort()]);
        switch (context.ret.getSort()) {
            case 0: {
                this.props.put("retvalue", " return ;");
                break;
            }
            case 1: {
                this.props.put("retvalue", " return temp0.z;");
                break;
            }
            case 2: {
                this.props.put("retvalue", " return temp0.c;");
                break;
            }
            case 3: {
                this.props.put("retvalue", " return temp0.b;");
                break;
            }
            case 4: {
                this.props.put("retvalue", " return temp0.s;");
                break;
            }
            case 5: {
                this.props.put("retvalue", " return temp0.i;");
                break;
            }
            case 6: {
                this.props.put("retvalue", " return temp0.f;");
                break;
            }
            case 7: {
                this.props.put("retvalue", " return temp0.j;");
                break;
            }
            case 8: {
                this.props.put("retvalue", " return temp0.d;");
                break;
            }
            case 9: {
                this.props.put("retvalue", " return (jarray)0;");
                break;
            }
            case 10:
            case 11: {
                this.props.put("retvalue", " return temp0.l;");
                break;
            }
            default: {
                this.props.put("retvalue", " return temp0.l;");
            }
        }
        this.trimmedTryCatchBlock = tryCatch.toString().trim().replace('\n', ' ');
        for (int i = -5; i <= 5; ++i) {
            this.props.put("stackindex" + (i >= 0 ? Integer.valueOf(i) : "m" + -i), String.valueOf(context.stackPointer + i));
        }
        this.process(context, node);
        if (this.instructionName != null) {
            if ("ATHROW".equals(this.instructionName)) {
                this.props.put("class_ptr", "c_" + context.getCachedClasses().getId("java/lang/NullPointerException") + "_");
            }
            if ("NEW".equals(this.instructionName) && tryCatchBlockNodeList.size() > 0) {
                this.instructionName = "NEW_CATCH";
            }
            context.output.append(context.obfuscator.getSnippets().getSnippet(this.instructionName, this.props));
        }
        context.output.append("\n");
    }

    protected abstract void process(MethodContext var1, T var2);
}
