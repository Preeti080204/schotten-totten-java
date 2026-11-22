package com.schottentotten.view.screens;

import com.schottentotten.app.GameSettings;
import com.schottentotten.model.Difficulty;
import com.schottentotten.view.Screen;
import com.schottentotten.view.ScreenManager;
import com.schottentotten.view.SoundManager;
import com.schottentotten.view.Styles;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Preferences: sound, music, volume, reduced motion, computer pace, default difficulty and
 * hints. Every change is applied and saved immediately through {@link GameSettings}.
 */
public final class SettingsScreen implements Screen {

    private final ScreenManager sm;
    private final Parent root;

    /**
     * Builds the screen.
     *
     * @param sm           screen manager
     * @param returnToGame {@code true} to go back to the board when closed
     */
    public SettingsScreen(ScreenManager sm, boolean returnToGame) {
        this.sm = sm;
        GameSettings settings = GameSettings.getInstance();
        GridPane grid = new GridPane();
        grid.setHgap(26);
        grid.setVgap(18);
        ColumnConstraints c0 = new ColumnConstraints(230);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);

        addRow(grid, 0, "Sound effects", "Card, claim and victory sounds.",
                onOff(settings.isSoundEnabled(), settings::setSoundEnabled));
        addRow(grid, 1, "Music", "The Highland ambience loop.",
                onOff(settings.isMusicEnabled(), settings::setMusicEnabled));
        Slider volume = new Slider(0, 1, settings.volume());
        volume.setPrefWidth(320);
        Label percent = Styles.label(Math.round(settings.volume() * 100) + " %", "body-text");
        volume.valueProperty().addListener((o, a, b) -> percent.setText(Math.round(b.doubleValue() * 100) + " %"));
        volume.valueChangingProperty().addListener((o, was, changing) -> {
            if (!changing) {
                settings.setVolume(volume.getValue());
                sm.sound().play(SoundManager.Sfx.CLICK);
            }
        });
        volume.setOnMouseReleased(e -> settings.setVolume(volume.getValue()));
        HBox volumeBox = new HBox(14, volume, percent);
        volumeBox.setAlignment(Pos.CENTER_LEFT);
        addRow(grid, 2, "Volume", null, volumeBox);
        addRow(grid, 3, "Reduce motion", "Turns off card slides, pulsing highlights, confetti and drifting clouds.",
                onOff(settings.isReduceMotion(), settings::setReduceMotion));
        ToggleGroup speed = new ToggleGroup();
        HBox speeds = new HBox(10);
        for (GameSettings.Speed s : GameSettings.Speed.values()) {
            speeds.getChildren().add(choice(s.displayName(), speed, s, settings.speed() == s, v -> {
                settings.setSpeed(v);
                sm.context().controller().setAiDelay(v.aiDelayMillis());
            }));
        }
        addRow(grid, 4, "Computer pace", "Pause between computer moves. Also adjustable on the board.", speeds);
        ToggleGroup difficulty = new ToggleGroup();
        HBox diffs = new HBox(10,
                choice("Easy", difficulty, Difficulty.EASY, settings.defaultDifficulty() == Difficulty.EASY,
                        settings::setDefaultDifficulty),
                choice("Hard", difficulty, Difficulty.HARD, settings.defaultDifficulty() == Difficulty.HARD,
                        settings::setDefaultDifficulty));
        addRow(grid, 5, "Default difficulty", "Preselected for computer players on the set-up screen.", diffs);
        addRow(grid, 6, "Hints", "Show the Hint button and claim reminders during your turn.",
                onOff(settings.isShowHints(), settings::setShowHints));

        Button back = Styles.plaque(returnToGame ? "Back to the Game" : "Back", "plaque-primary");
        back.setCancelButton(true);
        back.setDefaultButton(true);
        back.setOnAction(e -> sm.back(returnToGame));
        HBox footer = new HBox(back);
        footer.setAlignment(Pos.CENTER_RIGHT);
        VBox content = new VBox(26, grid, footer);
        root = Backdrop.framed("Settings", content, 860, 960, 760);
    }

    private static void addRow(GridPane grid, int row, String title, String hint, Node control) {
        Label t = Styles.label(title, "subheading");
        VBox label = new VBox(2, t);
        if (hint != null) {
            label.getChildren().add(Styles.label(hint, "hint-text"));
        }
        GridPane.setValignment(label, VPos.CENTER);
        GridPane.setValignment(control, VPos.CENTER);
        grid.add(label, 0, row);
        grid.add(control, 1, row);
    }

    private Node onOff(boolean initial, Consumer<Boolean> setter) {
        ToggleGroup group = new ToggleGroup();
        HBox box = new HBox(8, choice("On", group, Boolean.TRUE, initial, setter),
                choice("Off", group, Boolean.FALSE, !initial, setter));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private <T> ToggleButton choice(String text, ToggleGroup group, T value, boolean selected, Consumer<T> setter) {
        ToggleButton t = new ToggleButton(text);
        t.getStyleClass().setAll("toggle-button", "switch");
        t.setToggleGroup(group);
        t.setSelected(selected);
        t.setOnAction(e -> {
            if (!t.isSelected()) {
                t.setSelected(true);
                return;
            }
            setter.accept(value);
            sm.sound().play(SoundManager.Sfx.CLICK);
        });
        return t;
    }

    @Override
    public Parent root() {
        return root;
    }
}
