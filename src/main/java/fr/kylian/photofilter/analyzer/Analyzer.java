package fr.kylian.photofilter.analyzer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.IOException;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kylian.photofilter.filter.Filter;

public class Analyzer {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".heic", ".heif", ".webp", ".avif"
    ));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".mp4", ".mkv", ".mov"
    ));

    /**
     * Checks if the given file is an image or not
     * @param file the file to be checked
     * @return true if file is an image
     */
    public boolean isImage(File file) {
        if (file.isDirectory() || file.isHidden())
            return false;

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex <= 0)
            return false;

        String ext = name.substring(dotIndex).toLowerCase();
        return IMAGE_EXTENSIONS.contains(ext);
    }

    /**
     * Checks if the given file is a video or not
     * @param file the file to be checked
     * @return true if file is a video
     */
    public boolean isVideo(File file) {
        if (file.isDirectory() || file.isHidden())
            return false;

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex <= 0)
            return false;

        String ext = name.substring(dotIndex).toLowerCase();
        return VIDEO_EXTENSIONS.contains(ext);
    }

    /**
     * Analyzes a folder and returns every files in it recursively
     * @param folder the folder to be analyzed
     * @return List<File> = a list of every file in the source folder
     */
    private List<File> getFiles(final File folder) {
        List<File> files = new ArrayList<>();
        File[] entries = folder.listFiles();
        
        if (entries == null) return files;

        for (final File fileEntry : entries) {
            if (fileEntry.isDirectory()) {
                files.addAll(getFiles(fileEntry));
            } else if (isImage(fileEntry) || isVideo(fileEntry)) {
                files.add(fileEntry);
            }
        }

        return files;
    }

    /**
     * Gives the creation date of a file
     * @param file the file to be analyzed
     * @return LocalDateTime = the time photo/video was taken
     */
    public LocalDateTime getFileCreationDate(File file) {
        if (isVideo(file))
            return readJsonMetadata(file);
            
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                }
            }
        } catch (ImageProcessingException | IOException e) {
            System.err.println("Error while reading metadata: " + e.getMessage());
        }

        return readJsonMetadata(file);
    }

    /**
     * Used if cannot get file metadata directly. Especially useful when analyzing a Google Takeout folder
     * @param image the source image which metadata json file will be analyzed
     * @return LocalDateTime = the time the photo/video was taken
     */
    private LocalDateTime readJsonMetadata(File image) {
        File parent = image.getParentFile();
        if (parent == null || !parent.isDirectory()) {
            return null;
        }

        String fileName = image.getName();
        File[] matchingFiles = parent.listFiles((dir, name) -> name.startsWith(fileName) && name.endsWith(".json"));

        if (matchingFiles == null || matchingFiles.length == 0) {
            return null;
        }

        File jsonFile = matchingFiles[0];

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(jsonFile);

            JsonNode timestampNode = root.path("photoTakenTime").path("timestamp");
            if (!timestampNode.isMissingNode()) {
                long seconds = Long.parseLong(timestampNode.asText());
                return LocalDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
            }
        } catch (Exception e) {
            System.err.println("Error while fetching metadata from JSON file for " + image.getAbsolutePath());
        }

        return null;
    }

    /**
     * Compares dates depending on a filtering type
     * @param fileDateTime the file's creation date
     * @param filter the filtering date
     * @param mode the filter mode (FilterMode)
     * @return true if the two given dates are the same depending on the filtering mode
     */
    private boolean compareDates(LocalDateTime fileDateTime, LocalDate filter, FilterMode mode) {
        if (fileDateTime == null || filter == null) return false;
        
        LocalDate fileDate = fileDateTime.toLocalDate();
        
        return switch (mode) {

            case YEAR_ONLY -> (fileDate.getYear() == filter.getYear());

            case MONTH_ONLY -> (fileDate.getMonthValue() == filter.getMonthValue());

            case DAY_ONLY -> (fileDate.getDayOfMonth() == filter.getDayOfMonth());

            case MONTH_AND_YEAR -> (fileDate.getMonthValue() == filter.getMonthValue()
                    && fileDate.getYear() == filter.getYear());

            case DAY_AND_MONTH -> (fileDate.getDayOfMonth() == filter.getDayOfMonth() &&
                    fileDate.getMonthValue() == filter.getMonthValue());

            case ALL -> (fileDate.equals(filter));
        };
    }

    /**
     * Automatically moves files from a root folder depending on a filter
     * @param files the root folder in which the images are
     * @param filter the filter to be used (contains the name of the folder to be created if doesn't exist, the date, and the filtering mode)
     * @throws IOException
     */
    public void putInFolder(List<File> files, Filter filter) throws IOException {
        File destFolder = new File(filter.getName());
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        for (File f : files) {
            LocalDateTime creationDate = getFileCreationDate(f);
            if (compareDates(creationDate, filter.getDate(), filter.getFilterMode())) {
                File destFile = new File(destFolder, f.getName());
                Files.move(f.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
