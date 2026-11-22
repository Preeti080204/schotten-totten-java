package com.schottentotten.view.screens;

import com.schottentotten.controller.GameController;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveFormatException;
import com.schottentotten.persistence.SaveManager;
import com.schottentotten.view.Dialogs;
import com.schottentotten.view.Screen;
import com.schottentotten.view.ScreenManager;
import com.schottentotten.view.SoundManager;
import com.schottentotten.view.Styles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Saved games: one parchment row per save with its players, variant and progress. Damaged or
 * incompatible files are listed with the reason instead of being hidden.
 */
public final class LoadScreen implements Screen {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm");

    private final ScreenManager sm;
    private final GameController controller;
    private final VBox list = new VBox(10);
    private final Parent root;

    /**
     * Builds the screen.
     *
     * @param sm screen manager
     */
    public LoadScreen(ScreenManager sm) {
        this.sm = sm;
        this.controller = sm.context().controller();
        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefViewportHeight(470);
        list.setPadding(new Insets(4, 10, 4, 4));
        Label where = Styles.label("Saved games are kept in " + SaveManager.defaultDirectory(), "hint-text");
        Button back = Styles.plaque("Back");
        back.setCancelButton(true);
        back.setOnAction(e -> sm.back(true));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox footer = new HBox(16, where, gap, back);
        footer.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(16, scroll, footer);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root = Backdrop.framed("Saved Games", content, 900, 1000, 720);
        refresh();
    }

    private void refresh() {
        list.getChildren().clear();
        List<Path> saves = SaveManager.listSaves(SaveManager.defaultDirectory());
        if (saves.isEmpty()) {
            Label none = Styles.label("No saved games yet. During a game, open the Menu and choose Save Game.",
                    "body-text");
            none.setPadding(new Insets(30, 0, 30, 0));
            list.getChildren().add(none);
            return;
        }
        for (Path file : saves) {
            list.getChildren().add(row(file));
        }
    }

    private Region row(Path file) {
        String name = file.getFileName().toString();
        name = name.substring(0, name.length() - SaveManager.EXTENSION.length());
        if (name.equals("autosave")) {
            name = "Autosave (last game)";
        }
        Label title = Styles.label(name, "subheading");
        Label details;
        boolean loadable = true;
        try {
            details = Styles.label(describe(controller.saves().load(file)) + "  |  saved " + modified(file), "body-text");
        } catch (SaveFormatException e) {
            details = Styles.label("Cannot be opened: " + e.getMessage(), "body-text");
            details.setStyle("-fx-text-fill: #8E1F2A;");
            loadable = false;
        }
        VBox text = new VBox(4, title, details);
        HBox.setHgrow(text, Priority.ALWAYS);
        Button load = Styles.plaque("Load", "plaque-primary", "plaque-small");
        load.setDisable(!loadable);
        load.setOnAction(e -> load(file));
        Button delete = Styles.plaque("Delete", "plaque-small");
        String shown = name;
        delete.setOnAction(e -> Dialogs.confirm(sm, "Delete save?", "Delete \"" + shown + "\" permanently?",
                "Delete", "Keep", () -> delete(file)));
        load.setMinWidth(Region.USE_PREF_SIZE);
        delete.setMinWidth(Region.USE_PREF_SIZE);
        HBox row = new HBox(14, text, load, delete);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("parchment-card");
        row.setPadding(new Insets(12, 16, 12, 16));
        return row;
    }

    private static String describe(GameState s) {
        GameConfiguration c = s.config();
        String progress = s.phase() == TurnPhase.MATCH_OVER ? "finished"
                : "round " + s.round() + (c.isMultiRound() ? " of " + c.rounds() : "") + ", turn " + s.turn();
        return who(c.player1()) + " vs " + who(c.player2()) + "  |  "
                + (c.tacticVariant() ? "Tactic variant" : "Base game") + (c.expansions() ? " + expansion" : "")
                + "  |  " + progress;
    }

    private static String who(PlayerProfile p) {
        return p.name() + (p.isVirtual() ? " (computer, " + p.difficulty().name().toLowerCase() + ")" : "");
    }

    private static String modified(Path file) {
        try {
            Instant t = Files.getLastModifiedTime(file).toInstant();
            return DATE.format(t.atZone(ZoneId.systemDefault()));
        } catch (IOException e) {
            return "unknown date";
        }
    }

    private void load(Path file) {
        sm.sound().play(SoundManager.Sfx.CLICK);
        try {
            controller.load(file);
            sm.showGame();
        } catch (SaveFormatException e) {
            Dialogs.info(sm, "Cannot load this game", e.getMessage());
        }
    }

    private void delete(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            Dialogs.info(sm, "Cannot delete", e.getMessage());
        }
        refresh();
    }

    @Override
    public Parent root() {
        return root;
    }
}
