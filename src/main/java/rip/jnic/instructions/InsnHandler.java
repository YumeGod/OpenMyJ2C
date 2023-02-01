package rip.jnic.instructions;

import rip.jnic.MethodContext;
import rip.jnic.Util;
import org.objectweb.asm.tree.InsnNode;

public class InsnHandler extends GenericInstructionHandler<InsnNode>
{
    @Override
    protected void process(final MethodContext context, final InsnNode node) {
    }
    
    @Override
    public String insnToString(final MethodContext context, final InsnNode node) {
        return Util.getOpcodeString(node.getOpcode());
    }
    
    @Override
    public int getNewStackPointer(final InsnNode node, final int currentStackPointer) {
        switch (node.getOpcode()) {
            case 0:
            case 47:
            case 49:
            case 95:
            case 116:
            case 117:
            case 118:
            case 119:
            case 134:
            case 138:
            case 139:
            case 143:
            case 145:
            case 146:
            case 147:
            case 177:
            case 190: {
                return currentStackPointer;
            }
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 11:
            case 12:
            case 13:
            case 89:
            case 90:
            case 91:
            case 133:
            case 135:
            case 140:
            case 141: {
                return currentStackPointer + 1;
            }
            case 9:
            case 10:
            case 14:
            case 15:
            case 92:
            case 93:
            case 94: {
                return currentStackPointer + 2;
            }
            case 46:
            case 48:
            case 50:
            case 51:
            case 52:
            case 53:
            case 87:
            case 96:
            case 98:
            case 100:
            case 102:
            case 104:
            case 106:
            case 108:
            case 110:
            case 112:
            case 114:
            case 120:
            case 121:
            case 122:
            case 123:
            case 124:
            case 125:
            case 126:
            case 128:
            case 130:
            case 136:
            case 137:
            case 142:
            case 144:
            case 149:
            case 150:
            case 172:
            case 174:
            case 176:
            case 191:
            case 194:
            case 195: {
                return currentStackPointer - 1;
            }
            case 79:
            case 81:
            case 83:
            case 84:
            case 85:
            case 86:
            case 148:
            case 151:
            case 152: {
                return currentStackPointer - 3;
            }
            case 80:
            case 82: {
                return currentStackPointer - 4;
            }
            case 88:
            case 97:
            case 99:
            case 101:
            case 103:
            case 105:
            case 107:
            case 109:
            case 111:
            case 113:
            case 115:
            case 127:
            case 129:
            case 131:
            case 173:
            case 175: {
                return currentStackPointer - 2;
            }
            default: {
                throw new RuntimeException(String.valueOf(node.getOpcode()));
            }
        }
    }
}
