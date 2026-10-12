package fr.kylian.photofilter;

import com.github.junrar.exception.RarException;
import fr.kylian.photofilter.analyzer.Analyzer;
import fr.kylian.photofilter.compressor.MediaCompressor;
import fr.kylian.photofilter.filter.FiltersSaver;
import fr.kylian.photofilter.filter.FilterMode;
import fr.kylian.photofilter.filter.Filter;
import javafx.application.HostServices;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javafx.event.ActionEvent;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class PhotoFilterController implements Initializable {

    @FXML private MenuButton folderSelect;
    @FXML private TextArea selectedFolder;
    @FXML private SplitMenuButton start;
    @FXML private ProgressBar progressBar;
    @FXML private TextArea enabledFilters;
    @FXML private MenuItem help;

    private static HostServices hostServices;
    private ObservableList<Filter> filters;
    private File folder;
    private javafx.concurrent.Task<?> activeTask;

    private final FiltersSaver filtersSaver = new FiltersSaver();

    public boolean isTaskRunning() {
        return activeTask != null && activeTask.isRunning();
    }

    public void cancelRunningTasks() {
        if (activeTask != null && activeTask.isRunning()) {
            activeTask.cancel(true);
        }
    }

    private void resetProgressBarDelayed() {
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
        pause.setOnFinished(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0.0);
        });
        pause.play();
    }

    public void setHostServices(HostServices hostServices) {
        this.hostServices = hostServices;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filters = FXCollections.observableArrayList();

        start.setDisable(true);

        // Load saved filters on startup
        Filter[] savedFilters = filtersSaver.loadFilters();
        if (savedFilters != null && savedFilters.length > 0) {
            filters.addAll(savedFilters);
        } else {
            // Fallback default filters
            FilterMode[] dayAndMonth = new FilterMode[]{FilterMode.DAY, FilterMode.MONTH};
            filters.add(new Filter(I18n.get("filters.christmasEve"), LocalDate.of(0, 12, 24), dayAndMonth));
            filters.add(new Filter(I18n.get("filters.christmas"), LocalDate.of(0, 12, 25), dayAndMonth));
            filters.add(new Filter(I18n.get("filters.newYearsEve"), LocalDate.of(0, 12, 31), dayAndMonth));
            filters.add(new Filter(I18n.get("filters.newYear"), LocalDate.of(0, 1, 1), dayAndMonth));
        }

        updateEnabledFiltersTextArea();

        help.setOnAction(e -> {
            this.openDoc();
        });
    }

    public static void openDoc() {
        if (hostServices != null) {
            hostServices.showDocument("https://photofilter.fr/documentation/");
        } else {
            try {
                if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI("https://photofilter.fr/documentation/"));
                } else {
                    Runtime.getRuntime().exec("xdg-open https://photofilter.fr/documentation/");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void updateEnabledFiltersTextArea() {
        StringBuilder sb = new StringBuilder();
        for (Filter filter : filters) {
            if (filter.isEnabled()) {
                sb.append("- ").append(filter.getName())
                  .append(" (").append(filter.displayDate()).append(")\n");
            }
        }
        enabledFilters.setText(sb.toString());
    }

    private boolean isArchive(File file) {
        if (file == null || file.isDirectory()) return false;
        String name = file.getName().toLowerCase();
        return name.endsWith(".zip") || name.endsWith(".7z") ||
               name.endsWith(".rar") || name.endsWith(".tar.gz") ||
               name.endsWith(".tgz");
    }

    private boolean processSelectedInput(File input) {
        if (input == null || !input.exists()) {
            this.selectedFolder.setText(I18n.get("noSelectedArchive"));
            this.folder = null;
            return false;
        }

        if (input.isDirectory()) {
            this.folder = input;
            this.selectedFolder.setText(input.getAbsolutePath());
            start.setDisable(false);
            return true;
        } else if (isArchive(input)) {
            String baseName = input.getName();
            int dotIndex = baseName.lastIndexOf('.');
            if (dotIndex > 0) {
                baseName = baseName.substring(0, dotIndex);
            }
            File extractDir = new File(input.getParentFile(), baseName + "_extracted");

            folderSelect.setDisable(true);
            start.setDisable(true);
            selectedFolder.setText(I18n.get("extraction") + input.getName() + I18n.get("enCours"));
            progressBar.setProgress(0.0);

            javafx.concurrent.Task<Void> decompressTask = new javafx.concurrent.Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    Analyzer analyzer = new Analyzer();
                    analyzer.decompressArchive(input, extractDir, (bytesRead, totalBytes) -> {
                        if (isCancelled()) {
                            return;
                        }
                        if (totalBytes > 0) {
                            updateProgress(bytesRead, totalBytes);
                        }
                    });
                    return null;
                }
            };

            activeTask = decompressTask;

            decompressTask.setOnSucceeded(e -> {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1.0);
                folderSelect.setDisable(false);
                start.setDisable(false);
                this.folder = extractDir;
                selectedFolder.setText(I18n.get("extraction.extractedArchive") + "\n" + extractDir.getAbsolutePath());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
                alert.setTitle(I18n.get("extraction.done"));
                alert.setHeaderText(null);
                alert.setContentText(I18n.get("extraction.archive") + input.getName() + I18n.get("extraction.successful"));
                alert.showAndWait();

                resetProgressBarDelayed();
            });

            decompressTask.setOnFailed(e -> {
                progressBar.progressProperty().unbind();
                progressBar.setProgress(0.0);
                folderSelect.setDisable(false);
                start.setDisable(true);
                Throwable ex = decompressTask.getException();
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
                alert.setTitle(I18n.get("extraction.error"));
                alert.setHeaderText(null);
                alert.setContentText(I18n.get("extraction.impossible") + ex.getMessage());
                alert.showAndWait();
                selectedFolder.setText(I18n.get("extraction.error"));
                this.folder = null;
            });

            progressBar.progressProperty().bind(decompressTask.progressProperty());
            Thread thread = new Thread(decompressTask);
            thread.setDaemon(true);
            thread.start();
            return true;
        } else {
            this.selectedFolder.setText(I18n.get("unsupportedFile"));
            this.folder = null;
            start.setDisable(true);
            return false;
        }
    }

    @FXML
    void selectFolderOnly(ActionEvent event) {
        Stage stage = (Stage) folderSelect.getScene().getWindow();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(I18n.get("selectImagesFolder"));

        File initialFolder = new File(System.getProperty("user.home"));
        if (initialFolder.exists()) {
            directoryChooser.setInitialDirectory(initialFolder);
        }

        File selectedFile = directoryChooser.showDialog(stage);
        boolean success = processSelectedInput(selectedFile);
        start.setDisable(!success);
    }

    @FXML
    void selectArchiveOnly(ActionEvent event) {
        Stage stage = (Stage) folderSelect.getScene().getWindow();
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle(I18n.get("selectArchive")); // (.zip, .7z, .rar, .tar.gz, .tgz)
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter(I18n.get("supportedArchives"), "*.zip", "*.7z", "*.rar", "*.tar.gz", "*.tgz"),
            new javafx.stage.FileChooser.ExtensionFilter(I18n.get("everyFiles"), "*.*")
        );

        File initialFolder = new File(System.getProperty("user.home"));
        if (initialFolder.exists()) {
            fileChooser.setInitialDirectory(initialFolder);
        }

        File selectedFile = fileChooser.showOpenDialog(stage);
        boolean success = processSelectedInput(selectedFile);
        start.setDisable(!success);
    }

    @FXML
    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
        }
    }

    @FXML
    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles() && !db.getFiles().isEmpty()) {
            File droppedFile = db.getFiles().get(0);
            success = processSelectedInput(droppedFile);
        }

        start.setDisable(!success);
        event.setDropCompleted(success);
        event.consume();
    }

    @FXML
    void openFiltersHandler(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("DateSelector.fxml"),
                    I18n.getBundle()
            );
            Parent root = loader.load();

            FiltersController controller = loader.getController();
            controller.setFilters(filters);

            Stage stage = new Stage();
            stage.setTitle("Set Filters");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(folderSelect.getScene().getWindow());
            stage.setScene(new Scene(root));
            stage.setResizable(true);

            stage.setOnShown(e -> {
                stage.setMinWidth(stage.getWidth());
                stage.setMinHeight(stage.getHeight());
            });

            stage.showAndWait();

            filtersSaver.saveFilters(filters.toArray(new Filter[0]));
            updateEnabledFiltersTextArea();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void startFiltering() {
        if (folder == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            alert.setTitle(I18n.get("alert.missingFolder"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("alert.selectFolder"));
            alert.showAndWait();
            return;
        }

        Stage stage = (Stage) folderSelect.getScene().getWindow();

        File initialDestination = new File(System.getProperty("user.home"));
        DirectoryChooser destSelection = new DirectoryChooser();
        destSelection.setTitle(I18n.get("selectDest"));
        destSelection.setInitialDirectory(initialDestination);

        File destination = destSelection.showDialog(stage);
        if (destination == null) {
            return;
        }

        start.setDisable(true);
        progressBar.setProgress(0.0);

        javafx.concurrent.Task<Void> filterTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Analyzer analyzer = new Analyzer();
                List<File> files = analyzer.getFiles(folder);

                int count = files.size();
                long enabledCount = filters.stream().filter(Filter::isEnabled).count();
                int totalWork = (int) (count * enabledCount);
                int[] workDone = {0};

                Runnable onProgress = () -> {
                    if (isCancelled()) {
                        return;
                    }
                    workDone[0]++;
                    updateProgress(workDone[0], totalWork);
                };

                for (Filter filter : filters) {
                    if (isCancelled()) {
                        break;
                    }
                    if (filter.isEnabled()) {
                        analyzer.putInFolder(destination, files, filter, onProgress);
                    }
                }
                return null;
            }
        };

        activeTask = filterTask;

        filterTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            start.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            alert.setTitle(I18n.get("alert.done"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("alter.filteringDone"));
            alert.showAndWait();
            resetProgressBarDelayed();
        });

        filterTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0.0);
            start.setDisable(false);
            Throwable exception = filterTask.getException();
            exception.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            alert.setTitle(I18n.get("alert.error"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("alert.filteringError") + exception.getMessage());
            alert.showAndWait();
        });

        progressBar.progressProperty().bind(filterTask.progressProperty());
        Thread thread = new Thread(filterTask);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void startCompressing(ActionEvent event) {
        if (folder == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            alert.setTitle(I18n.get("alert.missingFolder"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("alert.selectSourceDir"));
            alert.showAndWait();
            return;
        }

        Stage stage = (Stage) folderSelect.getScene().getWindow();

        File initialDestination = new File(System.getProperty("user.home"));
        DirectoryChooser destSelection = new DirectoryChooser();
        destSelection.setTitle(I18n.get("chooseDir"));
        destSelection.setInitialDirectory(initialDestination);

        File destination = destSelection.showDialog(stage);
        if (destination == null) {
            return;
        }

        start.setDisable(true);
        progressBar.setProgress(0.0);

        javafx.concurrent.Task<Void> filterTask = new javafx.concurrent.Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Analyzer analyzer = new Analyzer();
                List<File> files = analyzer.getFiles(folder);
                
                File compressionFolder = new File(destination, "Compression");
                if (!compressionFolder.exists()) {
                    compressionFolder.mkdirs();
                }

                MediaCompressor mediaCompressor = new MediaCompressor();

                int totalWork = files.size();
                int workDone = 0;

                for (File file : files) {
                    if (isCancelled()) {
                        break;
                    }

                    File destFile = new File(compressionFolder, file.getName());

                    try {
                        if (analyzer.isImage(file)) {
                            mediaCompressor.compressImage(file, destFile);
                        } else if (analyzer.isVideo(file)) {
                            mediaCompressor.compressVideo(file, destFile);
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de la compression de " + file.getName() + " : " + e.getMessage());
                    }

                    workDone++;
                    updateProgress(workDone, totalWork);
                }
                return null;
            }
        };

        activeTask = filterTask;

        filterTask.setOnSucceeded(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1.0);
            start.setDisable(false);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            alert.setTitle(I18n.get("alert.done"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("alert.compressionDone"));
            alert.showAndWait();
            resetProgressBarDelayed();
        });

        filterTask.setOnFailed(e -> {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0.0);
            start.setDisable(false);
            Throwable exception = filterTask.getException();
            exception.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.getDialogPane().getStylesheets().add(getClass().getResource("style.css").toExternalForm());
            alert.setTitle(I18n.get("alert.error"));
            alert.setHeaderText(null);
            alert.setContentText(I18n.get("alert.compressionError") + exception.getMessage());
            alert.showAndWait();
        });

        progressBar.progressProperty().bind(filterTask.progressProperty());
        Thread thread = new Thread(filterTask);
        thread.setDaemon(true);
        thread.start();
    }
}
