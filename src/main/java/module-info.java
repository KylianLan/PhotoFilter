module fr.kylian.photofilter {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;
    requires java.desktop;
    requires metadata.extractor;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jdk8;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.github.oshi;
    requires java.net.http;
    requires java.prefs;
    requires org.apache.commons.compress;
    requires org.tukaani.xz;
    requires junrar;
    requires jave.core;
    requires org.apache.commons.codec;

    opens fr.kylian.photofilter to javafx.fxml;
    opens fr.kylian.photofilter.filter to com.fasterxml.jackson.databind;
    exports fr.kylian.photofilter;
}