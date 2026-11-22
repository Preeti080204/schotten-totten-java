package com.schottentotten.view.screens;

import com.schottentotten.controller.GameController;
import com.schottentotten.model.GameState;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveFormatException;
import com.schottentotten.view.Assets;
import com.schottentotten.view.Dialogs;
import com.schottentotten.view.Screen;
import com.schottentotten.view.ScreenManager;
import com.schottentotten.view.SoundManager;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.CoverImage;
import com.schottentotten.view.widgets.Ribbon;
import com.schottentotten.view.widgets.ScalingPane;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Title screen: a Highlands panorama with the two rival clan villages and the line of border
 * Stones, drifting clouds, the game title and the main actions.
 */
public final class MainMenuScreen implements Screen {

    private final ScreenManager sm;
    private final StackPane root = new StackPane();
    private final List<Animation> animations = new ArrayList<>();

    /**
     * Builds the screen.
     *
     * @param sm screen manager
     */
    public MainMenuScreen(ScreenManager sm) {
        this.sm = sm;
        CoverImage landscape = new CoverImage(Assets.image("menu-landscape.jpg"), 0.6);
        Pane clouds = new Pane();
        clouds.setMouseTransparent(true);
        Region shade = new Region();
        shade.setMouseTransparent(true);
        shade.setBackground(new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(10, 20, 30, 0.25)), new Stop(0.35, Color.TRANSPARENT),
                        new Stop(0.7, Color.TRANSPARENT), new Stop(1, Color.rgb(15, 10, 4, 0.55))),
                null, null)));

        Text title = new Text("Schotten Totten");
        title.setFont(Font.font(Assets.TITLE_FONT, 92));
        title.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF7DD")), new Stop(1, Color.web("#F2D27A"))));
        title.setStroke(Color.web("#4A2A10"));
        title.setStrokeWidth(2.5);
        title.setEffect(new DropShadow(18, 0, 5, Color.rgb(20, 10, 0, 0.8)));
        Ribbon subtitle = new Ribbon("Battle for the Border Stones", 24);

        VBox menu = Styles.parchment(new VBox(12));
        menu.setPadding(new Insets(26, 34, 26, 34));
        menu.setAlignment(Pos.CENTER);
        Button newGame = menuButton("New Game", "plaque-primary");
        newGame.setOnAction(e -> {
            click();
            sm.showSetup();
        });
        menu.getChildren().add(newGame);
        Path autosave = GameScreen.autosavePath();
        if (Files.exists(autosave)) {
            Button resume = menuButton("Continue", "plaque-emerald");
            resume.setOnAction(e -> {
                click();
                resume(autosave);
            });
            menu.getChildren().add(resume);
        }
        Button load = menuButton("Load Game");
        load.setOnAction(e -> {
            click();
            sm.showLoad();
        });
        Button rules = menuButton("Rules & Tutorial");
        rules.setOnAction(e -> {
            click();
            sm.showRules(false);
        });
        Button settings = menuButton("Settings");
        settings.setOnAction(e -> {
            click();
            sm.showSettings(false);
        });
        Button quit = menuButton("Quit");
        quit.setOnAction(e -> Dialogs.confirm(sm, "Leave the Highlands?", "Do you really want to quit the game?",
                "Quit", "Stay", sm::quit));
        menu.getChildren().addAll(load, rules, settings, quit);
        menu.setMaxWidth(Region.USE_PREF_SIZE);

        VBox column = new VBox(6, title, subtitle, spacer(26), menu);
        column.setAlignment(Pos.TOP_CENTER);
        column.setPadding(new Insets(40, 0, 0, 0));
        column.setPickOnBounds(false);
        ScalingPane scaled = new ScalingPane(column, 1200, 790);
        scaled.setPickOnBounds(false);
        StackPane.setMargin(scaled, new Insets(0, 0, 34, 0));

        Label footer = Styles.label("Original electronic edition of Reiner Knizia's Schotten Totten - LO02 project. "
                + "All illustrations, sounds and the music were created for this edition.", "hint-text", "light-text");
        footer.setStyle("-fx-text-fill: #FBF3DC; -fx-effect: dropshadow(gaussian, black, 4, 0.4, 0, 1);");
        StackPane.setAlignment(footer, Pos.BOTTOM_CENTER);
        StackPane.setMargin(footer, new Insets(0, 0, 14, 0));

        root.getChildren().addAll(landscape, clouds, shade, scaled, footer);
        root.getStyleClass().add("screen");
        if (!sm.reduceMotion()) {
            animate(landscape, clouds);
        }
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private Button menuButton(String text, String... extra) {
        Button b = Styles.plaque(text, extra);
        b.getStyleClass().add("menu-button-big");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void click() {
        sm.sound().play(SoundManager.Sfx.CLICK);
    }

    private void animate(CoverImage landscape, Pane clouds) {
        ScaleTransition zoom = new ScaleTransition(Duration.seconds(38), landscape.imageView());
        zoom.setFromX(1);
        zoom.setFromY(1);
        zoom.setToX(1.06);
        zoom.setToY(1.06);
        zoom.setAutoReverse(true);
        zoom.setCycleCount(Animation.INDEFINITE);
        zoom.setInterpolator(Interpolator.EASE_BOTH);
        animations.add(zoom);
        double[][] specs = {{0.08, 170, 38, 70}, {0.18, 240, 46, 95}, {0.04, 130, 30, 55}, {0.26, 200, 40, 120}};
        for (int i = 0; i < specs.length; i++) {
            double[] s = specs[i];
            Ellipse cloud = new Ellipse(s[1], s[2]);
            cloud.setFill(Color.rgb(255, 255, 255, 0.38));
            cloud.setEffect(new GaussianBlur(28));
            cloud.layoutYProperty().bind(root.heightProperty().multiply(s[0]));
            clouds.getChildren().add(cloud);
            TranslateTransition drift = new TranslateTransition(Duration.seconds(s[3]), cloud);
            drift.fromXProperty().bind(root.widthProperty().multiply(-0.15));
            drift.toXProperty().bind(root.widthProperty().multiply(1.15));
            drift.setCycleCount(Animation.INDEFINITE);
            drift.setInterpolator(Interpolator.LINEAR);
            drift.jumpTo(Duration.seconds(s[3] * (0.2 + 0.23 * i)));
            animations.add(drift);
        }
    }

    private void resume(Path file) {
        GameController controller = sm.context().controller();
        try {
            GameState probe = controller.saves().load(file);
            if (probe.phase() == TurnPhase.MATCH_OVER) {
                Dialogs.info(sm, "Nothing to continue", "The last game is already finished. Start a new game!");
                return;
            }
            controller.load(file);
            sm.showGame();
        } catch (SaveFormatException ex) {
            Dialogs.info(sm, "Cannot continue", ex.getMessage());
        }
    }

    @Override
    public Parent root() {
        return root;
    }

    @Override
    public void onShow() {
        animations.forEach(Animation::play);
    }

    @Override
    public void onHide() {
        animations.forEach(Animation::stop);
    }
}
