package edu.ifmg.Utils.Files;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FileHandler is a class used to import/export files from/to the disk.
 *
 * @author Casperento
 *
 */
public class FileHandler {
    private static final Logger logger = LoggerFactory.getLogger(FileHandler.class);

    public static List<String> importFile(String inputPath) {
        List<String> fileContent = new ArrayList<>();
        String line = new String();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputPath));
            while (line != null) {
                line = reader.readLine();
                fileContent.add(line);
            }
            reader.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
            return null;
        }
        return fileContent;
    }

    public static void exportFile(String content, String outputPath, String fileName) throws IOException {
        if (content.isEmpty() || outputPath.isEmpty() || fileName.isEmpty())
            throw new RuntimeException();

        File filePath = new File(outputPath, fileName);
        logger.debug(String.format("Exporting content to %s file", filePath.getAbsoluteFile()));

        byte[] bytes = content.getBytes();
        FileOutputStream outFile = new FileOutputStream(filePath);
        outFile.write(bytes);
        outFile.close();
    }
    
}
