package edu.ifmg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
            logger.error(e.getMessage());
            formatter.printHelp("SFEDroid", cli.getOptions());
            System.exit(1);
        }

        PermissionsMapper mapper = PermissionsMapper.getInstance(cli.getPermissionsMappingFolder());

        if (cli.getInputListFilePath() != null && !cli.getInputListFilePath().isEmpty()) {
            List<String> apks = FileHandler.importFile(cli.getInputListFilePath());
            List<String> failed = new ArrayList<>();
            if (apks == null || apks.isEmpty()) {
                logger.error("Input file is empty...");
                System.exit(1);
            }
            int totalApks = apks.size(), i = 1;
            for (String apk : apks) {
                System.out.printf("Analyzing sample (%d/%d): '%s'...%n", i, totalApks, apk);
                logger.info(String.format("Analyzing sample (%d/%d): '%s'...", i, totalApks, apk));
                i++;
                if (Files.exists(Path.of(apk))) {
                    Parameters p = new Parameters(cli, apk);
                    if (p.hasError()) {
                        System.out.printf("Failed to parse manifest file of '%s' file. Skipping...%n", apk);
                        logger.warn(String.format("Failed to analyze '%s' file. Skipping...", apk));
                        failed.add(apk);
                        continue;
                    }
                    Analyzer analyzer = new Analyzer(p);
                    analyzer.analyze();
                    if (!analyzer.hasError()) {
                        if (cli.getExportCallGraph()) {
                            analyzer.exportCallgraph();
                        }
                    } else {
                        System.out.printf("Failed to analyze '%s' file. Skipping...%n", apk);
                        logger.warn(String.format("File '%s' not found. Skipping...", apk));
                        failed.add(apk);
                    }
                } else {
                    System.out.printf("File '%s' not found. Skipping...%n", apk);
                    logger.warn(String.format("File '%s' not found. Skipping...", apk));
                    failed.add(apk);
                }
            }
            System.out.printf("\nAnalysis results:\n\tTotal of APKs analyzed: %d\n\tTotal of SUCCESS: %d\n\tList of APKs that failed (Total: %d): %s", totalApks, totalApks-failed.size(), failed.size(), failed);
        } else {
            Parameters p = new Parameters(cli, cli.getSourceFilePath());
            Analyzer analyzer = new Analyzer(p);
            analyzer.analyze();
            if (!analyzer.hasError()) {
                if (cli.getExportCallGraph()) {
                    analyzer.exportCallgraph();
                }
            } else {
                System.exit(1);
            }
        }
    }
}