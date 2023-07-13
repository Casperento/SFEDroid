package edu.ifmg.FileExport;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

public class FileExport {
    private Path filePath = Path.of("");
    public void export(String data) throws RuntimeException {
        if (filePath.toString().equals("")) {
            throw new RuntimeException();
        }

        byte[] bytes = data.getBytes();
        try {
            FileOutputStream outFile = new FileOutputStream(filePath.toFile());
            outFile.write(bytes);
            outFile.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setFilePath(String name) {
        this.filePath = Path.of(name);
    }

    public Path getFilePath() {
        return filePath;
    }
}
