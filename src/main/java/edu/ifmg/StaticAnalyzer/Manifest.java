package edu.ifmg.StaticAnalyzer;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParserException;

import soot.jimple.infoflow.android.manifest.ProcessManifest;

public class Manifest {
    private static Logger logger = LoggerFactory.getLogger(Logger.class);
    private String packageName = "";
    private String appPath = "";

    public Manifest(String path) {
        appPath = path;
    }

    public void process() throws IOException, XmlPullParserException {
        ProcessManifest manifest = new ProcessManifest(appPath);
        packageName = manifest.getPackageName();
        logger.info("Package Name: " + packageName);
        manifest.close();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

}
