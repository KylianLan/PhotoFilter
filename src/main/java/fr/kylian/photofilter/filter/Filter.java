package fr.kylian.photofilter.filter;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalDate;

public class Filter {

    private String name;
    private LocalDate date;
    private BooleanProperty enabled;

    public Filter(String name, LocalDate date, boolean enabled) {
        this.name = name;
        this.date = date;
        this.enabled = new SimpleBooleanProperty(enabled);
    }

    public Filter(String name, LocalDate date) {
        this(name, date, true);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public void toggle() {
        this.enabled.set(!enabled.get());
    }
}
