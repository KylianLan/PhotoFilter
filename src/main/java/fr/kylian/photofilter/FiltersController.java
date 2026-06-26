package fr.kylian.photofilter;

import fr.kylian.photofilter.filter.FilterMode;
import fr.kylian.photofilter.filter.Filter;
import fr.kylian.photofilter.filter.FilterRange;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the filter management window.
 */
public class FiltersController {

    @FXML private VBox filterContainer;

    private ObservableList<Filter> filters;
    private ToggleGroup selectionGroup = new ToggleGroup();

    /**
     * Links the list of filters to the UI and sets up listeners.
     */
    public void setFilters(ObservableList<Filter> filters) {
        this.filters = filters;
        
        // Initial population of the UI list
        for (Filter filter : filters) {
            addFilterRow(filter);
        }

        // Listen for changes in the filters list to update the UI
        this.filters.addListener((ListChangeListener<Filter>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Filter f : change.getAddedSubList()) {
                        addFilterRow(f);
                    }
                }
                if (change.wasRemoved()) {
                    for (Filter f : change.getRemoved()) {
                        removeFilterRow(f);
                    }
                }
            }
        });
    }

    /**
     * Creates and adds a UI row for a single filter.
     */
    private void addFilterRow(Filter filter) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));
        row.setUserData(filter);

        // 1. CheckBox for Enabling/Disabling the filter
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().bindBidirectional(filter.enabledProperty());
        Tooltip.install(checkBox, new Tooltip("Enable/Disable this filter"));

        // 2. ToggleButton for selection (deletion)
        // Displays filter name, date, and active modes
        String dateInfo = filter.getDate().toString();
        if (filter.getFilterRange() == FilterRange.RANGE && filter.getSecondDate().isPresent()) {
            dateInfo += " to " + filter.getSecondDate().get().toString();
        }
        
        String modeString = Arrays.toString(filter.getFilterParams());
        ToggleButton selectBtn = new ToggleButton(filter.getName() + " (" + dateInfo + ") " + modeString);
        selectBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(selectBtn, Priority.ALWAYS);
        selectBtn.setToggleGroup(selectionGroup);
        Tooltip.install(selectBtn, new Tooltip("Click to select for deletion"));

        row.getChildren().addAll(checkBox, selectBtn);
        filterContainer.getChildren().add(row);
    }

    /**
     * Removes a filter row from the UI.
     */
    private void removeFilterRow(Filter filter) {
        filterContainer.getChildren().removeIf(node -> node.getUserData() == filter);
    }

    /**
     * Action handler for the "Add Filter" button.
     * Follows the step-by-step creation process.
     */
    @FXML
    void addFilter() {
        // Step 1: Choose Filter Type (Parameters: Day, Month, Year)
        FilterMode[] params = askFilterParams();
        if (params == null) return;

        // Step 2: Choose Range (Single Date or Range)
        FilterRange range = askFilterRange();
        if (range == null) return;

        // Step 3, 4, 5: Choose Day, Month, and/or Year
        LocalDate firstDate = askDateValues("Première date / Date de début", params);
        if (firstDate == null) return;

        Optional<LocalDate> secondDate = Optional.empty();
        if (range == FilterRange.RANGE) {
            LocalDate date2 = askDateValues("Date de fin", params);
            if (date2 == null) return;
            secondDate = Optional.of(date2);
        }

        // Step 6: Ask for filter name
        String name = askFilterName();
        if (name == null) return;

        // Add to the list
        if (secondDate.isPresent()) {
            filters.add(new Filter(name, firstDate, secondDate, params));
        } else {
            filters.add(new Filter(name, firstDate, params));
        }
    }

    /**
     * Shows a dialog to select which parts of the date to use for filtering.
     */
    private FilterMode[] askFilterParams() {
        Dialog<FilterMode[]> dialog = new Dialog<>();
        dialog.setTitle("Filter Parameters");
        dialog.setHeaderText("Step 1: Choose date elements to use for filtering:");

        CheckBox dayCb = new CheckBox("Day");
        CheckBox monthCb = new CheckBox("Month");
        CheckBox yearCb = new CheckBox("Year");
        dayCb.setSelected(true);
        monthCb.setSelected(true);
        yearCb.setSelected(true);

        VBox vbox = new VBox(10, dayCb, monthCb, yearCb);
        vbox.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(vbox);

        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okButton) {
                List<FilterMode> modes = new ArrayList<>();
                if (dayCb.isSelected()) modes.add(FilterMode.DAY);
                if (monthCb.isSelected()) modes.add(FilterMode.MONTH);
                if (yearCb.isSelected()) modes.add(FilterMode.YEAR);
                return modes.toArray(new FilterMode[0]);
            }
            return null;
        });

        Optional<FilterMode[]> result = dialog.showAndWait();
        if (result.isPresent()) {
            if (result.get().length == 0) {
                showAlert("Error", "Please select at least one parameter.", Alert.AlertType.ERROR);
                return null;
            }
            return result.get();
        }
        return null;
    }

    /**
     * Shows a dialog to select the filter range (Single Date or Range).
     */
    private FilterRange askFilterRange() {
        ChoiceDialog<FilterRange> dialog = new ChoiceDialog<>(FilterRange.SINGLE_DATE, FilterRange.values());
        dialog.setTitle("Filter Range");
        dialog.setHeaderText("Step 2: Choose the filter range:");
        dialog.setContentText("Range type:");

        Optional<FilterRange> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Shows a dialog to input specific date values based on selected parameters.
     */
    private LocalDate askDateValues(String title, FilterMode[] params) {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Steps 3, 4, 5: Enter the date values:");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        List<FilterMode> paramList = Arrays.asList(params);
        
        Spinner<Integer> yearSpinner = new Spinner<>(1900, 2100, LocalDate.now().getYear());
        Spinner<Integer> monthSpinner = new Spinner<>(1, 12, LocalDate.now().getMonthValue());
        Spinner<Integer> daySpinner = new Spinner<>(1, 31, LocalDate.now().getDayOfMonth());

        int row = 0;
        if (paramList.contains(FilterMode.YEAR)) {
            grid.add(new Label("Year:"), 0, row);
            grid.add(yearSpinner, 1, row++);
        }
        if (paramList.contains(FilterMode.MONTH)) {
            grid.add(new Label("Month:"), 0, row);
            grid.add(monthSpinner, 1, row++);
        }
        if (paramList.contains(FilterMode.DAY)) {
            grid.add(new Label("Day:"), 0, row);
            grid.add(daySpinner, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okButton) {
                // If a parameter is not selected, we use a default value (e.g., 2000-01-01)
                // The analyzer should ignore these fields based on FilterMode[]
                int year = paramList.contains(FilterMode.YEAR) ? yearSpinner.getValue() : 2000;
                int month = paramList.contains(FilterMode.MONTH) ? monthSpinner.getValue() : 1;
                int day = paramList.contains(FilterMode.DAY) ? daySpinner.getValue() : 1;
                try {
                    return LocalDate.of(year, month, day);
                } catch (Exception e) {
                    showAlert("Error", "Invalid date entered.", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        Optional<LocalDate> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Shows a dialog to input the filter name.
     */
    private String askFilterName() {
        TextInputDialog dialog = new TextInputDialog("New Filter");
        dialog.setTitle("Filter Name");
        dialog.setHeaderText("Step 6: Enter a name for this filter:");
        dialog.setContentText("Name:");

        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }

    /**
     * Action handler for the "Remove Filter" button.
     */
    @FXML
    void removeSelectedFilter() {
        ToggleButton selectedBtn = (ToggleButton) selectionGroup.getSelectedToggle();
        if (selectedBtn == null) {
            showAlert("Info", "Select a filter by clicking its name before deleting.", Alert.AlertType.INFORMATION);
            return;
        }

        Filter filterToRemove = (Filter) selectedBtn.getParent().getUserData();
        filters.remove(filterToRemove);
    }

    /**
     * Action handler for the "Edit Filter" button.
     */
    @FXML
    void editSelectedFilter() {
        // TODO: Refaire la logique d'édition pour supporter FilterMode[] et FilterRange
        showAlert("En développement", "L'édition de filtres complexes arrive bientôt !", Alert.AlertType.INFORMATION);
    }

    /**
     * Closes the window.
     */
    @FXML
    void closeWindow() {
        Stage stage = (Stage) filterContainer.getScene().getWindow();
        stage.close();
    }

    /**
     * Helper to show alerts.
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
