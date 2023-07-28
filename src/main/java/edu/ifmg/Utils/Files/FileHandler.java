package edu.ifmg.Utils.Files;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHandler {
    private static Logger logger = LoggerFactory.getLogger(Logger.class);
    private File filePath = null;

    public void export(String data) throws IOException {
        if (filePath.toString().equals("")) {
            throw new RuntimeException();
        }

        byte[] bytes = data.getBytes();
        FileOutputStream outFile = new FileOutputStream(filePath);
        outFile.write(bytes);
        outFile.close();
    }

    public void setFilePath(String outputPath, String fileName) {
        logger.info(String.format("Files are being exported into: %s", outputPath));
        filePath = new File(outputPath, fileName);
    }
}
