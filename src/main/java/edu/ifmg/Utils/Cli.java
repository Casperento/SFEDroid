package edu.ifmg.Utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

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

/**
 * Cli is a facade class that handles Command-Line information before starting the static analysis. It's built upon
 * Apache Commons CLI.
 *
 * @author Casperento
 *
 */
public class Cli {
    private static final Logger logger = LoggerFactory.getLogger(Cli.class);
    private final Options options = new Options();
    private final CommandLineParser parser = new DefaultParser();
    private CommandLine cmd = null;
    private String sourceFilePath = new String();
    private String inputListFilePath = new String();
    private String outputFolderPath = new String();
    private String androidJarPath = new String();
    private String additionalClassPath = new String();
    private String permissionsMappingFolder = new String();
    private Integer timeOut = null;
    private Boolean exportCallGraph = false;
    private CallgraphAlgorithm cgAlgorithm = CallgraphAlgorithm.CHA;
    private String homePath;
    private static Cli instance;
    private boolean logMode = false;
    private int definedLabel = 0;
    private boolean createNewDatasetFile;

    private Cli() {
        Map<String, String> env = System.getenv();
        if (System.getProperty("os.name").startsWith("Windows"))
            homePath = env.get("USERPROFILE");
        else
            homePath = env.get("HOME");

        Option sourceFile = new Option("i", "source-file", true, "source apk file");
        sourceFile.setOptionalArg(true);
        options.addOption(sourceFile);

        Option inputListOption = new Option("l", "list-file", true, "a file containing a list of apks to analyze");
        inputListOption.setOptionalArg(true);
        options.addOption(inputListOption);

        Option permissionsMap = new Option("p", "permissions-mapping", true, "permissions' mapping input file");
        permissionsMap.setOptionalArg(true);
        options.addOption(permissionsMap);
        
        Option androidJarsPath = new Option("j", "android-jars", true, "path to android jars");
        androidJarsPath.setOptionalArg(true);
        options.addOption(androidJarsPath);
        
        Option outputFile = new Option("o", "output-folder", true, "output folder to save exported files related to the apk being analyzed");
        outputFile.setOptionalArg(true);
        options.addOption(outputFile);
        
        Option callGraphAlg = new Option("c", "callgraph-alg", true, "callgraph algorithm: AUTO, CHA (default), VTA, RTA, SPARK or GEOM");
        callGraphAlg.setOptionalArg(true);
        options.addOption(callGraphAlg);

        Option additionalClasspath = new Option("ac", "additional-classpath", true, "path to add into soot's classpath (separated by ':' or ';')");
        additionalClasspath.setOptionalArg(true);
        options.addOption(additionalClasspath);

        Option exportCg = new Option("e", "export-callgraph", false, "export callgraph as DOT file");
        exportCg.setOptionalArg(true);
        options.addOption(exportCg);

        Option logModeOption = new Option("v", "verbose", false, "turn on logs and write it to console and disk ('/src/main/resources/logs')");
        logModeOption.setOptionalArg(true);
        options.addOption(logModeOption);

        Option dataFlowAnalysisTimeout = new Option("t", "timeout", true, "set timeout in seconds to abort the taint analysis");
        dataFlowAnalysisTimeout.setOptionalArg(true);
        options.addOption(dataFlowAnalysisTimeout);

        Option defineLabelOption = new Option("r", "define-label", true, "define apks' labels, 1 for malware 0 for benign");
        defineLabelOption.setRequired(true);
        options.addOption(defineLabelOption);

        Option createNewDatasetFileOption = new Option("d", "create-dataset", false, "create a new dataset.tsv file and overwrite existing one in the output folder");
        createNewDatasetFileOption.setOptionalArg(true);
        options.addOption(createNewDatasetFileOption);
    }

    public static Cli getInstance() {
        if (instance == null)
            instance = new Cli();
        return  instance;
    }

    public void parse(String[] args) throws ParseException {
        cmd = parser.parse(options, args);
        sourceFilePath = cmd.getOptionValue("source-file");
        inputListFilePath = cmd.getOptionValue("list-file");
        definedLabel = Integer.parseInt(cmd.getOptionValue("define-label"));
        createNewDatasetFile = cmd.hasOption("create-dataset");

        if (cmd.getOptionValue("timeout") != null)
            timeOut = Integer.valueOf(cmd.getOptionValue("timeout"));

        if (sourceFilePath == null && inputListFilePath == null)
            throw new ParseException("You either need to specify an apk file or a txt file containing a list of apks to analyze...");

        if (sourceFilePath != null && inputListFilePath != null)
            throw new ParseException("Two kinds of input file provided, can't decide which one to consider in the analysis...");


        if (sourceFilePath != null && !Files.exists(Path.of(sourceFilePath)))
            throw new ParseException("Source file not found...");
        
        if (inputListFilePath != null && !Files.exists(Path.of(inputListFilePath)))
            throw new ParseException("List file not found...");

        additionalClassPath = cmd.getOptionValue("additional-classpath");
        exportCallGraph = cmd.hasOption("export-callgraph");
        logMode = cmd.hasOption("verbose");

        permissionsMappingFolder = cmd.getOptionValue("permissions-mapping");
        if (permissionsMappingFolder == null)
            permissionsMappingFolder = "axplorer/permissions";

        androidJarPath = cmd.getOptionValue("android-jars");
        if (androidJarPath == null)
            androidJarPath = "android-platforms";

        outputFolderPath = cmd.getOptionValue("output-folder");
        if (outputFolderPath == null)
            outputFolderPath = homePath;
        
        String cgAlgorithmOpt = cmd.getOptionValue("callgraph-alg");
        if (cgAlgorithmOpt != null) {
            if (cgAlgorithmOpt.equalsIgnoreCase("AUTO"))
                cgAlgorithm = CallgraphAlgorithm.AutomaticSelection;
            else if (cgAlgorithmOpt.equalsIgnoreCase("VTA"))
                cgAlgorithm = CallgraphAlgorithm.VTA;
            else if (cgAlgorithmOpt.equalsIgnoreCase("RTA"))
                cgAlgorithm = CallgraphAlgorithm.RTA;
            else if (cgAlgorithmOpt.equalsIgnoreCase("SPARK"))
                cgAlgorithm = CallgraphAlgorithm.SPARK;
            else if (cgAlgorithmOpt.equalsIgnoreCase("GEOM"))
                cgAlgorithm = CallgraphAlgorithm.GEOM;
            else {
                logger.warn("Callgraph algorithm not found. Setting default one (CHA)...");
            }
        }

        if (!logMode) {
            LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            Configuration config = ctx.getConfiguration();
            LoggerConfig rootConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
            rootConfig.setLevel(Level.OFF);
            ctx.updateLoggers();
        }

    }

    public Integer getTimeOut() {
        return timeOut;
    }

    public String getInputListFilePath() {
        return inputListFilePath;
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

    public String getOutputFolderPath() {
        return outputFolderPath;
    }

    public String getAndroidJarPath() {
        return androidJarPath;
    }

    public Options getOptions() {
        return options;
    }

    public CallgraphAlgorithm getCgAlgorithm() {
        return cgAlgorithm;
    }

    public int getDefinedLabel() {
        return definedLabel;
    }

    public boolean getCreateNewDatasetFile() { return createNewDatasetFile; }
}
