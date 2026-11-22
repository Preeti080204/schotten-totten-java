package com.schottentotten.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.GameState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves and loads games as versioned JSON files.
 *
 * <p>Files are written atomically (temporary file then move), so a crash never leaves a
 * half-written save. Loading checks the format version and the full integrity of the game
 * (every card exactly once) before returning a state.</p>
 */
public final class SaveManager {

    /** Current save format version. */
    public static final int FORMAT_VERSION = 1;
    /** Application identifier written in every save file. */
    public static final String APPLICATION = "schotten-totten";
    /** File extension of save files. */
    public static final String EXTENSION = ".stsave.json";

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final GameStateMapper stateMapper;

    /**
     * Creates a save manager.
     *
     * @param catalog card catalogue used to resolve card identifiers
     */
    public SaveManager(CardCatalog catalog) {
        this.stateMapper = new GameStateMapper(catalog);
    }

    /**
     * Returns the default directory for save files ({@code ~/.schotten-totten/saves}, or the
     * {@code schotten.saves} system property when set).
     *
     * @return save directory
     */
    public static Path defaultDirectory() {
        String override = System.getProperty("schotten.saves");
        return override != null ? Path.of(override)
                : Path.of(System.getProperty("user.home"), ".schotten-totten", "saves");
    }

    /**
     * Serialises a state to JSON text.
     *
     * @param state the state
     * @return JSON text
     */
    public String toJson(GameState state) {
        try {
            return mapper.writeValueAsString(new SaveFileDto(FORMAT_VERSION, APPLICATION, Instant.now().toString(),
                    stateMapper.toDto(state)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialise the game", e);
        }
    }

    /**
     * Parses a state from JSON text.
     *
     * @param json JSON text
     * @return the state
     * @throws SaveFormatException if the text is not a valid, compatible save
     */
    public GameState fromJson(String json) throws SaveFormatException {
        SaveFileDto file;
        try {
            file = mapper.readValue(json, SaveFileDto.class);
        } catch (IOException e) {
            throw new SaveFormatException("This is not a Schotten Totten save file (invalid JSON).", e);
        }
        if (file == null || !APPLICATION.equals(file.application())) {
            throw new SaveFormatException("This is not a Schotten Totten save file.");
        }
        if (file.formatVersion() != FORMAT_VERSION) {
            throw new SaveFormatException("This save uses format version " + file.formatVersion()
                    + ", but this version of the game reads version " + FORMAT_VERSION + ".");
        }
        return stateMapper.fromDto(file.game());
    }

    /**
     * Saves a state to a file.
     *
     * @param state the state
     * @param file  destination path
     * @throws IOException if the file cannot be written
     */
    public void save(GameState state, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, toJson(state), StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Loads a state from a file.
     *
     * @param file source path
     * @return the state
     * @throws SaveFormatException if the file is missing, unreadable or invalid
     */
    public GameState load(Path file) throws SaveFormatException {
        try {
            return fromJson(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SaveFormatException("Cannot read " + file.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Lists save files of a directory, most recent first.
     *
     * @param directory directory to scan
     * @return save files
     */
    public static List<Path> listSaves(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(directory)) {
            return files.filter(p -> p.getFileName().toString().endsWith(EXTENSION))
                    .sorted((a, b) -> Long.compare(b.toFile().lastModified(), a.toFile().lastModified()))
                    .toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
