package edu.ifmg.StaticAnalyzer;

import java.util.ArrayList;
import java.util.List;

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

    public void setSize(int size) {
        this.size = size;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public void setEntropy(double entropy) {
        this.entropy = entropy;
    }

    public void setLeakingMethods(List<String> leakingMethods) {
        this.leakingMethods = leakingMethods;
    }

    public int getSize() {
        return size;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public double getEntropy() {
        return entropy;
    }

    public List<String> getLeakingMethods() {
        return leakingMethods;
    }
}
