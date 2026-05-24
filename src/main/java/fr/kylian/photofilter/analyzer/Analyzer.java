package fr.kylian.photofilter.analyzer;

import java.awt.Image;
import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;

/**
 *
 */
public class Analyzer {

    private List<File> getFiles(final File folder) {
        List<File> files = new ArrayList<>();

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files.addAll(getFiles(fileEntry));
            } else {
                files.add(fileEntry);
            }
        }

        return files;
    }

    private FileTime getFileCreationDate(final File file) throws IOException {
        FileTime date;

        BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        date = attributes.lastModifiedTime();

        return date;
    }

    public void main() throws IOException {
        File folder = new File("/home/kylian/Pictures/images");
        List<File> files = getFiles(folder);
        for (File f : files) {
            System.out.println(f.getName() + " " + getFileCreationDate(f));
        }
    }

}
