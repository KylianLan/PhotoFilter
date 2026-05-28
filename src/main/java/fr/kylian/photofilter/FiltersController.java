package fr.kylian.photofilter;

import fr.kylian.photofilter.filter.Filter;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Optional;

public class FiltersController {

    @FXML private VBox filterContainer;
    @FXML private DatePicker datePicker;

    private ObservableList<Filter> filters;

    public void setFilters(ObservableList<Filter> filters) {
        this.filters = filters;
        
        // Initial population
        for (Filter filter : filters) {
            addFilterButton(filter);
        }

        // Listen for new filters
        this.filters.addListener((ListChangeListener<Filter>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Filter f : change.getAddedSubList()) {
                        addFilterButton(f);
                    }
                }
                // Optionnel: gérer change.wasRemoved() pour supprimer les boutons
            }
        });
    }

    private void addFilterButton(Filter filter) {
        ToggleButton btn = new ToggleButton(filter.getName() + " (" + filter.getDate() + ")");
        btn.setMaxWidth(Double.MAX_VALUE);
        
        // Bidirectional binding between UI and Data
        btn.selectedProperty().bindBidirectional(filter.enabledProperty());
        
        filterContainer.getChildren().add(btn);
    }

    @FXML
    void addFilter() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            showAlert("Erreur", "Veuillez sélectionner une date.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog("Nouveau Filtre");
        dialog.setTitle("Nom du filtre");
        dialog.setHeaderText("Donnez un nom à ce filtre :");
        dialog.setContentText("Nom :");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            filters.add(new Filter(name, date, true));
        });
    }

    @FXML
    void closeWindow() {
        Stage stage = (Stage) filterContainer.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
