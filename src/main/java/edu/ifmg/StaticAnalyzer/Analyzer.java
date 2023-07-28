package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

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
    
    public Analyzer(String sourceApk, String androidJars, String outputPath, InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm) {
        outputFolder = outputPath;
        
        // Configuring flowdroid options to generate Call Graphs
        config.getAnalysisFileConfig().setTargetAPKFile(sourceApk);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidJars);
        config.setCallgraphAlgorithm(cgAlgorithm);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);

        // Setting up application
        app = new SetupApplication(config);
    }

    public void exportCallgraph(String pkgName) {
        CallGraph callGraph = null;
        try {
            callGraph = Scene.v().getCallGraph();
        } catch (RuntimeException e) {
            logger.error("Call graph not found...");
            return;
        }

        // Printing callgraph's general inforamation into dot file
        int index = 0;
        Edge edge = null;
        List<String> methodsSigs = null;
        List<SootMethod> methods = null;
        String sourceSignature = null;
        String targetSignature = null;
        String aux = "";
        List<String> edgesStrings = new ArrayList<>();
        logger.info("Printing call graph's general information into dot file...");
        StringBuilder fileData = new StringBuilder();
        FileHandler exportedFile = new FileHandler();

        // Listing valid classes to generate dot file
        List<SootClass> validClasses = new ArrayList<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (!sootClass.getName().contains(pkgName))
                continue;
            if (sootClass.getName().contains(pkgName + ".R") || sootClass.getName().contains(pkgName + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }
        for (SootClass sootClass : validClasses) {
            exportedFile.setFilePath(outputFolder, String.format("%s.dot", sootClass.getName()));
            
            fileData.append("digraph ").append(sootClass.getName().replaceAll("\\.","_")).append(" {\n");
            methods = sootClass.getMethods();
            methodsSigs = methods.stream().map(SootMethod::getSignature).toList();
            for (SootMethod sootMethod : methods) {
                // Writing graph's nodes
                fileData.append(index).append(" [label=\"").append(sootMethod.getSignature()).append("\"];\n");
                // Writing graph's edges
                for (Iterator<Edge> it = callGraph.edgesInto(sootMethod); it.hasNext(); ) {
                    edge = it.next();
                    sourceSignature = edge.src().getSignature();
                    aux = methodsSigs.indexOf(sourceSignature) + " -> " + index + ";\n";
                    if (methodsSigs.contains(sourceSignature) && !edgesStrings.contains(aux)) {
                        fileData.append(aux);
                        edgesStrings.add(aux);
                    }
                }
                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext(); ) {
                    edge = it.next();
                    targetSignature = edge.tgt().getSignature();
                    aux = index + " -> " + methodsSigs.indexOf(targetSignature) + ";\n";
                    if (methodsSigs.contains(targetSignature) && !edgesStrings.contains(aux)) {
                        fileData.append(aux);
                        edgesStrings.add(aux);
                    }
                }
                index++;
            }
            index = 0;
            fileData.append("}");

            try {
                exportedFile.export(fileData.toString());
            } catch (IOException e) {
                logger.error("File name not set before exporting data...");
                e.printStackTrace();
            }

            fileData.delete(0, fileData.length());
        }
    }

    public void buildCallgraph() {
        app.constructCallgraph();
    }
}
