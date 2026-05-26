package fr.kylian.photofilter;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import javafx.event.ActionEvent;
import java.io.File;

public class PhotoFilterController {

    @FXML private Button dateSelect;
    @FXML private Button folderSelect;
    @FXML private TextArea selectedDates;
    @FXML private TextArea selectedFolder;

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

        System.out.println(selectedFolder.getAbsolutePath());
        this.selectedFolder.setText(selectedFolder.getAbsolutePath());
    }

    @FXML
    void selectDate(ActionEvent event) {

    }

}
