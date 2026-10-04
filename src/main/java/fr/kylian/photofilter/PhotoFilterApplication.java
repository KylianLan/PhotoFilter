package fr.kylian.photofilter;

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
        showMainApp(stage);
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

            stage.setOnCloseRequest(e -> {
                if (controller != null && controller.isTaskRunning()) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.getDialogPane().getStylesheets().add(PhotoFilterApplication.class.getResource("style.css").toExternalForm());
                    confirmAlert.setTitle("Opération en cours");
                    confirmAlert.setHeaderText("Un traitement est actuellement en cours.");
                    confirmAlert.setContentText("Voulez-vous vraiment annuler l'opération et quitter PhotoFilter ?");

                    Optional<javafx.scene.control.ButtonType> result = confirmAlert.showAndWait();
                    if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
                        controller.cancelRunningTasks();
                        Platform.exit();
                        System.exit(0);
                    } else {
                        // L'utilisateur a annulé : on consomme l'événement pour empêcher la fermeture
                        e.consume();
                    }
                } else {
                    Platform.exit();
                    System.exit(0);
                }
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
