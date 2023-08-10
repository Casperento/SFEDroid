package edu.ifmg.Utils;

import java.nio.file.Path;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;

public class Cli {
    private static final Logger logger = LoggerFactory.getLogger(Cli.class);
    private final Options options = new Options();
    private final CommandLineParser parser = new DefaultParser();
    private CommandLine cmd = null;
    private String sourceFilePath = new String();
    private String outputFilePath = new String();
    private String androidJarPath = new String();
    private String additionalClassPath = new String();
    private String permissionsMappingFolder = new String();
    private Boolean exportCallGraph = false;
    private InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm = CallgraphAlgorithm.SPARK;
    private String homePath = new String();
    private static Cli instance;

    private Cli() {
        Map<String, String> env = System.getenv();
        if (System.getProperty("os.name").startsWith("Windows"))
            homePath = env.get("USERPROFILE");
        else
            homePath = env.get("HOME");

        Option sourceFile = new Option("i", "source-file", true, "source apk file");
        sourceFile.setRequired(true);
        options.addOption(sourceFile);

        Option permissionsMap = new Option("p", "permissions-mapping", true, "permissions' mapping input file");
        permissionsMap.setRequired(true);
        options.addOption(permissionsMap);
        
        Option androidJarsPath = new Option("j", "android-jars", true, "path to android jars");
        androidJarsPath.setRequired(true);
        options.addOption(androidJarsPath);
        
        Option outputFile = new Option("o", "output-folder", true, "output folder to save exported files related to the apk being analyzed");
        outputFile.setOptionalArg(true);
        options.addOption(outputFile);
        
        Option callGraphAlg = new Option("c", "callgraph-alg", true, "callgraph algorithm: AUTO, CHA, VTA, RTA, (default) SPARK and GEOM");
        callGraphAlg.setOptionalArg(true);
        options.addOption(callGraphAlg);

        Option additionalClasspath = new Option("ac", "additional-classpath", true, "path to add into soot's classpath (separated by ':' or ';')");
        callGraphAlg.setOptionalArg(true);
        options.addOption(additionalClasspath);

        Option exportCg = new Option("e", "export-callgraph", false, "export callgraph as DOT file");
        exportCg.setOptionalArg(true);
        options.addOption(exportCg);
    }

    public static Cli getInstance() {
        if (instance == null)
            instance = new Cli();
        return  instance;
    }

    public void parse(String[] args) throws ParseException {
        cmd = parser.parse(options, args);
        
        sourceFilePath = cmd.getOptionValue("source-file");
        androidJarPath = cmd.getOptionValue("android-jars");
        additionalClassPath = cmd.getOptionValue("additional-classpath");
        exportCallGraph = cmd.hasOption("export-callgraph");
        permissionsMappingFolder = cmd.getOptionValue("permissions-mapping");
        outputFilePath = cmd.getOptionValue("output-folder");
        if (outputFilePath == null)
            outputFilePath = homePath;
        
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

    public String getPermissionsMappingFolder() {
        return permissionsMappingFolder;
    }

    public Boolean getExportCallGraph() {
        return exportCallGraph;
    }

    public String getAdditionalClassPath() {
        return additionalClassPath;
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

    public void updateOutputFilePath(String path) {
        this.outputFilePath = Path.of(outputFilePath, path).toString();
    }

}
