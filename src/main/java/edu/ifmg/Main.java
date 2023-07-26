package edu.ifmg;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import edu.ifmg.Utils.Cli;
import edu.ifmg.Utils.FileHandler;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Main {
    private static Logger logger = LoggerFactory.getLogger(Logger.class);

    public static void main(String[] args) {
        Cli cli = new Cli();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cli.parse(args, logger);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            formatter.printHelp("SFEDroid", cli.getOptions());
            System.exit(1);
        }
        logger.info("Source APK path: " + cli.getSourceFilePath());
        logger.info("Android Jars path: " + cli.getAndroidJarPath());

        // Processing AndroidManifest.xml
        String packageName = "";
        try {
            ProcessManifest manifest = new ProcessManifest(cli.getSourceFilePath());
            packageName = manifest.getPackageName();
            logger.info("Package Name: " + packageName);
            manifest.close();
        } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
        }

        // Configuring flowdroid options to generate Call Graphs
        final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(cli.getSourceFilePath());
        config.getAnalysisFileConfig().setAndroidPlatformDir(cli.getAndroidJarPath());
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);
        config.setCallgraphAlgorithm(cli.getCgAlgorithm());

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
        FileHandler exportedFile = new FileHandler();
        int index = 0;
        Edge edge = null;
        List<String> methodsSigs = null;
        List<SootMethod> methods = null;
        String sourceSignature = null;
        String targetSignature = null;
        String aux = "";
        File fileName = null;
        Path parentDir = null;
        List<String> edgesStrings = new ArrayList<>();

        Map<String, String> env = System.getenv();
        String homePath = "";
        if (System.getProperty("os.name").startsWith("Windows"))
            homePath = env.get("USERPROFILE");
        else
            homePath = env.get("HOME");
        
        for (SootClass sootClass : validClasses) {
            // Format parentDir and fileName
            if (cli.getOutputFilePath() == null) {
                logger.warn("Output folder not found. Setting current user's folder as the output folder...");

                parentDir = Path.of(homePath, packageName);
                fileName = new File(parentDir.toString(), sootClass.getName() + ".dot");
            } else {
                parentDir = Path.of(cli.getOutputFilePath(), packageName);
                fileName = new File(parentDir.toString(), sootClass.getName() + ".dot");
            }

            // Check and creation of output folder
            if (!Files.exists(parentDir))
                parentDir.toFile().mkdir();

            exportedFile.setFilePath(fileName.toPath());

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
            } catch (RuntimeException e) {
                logger.error("File name not set before exporting data...");
                e.printStackTrace();
            }

            fileData.delete(0, fileData.length());
        }
    }
}