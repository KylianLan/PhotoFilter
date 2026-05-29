package fr.kylian.photofilter;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class PhotoFilterApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(PhotoFilterApplication.class.getResource("PhotoFilter.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("PhotoFilter");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
}
