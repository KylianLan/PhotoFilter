package fr.kylian.photofilter.analyzer;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;
import fr.kylian.photofilter.filter.Filter;
import fr.kylian.photofilter.filter.FilterMode;
import fr.kylian.photofilter.filter.FilterRange;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

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

    // Jackson ObjectMapper réutilisé pour de meilleures performances
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    public List<File> getFiles(final File folder) {
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
     * Helper de sécurité contre la vulnérabilité Zip/Archive Slip.
     */
    private void checkArchiveSlip(File targetDir, File newFile) throws IOException {
        String canonicalTargetDir = targetDir.getCanonicalPath();
        String canonicalNewFile = newFile.getCanonicalPath();
        if (!canonicalNewFile.startsWith(canonicalTargetDir + File.separator) && !canonicalNewFile.equals(canonicalTargetDir)) {
            throw new IOException("Fichier d'archive malveillant (Zip/Path Slip détecté) : " + newFile.getName());
        }
    }

    /**
     * Normalise les séparateurs de sous-dossiers (Windows \ vers Linux /).
     */
    private String sanitizeEntryName(String entryName) {
        if (entryName == null) return "";
        return entryName.replace('\\', '/');
    }

    /**
     * Extracts an archive file into the target directory, notifying progress via callback (bytesRead, totalBytes).
     * @param archive The archive file to extract.
     * @param targetDir The destination directory.
     * @param onProgress Callback receiving (bytesRead, totalBytes).
     * @throws IOException If an I/O error occurs or if a file path is insecure (Zip Slip vulnerability).
     */
    public void decompressArchive(final File archive, File targetDir, java.util.function.BiConsumer<Long, Long> onProgress) throws IOException, RarException {
        String name = archive.getName().toLowerCase();
        byte[] buffer = new byte[8192];
        long totalBytes = archive.length();
        long[] bytesRead = {0};

        if (name.endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(new FileInputStream(archive))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    File newFile = new File(targetDir, sanitizeEntryName(zipEntry.getName()));
                    checkArchiveSlip(targetDir, newFile);

                    if (zipEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier : " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier parent : " + parent);
                        }

                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                                bytesRead[0] += len;
                                if (onProgress != null) {
                                    onProgress.accept(bytesRead[0], totalBytes);
                                }
                            }
                        }
                    }
                    zis.closeEntry();
                    zipEntry = zis.getNextEntry();
                }
            }
        } else if (name.endsWith(".7z")) {
            try (SevenZFile sevenZFile = SevenZFile.builder().setFile(archive).get()) {
                SevenZArchiveEntry entry = sevenZFile.getNextEntry();
                while (entry != null) {
                    File newFile = new File(targetDir, sanitizeEntryName(entry.getName()));
                    checkArchiveSlip(targetDir, newFile);

                    if (entry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier : " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier parent : " + parent);
                        }

                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = sevenZFile.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                                bytesRead[0] += len;
                                if (onProgress != null) {
                                    onProgress.accept(bytesRead[0], totalBytes);
                                }
                            }
                        }
                    }
                    entry = sevenZFile.getNextEntry();
                }
            }
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            try (InputStream fis = new FileInputStream(archive);
                 InputStream gzis = new GzipCompressorInputStream(fis);
                 TarArchiveInputStream tais = new TarArchiveInputStream(gzis)) {

                TarArchiveEntry entry = tais.getNextEntry();
                while (entry != null) {
                    File newFile = new File(targetDir, sanitizeEntryName(entry.getName()));
                    checkArchiveSlip(targetDir, newFile);

                    if (entry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier : " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier parent : " + parent);
                        }

                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            int len;
                            while ((len = tais.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                                bytesRead[0] += len;
                                if (onProgress != null) {
                                    onProgress.accept(bytesRead[0], totalBytes);
                                }
                            }
                        }
                    }
                    entry = tais.getNextEntry();
                }
            }
        } else if (name.endsWith(".rar")) {
            try (Archive rarArchive = new Archive(archive)) {
                FileHeader fileHeader = rarArchive.nextFileHeader();

                while (fileHeader != null) {
                    String rawName = fileHeader.isUnicode() ? fileHeader.getFileNameW() : fileHeader.getFileNameString();
                    File newFile = new File(targetDir, sanitizeEntryName(rawName.trim()));
                    checkArchiveSlip(targetDir, newFile);

                    if (fileHeader.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier : " + newFile);
                        }
                    } else {
                        File parent = newFile.getParentFile();
                        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
                            throw new IOException("Impossible de créer le dossier parent : " + parent);
                        }

                        try (FileOutputStream fos = new FileOutputStream(newFile)) {
                            rarArchive.extractFile(fileHeader, fos);
                        }
                        bytesRead[0] += fileHeader.getUnpSize();
                        if (onProgress != null) {
                            onProgress.accept(bytesRead[0], totalBytes);
                        }
                    }
                    fileHeader = rarArchive.nextFileHeader();
                }
            }
        }
    }

    public void decompressArchive(final File archive, File targetDir) throws IOException, RarException {
        decompressArchive(archive, targetDir, null);
    }

    /**
     * Retrieves the creation date of a file from its metadata, JSON takeout,
     * or fallback filesystem attributes.
     * @param file The file to analyze.
     * @return The creation date, or a filesystem/current fallback.
     */
    public LocalDate getFileCreationDate(File file) {
        if (isVideo(file)) {
            LocalDate date = readJsonMetadata(file);
            return date != null ? date : getFilesystemCreationDate(file);
        }

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
            // Silently fall back
        }

        LocalDate takeoutDate = readJsonMetadata(file);
        if (takeoutDate != null) return takeoutDate;

        return getFilesystemCreationDate(file);
    }

    /**
     * Fallback to retrieve the filesystem-level creation date or last modified date.
     */
    private LocalDate getFilesystemCreationDate(File file) {
        try {
            java.nio.file.attribute.BasicFileAttributes attrs = Files.readAttributes(file.toPath(), java.nio.file.attribute.BasicFileAttributes.class);
            Instant instant = attrs.creationTime().toInstant();
            if (attrs.lastModifiedTime().toInstant().isBefore(instant)) {
                instant = attrs.lastModifiedTime().toInstant();
            }
            return instant.atZone(ZoneId.systemDefault()).toLocalDate();
        } catch (IOException e) {
            return LocalDate.now(); // Worst case fallback
        }
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
            JsonNode root = OBJECT_MAPPER.readTree(jsonFile);
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

            // Si l'année n'est pas cochée dans les paramètres, on ignore l'année pour la comparaison de plage
            if (!paramList.contains(FilterMode.YEAR)) {
                int defaultYear = 2000;

                LocalDate fileDateNormalized;
                try {
                    fileDateNormalized = fileDate.withYear(defaultYear);
                } catch (java.time.DateTimeException e) {
                    fileDateNormalized = fileDate.withMonth(2).withDayOfMonth(28).withYear(defaultYear);
                }

                LocalDate startDateNormalized;
                try {
                    startDateNormalized = startDate.withYear(defaultYear);
                } catch (java.time.DateTimeException e) {
                    startDateNormalized = startDate.withMonth(2).withDayOfMonth(28).withYear(defaultYear);
                }

                LocalDate endDateNormalized;
                try {
                    endDateNormalized = endDate.withYear(defaultYear);
                } catch (java.time.DateTimeException e) {
                    endDateNormalized = endDate.withMonth(2).withDayOfMonth(28).withYear(defaultYear);
                }

                // Si la plage de fin est avant le début (ex: du 28 déc au 3 jan normalized dans la même année)
                if (endDateNormalized.isBefore(startDateNormalized)) {
                    return !fileDateNormalized.isBefore(startDateNormalized) || !fileDateNormalized.isAfter(endDateNormalized);
                } else {
                    return !fileDateNormalized.isBefore(startDateNormalized) && !fileDateNormalized.isAfter(endDateNormalized);
                }
            } else {
                // Si l'année est cochée, on compare la date complète
                return !fileDate.isBefore(startDate) && !fileDate.isAfter(endDate);
            }
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
     * @param baseFolder The folder in which the files will be put in after process
     * @param files The list of files to process.
     * @param filter The filter to apply.
     * @throws IOException If a file operation fails.
     */
    public void putInFolder(File baseFolder, List<File> files, Filter filter) throws IOException {
        putInFolder(baseFolder, files, filter, null);
    }

    /**
     * Moves files into folders based on the specified filter, with a progress callback.
     * @param baseFolder The folder in which the files will be put in after process
     * @param files The list of files to process.
     * @param filter The filter to apply.
     * @param onProgress Callback executed after each file is processed.
     * @throws IOException If a file operation fails.
     */
    public void putInFolder(File baseFolder, List<File> files, Filter filter, Runnable onProgress) throws IOException {
        File destFolder = new File(baseFolder, filter.getName());

        for (File f : files) {
            LocalDate creationDate = getFileCreationDate(f);
            if (compareDates(creationDate, filter)) {

                if (!destFolder.exists()) {
                    destFolder.mkdirs();
                }

                File destFile = new File(destFolder, f.getName());
                if (destFile.exists()) {
                    if (Files.mismatch(f.toPath(), destFile.toPath()) == -1L) {
                        // The files are identical; delete the source file to complete the move without duplicating
                        Files.delete(f.toPath());
                    } else {
                        // The files are different; find a unique name
                        String name = f.getName();
                        String baseName = name;
                        String extension = "";
                        int dotIndex = name.lastIndexOf('.');
                        if (dotIndex > 0) {
                            baseName = name.substring(0, dotIndex);
                            extension = name.substring(dotIndex);
                        }

                        int counter = 1;
                        File uniqueDestFile = destFile;
                        while (uniqueDestFile.exists()) {
                            uniqueDestFile = new File(destFolder, baseName + " (" + counter + ")" + extension);
                            counter++;
                        }
                        Files.move(f.toPath(), uniqueDestFile.toPath());
                    }
                } else {
                    Files.move(f.toPath(), destFile.toPath());
                }
            }
            if (onProgress != null) {
                onProgress.run();
            }
        }
    }
}
