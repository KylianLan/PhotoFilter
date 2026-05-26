package fr.kylian.photofilter.analyzer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.io.File;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    public Date getFileCreationDate(File image) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(image);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date;
                }
            }
        } catch (ImageProcessingException | IOException e) {
            System.err.println("Error while reading metadata: " + e.getMessage());
        }

        return null;
    }

    public void putInFolder(List<File> files, String folderName, Date filter) throws IOException {
        if (!files.contains(new File(folderName))) {
            new File(folderName).mkdir();
        }
        for (File f : files) {

        }
    }

    public void main() throws IOException {
        File folder = new File("/home/kylian/Pictures/images");
        List<File> files = getFiles(folder);
        for (File f : files) {
            System.out.println(f.getName() + " " + getFileCreationDate(f));
        }
        putInFolder(files, folder + "/test", new Date(2016, Calendar.AUGUST,12));
        System.out.println(folder + "/test");
    }

}
