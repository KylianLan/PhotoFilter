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

    public Filter(String name, LocalDate date, FilterMode filterMode, boolean enabled) {
        this.name = name;
        this.date = date;
        this.filterMode = filterMode;
        this.enabled = new SimpleBooleanProperty(enabled);
    }

    public Filter(String name, LocalDate date, FilterMode filterMode) {
        this(name, date, filterMode, true);
    }

    public Filter(String name, LocalDate date) {
        this(name, date, FilterMode.ALL, true);
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

    public FilterMode getFilterMode() {
        return filterMode;
    }

    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
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
