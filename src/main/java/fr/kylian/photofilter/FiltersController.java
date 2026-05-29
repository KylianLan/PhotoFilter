package fr.kylian.photofilter;

import fr.kylian.photofilter.analyzer.FilterMode;
import fr.kylian.photofilter.filter.Filter;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Optional;

public class FiltersController {

    @FXML private VBox filterContainer;
    @FXML private DatePicker datePicker;

    private ObservableList<Filter> filters;
    private ToggleGroup selectionGroup = new ToggleGroup();

    public void setFilters(ObservableList<Filter> filters) {
        this.filters = filters;
        
        // Initial population
        for (Filter filter : filters) {
            addFilterRow(filter);
        }

        // Listen for changes
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

    private void addFilterRow(Filter filter) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5));
        row.setUserData(filter);

        // 1. CheckBox pour Activer/Désactiver
        CheckBox checkBox = new CheckBox();
        checkBox.selectedProperty().bindBidirectional(filter.enabledProperty());
        Tooltip.install(checkBox, new Tooltip("Activer/Désactiver ce filtre"));

        // 2. ToggleButton pour la Sélection (Suppression)
        // On affiche aussi le mode pour information
        ToggleButton selectBtn = new ToggleButton(filter.getName() + " (" + filter.getDate() + ") [" + filter.getFilterMode() + "]");
        selectBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(selectBtn, Priority.ALWAYS);
        selectBtn.setToggleGroup(selectionGroup);
        Tooltip.install(selectBtn, new Tooltip("Cliquez pour sélectionner (suppression)"));

        row.getChildren().addAll(checkBox, selectBtn);
        filterContainer.getChildren().add(row);
    }

    private void removeFilterRow(Filter filter) {
        filterContainer.getChildren().removeIf(node -> node.getUserData() == filter);
    }

    @FXML
    void addFilter() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showAlert("Erreur", "Veuillez sélectionner une date.", Alert.AlertType.ERROR);
            return;
        }

        // 1. Demander le nom
        TextInputDialog nameDialog = new TextInputDialog("Nouveau Filtre");
        nameDialog.setTitle("Nom du filtre");
        nameDialog.setHeaderText("Donnez un nom à ce filtre :");
        nameDialog.setContentText("Nom :");

        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty()) return;
        String name = nameResult.get();

        // 2. Demander le mode de tri
        ChoiceDialog<FilterMode> modeDialog = new ChoiceDialog<>(FilterMode.DAY_AND_MONTH, FilterMode.values());
        modeDialog.setTitle("Mode de tri");
        modeDialog.setHeaderText("Choisissez le mode de tri pour ce filtre :");
        modeDialog.setContentText("Mode :");

        Optional<FilterMode> modeResult = modeDialog.showAndWait();
        FilterMode mode = modeResult.orElse(FilterMode.DAY_AND_MONTH);

        filters.add(new Filter(name, date, mode, true));
    }

    @FXML
    void removeSelectedFilter() {
        ToggleButton selectedBtn = (ToggleButton) selectionGroup.getSelectedToggle();
        if (selectedBtn == null) {
            showAlert("Info", "Sélectionnez un filtre en cliquant sur son nom avant de supprimer.", Alert.AlertType.INFORMATION);
            return;
        }

        Filter filterToRemove = (Filter) selectedBtn.getParent().getUserData();
        filters.remove(filterToRemove);
    }

    @FXML
    void closeWindow() {
        Stage stage = (Stage) filterContainer.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
