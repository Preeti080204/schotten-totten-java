package com.schottentotten.view;

import com.schottentotten.ai.PlayerStrategy;
import com.schottentotten.ai.PlayerView;
import com.schottentotten.ai.StrategyFactory;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.IllegalActionException;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveManager;
import com.schottentotten.util.SeededRandom;
import com.schottentotten.view.widgets.CardView;
import javafx.animation.PauseTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Visual QA tool: drives the real screens through a scripted tour (menu, set-up, a game in
 * progress, a Tactic card interaction, the hot-seat curtain, the victory screen, rules,
 * settings and saved games) and writes a PNG of the window at each step, at two window sizes.
 *
 * <p>Started only when the {@code schotten.screenshots} system property names an output
 * directory; it is never active in normal play. Point {@code schotten.saves} and
 * {@code schotten.settings} to a scratch directory so the tour leaves no trace.</p>
 */
final class ScreenshotTour {

    private record Step(Runnable action, double waitMillis, String shot) {
    }

    private final ScreenManager sm;
    private final Path directory;
    private final GameEngine engine;
    private final List<Step> steps = new ArrayList<>();

    ScreenshotTour(ScreenManager sm, Path directory) {
        this.sm = sm;
        this.directory = directory;
        this.engine = sm.context().engine();
    }

    /** Builds and runs the tour, then quits the application. */
    void start() {
        addSize(1440, 900, "");
        addSize(1280, 720, "-1280x720");
        steps.add(new Step(sm::quit, 100, null));
        run(0);
    }

    private void addSize(double width, double height, String suffix) {
        steps.add(new Step(() -> {
            sm.stage().setWidth(width);
            sm.stage().setHeight(height);
            sm.showMainMenu();
        }, 1600, "01-main-menu" + suffix));
        steps.add(new Step(sm::showSetup, 900, "02-setup" + suffix));
        steps.add(new Step(() -> sm.context().controller().resume(midGame()), 1800, "03-board" + suffix));
        steps.add(new Step(() -> {
            GameState s = traitorPosition();
            sm.context().controller().resume(s);
        }, 1400, null));
        steps.add(new Step(() -> clickCard("T-TRAITOR"), 900, "04-tactic-traitor-target" + suffix));
        steps.add(new Step(this::clickFirstTargetedCard, 900, "05-tactic-traitor-destination" + suffix));
        steps.add(new Step(() -> sm.context().controller().resume(hotSeat()), 1200, "06-hot-seat-curtain" + suffix));
        steps.add(new Step(() -> sm.context().controller().resume(finishedMatch()), 2600, "07-victory" + suffix));
        steps.add(new Step(() -> sm.showRules(false), 900, "08-rules" + suffix));
        steps.add(new Step(() -> selectTab("Tactic cards"), 700, "09-rules-tactic-cards" + suffix));
        steps.add(new Step(() -> selectTab("Formations"), 700, "10-rules-formations" + suffix));
        steps.add(new Step(() -> sm.showSettings(false), 900, "11-settings" + suffix));
        steps.add(new Step(() -> {
            sm.context().controller().resume(midGame());
            sm.showGame();
        }, 1200, null));
        steps.add(new Step(() -> find(sm.scene().getRoot(), n -> n instanceof Button b && "Menu".equals(b.getText()))
                .ifPresent(n -> ((Button) n).fire()), 800, "12-game-menu" + suffix));
        steps.add(new Step(() -> {
            try {
                sm.context().controller().save(SaveManager.defaultDirectory().resolve("Fergus vs Isla"
                        + SaveManager.EXTENSION));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            sm.showLoad();
        }, 900, "13-saved-games" + suffix));
    }

    private void run(int index) {
        if (index >= steps.size()) {
            return;
        }
        Step step = steps.get(index);
        try {
            step.action().run();
        } catch (RuntimeException e) {
            System.err.println("Screenshot step " + index + " failed: " + e);
        }
        PauseTransition wait = new PauseTransition(Duration.millis(step.waitMillis()));
        wait.setOnFinished(e -> {
            if (step.shot() != null) {
                capture(step.shot());
            }
            run(index + 1);
        });
        wait.play();
    }

    private void capture(String name) {
        WritableImage image = sm.scene().snapshot(null);
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        int[] pixels = new int[w * h];
        image.getPixelReader().getPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        out.setRGB(0, 0, w, h, pixels, 0, w);
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(name + ".png");
            ImageIO.write(out, "png", file.toFile());
            System.out.println("Screenshot " + file + " (" + w + "x" + h + ")");
        } catch (IOException e) {
            System.err.println("Screenshot " + name + " failed: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------ scripted positions

    private static GameConfiguration config(PlayerProfile p1, PlayerProfile p2, boolean tactic, boolean expansion,
                                            int rounds, long seed) {
        return new GameConfiguration(p1, p2, tactic, expansion, rounds, FirstPlayerRule.PLAYER_ONE, seed);
    }

    private GameState simulate(GameConfiguration config, Predicate<GameState> stop, int maxActions) {
        GameState state = engine.newGame(config);
        PlayerStrategy[] strategies = {StrategyFactory.create(Difficulty.HARD, engine, config.seed()),
                StrategyFactory.create(Difficulty.HARD, engine, config.seed() + 1)};
        SeededRandom rng = new SeededRandom(config.seed() + 2);
        for (int i = 0; i < maxActions && !stop.test(state); i++) {
            try {
                Optional<PlayerId> actor = engine.playerToAct(state);
                if (actor.isEmpty()) {
                    if (state.phase() != TurnPhase.ROUND_OVER) {
                        break;
                    }
                    engine.apply(state, state.currentPlayer(), new NextRoundAction());
                    continue;
                }
                PlayerId p = actor.get();
                Action a = strategies[p.ordinal()].chooseAction(PlayerView.of(state, p, rng));
                engine.apply(state, p, a);
            } catch (IllegalActionException e) {
                throw new IllegalStateException(e);
            }
        }
        return state;
    }

    private static boolean humanToPlay(GameState s, int minTurn) {
        return s.phase() == TurnPhase.PLAY && s.currentPlayer() == PlayerId.P1 && s.turn() >= minTurn;
    }

    private GameState midGame() {
        return simulate(config(PlayerProfile.human("Fergus"), PlayerProfile.virtual("Isla", Difficulty.HARD),
                true, true, 1, 11), s -> humanToPlay(s, 13), 400);
    }

    private GameState traitorPosition() {
        for (long seed = 1; seed < 400; seed++) {
            GameState s = simulate(config(PlayerProfile.human("Fergus"),
                    PlayerProfile.virtual("Isla", Difficulty.HARD), true, false, 1, seed),
                    st -> humanToPlay(st, 8) && st.hand(PlayerId.P1).stream().anyMatch(c -> c.id().equals("T-TRAITOR")),
                    400);
            boolean playable = s.phase() == TurnPhase.PLAY && engine.legalActions(s).stream()
                    .filter(a -> a instanceof PlayCardAction pc && pc.cardId().equals("T-TRAITOR")).count() > 1;
            if (playable) {
                return s;
            }
        }
        throw new IllegalStateException("No Traitor position found");
    }

    private GameState hotSeat() {
        return simulate(config(PlayerProfile.human("Fergus"), PlayerProfile.human("Isla"), true, false, 1, 5),
                s -> s.phase() == TurnPhase.PLAY && s.currentPlayer() == PlayerId.P2 && s.turn() >= 6, 400);
    }

    private GameState finishedMatch() {
        return simulate(config(PlayerProfile.human("Fergus"), PlayerProfile.virtual("Isla", Difficulty.EASY),
                true, true, 3, 21), s -> s.phase() == TurnPhase.MATCH_OVER, 3000);
    }

    // ------------------------------------------------------------ interactions

    private void clickCard(String id) {
        find(sm.scene().getRoot(), n -> n instanceof CardView cv && cv.card() != null && cv.card().id().equals(id))
                .ifPresent(ScreenshotTour::click);
    }

    private void clickFirstTargetedCard() {
        find(sm.scene().getRoot(), n -> n instanceof CardView cv && cv.isTargeted()).ifPresent(ScreenshotTour::click);
    }

    private void selectTab(String text) {
        find(sm.scene().getRoot(), n -> n instanceof ToggleButton t && t.getText().startsWith(text))
                .ifPresent(n -> ((ToggleButton) n).fire());
    }

    private static Optional<Node> find(Node node, Predicate<Node> test) {
        if (test.test(node) && node.isVisible()) {
            return Optional.of(node);
        }
        if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                Optional<Node> found = find(child, test);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private static void click(Node node) {
        node.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null));
    }
}
