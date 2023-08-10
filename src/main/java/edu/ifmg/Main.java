package edu.ifmg;

import edu.ifmg.StaticAnalyzer.PermissionsMapper;
import edu.ifmg.Utils.Files.FileHandler;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import edu.ifmg.StaticAnalyzer.Analyzer;
import edu.ifmg.StaticAnalyzer.Manifest;
import edu.ifmg.Utils.Cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

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
        logger.info(String.format("Call graph build algorithm: %s", cli.getCgAlgorithm()));
        logger.info(String.format("Print call graph: %b", cli.getExportCallGraph()));

        // Processing AndroidManifest.xml
        Manifest manifestHandler = new Manifest(cli.getSourceFilePath());
        try {
            manifestHandler.process();
        } catch (IOException | XmlPullParserException e) {
            logger.error(e.getMessage());
            System.exit(1);
        }

        String targetSdkVersion = String.valueOf(manifestHandler.getTargetSdkVersion());
        String minSdkVersion = String.valueOf(manifestHandler.getMinSdkVersion());
        Set<String> permissions = manifestHandler.getPermissions();

        cli.updateOutputFilePath(manifestHandler.getPackageName());
        logger.info(String.format("Output path: %s", cli.getOutputFilePath()));
        Path parentDir = Path.of(cli.getOutputFilePath());
        if (!Files.exists(parentDir)) {
            logger.info("Creating new output folder for the app under analysis...");
            parentDir.toFile().mkdir();
        }

        PermissionsMapper mapper = PermissionsMapper.getInstance(cli.getPermissionsMappingFolder());

        Analyzer analyzer = new Analyzer(cli.getSourceFilePath(), cli.getAndroidJarPath(), cli.getOutputFilePath(), cli.getCgAlgorithm(), manifestHandler.getPackageName(), cli.getAdditionalClassPath());

        analyzer.buildCallgraph(manifestHandler.getMainEntryPointSig());
        if (cli.getExportCallGraph()) {
            analyzer.exportCallgraph();
        }

        manifestHandler.close();
    }
}