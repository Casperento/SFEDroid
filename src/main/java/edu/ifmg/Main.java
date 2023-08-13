package edu.ifmg;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
            int totalApks = apks.size(), i = 1;
            for (String apk : apks) {
                logger.info(String.format("Analyzing sample (%d/%d): '%s'...", apk, i++, totalApks));
                if (Files.exists(Path.of(apk))) {
                    Parameters p = new Parameters(cli, apk);
                    Analyzer analyzer = new Analyzer(p);
                    analyzer.analyze();
                    if (!analyzer.hasError()) {
                        if (cli.getExportCallGraph()) {
                            analyzer.exportCallgraph();
                        }
                    } else {
                        // System.exit(1);
                    }
                    
                } else {
                    logger.info(String.format("File '%s' not found. Skipping...", apk));
                }
            }
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