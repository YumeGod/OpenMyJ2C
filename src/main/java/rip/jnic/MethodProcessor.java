// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic;

import rip.jnic.instructions.FieldHandler;
import rip.jnic.instructions.FrameHandler;
import rip.jnic.instructions.IincHandler;
import rip.jnic.instructions.InsnHandler;
import rip.jnic.instructions.InstructionHandlerContainer;
import rip.jnic.instructions.InstructionTypeHandler;
import rip.jnic.instructions.IntHandler;
import rip.jnic.instructions.InvokeDynamicHandler;
import rip.jnic.instructions.JumpHandler;
import rip.jnic.instructions.LabelHandler;
import rip.jnic.instructions.LdcHandler;
import rip.jnic.instructions.LineNumberHandler;
import rip.jnic.instructions.LookupSwitchHandler;
import rip.jnic.instructions.MethodHandler;
import rip.jnic.instructions.MultiANewArrayHandler;
import rip.jnic.instructions.TableSwitchHandler;
import rip.jnic.instructions.TypeHandler;
import rip.jnic.instructions.VarHandler;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import rip.jnic.special.ClInitSpecialMethodProcessor;
import rip.jnic.special.DefaultSpecialMethodProcessor;
import rip.jnic.special.SpecialMethodProcessor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MethodProcessor {
    public static final Map<Integer, String> INSTRUCTIONS = new HashMap<Integer, String>();
    public static final String[] CPP_TYPES;
    public static final int[] TYPE_TO_STACK;
    public static final int[] STACK_TO_STACK;
    private final NativeObfuscator obfuscator;
    private final InstructionHandlerContainer<?>[] handlers;

    public MethodProcessor(NativeObfuscator obfuscator) {
        this.obfuscator = obfuscator;
        this.handlers = new InstructionHandlerContainer[16];
        this.addHandler(0, new InsnHandler(), InsnNode.class);
        this.addHandler(1, new IntHandler(), IntInsnNode.class);
        this.addHandler(2, new VarHandler(), VarInsnNode.class);
        this.addHandler(3, new TypeHandler(), TypeInsnNode.class);
        this.addHandler(4, new FieldHandler(), FieldInsnNode.class);
        this.addHandler(5, new MethodHandler(), MethodInsnNode.class);
        this.addHandler(6, new InvokeDynamicHandler(), InvokeDynamicInsnNode.class);
        this.addHandler(7, new JumpHandler(), JumpInsnNode.class);
        this.addHandler(8, new LabelHandler(), LabelNode.class);
        this.addHandler(9, new LdcHandler(), LdcInsnNode.class);
        this.addHandler(10, new IincHandler(), IincInsnNode.class);
        this.addHandler(11, new TableSwitchHandler(), TableSwitchInsnNode.class);
        this.addHandler(12, new LookupSwitchHandler(), LookupSwitchInsnNode.class);
        this.addHandler(13, new MultiANewArrayHandler(), MultiANewArrayInsnNode.class);
        this.addHandler(14, new FrameHandler(), FrameNode.class);
        this.addHandler(15, new LineNumberHandler(), LineNumberNode.class);
    }

    private <T extends AbstractInsnNode> void addHandler(int id, InstructionTypeHandler<T> handler, Class<T> instructionClass) {
        this.handlers[id] = new InstructionHandlerContainer<T>(handler, instructionClass);
    }

    private SpecialMethodProcessor getSpecialMethodProcessor(String name) {
        switch (name) {
            case "<init>": {
                return null;
            }
            case "<clinit>": {
                return new ClInitSpecialMethodProcessor();
            }
        }
        return new DefaultSpecialMethodProcessor();
    }

    public static boolean shouldProcess(MethodNode method) {
        return !Util.getFlag(method.access, 1024) && !Util.getFlag(method.access, 256) && !method.name.equals("<init>");
    }

    public void processMethod(MethodContext context) {
        MethodNode method = context.method;
        SpecialMethodProcessor specialMethodProcessor = this.getSpecialMethodProcessor(method.name);
        if ("<clinit>".equals(method.name) && method.instructions.size() == 0) {
            context.obfuscator.getNoInitClassMap().put(context.clazz.name, "1");
            specialMethodProcessor.postProcess(context);
            return;
        }
        StringBuilder output = context.output;
        if (specialMethodProcessor == null) {
            throw new RuntimeException(String.format("Could not find special method processor for %s", method.name));
        }
        output.append("/* " + context.clazz.name + ".").append(Util.escapeCommentString(method.name)).append(Util.escapeCommentString(method.desc)).append("*/");
        output.append("\n");
        specialMethodProcessor.preProcess(context);
        String methodName = Util.escapeCppNameString("myj2c_" + context.methodIndex);
        context.obfuscator.getClassMethodNameMap().put(context.clazz.name + "." + method.name + method.desc, methodName);
        boolean isStatic = Util.getFlag(method.access, 8);
        context.ret = Type.getReturnType(method.desc);
        Type[] args = Type.getArgumentTypes(method.desc);
        context.argTypes = new ArrayList<Type>(Arrays.asList(args));
        if (!isStatic) {
            context.argTypes.add(0, Type.getType(Object.class));
        }
        if (Util.getFlag(context.clazz.access, 512)) {
            String targetDesc = String.format("(%s)%s", context.argTypes.stream().map(Type::getDescriptor).collect(Collectors.joining()), context.ret.getDescriptor());
            String outerJavaMethodName = String.format("iface_static_%d_%d", context.classIndex, context.methodIndex);
            context.nativeMethod = new MethodNode(458752, 4361, outerJavaMethodName, targetDesc, null, new String[0]);
            String methodSource = String.format("            { (char *)%s, (char *)%s, (void *)&%s },\n", outerJavaMethodName, targetDesc, methodName);
            this.obfuscator.getStaticClassProvider().addMethod(context.nativeMethod, methodSource);
        } else {
            context.nativeMethods.append(String.format("            { (char *)%s, (char *)%s, (void *)&%s },\n", context.proxyMethod.name, method.desc, methodName));
        }
        output.append(String.format("%s JNICALL %s(JNIEnv *env, ", CPP_TYPES[context.ret.getSort()], methodName));
        output.append("jobject obj");
        ArrayList<String> argNames = new ArrayList<String>();
        if (!isStatic) {
            argNames.add("obj");
        }
        for (int i = 0; i < args.length; ++i) {
            argNames.add("arg" + i);
            output.append(String.format(", %s arg%d", CPP_TYPES[args[i].getSort()], i));
        }
        output.append(") {").append("\n");
        if (method.tryCatchBlocks != null) {
            Set<String> classesForTryCatches = method.tryCatchBlocks.stream().filter(tryCatchBlock -> tryCatchBlock.type != null).map(x -> x.type).collect(Collectors.toSet());
            classesForTryCatches.forEach(clazz -> {
                int classId = context.getCachedClasses().getId((String)clazz);
            });
            for (TryCatchBlockNode tryCatch : method.tryCatchBlocks) {
                context.getLabelPool().getName(tryCatch.start.getLabel());
                context.getLabelPool().getName(tryCatch.end.getLabel());
                context.getLabelPool().getName(tryCatch.handler.getLabel());
            }
        }
        if (method.maxStack > 0) {
            for (int i = 0; i < method.maxStack; ++i) {
                output.append(String.format("jvalue cstack%s; memset(&cstack%s, 0, sizeof(jvalue));\n", i, i));
            }
            output.append("\n");
        }
        if (method.maxLocals > 0) {
            for (int i = 0; i < method.maxLocals; ++i) {
                output.append(String.format("jvalue clocal%s; memset(&clocal%s, 0, sizeof(jvalue));\n", i, i));
            }
            output.append("\n");
        }
        if (method.maxStack > 0 || method.maxLocals > 0) {
            output.append("jvalue temp0; memset(&temp0, 0, sizeof(jvalue));\n");
            output.append("\n");
        }
        int localIndex = 0;
        for (int i = 0; i < context.argTypes.size(); ++i) {
            Type current = context.argTypes.get(i);
            output.append(this.obfuscator.getSnippets().getSnippet("LOCAL_LOAD_ARG_" + current.getSort(), Util.createMap("index", localIndex, "arg", argNames.get(i)))).append("\n");
            localIndex += current.getSize();
        }
        if (context.argTypes.size() > 0) {
            output.append("\n");
        }
        context.argTypes.forEach(t -> context.locals.add(TYPE_TO_STACK[t.getSort()]));
        context.stackPointer = 0;
        for (int instruction = 0; instruction < method.instructions.size(); ++instruction) {
            AbstractInsnNode node = method.instructions.get(instruction);
            this.handlers[node.getType()].accept(context, node);
            context.stackPointer = this.handlers[node.getType()].getNewStackPointer(node, context.stackPointer);
        }
        boolean hasAddedNewBlocks = true;
        HashSet<CatchesBlock> proceedBlocks = new HashSet<CatchesBlock>();
        while (hasAddedNewBlocks) {
            hasAddedNewBlocks = false;
            for (CatchesBlock catchBlock : new ArrayList<CatchesBlock>(context.catches.keySet())) {
                if (proceedBlocks.contains(catchBlock)) continue;
                proceedBlocks.add(catchBlock);
                output.append("    ").append(context.catches.get(catchBlock)).append(": ");
                CatchesBlock.CatchBlock currentCatchBlock = catchBlock.getCatches().get(0);
                if (currentCatchBlock.getClazz() == null) {
                    output.append(context.getSnippets().getSnippet("TRYCATCH_ANY_L", Util.createMap("handler_block", context.getLabelPool().getName(currentCatchBlock.getHandler().getLabel()))));
                    output.append("\n");
                    continue;
                }
                output.append(context.getSnippets().getSnippet("TRYCATCH_CHECK_STACK", Util.createMap("exception_class_ptr", context.getCachedClasses().getPointer(currentCatchBlock.getClazz()), "class_ptr", "c_" + context.getCachedClasses().getId(currentCatchBlock.getClazz()) + "_", "handler_block", context.getLabelPool().getName(currentCatchBlock.getHandler().getLabel()))));
                output.append("\n");
                if (catchBlock.getCatches().size() == 1) {
                    if ("void".equals(CPP_TYPES[context.ret.getSort()])) {
                        output.append(context.getSnippets().getSnippet("TRYCATCH_END_STACK_VOID", Util.createMap(new Object[0])));
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
                        output.append(context.getSnippets().getSnippet("TRYCATCH_END_STACK", Util.createMap("rettype", type)));
                    }
                    output.append("\n");
                    continue;
                }
                CatchesBlock nextCatchesBlock = new CatchesBlock(catchBlock.getCatches().stream().skip(1L).collect(Collectors.toList()));
                if (context.catches.get(nextCatchesBlock) == null) {
                    context.catches.put(nextCatchesBlock, String.format("L_CATCH_%d", context.catches.size()));
                    hasAddedNewBlocks = true;
                }
                output.append("    ");
                output.append(context.getSnippets().getSnippet("TRYCATCH_ANY_L", Util.createMap("handler_block", context.catches.get(nextCatchesBlock))));
                output.append("\n");
            }
        }
        switch (context.ret.getSort()) {
            case 0: {
                output.append("    return ;\n");
                break;
            }
            case 1: {
                output.append("    return temp0.z;\n");
                break;
            }
            case 2: {
                output.append("    return temp0.c;\n");
                break;
            }
            case 3: {
                output.append("    return temp0.b;\n");
                break;
            }
            case 4: {
                output.append("    return temp0.s;\n");
                break;
            }
            case 5: {
                output.append("    return temp0.i;\n");
                break;
            }
            case 6: {
                output.append("    return temp0.f;\n");
                break;
            }
            case 7: {
                output.append("    return temp0.j;\n");
                break;
            }
            case 8: {
                output.append("    return temp0.d;\n");
                break;
            }
            case 9: {
                output.append("    return (jarray)0;\n");
                break;
            }
            case 10:
            case 11: {
                output.append("    return temp0.l;\n");
                break;
            }
            default: {
                output.append("    return temp0.l;\n");
            }
        }
        output.append("}\n\n");
        method.localVariables.clear();
        method.tryCatchBlocks.clear();
        specialMethodProcessor.postProcess(context);
    }

    public static String nameFromNode(MethodNode m, ClassNode cn) {
        return cn.name + '#' + m.name + '!' + m.desc;
    }

    static {
        try {
            for (Field f : Opcodes.class.getFields()) {
                INSTRUCTIONS.put((int)((Integer)f.get(null)), f.getName());
            }
        }
        catch (IllegalAccessException | IllegalArgumentException ex) {
            throw new RuntimeException(ex);
        }
        CPP_TYPES = new String[]{"void", "jboolean", "jchar", "jbyte", "jshort", "jint", "jfloat", "jlong", "jdouble", "jarray", "jobject", "jobject"};
        TYPE_TO_STACK = new int[]{1, 1, 1, 1, 1, 1, 1, 2, 2, 0, 0, 0};
        STACK_TO_STACK = new int[]{1, 1, 1, 2, 2, 0, 0, 0, 0};
    }
}
