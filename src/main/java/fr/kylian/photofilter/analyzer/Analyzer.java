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

public class Analyzer {

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jpg", ".jpeg", ".png", ".heic", ".heif", ".webp", ".avif"
    ));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".mp4", ".mkv", ".mov"
    ));

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

    public LocalDateTime getFileCreationDate(File image) {
        if (isVideo(image))
            return readJsonMetadata(image);
            
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(image);
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

        return readJsonMetadata(image);
    }

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

    private boolean compareDates(LocalDateTime fileDateTime, LocalDate filter, FilterMode mode) {
        if (fileDateTime == null || filter == null) return false;
        
        LocalDate fileDate = fileDateTime.toLocalDate();
        
        return switch (mode) {
            case DAY -> fileDate.equals(filter);

            case MONTH -> (fileDate.getDayOfMonth() == filter.getDayOfMonth()
                    && fileDate.getMonthValue() == filter.getMonthValue());

            case YEAR -> fileDate.equals(filter);

            case YEAR_ONLY -> (fileDate.getYear() == filter.getYear());

            case MONTH_ONLY -> (fileDate.getMonthValue() == filter.getMonthValue());

            case MONTH_AND_YEAR -> (fileDate.getMonthValue() == filter.getMonthValue() 
                    && fileDate.getYear() == filter.getYear());
        };
    }

    public void putInFolder(List<File> files, String folderName, LocalDate filter, FilterMode mode) throws IOException {
        File destFolder = new File(folderName);
        if (!destFolder.exists()) {
            destFolder.mkdirs();
        }

        for (File f : files) {
            LocalDateTime creationDate = getFileCreationDate(f);
            if (compareDates(creationDate, filter, mode)) {
                File destFile = new File(destFolder, f.getName());
                Files.move(f.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public void main() throws IOException {
        File folder = new File("/home/kylian/Pictures/images");
        List<File> files = getFiles(folder);
        for (File f : files) {
            System.out.println(f.getName() + " " + getFileCreationDate(f));
        }
    }
}
