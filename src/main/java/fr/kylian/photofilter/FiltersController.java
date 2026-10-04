package fr.kylian.photofilter;

import fr.kylian.photofilter.filter.FilterMode;
import fr.kylian.photofilter.filter.Filter;
import fr.kylian.photofilter.filter.FilterRange;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.lang.reflect.Array;
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
        refreshUI();

        // Listen for changes in the filters list to update the UI
        this.filters.addListener((ListChangeListener<Filter>) change -> {
            refreshUI();
        });
    }

    /**
     * Rebuilds the UI to match the current state and order of the filters list.
     * Restores selection state if possible.
     */
    private void refreshUI() {
        // Save selection
        Toggle selectedToggle = selectionGroup.getSelectedToggle();
        Filter selectedFilter = null;
        if (selectedToggle != null && selectedToggle instanceof ToggleButton) {
            selectedFilter = (Filter) ((ToggleButton) selectedToggle).getParent().getUserData();
        }

        filterContainer.getChildren().clear();
        for (Filter filter : filters) {
            addFilterRow(filter);
        }

        // Restore selection
        if (selectedFilter != null) {
            for (Node node : filterContainer.getChildren()) {
                if (node instanceof HBox && node.getUserData() == selectedFilter) {
                    for (Node child : ((HBox) node).getChildren()) {
                        if (child instanceof ToggleButton) {
                            selectionGroup.selectToggle((ToggleButton) child);
                            break;
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Creates and adds a UI row for a single filter.
     */
    private void addFilterRow(Filter filter) {
        HBox row = new HBox(10);
        row.getStyleClass().add("filter-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));
        row.setUserData(filter);

        // 0. Drag Handle
        Label dragHandle = new Label("☰");
        dragHandle.setStyle("-fx-cursor: move; -fx-text-fill: #888888; -fx-font-size: 14px; -fx-font-weight: bold;");
        dragHandle.setPadding(new Insets(0, 5, 0, 5));

        // 1. CheckBox for Enabling/Disabling the filter
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().bindBidirectional(filter.enabledProperty());
        Tooltip.install(checkBox, new Tooltip("Enable/Disable this filter"));

        // 2. ToggleButton for selection (deletion)
        // Displays filter name, date, and active modes
        ToggleButton selectBtn = new ToggleButton(filter.getName() + " (" + filter.displayDate() + ")");

        selectBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(selectBtn, Priority.ALWAYS);
        selectBtn.setToggleGroup(selectionGroup);
        Tooltip.install(selectBtn, new Tooltip("Click to select for deletion"));

        row.getChildren().addAll(dragHandle, checkBox, selectBtn);
        filterContainer.getChildren().add(row);

        // Set up Drag and Drop events
        dragHandle.setOnDragDetected(event -> {
            Dragboard db = dragHandle.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(String.valueOf(filterContainer.getChildren().indexOf(row)));
            db.setContent(content);
            row.setOpacity(0.5);
            event.consume();
        });

        row.setOnDragOver(event -> {
            if (event.getGestureSource() != dragHandle && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        row.setOnDragEntered(event -> {
            if (event.getGestureSource() != dragHandle && event.getDragboard().hasString()) {
                row.setStyle("-fx-border-color: #0d6efd; -fx-border-width: 1px; -fx-border-style: dashed; -fx-background-color: #0d6efd22;");
            }
            event.consume();
        });

        row.setOnDragExited(event -> {
            row.setStyle("");
            event.consume();
        });

        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasString()) {
                try {
                    int sourceIndex = Integer.parseInt(db.getString());
                    int targetIndex = filterContainer.getChildren().indexOf(row);
                    if (sourceIndex >= 0 && sourceIndex < filters.size() &&
                        targetIndex >= 0 && targetIndex < filters.size() &&
                        sourceIndex != targetIndex) {
                        
                        final int srcIdx = sourceIndex;
                        final int tgtIdx = targetIndex;
                        // Defer to runLater to avoid issues while the drag event is in progress
                        Platform.runLater(() -> {
                            Filter movedFilter = filters.remove(srcIdx);
                            filters.add(tgtIdx, movedFilter);
                        });
                        success = true;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        row.setOnDragDone(event -> {
            row.setOpacity(1.0);
            event.consume();
        });
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


    private FilterMode[] askFilterParams() {
        // Pour la création : par défaut tout est coché
        return askFilterParams(new FilterMode[]{FilterMode.DAY, FilterMode.MONTH, FilterMode.YEAR});
    }

    /**
     * Shows a dialog to select which parts of the date to use for filtering.
     */
    private FilterMode[] askFilterParams(FilterMode[] defaultParams) {
        Dialog<FilterMode[]> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialog.setTitle("Paramètres du Filtre");
        dialog.setHeaderText("Étape 1 : Choisissez les éléments de date à utiliser :");

        List<FilterMode> defaultList = defaultParams != null ? Arrays.asList(defaultParams) : new ArrayList<>();

        CheckBox dayCb = new CheckBox("Jour");
        CheckBox monthCb = new CheckBox("Mois");
        CheckBox yearCb = new CheckBox("Année");

        // On coche les cases en fonction des paramètres actuels du filtre
        dayCb.setSelected(defaultList.contains(FilterMode.DAY));
        monthCb.setSelected(defaultList.contains(FilterMode.MONTH));
        yearCb.setSelected(defaultList.contains(FilterMode.YEAR));

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
        if (result.isPresent() && result.get().length == 0) {
            showAlert("Erreur", "Veuillez sélectionner au moins un paramètre.", Alert.AlertType.ERROR);
            return null;
        }
        return result.orElse(null);
    }

    /**
     * Shows a dialog to select the filter range (Single Date or Range).
     */
    private FilterRange askFilterRange() {
        ChoiceDialog<String> dialog = new ChoiceDialog<>("Date simple", "Date simple", "Intervalle");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialog.setTitle("Type de Filtre");
        dialog.setHeaderText("Étape 2 : Choisissez un type de Filtre :");
        dialog.setContentText("Range type:");

        Optional<String> resultOpt = dialog.showAndWait();
        if (resultOpt.isEmpty()) {
            return null;
        }
        String resultat = resultOpt.get();
        if ("Date simple".equals(resultat)) {
            return FilterRange.SINGLE_DATE;
        } else {
            return FilterRange.RANGE;
        }
    }

    /**
     * Shows a dialog to input specific date values based on selected parameters.
     */
    private LocalDate askDateValues(String title, FilterMode[] params) {
        // Pour la création : on part d'aujourd'hui
        return askDateValues(title, params, LocalDate.now());
    }

    private LocalDate askDateValues(String title, FilterMode[] params, LocalDate defaultDate) {
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialog.setTitle(title);
        dialog.setHeaderText("Étape 3 : Entrez les valeurs de la date :");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        List<FilterMode> paramList = Arrays.asList(params);

        // On initialise les Spinners avec la date actuelle du filtre
        Spinner<Integer> yearSpinner = new Spinner<>(1900, 2100, defaultDate.getYear());
        Spinner<Integer> monthSpinner = new Spinner<>(1, 12, defaultDate.getMonthValue());
        Spinner<Integer> daySpinner = new Spinner<>(1, 31, defaultDate.getDayOfMonth());

        int row = 0;
        if (paramList.contains(FilterMode.DAY)) {
            grid.add(new Label("Jour :"), 0, row);
            grid.add(daySpinner, 1, row++);
        }
        if (paramList.contains(FilterMode.MONTH)) {
            grid.add(new Label("Mois :"), 0, row);
            grid.add(monthSpinner, 1, row++);
        }
        if (paramList.contains(FilterMode.YEAR)) {
            grid.add(new Label("Année :"), 0, row);
            grid.add(yearSpinner, 1, row++);
        }

        dialog.getDialogPane().setContent(grid);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == okButton) {
                int year = paramList.contains(FilterMode.YEAR) ? yearSpinner.getValue() : 2000;
                int month = paramList.contains(FilterMode.MONTH) ? monthSpinner.getValue() : 1;
                int day = paramList.contains(FilterMode.DAY) ? daySpinner.getValue() : 1;
                try {
                    return LocalDate.of(year, month, day);
                } catch (Exception e) {
                    showAlert("Erreur", "Date invalide.", Alert.AlertType.ERROR);
                    return null;
                }
            }
            return null;
        });

        return dialog.showAndWait().orElse(null);
    }

    /**
     * Shows a dialog to input the filter name.
     */
    private String askFilterName() {
        // Pour la création
        return askFilterName("Nouveau Filtre");
    }

    private String askFilterName(String defaultName) {
        // Le TextInputDialog sera pré-rempli avec defaultName
        TextInputDialog dialog = new TextInputDialog(defaultName);
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        dialog.setTitle("Nom du Filtre");
        dialog.setHeaderText("Étape 4 : Entrez un nom pour ce Filtre :");
        dialog.setContentText("Nom :");

        return dialog.showAndWait().orElse(null);
    }

    /**
     * Action handler for the "Remove Filter" button.
     */
    @FXML
    void removeSelectedFilter() {
        ToggleButton selectedBtn = (ToggleButton) selectionGroup.getSelectedToggle();
        if (selectedBtn == null) {
            showAlert("Info", "Sélectionnez un Filtre en cliquant sur son nom avant de le supprimer.", Alert.AlertType.INFORMATION);
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer ce Filtre ?", ButtonType.YES, ButtonType.NO);
        confirmation.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        confirmation.setTitle("Suppression du Filtre");

        confirmation.showAndWait();

        if (confirmation.getResult() == ButtonType.YES) {
            Filter filterToRemove = (Filter) selectedBtn.getParent().getUserData();
            filters.remove(filterToRemove);
        }
    }

    /**
     * Action handler for the "Edit Filter" button.
     */
    @FXML
    void editSelectedFilter() {
        ToggleButton selectedBtn = (ToggleButton) selectionGroup.getSelectedToggle();
        if (selectedBtn == null) {
            showAlert("Info", "Sélectionnez un Filtre en cliquant sur son nom avant de le modifier.", Alert.AlertType.INFORMATION);
            return;
        }

        // On récupère le filtre lié au bouton
        Filter filterToEdit = (Filter) selectedBtn.getParent().getUserData();

        // 1. Modifier les paramètres (Day, Month, Year) avec les valeurs actuelles pré-cochées
        FilterMode[] newParams = askFilterParams(filterToEdit.getFilterParams());
        if (newParams == null) return;

        // 2. Modifier la date principale avec la date actuelle
        LocalDate newFirstDate = askDateValues("Modifier la date principale", newParams, filterToEdit.getDate());
        if (newFirstDate == null) return;

        // 3. Modifier la deuxième date (UNIQUEMENT si c'est un RANGE)
        Optional<LocalDate> newSecondDate = filterToEdit.getSecondDate();
        if (filterToEdit.getFilterRange() == FilterRange.RANGE) {
            LocalDate defaultDate2 = filterToEdit.getSecondDate().orElse(LocalDate.now());
            LocalDate date2 = askDateValues("Modifier la date de fin", newParams, defaultDate2);
            if (date2 == null) return;
            newSecondDate = Optional.of(date2);
        }

        // 4. Modifier le nom avec l'ancien nom pré-rempli
        String newName = askFilterName(filterToEdit.getName());
        if (newName == null) return;

        // 5. Appliquer les modifications à l'objet
        filterToEdit.setFilterParams(newParams);
        filterToEdit.setDate(newFirstDate);
        filterToEdit.setSecondDate(newSecondDate);
        filterToEdit.setName(newName);

        // 6. Mettre à jour le texte du bouton dans l'interface
        selectedBtn.setText(filterToEdit.getName() + " (" + filterToEdit.displayDate() + ")");
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
        alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
