package edu.ifmg;

import edu.ifmg.StaticAnalyzer.PermissionsMapper;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ifmg.StaticAnalyzer.Analyzer;
import edu.ifmg.Utils.Cli;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
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
        Analyzer analyzer = new Analyzer();
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