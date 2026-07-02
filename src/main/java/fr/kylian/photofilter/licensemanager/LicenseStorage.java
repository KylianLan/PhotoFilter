package fr.kylian.photofilter.licensemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class LicenseStorage {

    private static File getLicenseFile() {
        String os = System.getProperty("os.name").toLowerCase();
        String saveDir;

        if (os.contains("win")) {
            saveDir = System.getenv("APPDATA") + File.separator + "PhotoFilter";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            saveDir = System.getProperty("user.home") + File.separator + ".config" + File.separator + "PhotoFilter";
        } else {
            saveDir = System.getProperty("user.home") + File.separator + ".PhotoFilter";
        }

        File dir = new File(saveDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return new File(dir, "license.key");
    }

    /**
     * Récupère la clé de licence sauvegardée.
     * @return La clé sous forme de String, ou null si elle n'existe pas.
     */
    public static String getSavedKey() {
        File file = getLicenseFile();
        if (!file.exists()) {
            return null;
        }
        try {
            return Files.readString(file.toPath()).trim();
        } catch (IOException e) {
            System.err.println("Erreur lors de la lecture de la clé de licence : " + e.getMessage());
            return null;
        }
    }

    /**
     * Sauvegarde la clé de licence.
     * @param key La clé à sauvegarder.
     */
    public static void saveKey(String key) {
        File file = getLicenseFile();
        try {
            Files.writeString(file.toPath(), key.trim());
        } catch (IOException e) {
            System.err.println("Erreur lors de la sauvegarde de la clé de licence : " + e.getMessage());
        }
    }

    /**
     * Supprime la clé de licence sauvegardée.
     */
    public static void removeKey() {
        File file = getLicenseFile();
        if (file.exists()) {
            file.delete();
        }
    }
}
