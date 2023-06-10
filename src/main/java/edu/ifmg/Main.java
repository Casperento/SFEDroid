package edu.ifmg;

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

        // Printing callgraph's general inforamation
        logger.info("Printing call graph's general information...");
        int classIndex = 0;
        for (SootClass sootClass : validClasses) {
            System.out.printf("Class %d: %s%n", ++classIndex, sootClass.getName());
            for (SootMethod sootMethod : sootClass.getMethods()) {
                int incomingEdge = 0;
                for (Iterator<Edge> it = callGraph.edgesInto(sootMethod); it.hasNext(); incomingEdge++, it.next());
                int outgoingEdge = 0;
                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext(); outgoingEdge++, it.next());
                System.out.printf("\tMethod %s, #IncomeEdges: %d, #OutgoingEdges: %d%n", sootMethod.getName(), incomingEdge, outgoingEdge);
            }
        }
    }
}