package edu.ifmg;

import org.apache.commons.cli.*;

public class Main {
    public static void main(String[] args) {
        Options options = new Options();
        Option apkSourceFile = new Option("a", "apkSourceFile", true, "source apk file");
        apkSourceFile.setRequired(true);
        options.addOption(apkSourceFile);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("SFEDroid", options);
            System.exit(1);
        }

        String apkFilePath = cmd.getOptionValue("apkSourceFile");
        System.out.println(apkFilePath);
    }
}