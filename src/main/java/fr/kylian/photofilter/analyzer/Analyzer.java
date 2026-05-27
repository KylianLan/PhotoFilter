package fr.kylian.photofilter.analyzer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.io.File;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Analyzer {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<String>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".heic", ".heif", ".webp", ".avif"
    ));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<String>(Arrays.asList(
            ".mp4", ".mkv", ".mov"
    ));

    public boolean isImage(File file) {
        if (file.isDirectory() || file.isHidden())  // Verifies if the file is a folder or hidden, ignores it if so
            return false;

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex <= 0) // Verifies if there is a dot in the file's name in order to check if the file has an extension
            return false;

        String ext = name.substring(dotIndex).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext); // Only returns true if the file is a supported image
    }

    public boolean isVideo(File file) {
        if (file.isDirectory() || file.isHidden())  // Verifies if the file is a folder or hidden, ignores it if so
            return false;

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex <= 0) // Verifies if there is a dot in the file's name in order to check if the file has an extension
            return false;

        String ext = name.substring(dotIndex).toLowerCase();
        return VIDEO_EXTENSIONS.contains(ext); // Only returns true if the file is a supported video
    }

    private List<File> getFiles(final File folder) {
        List<File> files = new ArrayList<>();

        for (final File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                files.addAll(getFiles(fileEntry));
            } else if (isImage(fileEntry) || isVideo(fileEntry)) {
                files.add(fileEntry);
            }
        }

        return files;
    }

    public Date getFileCreationDate(File image) {
        if (isVideo(image))
            return readJsonMetadata(image); // If the given file is a video, automatically read json file
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

        return readJsonMetadata(image); // null if Original Date Time does not exist
    }

    private Date readJsonMetadata(File image) {
        File parent = image.getParentFile();
        if (parent == null || !parent.isDirectory()) {
            return null;
        }

        String fileName = image.getName();
        File[] matchingFiles = parent.listFiles((dir, name) -> name.startsWith(fileName) && name.endsWith(".json"));

        if (matchingFiles == null || matchingFiles.length == 0) {
            return null;
        }

        File jsonFile = matchingFiles[0]; // Take the first match

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonFile);

            JsonNode timestampNode = root.path("photoTakenTime").path("timestamp");
            if (!timestampNode.isMissingNode()) {
                long seconds = Long.parseLong(timestampNode.asText());
                return new Date(seconds * 1000);
            }
        } catch (Exception e) {
            System.err.println("Error while fetching metadata from JSON file for " + image.getAbsolutePath());
        }

        return null;
    }

    /**
     * Compares two dates and returns true if the two given dates are the same depending on the filtering mode
     * @param fileDate should be a file's date
     * @param filter should be a human-readable date
     * @param mode desired filtering mode
     * @return true if fileDate matches filter based on the mode
     */
    private boolean compareDates(Date fileDate, Date filter, FilterMode mode) {
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

            case MONTH_AND_YEAR -> (fileDate.getMonth() == filter.getMonth() && fileDate.getYear() == filter.getYear());
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
        // Testing purposes only
//        File folder = new File("C:\\Users\\kylia\\Pictures\\Photos from 2014");
        File folder = new File("/home/kylian/Pictures/images");
        List<File> files = getFiles(folder);
        for (File f : files) {
            System.out.println(f.getName() + " " + getFileCreationDate(f));
        }
//        putInFolder(files, folder + "/Test", new Date(2014, Calendar.JUNE,2), FilterMode.MONTH_ONLY); // Windows
//        putInFolder(files, folder + "/Test", new Date(2026, Calendar.FEBRUARY,5), FilterMode.MONTH_AND_YEAR); // Linux
    }

}
