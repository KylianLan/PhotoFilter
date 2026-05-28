package fr.kylian.photofilter.filter;

import javafx.beans.property.BooleanProperty;

import java.util.Date;

public class Filter {

    private String name;
    private Date date;
    private BooleanProperty enabled;

    public Filter(String name, Date date, boolean enabled) {
        this.name = name;
        this.date = date;
        this.enabled.set(enabled);
    }

    public Filter(String name, Date date) {
        this.name = name;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    public void toogle() {
        this.enabled.set(!enabled.get());
    }
}
