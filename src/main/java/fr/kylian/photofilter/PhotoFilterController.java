package fr.kylian.photofilter;

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
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class PhotoFilterController implements Initializable {

    @FXML private Button folderSelect;
    @FXML private TextArea selectedFolder;
    @FXML private Button start;
    @FXML private TextArea enabledFilters;
    @FXML private MenuItem help;
    @FXML private MenuItem about;

    private ObservableList<Filter> filters;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filters = FXCollections.observableArrayList();

        FilterMode[] dayAndMonth = new FilterMode[]{FilterMode.DAY, FilterMode.MONTH};

        filters.add(new Filter("Noël", LocalDate.of(0, 12, 25), dayAndMonth));
        filters.add(new Filter("Réveillon de Noël", LocalDate.of(0, 12, 24), dayAndMonth));
        filters.add(new Filter("Veille du jour de l'an", LocalDate.of(0, 12, 31), dayAndMonth));
        filters.add(new Filter("Jour de l'an", LocalDate.of(0, 1, 1), dayAndMonth));

        about.setOnAction(e -> {
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("À propos");
            dialog.setHeaderText("Copyright");
            dialog.setContentText("© 2026 Kylian Langlois.\nTous droits réservés.");

            dialog.showAndWait();
        });
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
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
