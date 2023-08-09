package edu.ifmg;

import edu.ifmg.Utils.Files.FileHandler;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import edu.ifmg.StaticAnalyzer.Analyzer;
import edu.ifmg.StaticAnalyzer.Manifest;
import edu.ifmg.Utils.Cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        // Importing permissions' mappings
        logger.info(String.format("Permissions Mapping folder: %s", cli.getPermissionsMapping()));
        File permissionsMappingFolder = new File(cli.getPermissionsMapping());
        File[] permissionsDir = permissionsMappingFolder.listFiles();
        File[] apiFolder;
        List<String> mappings = new ArrayList<>(), temp;

        if (permissionsDir != null) {
            for (File folder : permissionsDir) {
                if (folder.isDirectory()) {
                    apiFolder = folder.listFiles();
                    if (apiFolder != null) {
                        for (File f : apiFolder) {
                            if (f.isFile() && (f.getName().startsWith("framework-map") || f.getName().startsWith("sdk-map"))) {
                                temp = FileHandler.importFile(f.getPath());
                                if (temp != null) {
                                    for (String t : temp) {
                                        if ((t != null) && !t.isEmpty() && !mappings.contains(t))
                                            mappings.add(t);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Translating permissions mappings' syntax to a Map based on SootMethod's signatures
        Map<String, List<String>> permissionMethods = new HashMap<>();
        String returnType, args_, declaringClass, methodName;
        Pattern signaturePattern = Pattern.compile("^(.*)\\.(.*)(\\(.*\\))(.*) {2}::");
        Pattern permissionPattern = Pattern.compile(":: {2}(.*)$");
        Matcher matcher;
        List<String> perms, methods;
        String methodSig;
        for (String m : mappings) {
            matcher = permissionPattern.matcher(m);
            if (matcher.find()) {
                perms = Arrays.asList(matcher.group(1).split(", "));
                logger.debug(String.format("Permissions found: %s", perms.toString()));
            } else {
                continue;
            }
            matcher = signaturePattern.matcher(m);
            if (matcher.find()) {
                returnType = matcher.group(4);
                args_ = matcher.group(3);
                methodName = matcher.group(2);
                declaringClass = matcher.group(1);
                logger.debug(String.format("Signature found:<%s: %s %s%s>\n", declaringClass, returnType, methodName, args_));
                methodSig = String.format("<%s: %s %s%s>", declaringClass, returnType, methodName, args_);
            } else {
                continue;
            }

            for (String p : perms) {
                methods = new ArrayList<>();
                if (permissionMethods.containsKey(p))
                    methods = permissionMethods.get(p);
                if (!methods.contains(methodSig))
                    methods.add(methodSig);
                permissionMethods.put(p, methods);
            }
        }

        Analyzer analyzer = new Analyzer(cli.getSourceFilePath(), cli.getAndroidJarPath(), cli.getOutputFilePath(), cli.getCgAlgorithm(), manifestHandler.getPackageName(), cli.getAdditionalClassPath());
        analyzer.buildCallgraph(manifestHandler.getMainEntryPointSig());

        if (cli.getExportCallGraph()) {
            analyzer.exportCallgraph();
        }

        manifestHandler.close();
    }
}