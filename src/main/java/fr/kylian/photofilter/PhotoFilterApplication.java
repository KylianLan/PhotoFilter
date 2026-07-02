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
    public void start(Stage stage) {
        // On lance le flux asynchrone de vérification de licence.
        // L'interface principale ne s'affichera que si la licence est valide.
        startLicenseCheckFlow(stage);
    }

    private void startLicenseCheckFlow(Stage mainStage) {
        Preferences prefs = Preferences.userNodeForPackage(PhotoFilterApplication.class);
        String savedKey = prefs.get(PREF_LICENSE_KEY, null);

        if (savedKey != null && !savedKey.trim().isEmpty()) {
            System.out.println("Vérification de la clé de licence sauvegardée en arrière-plan...");
            verifyKeyAsync(savedKey, true, mainStage, prefs);
        } else {
            promptForKey(mainStage, prefs);
        }
    }

    private void promptForKey(Stage mainStage, Preferences prefs) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Vérification de la licence");
        dialog.setHeaderText("Une clé de licence est requise pour utiliser PhotoFilter.");
        dialog.setContentText("Veuillez entrer votre clé de licence :");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String key = result.get().trim();
            if (key.isEmpty()) {
                showError("La clé de licence ne peut pas être vide.");
                promptForKey(mainStage, prefs); // On redemande récursivement
                return;
            }

            System.out.println("Vérification de la nouvelle clé saisie en arrière-plan...");
            verifyKeyAsync(key, false, mainStage, prefs);
        } else {
            // L'utilisateur a cliqué sur "Annuler" ou fermé la fenêtre
            Platform.exit();
        }
    }

    private void verifyKeyAsync(String key, boolean isSavedKey, Stage mainStage, Preferences prefs) {
        // Exécution de la vérification HTTP dans un thread séparé (supplyAsync) pour ne pas freezer l'UI
        java.util.concurrent.CompletableFuture.supplyAsync(() -> LicenseVerifier.checkLicense(key))
            .thenAcceptAsync(isValid -> {
                // Ce bloc s'exécute sur le thread JavaFX (grâce à Platform::runLater) quand le HTTP est terminé
                if (isValid) {
                    prefs.put(PREF_LICENSE_KEY, key);
                    
                    if (!isSavedKey) {
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.setTitle("Licence valide");
                        success.setHeaderText(null);
                        success.setContentText("Votre clé de licence a été validée avec succès !");
                        success.showAndWait();
                    }
                    
                    showMainApp(mainStage);
                } else {
                    if (isSavedKey) {
                        // Si la clé sauvegardée a expiré ou n'est plus valide, on la supprime
                        prefs.remove(PREF_LICENSE_KEY);
                    } else {
                        showError("La clé de licence est invalide, a expiré, ou le serveur est inaccessible.");
                    }
                    // On redemande une clé à l'utilisateur
                    promptForKey(mainStage, prefs);
                }
            }, Platform::runLater);
    }

    private void showMainApp(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(PhotoFilterApplication.class.getResource("PhotoFilter.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 600, 400);
            stage.setTitle("PhotoFilter");
            stage.setScene(scene);
            stage.setResizable(false);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'interface principale.");
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
