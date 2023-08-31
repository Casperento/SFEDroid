package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Array;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.util.ArraySet;

/**
 * Manifest is a Facade class to handle a sub-set of meta-data parsed by ProcessManifest.
 *
 * @author Casperento
 *
 */
public class Manifest {
    private static final Logger logger = LoggerFactory.getLogger(Manifest.class);
    private Path appPath = null;
    private String fileName = new String();
    private ProcessManifest manifest = null;
    private String mainEntryPointSig = new String();
    private List<String> permissions = new ArrayList<>();

    public Manifest(String path) {
        appPath = Path.of(path);
    }

    public void process() throws IOException, XmlPullParserException {
        manifest = new ProcessManifest(appPath.toString());
        fileName = manifest.getPackageName();
        if (fileName == null)
            fileName = appPath.getFileName().toString();

        // Getting 'android.intent.action.MAIN' activity
        for (AXmlNode activity : manifest.getAllActivities()) {
            if (activity.getChildren() == null)
               continue;
            for (AXmlNode child : activity.getChildren()) {
                if (child.getTag().equals("intent-filter")) {
                    for (AXmlNode sChild : child.getChildren()) {
                        if (sChild.getAttribute("name") != null && sChild.getAttribute("name").getValue().equals("android.intent.action.MAIN")) {
                            mainEntryPointSig = (String) activity.getAttribute("name").getValue();
                        }
                    }
                }
            }
        }
        manifest.close();
    }

    public String getPackageName() {
        return fileName;
    }

    public List<String> getPermissions() {
        permissions.addAll(manifest.getPermissions());
        Collections.sort(permissions);
        return permissions;
    }

    public int getMinSdkVersion() {
        return manifest.getMinSdkVersion();
    }

    public int getTargetSdkVersion() {
        return manifest.getTargetSdkVersion();
    }

    public String getMainEntryPointSig() {
        return mainEntryPointSig;
    }

}
