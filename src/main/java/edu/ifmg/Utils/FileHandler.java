package edu.ifmg.Utils;

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
                if (line != null && !line.isEmpty())
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

    public static int getFileSize(String filePath) {
        int fileSize = -1;
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            fileSize = fileInputStream.available();
            fileInputStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return fileSize;
    }

    public static double getFileEntropy(String filePath) {
        double entropy = 0.0;
        try {
            FileInputStream fileInputStream = new FileInputStream(filePath);
            int fileSize = fileInputStream.available();
            int[] byteFrequency = new int[256]; // Array to hold byte frequency

            // Count the frequency of each byte value
            int byteRead;
            while ((byteRead = fileInputStream.read()) != -1) {
                byteFrequency[byteRead]++;
            }

            // Calculate the entropy
            for (int frequency : byteFrequency) {
                if (frequency > 0) {
                    double probability = (double) frequency / fileSize;
                    entropy -= probability * (Math.log(probability) / Math.log(2));
                }
            }

            fileInputStream.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return entropy;
    }

}
