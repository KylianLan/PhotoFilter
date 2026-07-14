package fr.kylian.photofilter;

import fr.kylian.photofilter.licensemanager.LicenseVerifier;
import fr.kylian.photofilter.licensemanager.LicenseStorage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class PhotoFilterApplication extends Application {

    @Override
    public void start(Stage stage) {
        // Empêche JavaFX de se fermer automatiquement lorsque le dialogue de licence se ferme
        // (avant que la fenêtre principale ne soit affichée)
        Platform.setImplicitExit(false);
        
        // On lance le flux asynchrone de vérification de licence.
        // L'interface principale ne s'affichera que si la licence est valide.
        startLicenseCheckFlow(stage);
    }

    private void startLicenseCheckFlow(Stage mainStage) {
        String savedKey = LicenseStorage.getSavedKey();

        if (savedKey != null && !savedKey.trim().isEmpty()) {
            System.out.println("Vérification de la clé de licence sauvegardée en arrière-plan...");
            verifyKeyAsync(savedKey, true, mainStage);
        } else {
            promptForKey(mainStage);
        }
    }

    private void promptForKey(Stage mainStage) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.getDialogPane().getStylesheets().add(PhotoFilterApplication.class.getResource("style.css").toExternalForm());
        dialog.setTitle("Vérification de la licence");
        dialog.setHeaderText("Une clé de licence est requise pour utiliser PhotoFilter.");
        dialog.setContentText("Veuillez entrer votre clé de licence :");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String key = result.get().trim();
            if (key.isEmpty()) {
                showError("La clé de licence ne peut pas être vide.");
                promptForKey(mainStage); // On redemande récursivement
                return;
            }

            System.out.println("Vérification de la nouvelle clé saisie en arrière-plan...");
            verifyKeyAsync(key, false, mainStage);
        } else {
            // L'utilisateur a cliqué sur "Annuler" ou fermé la fenêtre
            Platform.exit();
        }
    }

    private void verifyKeyAsync(String key, boolean isSavedKey, Stage mainStage) {
        // Exécution de la vérification HTTP dans un thread séparé (supplyAsync) pour ne pas freezer l'UI
        java.util.concurrent.CompletableFuture.supplyAsync(() -> LicenseVerifier.checkLicense(key))
            .thenAcceptAsync(isValid -> {
                // Ce bloc s'exécute sur le thread JavaFX (grâce à Platform::runLater) quand le HTTP est terminé
                if (isValid) {
                    LicenseStorage.saveKey(key);
                    
                    if (!isSavedKey) {
                        Alert success = new Alert(Alert.AlertType.INFORMATION);
                        success.getDialogPane().getStylesheets().add(PhotoFilterApplication.class.getResource("style.css").toExternalForm());
                        success.setTitle("Licence valide");
                        success.setHeaderText(null);
                        success.setContentText("Votre clé de licence a été validée avec succès !");
                        success.showAndWait();
                    }
                    
                    showMainApp(mainStage);
                } else {
                    if (isSavedKey) {
                        // Si la clé sauvegardée a expiré ou n'est plus valide, on la supprime
                        LicenseStorage.removeKey();
                    } else {
                        showError("La clé de licence est invalide, a expiré, ou le serveur est inaccessible.");
                    }
                    // On redemande une clé à l'utilisateur
                    promptForKey(mainStage);
                }
            }, Platform::runLater);
    }

    private void showMainApp(Stage stage) {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(PhotoFilterApplication.class.getResource("PhotoFilter.fxml"));
            Parent root = fxmlLoader.load();
            
            PhotoFilterController controller = fxmlLoader.getController();
            controller.setHostServices(getHostServices());
            
            Scene scene = new Scene(root, 600, 400);
            stage.setTitle("PhotoFilter");
            stage.setScene(scene);
            stage.setResizable(true);

            stage.setOnShown(e -> {
                stage.setMinWidth(stage.getWidth());
                stage.setMinHeight(stage.getHeight());
            });

            stage.show();

            KeyboardController keyboardController = new KeyboardController();

            stage.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                keyboardController.onKeyPressed(e.getCode());
            });
            
            // Réactive la fermeture automatique de JavaFX quand on ferme cette fenêtre principale
            Platform.setImplicitExit(true);
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'interface principale.");
            Platform.exit();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add(PhotoFilterApplication.class.getResource("style.css").toExternalForm());
        alert.setTitle("Erreur de licence");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
