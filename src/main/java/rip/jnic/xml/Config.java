package rip.jnic.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@JacksonXmlRootElement (localName = "myj2c")
public class Config
{
    @JacksonXmlProperty (localName = "target")
    @JacksonXmlElementWrapper (localName = "targets")
    private List<String> targets;
    @JacksonXmlElementWrapper(localName = "include")
    private List<Match> includes;
    @JacksonXmlElementWrapper(localName = "exclude")
    private List<Match> excludes;
    
    public List<String> getTargets() {
        return this.targets;
    }
    
    public void setTargets(final List<String> targets) {
        this.targets = targets;
    }
    
    public List<Match> getIncludes() {
        return this.includes;
    }
    
    public void setIncludes(final List<Match> includes) {
        this.includes = includes;
    }
    
    public List<Match> getExcludes() {
        return this.excludes;
    }
    
    public void setExcludes(final List<Match> excludes) {
        this.excludes = excludes;
    }
}
