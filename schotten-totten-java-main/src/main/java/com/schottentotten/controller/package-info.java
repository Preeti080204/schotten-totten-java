/**
 * Controller layer of the MVC architecture, shared by the two competing views.
 *
 * <p>A {@link com.schottentotten.controller.GameSession} owns the live game: it applies
 * actions atomically (on a copy, committed only if the engine accepts them) and notifies every
 * {@link com.schottentotten.controller.SessionListener} with an immutable snapshot. The
 * {@link com.schottentotten.controller.SessionHub} holds the active session, so the graphical
 * view and the command line always act on the same game, and starts the
 * {@link com.schottentotten.controller.VirtualPlayerDriver} that plays for computer players.
 * {@link com.schottentotten.controller.GameController} is the facade used by the views (new
 * game, save, load, hint); {@link com.schottentotten.controller.ParameterChooser} and
 * {@link com.schottentotten.controller.TextDescriber} let both views ask for card targets and
 * describe moves in the same way.</p>
 */
package com.schottentotten.controller;
