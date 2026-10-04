package fr.kylian.photofilter;

import javafx.application.Application;

import java.util.Locale;

public class Launcher {
    public static void main(String[] args) {

        Locale.setDefault(Locale.FRANCE);

        Application.launch(PhotoFilterApplication.class, args);
    }
}
