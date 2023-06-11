package edu.ifmg.FileExport;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileExport {
    private String fileName;
    public FileExport(String name) {
        fileName = name;
    }
    public int export(String data) {
        byte[] bytes = data.getBytes();
        try {
            FileOutputStream outFile = new FileOutputStream(fileName);
            outFile.write(bytes);
            outFile.close();
            return 0;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
