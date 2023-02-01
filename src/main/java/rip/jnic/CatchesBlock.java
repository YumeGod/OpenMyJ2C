package rip.jnic;

import org.objectweb.asm.tree.LabelNode;

import java.util.List;
import java.util.Objects;

public class CatchesBlock
{
    private final List<CatchBlock> catches;
    
    public CatchesBlock(final List<CatchBlock> catches) {
        this.catches = catches;
    }
    
    public List<CatchBlock> getCatches() {
        return this.catches;
    }
    
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        final CatchesBlock that = (CatchesBlock)o;
        return Objects.equals(this.catches, that.catches);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(this.catches);
    }
    
    public static class CatchBlock
    {
        private final String clazz;
        private final LabelNode handler;
        
        public CatchBlock(final String clazz, final LabelNode handler) {
            this.clazz = clazz;
            this.handler = handler;
        }
        
        public String getClazz() {
            return this.clazz;
        }
        
        public LabelNode getHandler() {
            return this.handler;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            final CatchBlock that = (CatchBlock)o;
            return Objects.equals(this.clazz, that.clazz) && Objects.equals(this.handler, that.handler);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(this.clazz, this.handler);
        }
    }
}
