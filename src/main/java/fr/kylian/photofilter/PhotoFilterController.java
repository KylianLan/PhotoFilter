package fr.kylian.photofilter;

import fr.kylian.photofilter.filter.Filter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import javafx.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class PhotoFilterController implements Initializable {

    @FXML private Button folderSelect;
    @FXML private TextArea selectedFolder;

    private ObservableList<Filter> filters;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filters = FXCollections.observableArrayList();
        filters.add(new Filter("Noël",new Date(0,Calendar.DECEMBER,25)));
        filters.add(new Filter("Reveillon de Noël", new Date(0,Calendar.DECEMBER,24)));
        filters.add(new Filter("Veille du jour de l'an", new Date(0,Calendar.DECEMBER,31)));
        filters.add(new Filter("Jour de l'an", new Date(0,0,1)));
    }

    @FXML
    void selectFolder(ActionEvent event) {
        Stage stage = (Stage) folderSelect.getScene().getWindow();

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Choisissez un dossier");

        File intialFolder = new File(System.getProperty("user.home"));
        if (intialFolder.exists()) {
            directoryChooser.setInitialDirectory(intialFolder);
        }

        File selectedFolder = directoryChooser.showDialog(stage);

        this.selectedFolder.setText(selectedFolder.getAbsolutePath());
    }

    public void openFiltersHandler(ActionEvent actionEvent) {
        
    }
}
