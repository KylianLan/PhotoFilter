package fr.kylian.photofilter.filter;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private Optional<LocalDate> secondDate = Optional.empty();

    @JsonIgnore
    private BooleanProperty enabled;

    private FilterMode[] filterParams;
    private FilterRange filterRange;

    public Filter() {
        this.enabled = new SimpleBooleanProperty(true);
    }

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

    @JsonIgnore
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

    public String displayDate() {
        java.time.format.DateTimeFormatter fullFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // 1. Si c'est une plage de dates (RANGE)
        if (filterRange == FilterRange.RANGE && secondDate.isPresent()) {
            return "du " + date.format(fullFormatter) + " au " + secondDate.get().format(fullFormatter);
        }

        // 2. Si c'est une date unique, on adapte le format selon les paramètres cochés
        if (filterParams == null || filterParams.length == 0) {
            return date.format(fullFormatter);
        }

        java.util.List<FilterMode> paramList = java.util.Arrays.asList(filterParams);
        boolean hasDay = paramList.contains(FilterMode.DAY);
        boolean hasMonth = paramList.contains(FilterMode.MONTH);
        boolean hasYear = paramList.contains(FilterMode.YEAR);

        if (hasDay && hasMonth && hasYear) {
            return date.format(fullFormatter); // ex: 25/12/2026
        } else if (hasDay && hasMonth) {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")); // ex: 25/12 (Noël)
        } else if (hasMonth && hasYear) {
            return date.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy")); // ex: 12/2026
        } else if (hasDay) {
            return "Jour " + date.getDayOfMonth();
        } else if (hasMonth) {
            return "Mois " + date.getMonthValue();
        } else if (hasYear) {
            return "Année " + date.getYear();
        }

        return date.toString();
    }

}
