package edu.ifmg.StaticAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * ApkHandle is a class to encapsulate all features associated to the app
 * under analysis.
 *
 * @author Casperento
 *
 */
public class ApkHandler {
    private static ApkHandler instance;
    private int size;
    private List<String> permissions = new ArrayList<>();
    private double entropy;
    private List<String> leakingMethods = new ArrayList<>();

    public static ApkHandler getInstance() {
        if (ApkHandler.instance == null) {
            ApkHandler.instance = new ApkHandler();
        }
        return instance;
    }

    /**
     * <p>Setter method for the apk size.
     * </p>
     * @param size new apk size value
     * @return
     * @since 1.0
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * <p>Setter method for the list of permissions requested by the app.
     * </p>
     * @param permissions list of permissions requested by the app
     * @return
     * @since 1.0
     */
    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    /**
     * <p>Setter method for the apk's entropy value.
     * </p>
     * @param entropy new apk's entropy value
     * @return
     * @since 1.0
     */
    public void setEntropy(double entropy) {
        this.entropy = entropy;
    }

    /**
     * <p>Setter method for the list of sink methods that are related to a leak found by the Taint Analysis.
     * </p>
     * @param leakingMethods list of sink methods related to a leak found by the Taint Analysis
     * @return
     * @since 1.0
     */
    public void setLeakingMethods(List<String> leakingMethods) {
        this.leakingMethods = leakingMethods;
    }

    /**
     * <p>Getter method for apk's size.
     * </p>
     * @param
     * @return apk's size
     * @since 1.0
     */
    public int getSize() {
        return size;
    }

    /**
     * <p>Getter method for the list of permissions requested by the app.
     * </p>
     * @param
     * @return list of permissions requested by the app
     * @since 1.0
     */
    public List<String> getPermissions() {
        return permissions;
    }

    /**
     * <p>Getter method for the apk's entropy value.
     * </p>
     * @param
     * @return apk's entropy value
     * @since 1.0
     */
    public double getEntropy() {
        return entropy;
    }

    /**
     * <p>Getter method for the list of sink methods that are related to a leak found by the Taint Analysis.
     * </p>
     * @param
     * @return list of sink methods related to a leak found by the Taint Analysis
     * @since 1.0
     */
    public List<String> getLeakingMethods() {
        return leakingMethods;
    }
}
