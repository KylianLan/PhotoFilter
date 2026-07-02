package fr.kylian.photofilter;

import fr.kylian.photofilter.licensemanager.LicenseVerifier;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;
import java.util.prefs.Preferences;

public class PhotoFilterApplication extends Application {

    private static final String PREF_LICENSE_KEY = "license_key";

    @Override
    public void start(Stage stage) throws IOException {
        if (!verifyLicense()) {
            Platform.exit();
            return;
        }

        FXMLLoader fxmlLoader = new FXMLLoader(PhotoFilterApplication.class.getResource("PhotoFilter.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("PhotoFilter");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    /**
     * Vérifie la licence au démarrage. Demande une clé si aucune n'est sauvegardée ou si elle est invalide.
     * @return true si la licence est valide, false si l'utilisateur annule l'opération.
     */
    private boolean verifyLicense() {
        // Utilisation de Preferences pour sauvegarder la clé dans le registre/système de manière persistante
        Preferences prefs = Preferences.userNodeForPackage(PhotoFilterApplication.class);
        String savedKey = prefs.get(PREF_LICENSE_KEY, null);

        // Vérifie la clé existante si elle a déjà été sauvegardée
        if (savedKey != null && !savedKey.trim().isEmpty()) {
            System.out.println("Vérification de la clé de licence sauvegardée...");
            if (LicenseVerifier.checkLicense(savedKey)) {
                return true;
            }
        }

        // Boucle pour redemander la clé tant qu'elle est invalide
        while (true) {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Vérification de la licence");
            dialog.setHeaderText("Une clé de licence est requise pour utiliser PhotoFilter.");
            dialog.setContentText("Veuillez entrer votre clé de licence :");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String key = result.get().trim();
                if (key.isEmpty()) {
                    showError("La clé de licence ne peut pas être vide.");
                    continue;
                }

                System.out.println("Vérification de la nouvelle clé saisie...");
                if (LicenseVerifier.checkLicense(key)) {
                    // Sauvegarde la clé valide pour les prochains lancements
                    prefs.put(PREF_LICENSE_KEY, key);
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION);
                    success.setTitle("Licence valide");
                    success.setHeaderText(null);
                    success.setContentText("Votre clé de licence a été validée avec succès !");
                    success.showAndWait();
                    
                    return true;
                } else {
                    showError("La clé de licence est invalide ou cet appareil n'est pas autorisé.");
                }
            } else {
                // L'utilisateur a cliqué sur "Annuler" ou fermé la fenêtre
                return false;
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur de licence");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
