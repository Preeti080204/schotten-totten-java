package com.schottentotten.app;

import com.schottentotten.cli.CommandLineView;

import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Entry point of the command-line version (no window).
 *
 * <pre>
 *   mvn exec:java
 *   java -cp target/schotten-totten-1.0.0-all.jar com.schottentotten.app.CliLauncher [--no-color]
 * </pre>
 */
public final class CliLauncher {

    private CliLauncher() {
    }

    /**
     * Starts the command line.
     *
     * @param args {@code --no-color} disables ANSI colours
     */
    public static void main(String[] args) {
        AppContext context = AppContext.create();
        CommandLineView cli = new CommandLineView(context.controller(), context.rules(),
                new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new PrintStream(System.out, true, StandardCharsets.UTF_8), useColor(args), false);
        cli.run();
        context.shutdown();
        System.exit(0);
    }

    static boolean useColor(String[] args) {
        return System.console() != null && System.getenv("NO_COLOR") == null
                && Arrays.stream(args).noneMatch("--no-color"::equals);
    }
}
