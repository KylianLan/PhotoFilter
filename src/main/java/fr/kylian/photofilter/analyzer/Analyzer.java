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

    /**
     * Compares two dates
     * Returns 0 if the dates are the same, less than 0 if d1 is before d2, or more than 0 if d1 is after d2.
     */
    public int compareDates(Date d1, Date d2) {
        
    }

    public void putInFolder(List<File> files, String folderName, Date filter) throws IOException {
        File destFolder = new File(folderName);
        if (!files.contains(destFolder)) {
            destFolder.mkdir();
        }
        for (File f : files) {
            if (getFileCreationDate(f).compareTo(filter) == 0) {
                f.renameTo(new File(destFolder.getAbsolutePath() + "/" + f.getName()));
            }
        }
    }

    public void main() throws IOException {
        File folder = new File("/home/kylian/Pictures/images");
        List<File> files = getFiles(folder);
        for (File f : files) {
            System.out.println(f.getName() + " " + getFileCreationDate(f));
        }
        putInFolder(files, folder + "/test", new Date(2024, Calendar.JANUARY,27));
    }

}
