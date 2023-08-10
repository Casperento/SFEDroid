package edu.ifmg.StaticAnalyzer;

import edu.ifmg.Main;
import edu.ifmg.Utils.Files.FileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PermissionsMapper {
    private static final Logger logger = LoggerFactory.getLogger(PermissionsMapper.class);
    private static PermissionsMapper instance;
    private List<String> mappings = new ArrayList<>();
    private File permissionsMappingFolder;

    Map<String, List<String>> permissionMethods = new HashMap<>();
    private PermissionsMapper(String path) {
        permissionsMappingFolder = new File(path);
        if (!permissionsMappingFolder.exists() || !permissionsMappingFolder.isDirectory()) {
            logger.error("Permissions Mapping folder not found...");
            instance = null;
            return;
        }

        logger.info(String.format("Permissions Mapping folder: %s", permissionsMappingFolder.getAbsoluteFile()));
        if (importPermissionsFiles()) {
            logger.info("Mapping permissions to methods they allow access to...");
            if (mapPermissionMethods())
                logger.info("Permissions mapped!");
            else {
                logger.error("PermissionsMapper could not map permission and methods...");
                instance = null;
            }
        } else {
            logger.error("Failed to import permissions files...");
            instance = null;
        }
    }

    public static PermissionsMapper getInstance(String path) {
        if (instance == null)
            instance = new PermissionsMapper(path);
        return instance;
    }

    public Map<String, List<String>> getPermissionMethods() {
        return permissionMethods;
    }

    // Translating permissions mappings' syntax to a Map based on SootMethod's signatures
    public boolean mapPermissionMethods() {
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

        return !permissionMethods.isEmpty();
    }

    private boolean importPermissionsFiles() {
        File[] permissionsDir = permissionsMappingFolder.listFiles();
        File[] apiFolder;
        List<String> temp;
        if (permissionsDir != null && !Arrays.asList(permissionsDir).isEmpty()) {
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
            return true;
        }
        return false;
    }
}
