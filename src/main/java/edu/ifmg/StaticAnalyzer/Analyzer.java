package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ifmg.Utils.Files.FileHandler;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class Analyzer {
    private static Logger logger = LoggerFactory.getLogger(Logger.class);
    private SetupApplication app = null;
    private final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
    private String outputFolder = new String();
    private CallGraph callGraph = null;
    private Map<SootMethod, SootMethod> callGraphEdges = new HashMap<>();
    private List<SootClass> validClasses = new ArrayList<>();
    private String pkgName = new String();
    private String mainActivityClassName = new String();
    private String mainActivityEntryPointSig = new String();
    private SootMethod mainActivityEntryPointMethod = null;
    
    public Analyzer(String sourceApk, String androidJars, String outputPath, InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm, String packageName) {
        outputFolder = outputPath;
        pkgName = packageName;
        
        // Configuring flowdroid options to generate Call Graphs
        config.getAnalysisFileConfig().setTargetAPKFile(sourceApk);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidJars);
        config.setCallgraphAlgorithm(cgAlgorithm);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);

        // Setting up application
        app = new SetupApplication(config);
    }

    public void exportCallgraph() {
        if (callGraph == null) {
            logger.error("Call graph not built yet...");
            return;
        }

        // Printing callgraph's general inforamation into dot file
        logger.info("Printing call graph's general information into dot file...");
        StringBuilder fileData = new StringBuilder();
        FileHandler exportedFile = new FileHandler();

        exportedFile.setFilePath(outputFolder, String.format("%s.dot", pkgName));
        fileData.append(String.format("digraph %s {\nnode [style=filled];", pkgName.replaceAll("\\.","_")));

        // Traverse callgraph edges' mapping and write output dot
        Map<SootMethod, Integer> keyIndex = new HashMap<>();
        int i = 0;
        String activityColor = " fontcolor=black, color=cornflowerblue fillcolor=cornflowerblue];\n";
        String methodName = new String();
        SootMethod child = null, parent = null;
        for (SootMethod key : callGraphEdges.keySet()) {
            child = key;
            parent = callGraphEdges.get(child);
            
            if (!keyIndex.keySet().contains(key)) {
                methodName = String.format("%s.%s", child.getDeclaringClass().getShortName(), child.getName());
                fileData.append(i).append(" [label=\"").append(methodName).append("\"];\n");
                keyIndex.put(key, i);
                i++;
            }
            
            if (!keyIndex.keySet().contains(parent)) {
                methodName = String.format("%s.%s", parent.getDeclaringClass().getShortName(), parent.getName());
                keyIndex.put(parent, i);
                
                if (!parent.getDeclaringClass().getShortName().equals("dummyMainClass"))
                    fileData.append(i).append(" [label=\"").append(methodName).append("\"];\n");
                else {
                    methodName = StringUtils.remove(parent.getReturnType().toString(), String.format("%s.", pkgName));
                    fileData.append(i).append(" [label=\"").append(methodName).append("\"").append(activityColor);
                }
                
                i++;
            }
        }

        int childInt = 0, parentInt = 0;
        for (SootMethod key : callGraphEdges.keySet()) {
            childInt = keyIndex.get(key);
            parentInt = keyIndex.get(callGraphEdges.get(key));
            fileData.append(String.format("%d -> %d ;\n", parentInt, childInt));
        }

        fileData.append("}");

        try {
            exportedFile.export(fileData.toString());
        } catch (IOException e) {
            logger.error("File name not set before exporting data...");
        }
    }

    private List<SootClass> getValidClasses() {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (!sootClass.getName().contains(pkgName))
                continue;
            if (sootClass.getName().contains(pkgName + ".R") || sootClass.getName().contains(pkgName + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }
        return validClasses;
    }

    public void buildCallgraph(String mainEntryPointClassName) {
        app.constructCallgraph();
        
        try {
            callGraph = Scene.v().getCallGraph();
        } catch (RuntimeException e) {
            logger.error("Call graph not built correctly...");
            return;
        }

        // Listing valid classes to generate edges' mapping
        validClasses = getValidClasses();

        for (SootClass sootClass : validClasses) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!isValidMethod(sootMethod))
                    continue;
                
                for (Iterator<Edge> it = callGraph.edgesInto(sootMethod); it.hasNext();) {
                    Edge edge = it.next();
                    if (!isValidEdge(edge))
                        continue;
                    SootMethod parentMethod = edge.src();
                    callGraphEdges.put(sootMethod, parentMethod);
                }

                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext();) {
                    Edge edge = it.next();
                    if (!isValidEdge(edge))
                        continue;
                    SootMethod childMethod = edge.tgt();
                    callGraphEdges.put(childMethod, sootMethod);
                }
            }
        }
        
        // Getting mainActivity info.
        mainActivityClassName = mainEntryPointClassName;
        for (SootMethod sootMethod : app.getDummyMainMethod().getDeclaringClass().getMethods()) {
            if (sootMethod.getReturnType().toString().equals(mainActivityClassName)) {
                mainActivityEntryPointSig = sootMethod.getSignature();
                mainActivityEntryPointMethod = sootMethod;
            }
        }

    }

    private boolean isValidMethod(SootMethod sootMethod) {
        if (sootMethod.getName().equals("<init>") || sootMethod.getName().equals("<clinit>"))
            return false;
        if (sootMethod.getDeclaringClass().getName().startsWith("android.") || sootMethod.getDeclaringClass().getName().startsWith("com.google.android") || sootMethod.getDeclaringClass().getName().startsWith("androidx."))
            return false;
        if (sootMethod.getDeclaringClass().getPackageName().startsWith("java"))
            return false;
        if (sootMethod.getName().equals("dummyMainMethod"))
            return false;
        return true;
    }

    private boolean isValidEdge(Edge edge) {
        if (!edge.src().getDeclaringClass().isApplicationClass())
            return false;
        if (!isValidMethod(edge.src()) || !isValidMethod(edge.tgt()))
            return false;
        return validClasses.contains(edge.src().getDeclaringClass()) || validClasses.contains(edge.tgt().getDeclaringClass());
    }

}
