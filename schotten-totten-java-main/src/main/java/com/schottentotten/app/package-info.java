/**
 * Application entry points and application-wide services.
 *
 * <p>{@link com.schottentotten.app.Launcher} starts the graphical interface (optionally with the
 * command line attached to the same game), {@link com.schottentotten.app.CliLauncher} starts
 * the command line alone. {@link com.schottentotten.app.AppContext} wires the engine, the
 * session hub and the controller once, and {@link com.schottentotten.app.GameSettings} is the
 * Singleton that holds the player's preferences.</p>
 */
package com.schottentotten.app;
