package fr.kylian.photofilter;

import fr.kylian.photofilter.licensemanager.HardwareUtils;
import javafx.application.Application;

import java.util.Locale;

public class Launcher {
    public static void main(String[] args) {

        Locale.setDefault(Locale.FRANCE);

            System.out.println("UUID Brut de la machine : " + HardwareUtils.getSystemUUID());
            System.out.println("HWID Haché (SHA-256) : " + HardwareUtils.generateHWID());
            System.out.println("Nom de l'appareil : " + HardwareUtils.getDeviceName());

//        Application.launch(PhotoFilterApplication.class, args);
    }
}
