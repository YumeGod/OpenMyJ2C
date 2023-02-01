// Decompiled with: CFR 0.152
// Class Version: 8
package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.MethodProcessor;
import rip.jnic.Util;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.LabelNode;

import java.util.Arrays;
import java.util.function.Consumer;

public class FrameHandler
        implements InstructionTypeHandler<FrameNode> {
    @Override
    public void accept(MethodContext context, FrameNode node) {
        Consumer<Object> appendLocal = local -> {
            if (local instanceof String) {
                context.locals.add(MethodProcessor.TYPE_TO_STACK[10]);
            } else if (local instanceof LabelNode) {
                context.locals.add(MethodProcessor.TYPE_TO_STACK[10]);
            } else {
                context.locals.add(MethodProcessor.STACK_TO_STACK[(Integer)local]);
            }
        };
        Consumer<Object> appendStack = stack -> {
            if (stack instanceof String) {
                context.stack.add(MethodProcessor.TYPE_TO_STACK[10]);
            } else if (stack instanceof LabelNode) {
                context.stack.add(MethodProcessor.TYPE_TO_STACK[10]);
            } else {
                context.stack.add(MethodProcessor.STACK_TO_STACK[(Integer)stack]);
            }
        };
        switch (node.type) {
            case 1: {
                node.local.forEach(appendLocal);
                context.stack.clear();
                break;
            }
            case 2: {
                node.local.forEach(item -> context.locals.remove(context.locals.size() - 1));
                context.stack.clear();
                break;
            }
            case -1:
            case 0: {
                context.locals.clear();
                context.stack.clear();
                node.local.forEach(appendLocal);
                node.stack.forEach(appendStack);
                break;
            }
            case 3: {
                context.stack.clear();
                break;
            }
            case 4: {
                context.stack.clear();
                appendStack.accept(node.stack.get(0));
            }
        }
    }

    @Override
    public String insnToString(MethodContext context, FrameNode node) {
        return String.format("FRAME %s L: %s S: %s", Util.getOpcodesString(node.type, "F_"), node.local == null ? "null" : Arrays.toString(node.local.toArray(new Object[0])), node.stack == null ? "null" : Arrays.toString(node.stack.toArray(new Object[0])));
    }

    @Override
    public int getNewStackPointer(FrameNode node, int currentStackPointer) {
        switch (node.type) {
            case 1:
            case 2:
            case 3: {
                return 0;
            }
            case -1:
            case 0: {
                return node.stack.stream().mapToInt(argument -> Math.max(1, argument instanceof Integer ? MethodProcessor.STACK_TO_STACK[(Integer)argument] : MethodProcessor.TYPE_TO_STACK[10])).sum();
            }
            case 4: {
                return node.stack.stream().limit(1L).mapToInt(argument -> Math.max(1, argument instanceof Integer ? MethodProcessor.STACK_TO_STACK[(Integer)argument] : MethodProcessor.TYPE_TO_STACK[10])).sum();
            }
        }
        throw new RuntimeException();
    }
}
