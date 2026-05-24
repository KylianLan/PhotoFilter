module fr.kylian.photofilter {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;

    opens fr.kylian.photofilter to javafx.fxml;
    exports fr.kylian.photofilter;
}