package edu.ifmg.StaticAnalyzer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import edu.ifmg.Utils.Cli;
import edu.ifmg.Utils.FileHandler;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootResolver;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.resources.ARSCFileParser;
import soot.jimple.infoflow.config.IInfoflowConfig;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.jimple.toolkits.callgraph.ReachableMethods;
import soot.options.Options;

/**
 * Analyzer is the main class used to parse information from FlowDroid's CallGraph and Taint Analysis results. It works
 * as a Facade to SetupApplication and Scene classes.
 *
 * @author Casperento
 *
 */
public class Analyzer {
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

    private SetupApplication app;
    private CallGraph callGraph = null;
    private Map<SootMethod, List<SootMethod>> filteredCallGraph = new HashMap<>();
    private List<SootClass> validClasses = new ArrayList<>();
    private Parameters params;
    private ReachableMethods reachableMethods;
    private boolean hasError = false;
    private Set<Stmt> collectedSinks;

    public Analyzer(Parameters p) {
        params = p;

        // Configuring flowdroid options to generate Call Graphs
        InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(params.getSourceFilePath());
        config.getAnalysisFileConfig().setAdditionalClasspath(params.getAdditionalClassPath());
        config.setCallgraphAlgorithm(params.getCgAlgorithm());
        config.setEnableReflection(true);

        config.getAnalysisFileConfig().setAndroidPlatformDir(params.getAndroidJarPath());

        // Taint Analysis related settings
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
//        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.PropagateConstants);
//        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.RemoveSideEffectFreeCode);
        config.setLogSourcesAndSinks(true);
//        config.setEnableExceptionTracking(false); // exclude try-catch from the analysis

        // Timeout settings
        if (params.getTimeOut() != null) {
            int timeout = params.getTimeOut();
            config.setDataFlowTimeout(timeout);
            config.getCallbackConfig().setCallbackAnalysisTimeout(timeout);
            config.getPathConfiguration().setPathReconstructionTimeout(timeout);
        }

        // Setting up application
        app = new SetupApplication(config);
    }

    public void analyze() {
        try {
            app.runInfoflow("SourcesAndSinks.txt");
            callGraph = Scene.v().getCallGraph();
        } catch (IOException | RuntimeException | XmlPullParserException e) {
            logger.error(e.getMessage());
            hasError = true;
            return;
        }

        reachableMethods = Scene.v().getReachableMethods();
        collectedSinks = app.getCollectedSinks();

        // Listing valid classes to generate edges' mapping
        filterCallGraph();
    }

    private void filterCallGraph() {
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
                    if (filteredCallGraph.containsKey(sootMethod))
                        parents = filteredCallGraph.get(sootMethod);

                    if (!parents.contains(parentMethod))
                        parents.add(parentMethod);

                    filteredCallGraph.put(sootMethod, parents);
                }

                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext();) {
                    Edge edge = it.next();
                    if (!isValidEdge(edge))
                        continue;
                    SootMethod childMethod = edge.tgt();
                    List<SootMethod> parents = new ArrayList<>();
                    if (filteredCallGraph.containsKey(childMethod))
                        parents = filteredCallGraph.get(childMethod);

                    if (!parents.contains(sootMethod))
                        parents.add(sootMethod);

                    filteredCallGraph.put(childMethod, parents);
                }
            }
        }
    }

    public boolean hasError() {
        return hasError;
    }

    public List<String> listReachableMethods(PermissionsMapper mapper) {
        List<String> reachable = new ArrayList<>();
        for (String perm : params.getPermissions()) {
            List<String> methods = mapper.getPermissionMethods().get(perm);
            if (methods != null) {
                for (String sig : methods) {
                    if (isMethodReachable(sig)) {
                        logger.info("Method reachable: " + sig);
                        reachable.add(sig);
                    }
                }
            }
        }
        return reachable;
    }

    private Boolean isMethodReachable(String signature) {
        if (reachableMethods == null || reachableMethods.size() < 1) {
            logger.error("reachableMethods variable empty or not initialized...");
            return false;
        }

        SootMethod method;
        try {
            method = Scene.v().getMethod(signature);
        } catch (RuntimeException e) {
            return false;
        }
        return reachableMethods.contains(method);
    }

    private List<SootClass> getValidClasses() {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (sootClass.getName().contains(params.getPkgName() + ".R") || sootClass.getName().contains(params.getPkgName() + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }
        return validClasses;
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

    public void exportCallgraph() {
        if (callGraph == null) {
            logger.error("Cannot export null call graph...");
            return;
        }

        // Printing callgraph's general inforamation into dot file
        StringBuilder fileData = new StringBuilder();

        logger.info(String.format("Printing app's call graph into: %s...", params.getOutputFolderPath()));
        fileData.append(String.format("digraph %s {\nnode [style=filled];\n", params.getPkgName().replaceAll("\\.","_")));

        // Traverse callgraph edges' mapping and write output dot
        Map<SootMethod, Integer> keyIndex = new HashMap<>();
        int i = 0;
        String activityColor = " fontcolor=black, color=cornflowerblue fillcolor=cornflowerblue];\n";
        String methodName;
        List<SootMethod> parents = null;
        SootMethod child = null;
        for (SootMethod key : filteredCallGraph.keySet()) {
            child = key;
            parents = filteredCallGraph.get(child);

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
                        methodName = StringUtils.remove(parent.getReturnType().toString(), String.format("%s.", params.getPkgName()));
                        fileData.append(i).append(" [label=\"").append(methodName).append("\"").append(activityColor);
                    }

                    i++;
                }
            }
        }

        int childInt = 0, parentInt = 0;
        for (SootMethod key : filteredCallGraph.keySet()) {
            childInt = keyIndex.get(key);
            parents = filteredCallGraph.get(key);
            for (SootMethod parent : parents) {
                parentInt = keyIndex.get(parent);
                fileData.append(String.format("%d -> %d ;\n", parentInt, childInt));
            }
        }

        fileData.append("}");

        try {
            String dotName = String.format("%s.dot", Path.of(params.getSourceFilePath()).getFileName());
            FileHandler.exportFile(String.valueOf(fileData), params.getOutputFolderPath().toString(),  dotName);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

}
