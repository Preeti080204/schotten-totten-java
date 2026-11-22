package com.schottentotten.view;

import com.schottentotten.app.AppContext;
import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * JavaFX application: builds the window and shows the main menu. The {@link AppContext} is
 * provided by the launcher so that a command line started together with the window shares the
 * same live game.
 */
public final class SchottenTottenApp extends Application {

    private static volatile AppContext sharedContext;
    private static volatile Runnable afterStart;

    private AppContext context;

    /** Creates the application; called by the JavaFX launcher. */
    public SchottenTottenApp() {
    }

    /**
     * Provides the context to use (must be called before {@link Application#launch}).
     *
     * @param context    application context
     * @param onStarted  action run on the JavaFX thread once the window is visible (may be {@code null})
     */
    public static void configure(AppContext context, Runnable onStarted) {
        sharedContext = context;
        afterStart = onStarted;
    }

    @Override
    public void start(Stage stage) {
        context = sharedContext != null ? sharedContext : AppContext.create();
        Assets.loadFonts();
        SoundManager sound = new SoundManager();
        ScreenManager screens = new ScreenManager(stage, context, sound);
        stage.setTitle("Schotten Totten - Battle for the Border Stones");
        stage.getIcons().add(new Image(Assets.url("images/stone-tile.png").toExternalForm()));
        stage.setScene(screens.scene());
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
        stage.setWidth(Math.min(1440, bounds.getWidth() * 0.94));
        stage.setHeight(Math.min(900, bounds.getHeight() * 0.94));
        stage.centerOnScreen();
        screens.showMainMenu();
        stage.show();
        sound.startMusic();
        String screenshotDir = System.getProperty("schotten.screenshots");
        if (screenshotDir != null) {
            new ScreenshotTour(screens, java.nio.file.Path.of(screenshotDir)).start();
        }
        if (afterStart != null) {
            afterStart.run();
        }
    }

    @Override
    public void stop() {
        if (context != null) {
            context.shutdown();
        }
    }
}
