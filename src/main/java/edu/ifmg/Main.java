package edu.ifmg;

import edu.ifmg.FileExport.FileExport;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Options;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    private static Logger logger = LoggerFactory.getLogger(Logger.class);

    public static void main(String[] args) {
        Options options = new Options();
        Option sourceFile = new Option("i", "source-file", true, "source apk file");
        sourceFile.setRequired(true);
        options.addOption(sourceFile);
        Option androidJarsPath = new Option("j", "android-jars", true, "path to android jars");
        androidJarsPath.setRequired(true);
        options.addOption(androidJarsPath);
//        TODO: callgraph algorithm CLI option
//        Option callGraphAlg = new Option("ca", "callgraph-alg", true, "call graph algorithm: SPARK, CHA, etc");
//        options.addOption(callGraphAlg);
        Option outputFile = new Option("o", "output-file", true, "output file");
        options.addOption(outputFile);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            formatter.printHelp("SFEDroid", options);
            System.exit(1);
        }

        String sourceFilePath = cmd.getOptionValue("source-file");
        String outputFilePath = cmd.getOptionValue("output-file");
        if (!outputFilePath.endsWith("/"))
            outputFilePath += "/";
        String androidJar = cmd.getOptionValue("android-jars");

        logger.info("Source file path: " + sourceFilePath);

        // Processing AndroidManifest.xml
        String packageName = "";
        try {
            ProcessManifest manifest = new ProcessManifest(sourceFilePath);
            packageName = manifest.getPackageName();
            logger.info("Package Name: " + packageName);
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        // Configuring flowdroid options to generate Call Graphs
        InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm = InfoflowConfiguration.CallgraphAlgorithm.SPARK;
        final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(sourceFilePath);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidJar);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);
        config.setCallgraphAlgorithm(cgAlgorithm);

        // Setting up application
        SetupApplication app = new SetupApplication(config);

        // Constructing callgraph
        app.constructCallgraph();
        CallGraph callGraph = Scene.v().getCallGraph();

        // Listing valid classes
        List<SootClass> validClasses = new ArrayList<>();
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            if (!sootClass.getName().contains(packageName))
                continue;
            if (sootClass.getName().contains(packageName + ".R") || sootClass.getName().contains(packageName + ".BuildConfig"))
                continue;
            validClasses.add(sootClass);
        }

        // Printing callgraph's general inforamation into dot file
        logger.info("Printing call graph's general information into dot file...");
        StringBuilder fileData = new StringBuilder();
        FileExport exportedFile = new FileExport("");
        int index = 0;
        Edge edge = null;
        List<String> methodsSigs = null;
        List<SootMethod> methods = null;
        String sourceSignature = null;
        String targetSignature = null;
        String aux = "";
        List<String> edgesStrings = new ArrayList<>();
        for (SootClass sootClass : validClasses) {
            exportedFile.setFileName(outputFilePath + sootClass.getName() + ".dot");
            fileData.append("digraph ").append(sootClass.getName().replaceAll("\\.","_")).append(" {\n");
            methods = sootClass.getMethods();
            methodsSigs = methods.stream().map(SootMethod::getSignature).toList();
            for (SootMethod sootMethod : methods) {
                // Writing graph's nodes
                fileData.append(index + " [label=\"" + sootMethod.getSignature() + "\"];\n");
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
            exportedFile.export(fileData.toString());
            fileData.delete(0, fileData.length());
        }
    }
}