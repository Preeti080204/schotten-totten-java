package com.schottentotten.app;

import com.schottentotten.cli.CommandLineView;
import com.schottentotten.view.SchottenTottenApp;
import javafx.application.Application;

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Main entry point of the graphical version. It is deliberately not a JavaFX
 * {@code Application} subclass so that the executable JAR also starts on a plain class path.
 *
 * <pre>
 *   mvn javafx:run                         window only
 *   mvn javafx:run -Djavafx.args=--with-cli    window + command line on the same live game
 *   java -jar target/schotten-totten-1.0.0-all.jar [--with-cli] [--no-color]
 * </pre>
 */
public final class Launcher {

    private Launcher() {
    }

    /**
     * Starts the window, optionally together with a command line reading this terminal.
     *
     * @param args {@code --with-cli} also starts the command line; {@code --no-color} disables
     *             ANSI colours
     */
    public static void main(String[] args) {
        AppContext context = AppContext.create();
        boolean withCli = Arrays.asList(args).contains("--with-cli");
        Runnable startCli = null;
        if (withCli) {
            startCli = () -> {
                Thread cli = new Thread(() -> new CommandLineView(context.controller(), context.rules(),
                        new InputStreamReader(System.in, StandardCharsets.UTF_8),
                        new PrintStream(System.out, true, StandardCharsets.UTF_8),
                        CliLauncher.useColor(args), true).run(), "command-line");
                cli.setDaemon(true);
                cli.start();
            };
        }
        SchottenTottenApp.configure(context, startCli);
        Application.launch(SchottenTottenApp.class, args);
    }
}
