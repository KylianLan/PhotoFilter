package fr.kylian.photofilter;

import javafx.scene.input.KeyCode;

public class KeyboardController {

    public void onKeyPressed(final KeyCode key) {
        System.out.println(key.toString());
        switch (key.toString()) {
            case "F1":
                PhotoFilterController.openDoc();
                break;
        }
    }

}
