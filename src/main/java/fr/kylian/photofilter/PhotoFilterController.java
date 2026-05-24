package fr.kylian.photofilter;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class PhotoFilterController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
    }
}
