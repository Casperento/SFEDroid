package edu.ifmg.StaticAnalyzer;

import soot.jimple.infoflow.sourcesSinks.definitions.ISourceSinkDefinition;

import java.util.Collection;
import java.util.HashSet;

public class SourcesSinks {
    private static SourcesSinks instance;
    private static Collection<? extends ISourceSinkDefinition> sinksDefinitions;
    private final HashSet<String> sinkMethodsSigs = new HashSet<>();

    public static SourcesSinks getInstance() {
        if (SourcesSinks.instance == null) {
            instance = new SourcesSinks();
        }
        return instance;
    }

    public void setSinksDefinitions(Collection<? extends ISourceSinkDefinition> sinks) {
        if (sinksDefinitions == null) {
            sinksDefinitions = sinks;
            for (ISourceSinkDefinition ssd : sinksDefinitions)
                sinkMethodsSigs.add(ssd.getSinkOnlyDefinition().toString());
        }
    }

    public HashSet<String> getSinksMethodsSigs() {
        return sinkMethodsSigs;
    }
}
