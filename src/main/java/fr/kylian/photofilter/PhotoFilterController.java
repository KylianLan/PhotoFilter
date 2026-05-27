package fr.kylian.photofilter;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class PhotoFilterController {

    @FXML private DatePicker dateSelect;
    @FXML private Button folderSelect;
    @FXML private TextArea selectedDates;
    @FXML private TextArea selectedFolder;

    private static Date reveillonNoel = new Date(0,12,24);
    private static Date noel = new Date(0,12,25);
    private static Date veilleNouvelAn = new Date(0,12,31);
    private static Date nouvelAn = new Date(0,1,1);


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

    @FXML
    void selectDate(ActionEvent event) {
        if (dateSelect.getValue() != null) {
            selectedDates.appendText(dateSelect.getValue().toString() + "\n");
        }
    }

}
