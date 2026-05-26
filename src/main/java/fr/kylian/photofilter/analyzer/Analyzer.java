package fr.kylian.photofilter.analyzer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.io.File;

import java.nio.file.DirectoryStream;
import java.util.*;

public class Analyzer {

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".heic", ".heif", ".webp"
    );

    public boolean isImageFile(File file) {
        if (file.isDirectory() || file.isHidden()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        for (String ext : IMAGE_EXTENSIONS) {
            if (name.endsWith(ext))
                return true;
        }
        return false;
    }

    private List<File> getFiles(final File folder) {
        List<File> files = new ArrayList<>();

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files.addAll(getFiles(fileEntry));
            } else if (isImageFile(fileEntry)) {
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
     * Compares two dates and returns true if the two given dates are the same depending on the filtering mode
     * filter date should be human-readable
     * fileDate should be a file's date
     */
    public boolean compareDates(Date fileDate, Date filter, FilterMode mode) {
        filter = new Date(filter.getYear() - 1900, filter.getMonth(), filter.getDate());
        return switch (mode) {
            case DAY -> (fileDate.getDate() == filter.getDate());

            case MONTH -> (fileDate.getDate() == filter.getDate()
                    && fileDate.getMonth() == filter.getMonth());

            case YEAR -> (fileDate.getDate() == filter.getDate()
                    && fileDate.getMonth() == filter.getMonth()
                    && fileDate.getYear() == filter.getYear());

            case YEAR_ONLY -> (fileDate.getYear() == filter.getYear());

            case MONTH_ONLY -> (fileDate.getMonth() == filter.getMonth());
        };
    }

    public void putInFolder(List<File> files, String folderName, Date filter, FilterMode mode) throws IOException {
        File destFolder = new File(folderName);
        destFolder.mkdir();

        for (File f : files) {
            if (compareDates(getFileCreationDate(f), filter, mode)) {
                f.renameTo(new File(destFolder.getAbsolutePath() + "/" + f.getName()));
            }
        }
    }

    public void main() throws IOException {
        File folder = new File("C:\\Users\\kylia\\Pictures\\Photos from 2014");
        List<File> files = getFiles(folder);
        for (File f : files) {
            System.out.println(f.getName() + " " + getFileCreationDate(f).getDate());
        }
        putInFolder(files, folder + "/Test", new Date(2014, Calendar.JUNE,2), FilterMode.MONTH_ONLY);
    }

}
