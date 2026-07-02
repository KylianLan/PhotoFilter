package fr.kylian.photofilter.licensemanager;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.ComputerSystem;
import oshi.hardware.NetworkIF;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HardwareUtils {

    /**
     * Récupère l'UUID unique de la carte mère (identique sur Windows et Linux).
     */
    public static String getSystemUUID() {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        // 1. L'ID du processeur
        String cpuId = hal.getProcessor().getProcessorIdentifier().getProcessorID();

        // 2. Récupération et tri de toutes les adresses MAC physiques
        List<String> macAddresses = new ArrayList<>();
        List<NetworkIF> networkIFs = hal.getNetworkIFs();

        for (NetworkIF net : networkIFs) {
            String mac = net.getMacaddr();
            String name = net.getName().toLowerCase();

            // On filtre drastiquement :
            // - L'adresse MAC doit faire 17 caractères (format standard XX:XX:XX:XX:XX:XX)
            // - On exclut les boucles locales (lo), les VM, Docker, etc.
            if (mac != null && mac.length() == 17
                    && !name.contains("lo")
                    && !name.contains("docker")
                    && !name.contains("vbox")
                    && !name.contains("vmware")) {

                macAddresses.add(mac);
            }
        }

        // On trie la liste pour que l'ordre soit identique peu importe l'OS
        Collections.sort(macAddresses);

        // On fusionne toutes les adresses MAC valides trouvées
        String combinedMacs = String.join("_", macAddresses);
        if (combinedMacs.isEmpty()) {
            combinedMacs = "no_mac_found";
        }

        // 3. Le HWID final : CPU + Toutes les MAC physiques
        return cpuId + "-" + combinedMacs;
    }

    /**
     * Hache une chaîne en SHA-256 (pour anonymiser et formater le HWID).
     */
    public static String generateHWID() {
        try {
            String rawUUID = getSystemUUID();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(rawUUID.getBytes(StandardCharsets.UTF_8));

            // Conversion des octets en chaîne hexadécimale
            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors du hachage du HWID", e);
        }
    }

    /**
     * Optionnel : Récupère le nom de la machine (ex: "PC-Kylian") 
     * pour l'envoyer au paramètre device_name de ton API.
     */
    public static String getDeviceName() {
        SystemInfo si = new SystemInfo();
        return si.getOperatingSystem().getNetworkParams().getHostName();
    }
}