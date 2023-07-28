package edu.ifmg;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import edu.ifmg.StaticAnalyzer.Manifest;
import edu.ifmg.Utils.Cli;
import edu.ifmg.Utils.Files.FileHandler;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Main {
    private static Logger logger = LoggerFactory.getLogger(Logger.class);

    public static void main(String[] args) {
        Cli cli = new Cli();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cli.parse(args);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            formatter.printHelp("SFEDroid", cli.getOptions());
            System.exit(1);
        }
        logger.info(String.format("Source APK path: %s", cli.getSourceFilePath()));
        logger.info(String.format("Android Jars path: %s", cli.getAndroidJarPath()));

        // Processing AndroidManifest.xml
        Manifest manifestHandler = new Manifest(cli.getSourceFilePath());
        try {
            manifestHandler.process();
        } catch (IOException | XmlPullParserException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        
        cli.concatOutputFilePath(manifestHandler.getPackageName());
        logger.info(String.format("Output path: %s", cli.getOutputFilePath()));
        Path parentDir = Path.of(cli.getOutputFilePath());
        if (!Files.exists(parentDir)) {
            logger.info("Creating new folder for the app being analyzed...");
            parentDir.toFile().mkdir();
        }
        
        // Configuring flowdroid options to generate Call Graphs
        final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(cli.getSourceFilePath());
        config.getAnalysisFileConfig().setAndroidPlatformDir(cli.getAndroidJarPath());
        config.getAnalysisFileConfig().setOutputFile(cli.getOutputFilePath());
        config.setCallgraphAlgorithm(cli.getCgAlgorithm());
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);

        // Setting up application and constructing callgraph
        SetupApplication app = new SetupApplication(config);
        app.constructCallgraph();
        CallGraph callGraph = Scene.v().getCallGraph();

        // Listing valid classes
        List<SootClass> validClasses = new ArrayList<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (!sootClass.getName().contains(manifestHandler.getPackageName()))
                continue;
            if (sootClass.getName().contains(manifestHandler.getPackageName() + ".R") || sootClass.getName().contains(manifestHandler.getPackageName() + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }

        // Printing callgraph's general inforamation into dot file
        logger.info("Printing call graph's general information into dot file...");
        
        int index = 0;
        Edge edge = null;
        List<String> methodsSigs = null;
        List<SootMethod> methods = null;
        String sourceSignature = null;
        String targetSignature = null;
        String aux = "";
        List<String> edgesStrings = new ArrayList<>();

        StringBuilder fileData = new StringBuilder();
        FileHandler exportedFile = new FileHandler();
        
        for (SootClass sootClass : validClasses) {

            exportedFile.setFilePath(cli.getOutputFilePath(), String.format("%s.dot", sootClass.getName()));

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
}