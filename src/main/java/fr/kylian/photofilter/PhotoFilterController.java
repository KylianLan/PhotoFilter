package fr.kylian.photofilter;

import fr.kylian.photofilter.analyzer.Analyzer;
import fr.kylian.photofilter.analyzer.FiltersSaver;
import fr.kylian.photofilter.filter.FilterMode;
import fr.kylian.photofilter.filter.Filter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class PhotoFilterController implements Initializable {

    @FXML private Button folderSelect;
    @FXML private TextArea selectedFolder;
    @FXML private Button start;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea enabledFilters;
    @FXML private MenuItem help;
    @FXML private MenuItem about;

    private ObservableList<Filter> filters;
    private File folder;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filters = FXCollections.observableArrayList();

        // Load saved filters on startup
        Filter[] savedFilters = FiltersSaver.loadFilters();
        if (savedFilters != null && savedFilters.length > 0) {
            filters.addAll(savedFilters);
        } else {
            // Fallback default filters
            FilterMode[] dayAndMonth = new FilterMode[]{FilterMode.DAY, FilterMode.MONTH};
            filters.add(new Filter("Noël", LocalDate.of(0, 12, 25), dayAndMonth));
            filters.add(new Filter("Réveillon de Noël", LocalDate.of(0, 12, 24), dayAndMonth));
            filters.add(new Filter("Veille du jour de l'an", LocalDate.of(0, 12, 31), dayAndMonth));
            filters.add(new Filter("Jour de l'an", LocalDate.of(0, 1, 1), dayAndMonth));
        }

        updateEnabledFiltersTextArea();

        about.setOnAction(e -> {
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("À propos");
            dialog.setHeaderText("Copyright");
            dialog.setContentText("© 2026 Kylian Langlois.\nTous droits réservés.");

            dialog.showAndWait();
        });
    }

    private void updateEnabledFiltersTextArea() {
        StringBuilder sb = new StringBuilder();
        for (Filter filter : filters) {
            if (filter.isEnabled()) {
                sb.append("- ").append(filter.getName())
                  .append(" (").append(filter.displayDate()).append(")\n");
            }
        }
        enabledFilters.setText(sb.toString());
    }

    @FXML
    void selectFolder(ActionEvent event) {
        Stage stage = (Stage) folderSelect.getScene().getWindow();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisissez un dossier");

        File initialFolder = new File(System.getProperty("user.home"));
        if (initialFolder.exists()) {
            directoryChooser.setInitialDirectory(initialFolder);
        }

        File selectedFolder = directoryChooser.showDialog(stage);

        if (selectedFolder != null) {
            this.selectedFolder.setText(selectedFolder.getAbsolutePath());
        }

        this.folder = selectedFolder;
    }

    @FXML
    void openFiltersHandler(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("DateSelector.fxml"));
            Parent root = loader.load();

            FiltersController controller = loader.getController();
            controller.setFilters(filters);

            Stage stage = new Stage();
            stage.setTitle("Set Filters");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(folderSelect.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(false);
            stage.showAndWait();

            // Save filters whenever the window is closed (by button or window cross)
            FiltersSaver.saveFilters(filters.toArray(new Filter[0]));
            updateEnabledFiltersTextArea();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startFiltering() {
        if (folder == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Dossier manquant");
            alert.setHeaderText(null);
            alert.setContentText("Veuillez choisir un dossier source contenant les photos à filtrer.");
            alert.showAndWait();
            return;
        }

        Stage stage = (Stage) folderSelect.getScene().getWindow();

        File initialDestination = new File(System.getProperty("user.home"));
        DirectoryChooser destSelection = new DirectoryChooser();
        destSelection.setTitle("Choisir une destination");
        destSelection.setInitialDirectory(initialDestination);

        File destination = destSelection.showDialog(stage);
        if (destination == null) {
            return; // L'utilisateur a annulé la sélection
        }

        System.out.println("Source: " + folder.getAbsolutePath());
        System.out.println("Destination: " + destination.getAbsolutePath());
        System.out.println("Filters: " + filters);

        start.setDisable(true);
        progressBar.setProgress(0.0);

        javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Analyzer analyzer = new Analyzer();
                List<File> files = analyzer.getFiles(folder);

                int count = files.size();
                System.out.println("Fichiers trouvés: " + count);
                
                long enabledCount = filters.stream().filter(Filter::isEnabled).count();
                int totalWork = (int) (count * enabledCount);
                int[] workDone = {0};

                Runnable onProgress = () -> {
                    workDone[0]++;
                    updateProgress(workDone[0], totalWork);
                };

                for (Filter filter : filters) {
                    if (filter.isEnabled()) {
                        analyzer.putInFolder(destination, files, filter, onProgress);
                    }
                }
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            start.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Terminé");
            alert.setHeaderText(null);
            alert.setContentText("Le filtrage est terminé !");
            alert.showAndWait();
        });

        task.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0.0);
            start.setDisable(false);
            Throwable exception = task.getException();
            System.err.println("Error while filtering files:\n\t" + exception.getMessage());
            exception.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText(null);
            alert.setContentText("Une erreur s'est produite lors du filtrage : " + exception.getMessage());
            alert.showAndWait();
        });

        progressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }
}
