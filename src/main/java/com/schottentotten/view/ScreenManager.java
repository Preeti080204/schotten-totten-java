package com.schottentotten.view;

import com.schottentotten.app.AppContext;
import com.schottentotten.app.GameSettings;
import com.schottentotten.view.screens.GameScreen;
import com.schottentotten.view.screens.LoadScreen;
import com.schottentotten.view.screens.MainMenuScreen;
import com.schottentotten.view.screens.RulesScreen;
import com.schottentotten.view.screens.SettingsScreen;
import com.schottentotten.view.screens.SetupScreen;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Owns the window content: the current screen, modal overlays (custom dialogs) and toast
 * messages. Screens navigate through it, so there is a single place for transitions.
 */
public final class ScreenManager {

    /**
     * Handle of an open overlay.
     */
    public final class Overlay {
        private final StackPane layer;
        private Runnable onClose;

        private Overlay(StackPane layer) {
            this.layer = layer;
        }

        /**
         * Closes the overlay.
         */
        public void close() {
            if (!overlays.remove(this)) {
                return;
            }
            fade(layer, 1, 0, 160, () -> overlayLayer.getChildren().remove(layer));
            if (onClose != null) {
                onClose.run();
            }
        }

        /**
         * Registers an action run when the overlay closes.
         *
         * @param action action
         */
        public void setOnClose(Runnable action) {
            this.onClose = action;
        }
    }

    private final Stage stage;
    private final AppContext context;
    private final SoundManager sound;
    private final StackPane root = new StackPane();
    private final StackPane screenLayer = new StackPane();
    private final StackPane overlayLayer = new StackPane();
    private final StackPane toastLayer = new StackPane();
    private final Deque<Overlay> overlays = new ArrayDeque<>();
    private final Scene scene;
    private Screen current;
    private GameScreen gameScreen;

    /**
     * Creates the manager and the scene.
     *
     * @param stage   main window
     * @param context application context
     * @param sound   sound manager
     */
    public ScreenManager(Stage stage, AppContext context, SoundManager sound) {
        this.stage = stage;
        this.context = context;
        this.sound = sound;
        overlayLayer.setPickOnBounds(false);
        toastLayer.setPickOnBounds(false);
        toastLayer.setMouseTransparent(true);
        toastLayer.setAlignment(Pos.BOTTOM_CENTER);
        root.getChildren().addAll(screenLayer, overlayLayer, toastLayer);
        root.getStyleClass().add("screen");
        scene = new Scene(root, 1440, 900);
        scene.getStylesheets().add(Assets.stylesheet());
        context.hub().addSessionChangeListener(session -> javafx.application.Platform.runLater(() -> {
            if (session != null && context.hub().current().orElse(null) == session) {
                showGame();
            }
        }));
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ESCAPE && !overlays.isEmpty() && overlays.peekLast().layer.getUserData() != null) {
                overlays.peekLast().close();
                e.consume();
            }
        });
    }

    /**
     * Returns the scene.
     *
     * @return scene
     */
    public Scene scene() {
        return scene;
    }

    /**
     * Returns the window.
     *
     * @return stage
     */
    public Stage stage() {
        return stage;
    }

    /**
     * Returns the application context.
     *
     * @return context
     */
    public AppContext context() {
        return context;
    }

    /**
     * Returns the sound manager.
     *
     * @return sound
     */
    public SoundManager sound() {
        return sound;
    }

    /**
     * Returns the screen currently shown.
     *
     * @return current screen ({@code null} before the first screen)
     */
    public Screen current() {
        return current;
    }

    /**
     * Tells whether animations should be reduced.
     *
     * @return reduced-motion flag
     */
    public boolean reduceMotion() {
        return GameSettings.getInstance().isReduceMotion();
    }

    /**
     * Replaces the current screen with a cross-fade.
     *
     * @param screen new screen
     */
    public void show(Screen screen) {
        closeAllOverlays();
        if (screen == current) {
            return;
        }
        Screen previous = current;
        if (previous != null) {
            previous.onHide();
        }
        current = screen;
        Parent node = screen.root();
        screenLayer.getChildren().add(node);
        if (previous != null) {
            Node old = previous.root();
            fade(node, 0, 1, 260, () -> screenLayer.getChildren().remove(old));
        }
        screen.onShow();
        node.requestFocus();
    }

    /**
     * Closes the window and exits the application.
     */
    public void quit() {
        stage.close();
        javafx.application.Platform.exit();
    }

    /**
     * Shows the main menu.
     */
    public void showMainMenu() {
        show(new MainMenuScreen(this));
    }

    /**
     * Shows the new-game set-up screen.
     */
    public void showSetup() {
        show(new SetupScreen(this));
    }

    /**
     * Shows the game board for the active session.
     */
    public void showGame() {
        if (gameScreen == null) {
            gameScreen = new GameScreen(this);
        }
        show(gameScreen);
    }

    /**
     * Shows the rules and tutorial.
     *
     * @param returnToGame {@code true} to come back to the board when closed
     */
    public void showRules(boolean returnToGame) {
        show(new RulesScreen(this, returnToGame));
    }

    /**
     * Shows the settings.
     *
     * @param returnToGame {@code true} to come back to the board when closed
     */
    public void showSettings(boolean returnToGame) {
        show(new SettingsScreen(this, returnToGame));
    }

    /**
     * Shows the saved-games screen.
     */
    public void showLoad() {
        show(new LoadScreen(this));
    }

    /**
     * Returns to the board if a game is active, else to the main menu.
     *
     * @param toGame {@code true} to prefer the board
     */
    public void back(boolean toGame) {
        if (toGame && context.hub().current().isPresent()) {
            showGame();
        } else {
            showMainMenu();
        }
    }

    /**
     * Opens a modal overlay with a veil.
     *
     * @param panel       content (usually a parchment panel)
     * @param dismissable {@code true} if Escape closes it
     * @return overlay handle
     */
    public Overlay openOverlay(Node panel, boolean dismissable) {
        Region veil = new Region();
        veil.getStyleClass().add("modal-veil");
        StackPane layer = new StackPane(veil, panel);
        layer.setAlignment(Pos.CENTER);
        if (dismissable) {
            layer.setUserData(Boolean.TRUE);
        }
        if (panel instanceof Region r) {
            r.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        }
        Overlay overlay = new Overlay(layer);
        overlays.addLast(overlay);
        overlayLayer.getChildren().add(layer);
        fade(layer, 0, 1, 180, null);
        if (!reduceMotion()) {
            ScaleTransition st = new ScaleTransition(Duration.millis(220), panel);
            st.setFromX(0.92);
            st.setFromY(0.92);
            st.setToX(1);
            st.setToY(1);
            st.play();
        }
        panel.requestFocus();
        return overlay;
    }

    /**
     * Closes every overlay.
     */
    public void closeAllOverlays() {
        while (!overlays.isEmpty()) {
            overlays.peekLast().close();
        }
    }

    /**
     * Tells whether a modal overlay is open.
     *
     * @return overlay flag
     */
    public boolean hasOverlay() {
        return !overlays.isEmpty();
    }

    /**
     * Shows a short message at the bottom of the window.
     *
     * @param message text
     * @param error   {@code true} for a refusal or error
     */
    public void toast(String message, boolean error) {
        Label label = new Label(message);
        label.getStyleClass().add("toast");
        if (error) {
            label.getStyleClass().add("toast-error");
        }
        label.setWrapText(true);
        label.setMaxWidth(640);
        StackPane.setMargin(label, new javafx.geometry.Insets(0, 0, 36, 0));
        toastLayer.getChildren().removeIf(n -> n instanceof Label);
        toastLayer.getChildren().add(label);
        FadeTransition in = new FadeTransition(Duration.millis(reduceMotion() ? 1 : 180), label);
        in.setFromValue(0);
        in.setToValue(1);
        PauseTransition stay = new PauseTransition(Duration.seconds(error ? 3.6 : 2.6));
        FadeTransition out = new FadeTransition(Duration.millis(400), label);
        out.setToValue(0);
        SequentialTransition seq = new SequentialTransition(in, stay, out);
        seq.setOnFinished(e -> toastLayer.getChildren().remove(label));
        seq.play();
        if (error) {
            sound.play(SoundManager.Sfx.ERROR);
        }
    }

    /**
     * Rains tartan-coloured confetti over the window (skipped with reduced motion).
     */
    public void confetti() {
        if (reduceMotion()) {
            return;
        }
        javafx.scene.layout.Pane pane = new javafx.scene.layout.Pane();
        pane.setMouseTransparent(true);
        double w = scene.getWidth();
        double h = scene.getHeight();
        java.util.Random random = new java.util.Random();
        javafx.scene.paint.Color[] colors = {Palette.clan(com.schottentotten.model.ClanColor.RED),
                Palette.clan(com.schottentotten.model.ClanColor.YELLOW), Palette.clan(com.schottentotten.model.ClanColor.GREEN),
                Palette.clan(com.schottentotten.model.ClanColor.BLUE), Palette.clan(com.schottentotten.model.ClanColor.PURPLE),
                Palette.GOLD, Palette.PARCHMENT};
        for (int i = 0; i < 150; i++) {
            javafx.scene.shape.Rectangle piece = new javafx.scene.shape.Rectangle(7 + random.nextDouble() * 6,
                    10 + random.nextDouble() * 8, colors[random.nextInt(colors.length)]);
            piece.setArcWidth(3);
            piece.setArcHeight(3);
            piece.setLayoutX(random.nextDouble() * w);
            piece.setLayoutY(-30);
            pane.getChildren().add(piece);
            double seconds = 2.6 + random.nextDouble() * 2.8;
            javafx.animation.TranslateTransition fall = new javafx.animation.TranslateTransition(Duration.seconds(seconds), piece);
            fall.setByY(h + 80);
            fall.setByX((random.nextDouble() - 0.5) * 240);
            fall.setDelay(Duration.seconds(random.nextDouble() * 1.4));
            javafx.animation.RotateTransition spin = new javafx.animation.RotateTransition(Duration.seconds(seconds), piece);
            spin.setByAngle((random.nextBoolean() ? 1 : -1) * (360 + random.nextDouble() * 720));
            spin.setDelay(fall.getDelay());
            fall.play();
            spin.play();
        }
        toastLayer.getChildren().add(0, pane);
        PauseTransition cleanup = new PauseTransition(Duration.seconds(7));
        cleanup.setOnFinished(e -> toastLayer.getChildren().remove(pane));
        cleanup.play();
    }

    private void fade(Node node, double from, double to, double millis, Runnable after) {
        if (reduceMotion()) {
            node.setOpacity(to);
            if (after != null) {
                after.run();
            }
            return;
        }
        FadeTransition ft = new FadeTransition(Duration.millis(millis), node);
        ft.setFromValue(from);
        ft.setToValue(to);
        if (after != null) {
            ft.setOnFinished(e -> after.run());
        }
        new ParallelTransition(ft).play();
    }
}
