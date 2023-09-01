package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

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
    private Path outputFilePath;
    private Path outputFolderPath;
    private String minSdkVersion;
    private List<String> permissions;
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

    public Parameters(Cli cli, String inputFile) {
        sourceFilePath = inputFile;
        additionalClassPath = cli.getAdditionalClassPath();
        cgAlgorithm = cli.getCgAlgorithm();
        timeOut = cli.getTimeOut();
        definedLabel = cli.getDefinedLabel();
        outputFolderPath = Path.of(cli.getOutputFolderPath());
        createNewDatasetFile = cli.getCreateNewDatasetFile();

        Manifest manifestHandler = new Manifest(inputFile);

        // Processing AndroidManifest.xml
        try {
            manifestHandler.process();
        } catch (RuntimeException | IOException | XmlPullParserException e) {
            logger.error(e.getMessage());
            hasError = true;
            return;
        }

        // Setting and creating output folder
        String apkFileName = Path.of(sourceFilePath).getFileName().toString();
        apkFileName = apkFileName.split("\\.")[0];
        outputFilePath = Path.of(cli.getOutputFolderPath(), String.format("%s_%s", manifestHandler.getPackageName(), apkFileName));
        if (!Files.exists(outputFilePath)) {
            logger.info("Creating new output folder for the app under analysis...");
            if (!outputFilePath.toFile().mkdirs()) {
                logger.error(String.format("Failed when trying to create output folder: %s", outputFilePath.toString()));
                return;
            }
        }

        // Getting app's meta-data
        pkgName = manifestHandler.getPackageName();
        permissions = manifestHandler.getPermissions();

        logger.info(String.format("Source APK path: %s", sourceFilePath));
        androidJarPath = cli.getAndroidJarPath();

        targetSdkVersion = String.valueOf(manifestHandler.getTargetSdkVersion());
        logger.info(String.format("Target SDK version: %s", targetSdkVersion));

        minSdkVersion = String.valueOf(manifestHandler.getMinSdkVersion());
        logger.info(String.format("Min SDK version: %s", minSdkVersion));

        logger.info(String.format("Android Jars path: %s", cli.getAndroidJarPath()));
        logger.info(String.format("Call graph build algorithm: %s", cli.getCgAlgorithm()));
        logger.info(String.format("Output path: %s", outputFilePath.toString()));
        logger.info(String.format("Print call graph: %b", cli.getExportCallGraph()));
        logger.info(String.format("Package Name: %s", pkgName));
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
    public List<String> getPermissions() {
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
}
