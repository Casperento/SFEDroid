package edu.ifmg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import edu.ifmg.StaticAnalyzer.*;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.jimple.infoflow.android.source.parsers.xml.ResourceUtils;

import edu.ifmg.Utils.Cli;
import edu.ifmg.Utils.FileHandler;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        Cli cli = Cli.getInstance();
        HelpFormatter formatter = new HelpFormatter();
        try {
            cli.parse(args);
        } catch (ParseException e) {
            formatter.printHelp("SFEDroid", cli.getOptions());
            logger.error(e.getMessage());
            System.exit(1);
        }

        PermissionsMapper mapper = PermissionsMapper.getInstance(cli.getPermissionsMappingFolder());

        if (cli.getInputListFilePath() != null && !cli.getInputListFilePath().isEmpty()) {
            List<String> apks = FileHandler.importFile(cli.getInputListFilePath());
            if (apks == null || apks.isEmpty()) {
                logger.error("Input file is empty...");
                System.exit(1);
            }
            int totalApks = apks.size(), i = 1, failed = 0;
            for (String apk : apks) {
                System.out.printf("Analyzing sample (%d/%d): '%s'...%n", i, totalApks, apk);
                logger.info(String.format("Analyzing sample (%d/%d): '%s'...", i, totalApks, apk));
                i++;
                if (Files.exists(Path.of(apk))) {
                    Parameters p = Parameters.getInstance(cli, apk);
                    if (p.hasError()) {
                        System.out.printf("Failed to parse manifest file of '%s' file. Skipping...%n", apk);
                        logger.warn(String.format("Failed to parse manifest file of '%s' file. Skipping...%n", apk));
                        failed++;
                        continue;
                    }
                    Analyzer analyzer = Analyzer.getInstance(p);
                    analyzer.analyze();
                    if (!analyzer.hasError()) {
                        analyzer.prepareBasicFeatures(mapper);
                        postAnalysis(analyzer, mapper, cli);
                    } else {
                        System.out.printf("Failed to analyze '%s' file. Skipping...%n", apk);
                        logger.warn(String.format("Failed to analyze '%s' file. Skipping...%n", apk));
                        FileHandler.deleteFolder(p.getOutputFilePath().toString());
                        failed++;
                    }
                } else {
                    System.out.printf("File '%s' not found. Skipping...%n", apk);
                    logger.warn(String.format("File '%s' not found. Skipping...", apk));
                    failed++;
                }
            }
            String analysisResults = String.format("""

                    Analysis results:
                    \tTotal of APKs analyzed: %d
                    \tTotal of SUCCESS: %d
                    \tTotal of SKIPPED: %d (%d%%)""", totalApks, (totalApks-failed), failed, (100*failed/totalApks));
            System.out.println(analysisResults);
            logger.info(analysisResults);
        } else {
            Parameters p = Parameters.getInstance(cli, cli.getSourceFilePath());
            if (p.hasError()) {
                System.out.println("Failed to parse manifest file of the current apk...");
                logger.warn("Failed to parse manifest file of the current apk...");
            }
            Analyzer analyzer = Analyzer.getInstance(p);
            analyzer.analyze();
            if (!analyzer.hasError()) {
                analyzer.prepareBasicFeatures(mapper);
                postAnalysis(analyzer, mapper, cli);
            } else {
                FileHandler.deleteFolder(p.getOutputFilePath().toString());
                System.exit(1);
            }
        }
    }

    private static void postAnalysis(Analyzer analyzer, PermissionsMapper mapper, Cli cli) {
        // Check for reachability of methods allowed by permissions
        analyzer.listReachableMethods(mapper);
        if (analyzer.getReachable().isEmpty())
            logger.info("No reachable methods found...");

        if (cli.getExportCallGraph()) {
            analyzer.exportCallgraph();
        }

        // Apk-specific features
        analyzer.prepareApkFeatures();

        // Export dataset
        analyzer.exportDataSet();
    }
}
