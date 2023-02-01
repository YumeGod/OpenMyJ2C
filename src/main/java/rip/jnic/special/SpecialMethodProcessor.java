package rip.jnic.special;

import rip.jnic.MethodContext;

public interface SpecialMethodProcessor
{
    String preProcess(final MethodContext p0);
    
    void postProcess(final MethodContext p0);
}
