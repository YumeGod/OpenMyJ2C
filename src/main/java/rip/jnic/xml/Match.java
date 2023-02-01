package rip.jnic.xml;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class Match
{
    @JacksonXmlProperty (localName = "className")
    private String className;
    @JacksonXmlProperty(localName = "methodName")
    private String methodName;
    @JacksonXmlProperty(localName = "methodDesc")
    private String methodDesc;
    
    public String getClassName() {
        return this.className;
    }
    
    public void setClassName(final String className) {
        this.className = className;
    }
    
    public String getMethodName() {
        return this.methodName;
    }
    
    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }
    
    public String getMethodDesc() {
        return this.methodDesc;
    }
    
    public void setMethodDesc(final String methodDesc) {
        this.methodDesc = methodDesc;
    }
}
