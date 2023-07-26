package edu.ifmg.Utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;

public class Cli {
    private final Options options = new Options();
    private final CommandLineParser parser = new DefaultParser();
    private CommandLine cmd = null;
    private String sourceFilePath = "";
    private String outputFilePath = "";
    private String androidJarPath = "";
    private InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm = CallgraphAlgorithm.SPARK;

    public Cli() {
        Option sourceFile = new Option("i", "source-file", true, "source apk file");
        sourceFile.setRequired(true);
        options.addOption(sourceFile);
        
        Option androidJarsPath = new Option("j", "android-jars", true, "path to android jars");
        androidJarsPath.setRequired(true);
        options.addOption(androidJarsPath);
        
        Option outputFile = new Option("o", "output-folder", true, "output folder to save '<package-name>/*.dot' files into (current user folder is the default one)");
        outputFile.setOptionalArg(true);
        options.addOption(outputFile);
        
        Option callGraphAlg = new Option("c", "callgraph-alg", false, "callgraph algorithm: AUTO, CHA, VTA, RTA, (default) SPARK and GEOM");
        callGraphAlg.setOptionalArg(true);
        options.addOption(callGraphAlg);
    }

    public void parse(String[] args, Logger logger) throws ParseException {
        cmd = parser.parse(options, args);
        
        sourceFilePath = cmd.getOptionValue("source-file");
        outputFilePath = cmd.getOptionValue("output-folder");
        androidJarPath = cmd.getOptionValue("android-jars");
        String cgAlgorithmOpt = cmd.getOptionValue("callgraph-alg");

        if (cgAlgorithmOpt != null) {
            if (cgAlgorithmOpt.toString().equalsIgnoreCase("AUTO"))
                cgAlgorithm = CallgraphAlgorithm.AutomaticSelection;
            else if (cgAlgorithmOpt.toString().equalsIgnoreCase("CHA"))
                cgAlgorithm = CallgraphAlgorithm.CHA;
            else if (cgAlgorithmOpt.toString().equalsIgnoreCase("VTA"))
                cgAlgorithm = CallgraphAlgorithm.VTA;
            else if (cgAlgorithmOpt.toString().equalsIgnoreCase("RTA"))
                cgAlgorithm = CallgraphAlgorithm.RTA;
            else if (cgAlgorithmOpt.toString().equalsIgnoreCase("SPARK"))
                cgAlgorithm = CallgraphAlgorithm.SPARK;
            else if (cgAlgorithmOpt.toString().equalsIgnoreCase("GEOM"))
                cgAlgorithm = CallgraphAlgorithm.GEOM;
            else {
                logger.warn("Callgraph algorithm not found. Setting default one (SPARK)...");
            }
        }
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public String getAndroidJarPath() {
        return androidJarPath;
    }

    public Options getOptions() {
        return options;
    }

    public InfoflowConfiguration.CallgraphAlgorithm getCgAlgorithm() {
        return cgAlgorithm;
    }

}
