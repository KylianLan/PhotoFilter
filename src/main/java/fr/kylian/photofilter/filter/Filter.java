package fr.kylian.photofilter.filter;

import fr.kylian.photofilter.filter.FilterMode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.lang.reflect.Array;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Optional;

public class Filter {

    private String name;
    private LocalDate date;
    private Optional<LocalDate> secondDate;
    private BooleanProperty enabled;
    private FilterMode[] filterParams;
    private FilterRange filterRange;

    // Full constructor for filter with a two-dates range
    public Filter(String name, LocalDate date, Optional<LocalDate> secondDate, FilterMode[] filterParams) {
        this.name = name;
        this.date = date;
        this.secondDate = secondDate;
        this.enabled = new SimpleBooleanProperty(true);
        this.filterParams = filterParams;
        this.filterRange = FilterRange.RANGE;
    }

    // Simplified constructor for single date filters
    public Filter(String name, LocalDate date, FilterMode[] filterParams) {
        this.name = name;
        this.date = date;
        this.enabled = new SimpleBooleanProperty(true);
        this.filterParams = filterParams;
        this.filterRange = FilterRange.SINGLE_DATE;
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

    public FilterMode[] getFilterParams() {
        return filterParams;
    }

    public void setFilterParams(FilterMode[] filterMode) {
        this.filterParams = filterMode;
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

    public Optional<LocalDate> getSecondDate() {
        return secondDate;
    }

    public void setSecondDate(Optional<LocalDate> secondDate) {
        this.secondDate = secondDate;
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
    }

    public FilterRange getFilterRange() {
        return filterRange;
    }

    public void setFilterRange(FilterRange filterRange) {
        this.filterRange = filterRange;
    }
}
