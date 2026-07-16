package fr.kylian.photofilter.licensemanager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class LicenseVerifier {

    // Remplace par la vraie URL de ton serveur Debian
    private static final String API_URL = "https://photofilter.fr/API/key-verifier.php";
    private static final ObjectMapper mapper = new ObjectMapper();

    private static boolean trialMode = false;

    public static boolean isTrialMode() {
        return trialMode;
    }

    public static void setTrialMode(boolean trial) {
        trialMode = trial;
    }

    /**
     * Vérifie la clé de licence auprès de l'API.
     * @param licenseKey La clé entrée par l'utilisateur
     * @return true si la licence est valide et l'appareil autorisé, false sinon.
     */
    public static boolean checkLicense(String licenseKey) {
        try {
            // 1. Récupération des identifiants de la machine
            String hwid = HardwareUtils.generateHWID();
            String deviceName = HardwareUtils.getDeviceName();

            // 2. Préparation des données (encodage URL pour éviter les bugs avec les espaces/caractères spéciaux)
            String requestBody = "key=" + URLEncoder.encode(licenseKey, StandardCharsets.UTF_8) +
                    "&hwid=" + URLEncoder.encode(hwid, StandardCharsets.UTF_8) +
                    "&device_name=" + URLEncoder.encode(deviceName, StandardCharsets.UTF_8);

            // 3. Création du client HTTP avec un timeout de sécurité (10s)
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            // 4. Création de la requête POST
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    // Ce header est OBLIGATOIRE pour que PHP puisse lire les données dans $_POST
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            // 5. Envoi de la requête et récupération de la réponse
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Vérification du code HTTP pour éviter de parser une erreur du serveur (ex: 500, 404)
            if (response.statusCode() != 200) {
                System.err.println("[API PhotoFilter] Erreur serveur. Code HTTP : " + response.statusCode());
                return false;
            }

            // 6. Lecture du JSON renvoyé par PHP avec Jackson
            JsonNode rootNode = mapper.readTree(response.body());
            boolean status = rootNode.path("status").asBoolean();
            String message = rootNode.path("message").asText();
            JsonNode dataNode = rootNode.path("data");

            // Affichage dans la console pour le debug
            System.out.println("[API PhotoFilter] Statut : " + (status ? "SUCCÈS" : "ÉCHEC"));
            System.out.println("[API PhotoFilter] Message serveur : " + message);
            
            // Traitement des données additionnelles si l'API renvoie des informations spécifiques
            if (status && !dataNode.isMissingNode() && !dataNode.isNull()) {
                String hwidStatus = dataNode.path("hwid").asText("");
                if ("recognized".equals(hwidStatus)) {
                    System.out.println("[API PhotoFilter] Info : Cet appareil est déjà enregistré.");
                } else if ("registered".equals(hwidStatus)) {
                    System.out.println("[API PhotoFilter] Info : Nouvel appareil enregistré avec succès.");
                }
            }

            return status;

        } catch (Exception e) {
            System.err.println("Erreur de communication avec le serveur de licences : " + e.getMessage());
            return false; // Par sécurité, si le serveur est inaccessible, on bloque l'accès
        }
    }
}