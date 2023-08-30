package edu.ifmg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import edu.ifmg.StaticAnalyzer.SourcesSinks;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.jimple.infoflow.android.source.parsers.xml.ResourceUtils;

import edu.ifmg.StaticAnalyzer.PermissionsMapper;
import edu.ifmg.StaticAnalyzer.Analyzer;
import edu.ifmg.StaticAnalyzer.Parameters;
import edu.ifmg.Utils.Cli;
import edu.ifmg.Utils.FileHandler;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        // Copying latest SourcesAndSinks.txt file from soot-infoflow-android jar
        if (!Files.exists(Path.of("SourcesAndSinks.txt"))) {
            try {
                InputStream sourcesSinksFile = ResourceUtils.getResourceStream("/SourcesAndSinks.txt");
                byte[] bytes = sourcesSinksFile.readAllBytes();
                FileOutputStream file = new FileOutputStream("SourcesAndSinks.txt");
                file.write(bytes);
                file.close();
                sourcesSinksFile.close();
            } catch (IOException | RuntimeException e) {
                logger.error("SourcesAndSinks.txt not found...");
                System.exit(1);
            }
        }

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
        SourcesSinks sourcesSinks = SourcesSinks.getInstance();

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
                    Parameters p = new Parameters(cli, apk);
                    if (p.hasError()) {
                        System.out.printf("Failed to parse manifest file of '%s' file. Skipping...%n", apk);
                        logger.warn(String.format("Failed to parse manifest file of '%s' file. Skipping...%n", apk));
                        failed++;
                        continue;
                    }
                    Analyzer analyzer = new Analyzer(p);
                    analyzer.analyze();
                    if (!analyzer.hasError()) {

                        // Check for reachability of methods allowed by permissions
                        analyzer.listReachableMethods(mapper);
                        List<String> reachableMethods = analyzer.getReachable();
                        if (reachableMethods.isEmpty())
                            logger.info("No reachable methods found...");

                        if (cli.getExportCallGraph()) {
                            analyzer.exportCallgraph();
                        }
                    } else {
                        System.out.printf("Failed to analyze '%s' file. Skipping...%n", apk);
                        logger.warn(String.format("Failed to analyze '%s' file. Skipping...%n", apk));
                        failed++;
                    }
                } else {
                    System.out.printf("File '%s' not found. Skipping...%n", apk);
                    logger.warn(String.format("File '%s' not found. Skipping...", apk));
                    failed++;
                }
            }
            System.out.printf("\nAnalysis results:\n" +
                    "\tTotal of APKs analyzed: %d\n" +
                    "\tTotal of SUCCESS: %d\n" +
                    "\tTotal of SKIPPED: %d (%d%%)", totalApks, (totalApks-failed), failed, (100*failed/totalApks));
        } else {
            Parameters p = new Parameters(cli, cli.getSourceFilePath());
            Analyzer analyzer = new Analyzer(p);
            analyzer.analyze();
            if (!analyzer.hasError()) {

                // Check for reachability of methods allowed by permissions
                analyzer.listReachableMethods(mapper);
                List<String> reachableMethods = analyzer.getReachable();
                if (reachableMethods.isEmpty())
                    logger.info("No reachable methods found...");

                if (cli.getExportCallGraph()) {
                    analyzer.exportCallgraph();
                }

                // Preparing features
                HashSet<String> sourcesSinksMethodsSigs = sourcesSinks.getSinksMethodsSigs();
                int apkSize = FileHandler.getFileSize(cli.getSourceFilePath());
                double apkEntropy = FileHandler.getFileEntropy(cli.getSourceFilePath()); // TODO: calculate .dex entropy
                Set<String> permissions = mapper.getPermissionMethods().keySet();

                System.out.println("----------------------------------------------Feature-Set-----------------------------------------------");
                System.out.printf("Label: %s\n", "1"); // 1: malware, 0: benign | TODO: cli option to set label
                System.out.printf("Package name: %s\n", p.getPkgName());
                System.out.printf("MinSdkVersion: %s\n", p.getMinSdkVersion());
                System.out.printf("TargetSdkVersion: %s\n", p.getTargetSdkVersion());
                System.out.printf("File size: %d\n", apkSize);
                System.out.printf("File entropy: %.2f\n", apkEntropy);
                System.out.printf("Number of permissions mapped: %d\n", permissions.size());
                System.out.printf("Number of reachable methods: %d\n", reachableMethods.size());
                System.out.printf("Number of sink methods loaded by FlowDroid: %d\n", sourcesSinksMethodsSigs.size()); // TODO: read found leanks and match against loaded sinks
                System.out.println("--------------------------------------------------------------------------------------------------------");

                // TODO: Build file content to export
                // TODO: Export feature set to CSV file
            } else {
                System.exit(1);
            }
        }
    }
}
