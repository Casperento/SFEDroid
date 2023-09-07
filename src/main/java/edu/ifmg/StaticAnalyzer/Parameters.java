package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import edu.ifmg.Utils.Cli;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;

/**
 * Parameters handles static analysis parameters parsed from the cli
 *
 * @author Casperento
 *
 */
public class Parameters {
    private static final Logger logger = LoggerFactory.getLogger(Parameters.class);
    private static Parameters instance;
    private Path outputFilePath;
    private Path outputFolderPath;
    private String minSdkVersion;
    private Set<String> permissions;
    private String targetSdkVersion;
    private String pkgName;
    private String sourceFilePath;
    private String androidJarPath;
    private String additionalClassPath;
    private CallgraphAlgorithm cgAlgorithm;
    private Integer timeOut;
    private int definedLabel;
    private boolean hasError = false;
    private boolean createNewDatasetFile;
    private String mainEntryPointClass;

    public static Parameters getInstance(Cli cli, String inputFile) {
        if (Parameters.instance == null) {
            Parameters.instance = new Parameters();
        }
        clearOldValues(instance);
        setupParams(instance, cli, inputFile);
        return instance;
    }

    private static void clearOldValues(Parameters instance) {
        instance.outputFilePath = null;
        instance.outputFolderPath = null;
        instance.minSdkVersion = null;
        instance.permissions = null;
        instance.targetSdkVersion = null;
        instance.pkgName = null;
        instance.sourceFilePath = null;
        instance.androidJarPath = null;
        instance.additionalClassPath = null;
        instance.cgAlgorithm = null;
        instance.timeOut = null;
        instance.hasError = false;
        instance.createNewDatasetFile = false;
        instance.mainEntryPointClass = null;
    }

    private static void setupParams(Parameters instance, Cli cli, String inputFile) {
        instance.sourceFilePath = inputFile;
        instance.additionalClassPath = cli.getAdditionalClassPath();
        instance.cgAlgorithm = cli.getCgAlgorithm();
        instance.timeOut = cli.getTimeOut();
        instance.definedLabel = cli.getDefinedLabel();
        instance.outputFolderPath = Path.of(cli.getOutputFolderPath());
        instance.createNewDatasetFile = cli.getCreateNewDatasetFile();

        Manifest manifestHandler = Manifest.getInstance(inputFile);

        // Processing AndroidManifest.xml
        try {
            manifestHandler.process();
        } catch (RuntimeException | IOException | XmlPullParserException e) {
            logger.error(e.getMessage());
            instance.hasError = true;
            return;
        }

        // Setting and creating output folder
        String apkFileName = Path.of(instance.sourceFilePath).getFileName().toString();
        apkFileName = apkFileName.split("\\.")[0];
        instance.outputFilePath = Path.of(cli.getOutputFolderPath(), String.format("%s_%s", manifestHandler.getPackageName(), apkFileName));
        if (!Files.exists(instance.outputFilePath)) {
            logger.info("Creating new output folder for the app under analysis...");
            if (!instance.outputFilePath.toFile().mkdirs()) {
                logger.error(String.format("Failed when trying to create output folder: %s", instance.outputFilePath.toString()));
                return;
            }
        }

        // Getting app's meta-data
        instance.pkgName = manifestHandler.getPackageName();
        instance.permissions = manifestHandler.getPermissions();
        instance.mainEntryPointClass = manifestHandler.getMainEntryPointSig();

        logger.info(String.format("Source APK path: %s", instance.sourceFilePath));
        instance.androidJarPath = cli.getAndroidJarPath();
        logger.info(String.format("App main entry point class: %s", manifestHandler.getMainEntryPointSig()));
        instance.targetSdkVersion = String.valueOf(manifestHandler.getTargetSdkVersion());
        logger.info(String.format("Target SDK version: %s", instance.targetSdkVersion));
        instance.minSdkVersion = String.valueOf(manifestHandler.getMinSdkVersion());
        logger.info(String.format("Min SDK version: %s", instance.minSdkVersion));
        logger.info(String.format("Android Jars path: %s", cli.getAndroidJarPath()));
        logger.info(String.format("Call graph build algorithm: %s", cli.getCgAlgorithm()));
        logger.info(String.format("Output path: %s", instance.outputFilePath.toString()));
        logger.info(String.format("Print call graph: %b", cli.getExportCallGraph()));
        logger.info(String.format("Package Name: %s", instance.pkgName));
    }

    public boolean hasError() {
        return hasError;
    }
    public Integer getTimeOut() {
        return timeOut;
    }
    public String getSourceFilePath() {
        return sourceFilePath;
    }
    public String getAndroidJarPath() {
        return androidJarPath;
    }
    public String getAdditionalClassPath() {
        return additionalClassPath;
    }
    public CallgraphAlgorithm getCgAlgorithm() {
        return cgAlgorithm;
    }
    public Path getOutputFilePath() {
        return outputFilePath;
    }
    public String getMinSdkVersion() {
        return minSdkVersion;
    }
    public Set<String> getPermissions() {
        return permissions;
    }
    public String getTargetSdkVersion() {
        return targetSdkVersion;
    }
    public String getPkgName() {
        return pkgName;
    }
    public int getDefinedLabel() { return definedLabel; }
    public Path getOutputFolderPath() { return outputFolderPath; }
    public boolean getCreateNewDatasetFile() { return createNewDatasetFile; }

    public String getMainEntryPointClass() {
        return mainEntryPointClass;
    }
}
