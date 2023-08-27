package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import com.esotericsoftware.kryo.util.Null;
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
    private Path outputFolderPath;
    private String minSdkVersion;
    private Set<String> permissions;
    private String targetSdkVersion;
    private String mainEntryPointClassName;
    private String pkgName;
    private String sourceFilePath;
    private String androidJarPath;
    private String additionalClassPath;
    private CallgraphAlgorithm cgAlgorithm;
    private Integer timeOut;
    private boolean hasError = false;
    public Parameters(Cli cli, String inputFile) {
        sourceFilePath = inputFile;
        additionalClassPath = cli.getAdditionalClassPath();
        cgAlgorithm = cli.getCgAlgorithm();
        timeOut = cli.getTimeOut();

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
        outputFolderPath = Path.of(cli.getOutputFilePath(), manifestHandler.getPackageName());
        if (!Files.exists(outputFolderPath)) {
            logger.info("Creating new output folder for the app under analysis...");
            if (!outputFolderPath.toFile().mkdirs()) {
                logger.error(String.format("Failed when trying to create output folder: %s", outputFolderPath.toString()));
                return;
            }
        }

        // Getting app's meta-data
        pkgName = manifestHandler.getPackageName();
        mainEntryPointClassName = manifestHandler.getMainEntryPointSig();
        permissions = manifestHandler.getPermissions();

        logger.info(String.format("Source APK path: %s", sourceFilePath));
        androidJarPath = cli.getAndroidJarPath();

        targetSdkVersion = String.valueOf(manifestHandler.getTargetSdkVersion());
//        if (targetSdkVersion.equals("-1")) {
//            targetSdkVersion = "18";
//            androidJarPath = Path.of(androidJarPath, "android-18/android.jar").toString(); // forces a specific .jar from the SDK
//            logger.info("Target SDK version not defined. Setting to it 18...");
//        } else {
            logger.info(String.format("Target SDK version: %s", targetSdkVersion));
//        }

        minSdkVersion = String.valueOf(manifestHandler.getMinSdkVersion());
//        if (minSdkVersion.equals("-1")) {
//            minSdkVersion = "16";
//            androidJarPath = Path.of(androidJarPath, "android-16/android.jar").toString(); // forces a specific .jar from the SDK
//            logger.info("Min SDK version not defined. Setting to it 16...");
//        } else {
            logger.info(String.format("Min SDK version: %s", minSdkVersion));
//        }

        logger.info(String.format("Android Jars path: %s", cli.getAndroidJarPath()));
        logger.info(String.format("Call graph build algorithm: %s", cli.getCgAlgorithm()));
        logger.info(String.format("Output path: %s", outputFolderPath.toString()));
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

    public Path getOutputFolderPath() {
        return outputFolderPath;
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
    public String getMainEntryPointClassName() {
        return mainEntryPointClassName;
    }
    public String getPkgName() {
        return pkgName;
    }
    
    
}
