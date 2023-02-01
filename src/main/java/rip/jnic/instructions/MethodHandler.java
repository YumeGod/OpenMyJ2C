// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import rip.jnic.cache.CachedClassInfo;
import rip.jnic.cache.CachedFieldInfo;
import rip.jnic.cache.CachedMethodInfo;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MethodHandler
        extends GenericInstructionHandler<MethodInsnNode> {
    private static Type simplifyType(Type type) {
        switch (type.getSort()) {
            case 9:
            case 10: {
                return Type.getObjectType("java/lang/Object");
            }
            case 11: {
                throw new RuntimeException();
            }
        }
        return type;
    }

    private static String simplifyDesc(String desc) {
        return Type.getMethodType(MethodHandler.simplifyType(Type.getReturnType(desc)), (Type[])Arrays.stream(Type.getArgumentTypes(desc)).map(MethodHandler::simplifyType).toArray(Type[]::new)).getDescriptor();
    }

    @Override
    protected void process(MethodContext context, MethodInsnNode node) {
        if (node.owner.equals("java/lang/invoke/MethodHandle") && (node.name.equals("invokeExact") || node.name.equals("invoke")) && node.getOpcode() == 182) {
            String methodDesc = MethodHandler.simplifyDesc(Type.getMethodType(Type.getReturnType(node.desc), (Type[])Stream.concat(Arrays.stream(new Type[]{Type.getObjectType("java/lang/invoke/MethodHandle")}), Arrays.stream(Type.getArgumentTypes(node.desc))).toArray(Type[]::new)).getDescriptor());
            Type[] methodArguments = Type.getArgumentTypes(methodDesc);
            methodArguments[0] = Type.getObjectType("java/lang/invoke/MethodHandle");
            methodDesc = Type.getMethodDescriptor(Type.getReturnType(methodDesc), methodArguments);
            String mhDesc = MethodHandler.simplifyDesc(node.desc);
            context.output.append("temp0.l = (*env)->NewObjectArray(env, " + Type.getArgumentTypes(mhDesc).length + ", c_" + context.getCachedClasses().getClass("java/lang/Object").getId() + "_(env)->clazz, NULL);\n");
            block19: for (int i = 0; i < Type.getArgumentTypes(mhDesc).length; ++i) {
                Type argumentType = Type.getArgumentTypes(mhDesc)[i];
                switch (argumentType.getSort()) {
                    case 1:
                    case 2:
                    case 3:
                    case 5: {
                        CachedClassInfo integer = context.getCachedClasses().getClass("java/lang/Integer");
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", (*env)->CallStaticObjectMethod(env, c_" + integer.getId() + "_(env)->clazz, c_" + integer.getId() + "_(env)->method_" + integer.getCachedMethodId(new CachedMethodInfo("java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", true)) + ", cstack" + (1 + i) + ".l));\n");
                        continue block19;
                    }
                    case 6: {
                        CachedClassInfo jfloat = context.getCachedClasses().getClass("java/lang/Float");
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", (*env)->CallStaticObjectMethod(env, c_" + jfloat.getId() + "_(env)->clazz, c_" + jfloat.getId() + "_(env)->method_" + jfloat.getCachedMethodId(new CachedMethodInfo("java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", true)) + ", cstack" + (1 + i) + ".l));\n");
                        continue block19;
                    }
                    case 7: {
                        CachedClassInfo jlong = context.getCachedClasses().getClass("java/lang/Long");
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", (*env)->CallStaticObjectMethod(env, c_" + jlong.getId() + "_(env)->clazz, c_" + jlong.getId() + "_(env)->method_" + jlong.getCachedMethodId(new CachedMethodInfo("java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", true)) + ", cstack" + (1 + i) + ".l));\n");
                        continue block19;
                    }
                    case 8: {
                        CachedClassInfo jdouble = context.getCachedClasses().getClass("java/lang/Double");
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", (*env)->CallStaticObjectMethod(env, c_" + jdouble.getId() + "_(env)->clazz, c_" + jdouble.getId() + "_(env)->method_" + jdouble.getCachedMethodId(new CachedMethodInfo("java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", true)) + ", cstack" + (1 + i) + ".l));\n");
                        continue block19;
                    }
                    case 9:
                    case 10: {
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", cstack" + (1 + i) + ".l);\n");
                        continue block19;
                    }
                    case 11: {
                        CachedClassInfo clazz = context.getCachedClasses().getClass(context.clazz.name);
                        CachedClassInfo javaClass = context.getCachedClasses().getClass("java/lang/Class");
                        CachedClassInfo methodType = context.getCachedClasses().getClass("java/lang/invoke/MethodType");
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", (*env)->CallStaticObjectMethod(env, /*java/lang/invoke/MethodType*/c_" + methodType.getId() + "_(env)->clazz, /*fromMethodDescriptorString*/c_" + methodType.getId() + "_(env)->method_" + methodType.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodType", "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", true)) + ", /*" + argumentType + "*/(*env)->NewString(env, (unsigned short[]) {" + Util.utf82unicode(argumentType.toString()) + "}, " + argumentType.toString().length() + "), (*env)->CallObjectMethod(env,/*" + context.clazz.name + "*/c_" + clazz.getId() + "_(env)->clazz, /*java/lang/Class.getClassLoader*/c_" + javaClass.getId() + "_(env)->method_" + javaClass.getCachedMethodId(new CachedMethodInfo("java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false)) + ")));\n");
                        continue block19;
                    }
                    default: {
                        context.output.append("(*env)->SetObjectArrayElement(env, temp0.l, " + i + ", cstack" + (1 + i) + ".l);\n");
                    }
                }
            }
            CachedClassInfo methodHandle = context.getCachedClasses().getClass("java/lang/invoke/MethodHandle");
            if (Type.getReturnType(mhDesc).getSize() == 0) {
                context.output.append("(*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l);\n");
            } else {
                Type returnType = Type.getReturnType(mhDesc);
                switch (returnType.getSort()) {
                    case 1: {
                        CachedClassInfo jbool = context.getCachedClasses().getClass("java/lang/Boolean");
                        context.output.append("cstack0.z = (*env)->CallBooleanMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jbool.getId() + "_(env)->method_" + jbool.getCachedMethodId(new CachedMethodInfo("java/lang/Boolean", "booleanValue", "()Z", false)) + ");\n");
                        break;
                    }
                    case 2: {
                        CachedClassInfo jchar = context.getCachedClasses().getClass("java/lang/Character");
                        context.output.append("cstack0.c = (*env)->CallCharMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jchar.getId() + "_(env)->method_" + jchar.getCachedMethodId(new CachedMethodInfo("java/lang/Character", "charValue", "()C", false)) + ");\n");
                        break;
                    }
                    case 3: {
                        CachedClassInfo jbyte = context.getCachedClasses().getClass("java/lang/Byte");
                        context.output.append("cstack0.b = (*env)->CallByteMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jbyte.getId() + "_(env)->method_" + jbyte.getCachedMethodId(new CachedMethodInfo("java/lang/Byte", "byteValue", "()B", false)) + ");\n");
                        break;
                    }
                    case 5: {
                        CachedClassInfo jint = context.getCachedClasses().getClass("java/lang/Integer");
                        context.output.append("cstack0.i = (*env)->CallIntMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jint.getId() + "_(env)->method_" + jint.getCachedMethodId(new CachedMethodInfo("java/lang/Integer", "intValue", "()I", false)) + ");\n");
                        break;
                    }
                    case 6: {
                        CachedClassInfo jfloat = context.getCachedClasses().getClass("java/lang/Float");
                        context.output.append("cstack0.f = (*env)->CallFloatMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jfloat.getId() + "_(env)->method_" + jfloat.getCachedMethodId(new CachedMethodInfo("java/lang/Float", "floatValue", "()F", false)) + ");\n");
                        break;
                    }
                    case 7: {
                        CachedClassInfo jlong = context.getCachedClasses().getClass("java/lang/Long");
                        context.output.append("cstack0.j = (*env)->CallLongMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jlong.getId() + "_(env)->method_" + jlong.getCachedMethodId(new CachedMethodInfo("java/lang/Long", "longValue", "()J", false)) + ");\n");
                        break;
                    }
                    case 8: {
                        CachedClassInfo jdouble = context.getCachedClasses().getClass("java/lang/Double");
                        context.output.append("cstack0.d = (*env)->CallDoubleMethod(env, (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l),c_" + jdouble.getId() + "_(env)->method_" + jdouble.getCachedMethodId(new CachedMethodInfo("java/lang/Double", "doubleValue", "()D", false)) + ");\n");
                        break;
                    }
                    case 9:
                    case 10: {
                        context.output.append("cstack0.l = (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l);\n");
                        break;
                    }
                    case 11: {
                        break;
                    }
                    default: {
                        context.output.append("cstack0.l = (*env)->CallObjectMethod(env, cstack0.l, c_" + methodHandle.getId() + "_(env)->method_" + methodHandle.getCachedMethodId(new CachedMethodInfo("java/lang/invoke/MethodHandle", "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;", false)) + ", temp0.l);\n");
                    }
                }
            }
            context.output.append((String)this.props.get("trycatchhandler"));
            return;
        }
        this.props.put("class_ptr", "c_" + context.getCachedClasses().getId(node.owner) + "_");
        CachedClassInfo classInfo = context.getCachedClasses().getClass(node.owner);
        List<CachedFieldInfo> cachedFields = classInfo.getCachedFields();
        for (int i = 0; i < cachedFields.size(); ++i) {
            CachedFieldInfo fieldNode = cachedFields.get(i);
            if (!fieldNode.getName().equals(node.name)) continue;
            this.props.put("field_id", "id_" + i);
        }
        Type returnType = Type.getReturnType(node.desc);
        Type[] args = Type.getArgumentTypes(node.desc);
        this.instructionName = this.instructionName + "_" + returnType.getSort();
        StringBuilder argsBuilder = new StringBuilder();
        ArrayList<Integer> argOffsets = new ArrayList<Integer>();
        int stackOffset = context.stackPointer;
        for (Type argType : args) {
            stackOffset -= argType.getSize();
        }
        int argumentOffset = stackOffset;
        for (Type argType : args) {
            argOffsets.add(argumentOffset);
            argumentOffset += argType.getSize();
        }
        boolean isStatic = node.getOpcode() == 184;
        int objectOffset = isStatic ? 0 : 1;
        for (int i = 0; i < argOffsets.size(); ++i) {
            argsBuilder.append(", ").append(context.getSnippets().getSnippet("INVOKE_ARG_" + args[i].getSort(), Util.createMap("index", argOffsets.get(i))));
        }
        this.props.put("objectstackindex", String.valueOf(stackOffset - objectOffset));
        this.props.put("returnstackindex", String.valueOf(stackOffset - objectOffset));
        List<CachedMethodInfo> cachedMethods = classInfo.getCachedMethods();
        for (int i = 0; i < cachedMethods.size(); ++i) {
            CachedMethodInfo cachedMethodInfo = cachedMethods.get(i);
            if (!cachedMethodInfo.getName().equals(node.name) || !cachedMethodInfo.getDesc().equals(node.desc)) continue;
            this.props.put("methodid", "method_" + i);
        }
        if (this.props.get("methodid") == null) {
            CachedMethodInfo methodInfo = new CachedMethodInfo(node.owner, node.name, node.desc, isStatic);
            methodInfo.setId(cachedMethods.size());
            cachedMethods.add(methodInfo);
            this.props.put("methodid", "method_" + (cachedMethods.size() - 1));
        }
        this.props.put("args", argsBuilder.toString());
    }

    @Override
    public String insnToString(MethodContext context, MethodInsnNode node) {
        return String.format("%s %s.%s%s", Util.getOpcodeString(node.getOpcode()), node.owner, node.name, node.desc);
    }

    @Override
    public int getNewStackPointer(MethodInsnNode node, int currentStackPointer) {
        if (node.getOpcode() != 184) {
            --currentStackPointer;
        }
        return currentStackPointer - Arrays.stream(Type.getArgumentTypes(node.desc)).mapToInt(Type::getSize).sum() + Type.getReturnType(node.desc).getSize();
    }
}
