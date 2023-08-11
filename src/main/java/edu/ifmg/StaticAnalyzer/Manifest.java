package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.axml.AXmlNode;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

public class Manifest {
    private static final Logger logger = LoggerFactory.getLogger(Manifest.class);
    private Path appPath = null;
    private String fileName = new String();
    private ProcessManifest manifest = null;
    private String mainEntryPointSig = new String();
    private static Manifest instance;

    private Manifest(String path) {
        appPath = Path.of(path);
    }

    public static Manifest getInstance(String path) {
        if (instance == null)
            instance = new Manifest(path);
        return instance;
    }

    public void process() throws IOException, XmlPullParserException {
        manifest = new ProcessManifest(appPath.toString());
        fileName = manifest.getPackageName();
        if (fileName == null)
            fileName = appPath.getFileName().toString();
        logger.info(String.format("Package Name: %s", fileName));

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

    public Set<String> getPermissions() {
        return manifest.getPermissions();
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
