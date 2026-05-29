package fr.kylian.photofilter.filter;

import fr.kylian.photofilter.analyzer.FilterMode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import java.time.LocalDate;

public class Filter {

    private String name;
    private LocalDate date;
    private BooleanProperty enabled;
    private FilterMode filterMode;

    public Filter(String name, LocalDate date, boolean b, FilterMode filterMode) {
        this.name = name;
        this.date = date;
        this.enabled = new SimpleBooleanProperty(true);
        this.filterMode = filterMode;

    }

    public Filter(String name, LocalDate date, boolean enabled) {
        this(name, date, enabled, FilterMode.ALL);
    }

    public Filter(String name, LocalDate date, FilterMode filterMode) {
        this(name, date, true, filterMode);
    }

    public Filter(String name, LocalDate date) {
        this(name, date, true, FilterMode.ALL);
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
