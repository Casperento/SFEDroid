package edu.ifmg.StaticAnalyzer;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ifmg.Utils.Files.FileHandler;
import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.source.parsers.xml.ResourceUtils;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;

public class Analyzer {
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);
    private SetupApplication app;
    private final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
    private String outputFolder;
    private CallGraph callGraph = null;
    private Map<SootMethod, List<SootMethod>> callGraphEdges = new HashMap<>();
    private List<SootClass> validClasses = new ArrayList<>();
    private String pkgName;
    private String mainActivityClassName = new String();
    private String mainActivityEntryPointSig = new String();
    private SootMethod mainActivityEntryPointMethod = null;
    private ReachableMethods reachableMethods;
    
    public Analyzer(String sourceApk, String androidJars, String outputPath, InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm, String packageName, String additionalClasspath) {
        outputFolder = outputPath;
        pkgName = packageName;
        
        // Configuring flowdroid options to generate Call Graphs
        config.getAnalysisFileConfig().setTargetAPKFile(sourceApk);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidJars);
        config.getAnalysisFileConfig().setAdditionalClasspath(additionalClasspath);
        config.setLogSourcesAndSinks(true);
        config.setCallgraphAlgorithm(cgAlgorithm);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);

        // Setting up application
        app = new SetupApplication(config);

        // Copying SourcesAndSinks.txt file from soot-infoflow-android
        try {
            InputStream sourcesSinksFile = ResourceUtils.getResourceStream("/SourcesAndSinks.txt");
            byte[] bytes = sourcesSinksFile.readAllBytes();
            FileOutputStream file = new FileOutputStream("SourcesAndSinks.txt");
            file.write(bytes);
            file.close();
            sourcesSinksFile.close();
        } catch (IOException | RuntimeException e) {
            logger.error(e.getMessage());
        }
    }

    public void exportCallgraph() {
        if (callGraph == null) {
            logger.error("Cannot export null call graph...");
            return;
        }

        // Printing callgraph's general inforamation into dot file
        StringBuilder fileData = new StringBuilder();
        String dotName = String.format("%s.dot", pkgName);

        logger.info(String.format("Printing app's call graph into: %s...", outputFolder));
        fileData.append(String.format("digraph %s {\nnode [style=filled];\n", pkgName.replaceAll("\\.","_")));

        // Traverse callgraph edges' mapping and write output dot
        Map<SootMethod, Integer> keyIndex = new HashMap<>();
        int i = 0;
        String activityColor = " fontcolor=black, color=cornflowerblue fillcolor=cornflowerblue];\n";
        String methodName;
        List<SootMethod> parents = null;
        SootMethod child = null;
        for (SootMethod key : callGraphEdges.keySet()) {
            child = key;
            parents = callGraphEdges.get(child);
            
            if (!keyIndex.containsKey(key)) {
                methodName = String.format("%s.%s", child.getDeclaringClass().getShortName(), child.getName());
                fileData.append(i).append(" [label=\"").append(methodName).append("\"];\n");
                keyIndex.put(key, i);
                i++;
            }
            
            for (SootMethod parent : parents) {
                if (!keyIndex.containsKey(parent)) {
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
        }

        int childInt = 0, parentInt = 0;
        for (SootMethod key : callGraphEdges.keySet()) {
            childInt = keyIndex.get(key);
            parents = callGraphEdges.get(key);
            for (SootMethod parent : parents) {
                parentInt = keyIndex.get(parent);
                fileData.append(String.format("%d -> %d ;\n", parentInt, childInt));
            }
        }

        fileData.append("}");

        try {
            FileHandler.exportFile(String.valueOf(fileData), outputFolder, dotName);
        } catch (IOException e) {
            logger.error("File not found...");
        }
    }

    private List<SootClass> getValidClasses() {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (sootClass.getName().contains(pkgName + ".R") || sootClass.getName().contains(pkgName + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }
        return validClasses;
    }

    public void buildCallgraph(String mainEntryPointClassName) {
//        app.constructCallgraph();
        try {
            app.runInfoflow("SourcesAndSinks.txt");
            callGraph = Scene.v().getCallGraph();
        } catch (IOException | RuntimeException | XmlPullParserException e) {
            logger.error(e.getMessage());
            return;
        }

//        reachableMethods = Scene.v().getReachableMethods();
//        Boolean test = isMethodReachable("<package.ClassName: void methodName()>");
//        Set<Stmt> sinks = app.getCollectedSinks();

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
                    List<SootMethod> parents = new ArrayList<>();
                    if (callGraphEdges.containsKey(sootMethod))
                        parents = callGraphEdges.get(sootMethod);
                    
                    if (!parents.contains(parentMethod))
                        parents.add(parentMethod);
                    
                    callGraphEdges.put(sootMethod, parents);
                }

                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext();) {
                    Edge edge = it.next();
                    if (!isValidEdge(edge))
                        continue;
                    SootMethod childMethod = edge.tgt();
                    List<SootMethod> parents = new ArrayList<>();
                    if (callGraphEdges.containsKey(childMethod))
                        parents = callGraphEdges.get(childMethod);
                    
                    if (!parents.contains(sootMethod))
                        parents.add(sootMethod);
                    
                    callGraphEdges.put(childMethod, parents);
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

    public Boolean isMethodReachable(String signature) {
        if (reachableMethods == null || reachableMethods.size() < 1) {
            logger.error("reachableMethods variable empty or not initialized...");
            return false;
        }

        SootMethod method;
        try {
            method = Scene.v().getMethod(signature);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
            return false;
        }
        return reachableMethods.contains(method);
    }

    private boolean isValidMethod(SootMethod sootMethod) {
        if (sootMethod.getName().equals("<init>") || sootMethod.getName().equals("<clinit>"))
            return false;
        if (/*sootMethod.getDeclaringClass().getName().startsWith("android.") ||*/ sootMethod.getDeclaringClass().getName().startsWith("com.google.android") || sootMethod.getDeclaringClass().getName().startsWith("androidx."))
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
