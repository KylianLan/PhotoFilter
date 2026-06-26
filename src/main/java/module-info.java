module fr.kylian.photofilter {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires metadata.extractor;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens fr.kylian.photofilter to javafx.fxml;
    exports fr.kylian.photofilter;
}