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
import java.time.ZoneId;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.kylian.photofilter.filter.Filter;
import fr.kylian.photofilter.filter.FilterMode;
import fr.kylian.photofilter.filter.FilterRange;

/**
 * Core logic for analyzing photo/video metadata and organizing files.
 */
public class Analyzer {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".heic", ".heif", ".webp", ".avif"
    ));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".mp4", ".mkv", ".mov"
    ));

    /**
     * Checks if the given file is an image.
     * @param file The file to check.
     * @return true if the file is an image.
     */
    public boolean isImage(File file) {
        if (file.isDirectory() || file.isHidden())
            return false;

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex <= 0)
            return false;

        String ext = name.substring(dotIndex);
        return IMAGE_EXTENSIONS.contains(ext);
    }

    /**
     * Checks if the given file is a video.
     * @param file The file to check.
     * @return true if the file is a video.
     */
    public boolean isVideo(File file) {
        if (file.isDirectory() || file.isHidden())
            return false;

        String name = file.getName().toLowerCase();
        int dotIndex = name.lastIndexOf(".");

        if (dotIndex <= 0)
            return false;

        String ext = name.substring(dotIndex);
        return VIDEO_EXTENSIONS.contains(ext);
    }

    /**
     * Recursively scans a folder for image and video files.
     * @param folder The folder to scan.
     * @return List of files found.
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
     * Retrieves the creation date of a file from its metadata.
     * @param file The file to analyze.
     * @return The creation date, or null if not found.
     */
    public LocalDate getFileCreationDate(File file) {
        if (isVideo(file))
            return readJsonMetadata(file);
            
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory != null) {
                Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                }
            }
        } catch (ImageProcessingException | IOException e) {
            System.err.println("Error while reading EXIF metadata: " + e.getMessage());
        }

        return readJsonMetadata(file);
    }

    /**
     * Reads creation date from a sidecar JSON file (common in Google Takeout).
     * @param image The original file.
     * @return The creation date from JSON, or null if not found.
     */
    private LocalDate readJsonMetadata(File image) {
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
                return LocalDate.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault());
            }
        } catch (Exception e) {
            System.err.println("Error while fetching metadata from JSON file for " + image.getAbsolutePath());
        }

        return null;
    }

    /**
     * Compares a file's creation date against a filter's criteria.
     * @param fileDate The file's creation date.
     * @param filter The filter to check against.
     * @return true if the file matches the filter.
     */
    private boolean compareDates(LocalDate fileDate, Filter filter) {
        if (fileDate == null || filter == null) return false;

        FilterMode[] params = filter.getFilterParams();
        if (params == null) return false;

        List<FilterMode> paramList = Arrays.asList(params);

        // 1. Si c'est une plage de dates (RANGE)
        if (filter.getFilterRange() == FilterRange.RANGE && filter.getSecondDate().isPresent()) {
            LocalDate startDate = filter.getDate();
            LocalDate endDate = filter.getSecondDate().get();

            // On vérifie simplement si la date du fichier est comprise entre le début et la fin
            // (On suppose ici que pour un range, on compare la date complète)
            return !fileDate.isBefore(startDate) && !fileDate.isAfter(endDate);
        }

        // 2. Si c'est une date unique (SINGLE_DATE), on garde ta logique avec les paramètres
        boolean matchesDay = true;
        boolean matchesMonth = true;
        boolean matchesYear = true;

        if (paramList.contains(FilterMode.DAY)) {
            matchesDay = (fileDate.getDayOfMonth() == filter.getDate().getDayOfMonth());
        }
        if (paramList.contains(FilterMode.MONTH)) {
            matchesMonth = (fileDate.getMonthValue() == filter.getDate().getMonthValue());
        }
        if (paramList.contains(FilterMode.YEAR)) {
            matchesYear = (fileDate.getYear() == filter.getDate().getYear());
        }

        return (matchesDay && matchesMonth && matchesYear);
    }

    /**
     * Moves files into folders based on the specified filter.
     * @param files The list of files to process.
     * @param filter The filter to apply.
     * @throws IOException If a file operation fails.
     */
    public void putInFolder(List<File> files, Filter filter) throws IOException {
        File destFolder = new File(filter.getName());
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        for (File f : files) {
            LocalDate creationDate = getFileCreationDate(f);
            if (compareDates(creationDate, filter)) {
                File destFile = new File(destFolder, f.getName());
                Files.move(f.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
