package fr.kylian.photofilter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;

public class FiltersSaver {

    private final File saveFile;

    public FiltersSaver() {
        String os = System.getProperty("os.name").toLowerCase();
        String saveDir;

        if (os.contains("win")) {
            saveDir = System.getenv("APPDATA") + File.separator + "PhotoFilter";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            saveDir = System.getProperty("user.home") + File.separator + ".config" + File.separator + "PhotoFilter";
        } else {
            saveDir = System.getProperty("user.home") + File.separator + ".PhotoFilter";
        }

        File dir = new File(saveDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        this.saveFile = new File(dir, "filters.json");
    }

    // Création de l'ObjectMapper configuré avec les modules Date et Optional
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            // Optionnel mais recommandé : sauvegarde les dates sous un beau format (ex: "2026-12-25")
            // au lieu d'un format timestamp numérique illisible
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Saves the filters in a json format
     * @param filters The filters to be saved
     */
    public void saveFilters(Filter[] filters) {
        try {
            mapper.writeValue(saveFile, filters);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads the filters from the json save file
     * @return An array of the loaded filters
     */
    public Filter[] loadFilters() {
        if (!saveFile.exists()) {
            return new Filter[0];
        }

        try {
            // Lecture en tant que tableau : Filter[].class
            return mapper.readValue(saveFile, Filter[].class);
        } catch (IOException e) {
            e.printStackTrace();
            return new Filter[0];
        }
    }
}