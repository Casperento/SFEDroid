package edu.ifmg.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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

    public static double getFileEntropy(InputStream fileInputStream) {
        double entropy = 0.0;
        try {
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

    public static InputStream getDexFileFromApk(String apkPath) {
        String targetFileName = "classes.dex";
        try {
            ZipFile zipFile = new ZipFile(apkPath);
            ZipEntry targetEntry = zipFile.getEntry(targetFileName);
            if (targetEntry != null)
                return zipFile.getInputStream(targetEntry);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    public static void appendContentToFile(String filePath, String content) {
        try {
            FileWriter fileWriter = new FileWriter(filePath, true);
            fileWriter.write(content);
            fileWriter.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public static void deleteFolder(String folderPath) {
        Path path = (new File(folderPath)).toPath();
        if (!Files.exists(path)) {
            logger.info("Folder being deleted does not exist on disk...");
            return;
        }
        try {
            if (Files.list(path).findAny().isEmpty()) {
                Files.delete(path);
            } else {
                deleteFilesRecursively(path);
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    private static void deleteFilesRecursively(Path path) throws IOException {
        if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry: entries) {
                    deleteFilesRecursively(entry);
                }
            }
        }
        Files.delete(path);
    }

}
