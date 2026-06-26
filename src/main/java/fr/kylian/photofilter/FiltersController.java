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
        ToggleButton selectBtn = new ToggleButton(filter.getName() + " (" + filter.displayDate() + ")");
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

        TextInputDialog nameDialog = new TextInputDialog("Nouveau Filtre");
        nameDialog.setTitle("Nom du filtre");
        nameDialog.setHeaderText("Donnez un nom à ce filtre :");
        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isEmpty()) return;
        String name = nameResult.get();

        VBox optionsContainer = new VBox(5);
        optionsContainer.setAlignment(Pos.CENTER);

        ToggleButton day = new ToggleButton("Jour");
        ToggleButton month = new ToggleButton("Mois");
        ToggleButton year = new ToggleButton("Année");

        day.setMaxWidth(Double.MAX_VALUE);
        month.setMaxWidth(Double.MAX_VALUE);
        year.setMaxWidth(Double.MAX_VALUE);

        optionsContainer.getChildren().addAll(day, month, year);

        Dialog<FilterMode> modeDialog = new Dialog<>();
        modeDialog.setTitle("Mode de tri");
        modeDialog.setHeaderText("Sélectionnez le mode de filtrage :");
        modeDialog.getDialogPane().setContent(optionsContainer);
        modeDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        modeDialog.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                boolean isDay = day.isSelected();
                boolean isMonth = month.isSelected();
                boolean isYear = year.isSelected();

                if (isDay && isMonth && isYear) {
                    return FilterMode.ALL;
                } else if (isDay && isMonth) {
                    return FilterMode.DAY_AND_MONTH;
                } else if (isMonth && isYear) {
                    return FilterMode.MONTH_AND_YEAR;
                } else if (isDay && !isMonth && !isYear) {
                    return FilterMode.DAY_ONLY;
                } else if (!isDay && isMonth && !isYear) {
                    return FilterMode.MONTH_ONLY;
                } else if (!isDay && !isMonth && isYear) {
                    return FilterMode.YEAR_ONLY;
                } else if (isDay && !isMonth && isYear) {
                    System.err.println("La combinaison du jour et de l'année n'existe pas !");
                    return null;
                }
            }
            System.err.println("Aucun FilterMode sélectionné !");
            return null;
        });

        Optional<FilterMode> modeResult = modeDialog.showAndWait();
        if (modeResult.isPresent()) {
            FilterMode mode =   modeResult.get();
            filters.add(new Filter(name, date, mode, true));
        } else {
            showAlert("Info", "Aucun mode de filtre valide n'a été sélectionné.", Alert.AlertType.WARNING);
        }
    }

    @FXML
    void editSelectedFilter() {
        ToggleButton selectedBtn = (ToggleButton) selectionGroup.getSelectedToggle();
        if (selectedBtn == null) {
            showAlert("Info", "Sélectionnez un filtre avant de modifier.", Alert.AlertType.INFORMATION);
            return;
        }

        Filter filterToEdit = (Filter) selectedBtn.getParent().getUserData();

        // 1. Demander le nouveau nom
        TextInputDialog nameDialog = new TextInputDialog(filterToEdit.getName());
        nameDialog.setTitle("Modifier le nom");
        nameDialog.setHeaderText("Entrez le nouveau nom :");

        Optional<String> nameResult = nameDialog.showAndWait();
        if (nameResult.isPresent()) {
            filterToEdit.setName(nameResult.get()); // Mise à jour du modèle
        }

        // 2. Demander la nouvelle date
        Dialog<LocalDate> dateDialog = new Dialog<>();
        dateDialog.setTitle("Modifier la date");
        DatePicker datePicker = new DatePicker(filterToEdit.getDate());
        dateDialog.getDialogPane().setContent(datePicker);
        dateDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<LocalDate> dateResult = dateDialog.showAndWait();
        if (dateResult.isPresent()) {
            filterToEdit.setDate(dateResult.get()); // Mise à jour du modèle [cite: 134]
        }

        // 3. Rafraîchir l'affichage sur le bouton
        selectedBtn.setText(filterToEdit.getName() + " (" + filterToEdit.displayDate() + ")");
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
