package fr.kylian.photofilter.analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.kylian.photofilter.filter.Filter;

import java.io.File;
import java.io.IOException;

public class FiltersSaver {

    private static final File saveFile = new File("filters.json");

    // Création de l'ObjectMapper configuré avec les modules Date et Optional
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            // Optionnel mais recommandé : sauvegarde les dates sous un beau format (ex: "2026-12-25")
            // au lieu d'un format timestamp numérique illisible
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void saveFilters(Filter[] filters) {
        try {
            mapper.writeValue(saveFile, filters);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Filter[] loadFilters() {
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