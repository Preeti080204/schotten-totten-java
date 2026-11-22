package com.schottentotten.view;

import com.schottentotten.app.AppContext;
import com.schottentotten.app.GameSettings;
import com.schottentotten.cli.CommandLineView;
import com.schottentotten.controller.GameSession;
import com.schottentotten.controller.TextDescriber;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.view.screens.GameScreen;
import com.schottentotten.view.widgets.CardView;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * End-to-end GUI test: plays complete matches in a real window, only through the widgets a
 * player uses (hand cards, Stones, claim buttons, draw piles, dialogs, the hot-seat curtain
 * and the victory screen). The human moves are the hint suggestions, so every kind of card
 * interaction the advanced strategy picks is exercised. Needs a display, hence opt-in:
 * {@code mvn test -Dtest=GuiPlaythroughTest -Dschotten.gui.tests=true}.
 */
@EnabledIfSystemProperty(named = "schotten.gui.tests", matches = "true")
class GuiPlaythroughTest {

    private static final long TIMEOUT_MILLIS = 240_000;

    private AppContext context;
    private ScreenManager sm;
    private Stage stage;
    private TextDescriber describer;
    private Action pending;
    private long pendingVersion = -1;
    private int uiActions;
    private int idleSteps;
    private int curtainsLifted;
    private final Set<String> specialsPlayed = new TreeSet<>();
    private final List<String> errors = new ArrayList<>();

    @BeforeAll
    static void startToolkit() throws Exception {
        Path dir = Files.createTempDirectory("schotten-gui");
        System.setProperty("schotten.saves", dir.resolve("saves").toString());
        GameSettings settings = GameSettings.getInstance();
        settings.setSoundEnabled(false);
        settings.setMusicEnabled(false);
        settings.setSpeed(GameSettings.Speed.INSTANT);
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException alreadyRunning) {
            latch.countDown();
        }
        assertTrue(latch.await(20, TimeUnit.SECONDS));
        Platform.setImplicitExit(false);
    }

    @AfterEach
    void closeWindow() {
        onFx(() -> {
            if (context != null) {
                context.controller().hub().clear();
                context.shutdown();
            }
            if (stage != null) {
                stage.close();
            }
            return null;
        });
    }

    @Test
    void humanBeatsOrLosesAgainstComputerInTacticExpansionMatch() throws Exception {
        GameSettings.getInstance().setReduceMotion(false);
        GameState end = play(new GameConfiguration(PlayerProfile.human("Fergus"),
                PlayerProfile.virtual("Isla", Difficulty.HARD), true, true, 2, FirstPlayerRule.PLAYER_ONE, 31));
        assertEquals(TurnPhase.MATCH_OVER, end.phase());
        assertEquals(2, end.match().results().size());
    }

    @ParameterizedTest
    @ValueSource(longs = {8, 19})
    void hotSeatTacticExpansionMatchFinishes(long seed) throws Exception {
        GameSettings.getInstance().setReduceMotion(true);
        GameState end = play(new GameConfiguration(PlayerProfile.human("Fergus"), PlayerProfile.human("Isla"),
                true, true, 2, FirstPlayerRule.PLAYER_TWO, seed));
        assertEquals(TurnPhase.MATCH_OVER, end.phase());
        assertTrue(curtainsLifted > 10, "the hands were not hidden between turns");
    }

    @Test
    void baseGameAgainstEasyComputer() throws Exception {
        GameSettings.getInstance().setReduceMotion(true);
        GameState end = play(new GameConfiguration(PlayerProfile.human("Fergus"),
                PlayerProfile.virtual("Isla", Difficulty.EASY), false, false, 1, FirstPlayerRule.RANDOM, 77));
        assertEquals(TurnPhase.MATCH_OVER, end.phase());
    }

    @Test
    void computerVsComputerCanBeWatchedPausedRevealedAndMuted() throws Exception {
        GameSettings settings = GameSettings.getInstance();
        settings.setReduceMotion(true);
        settings.setSpeed(GameSettings.Speed.FAST);
        open(new GameConfiguration(PlayerProfile.virtual("Fergus", Difficulty.HARD),
                PlayerProfile.virtual("Isla", Difficulty.EASY), true, true, 2, FirstPlayerRule.RANDOM, 5));
        waitUntil(() -> session().snapshot().turn() >= 4, 20_000, "the computers do not play");

        onFxRun(() -> toggle("Pause").fire());
        Thread.sleep(1_000);
        long frozen = onFx(() -> session().snapshot().version());
        Thread.sleep(2_000);
        assertEquals(frozen, onFx(() -> session().snapshot().version()), "the game moved while paused");

        assertTrue(onFx(() -> faceUpHandCards().isEmpty()), "hands are visible before 'Show hands'");
        onFxRun(() -> toggle("Show hands").fire());
        assertTrue(onFx(() -> !faceUpHandCards().isEmpty()), "'Show hands' does not reveal the hands");

        onFxRun(() -> button(b -> "mute".equals(b.getId())).orElseThrow().fire());
        assertTrue(settings.isSoundEnabled() && settings.isMusicEnabled(), "unmute restores sound and music");
        onFxRun(() -> button(b -> "mute".equals(b.getId())).orElseThrow().fire());
        assertTrue(!settings.isSoundEnabled() && !settings.isMusicEnabled(), "mute silences sound and music");

        onFx(() -> {
            node(n -> n instanceof Slider).map(n -> (Slider) n).orElseThrow().setValue(3);
            return null;
        });
        assertEquals(GameSettings.Speed.INSTANT, settings.speed());
        onFxRun(() -> toggle("Pause").fire());
        waitUntil(() -> button(b -> "Play Again".equals(b.getText())).isPresent(), TIMEOUT_MILLIS,
                "the computer match does not reach the victory screen");
        GameState end = onFx(() -> session().snapshot());
        assertEquals(TurnPhase.MATCH_OVER, end.phase());
        assertEquals(2, end.match().results().size());
    }

    @Test
    void commandLineAndWindowPlayTheSameGame() throws Exception {
        GameSettings.getInstance().setReduceMotion(true);
        open(new GameConfiguration(PlayerProfile.human("Fergus"), PlayerProfile.human("Isla"), false, false, 1,
                FirstPlayerRule.PLAYER_ONE, 3));
        ByteArrayOutputStream printed = new ByteArrayOutputStream();
        CommandLineView cli = new CommandLineView(context.controller(), context.rules(), new StringReader(""),
                new PrintStream(printed, true, StandardCharsets.UTF_8), false, true);
        cli.connect();
        liftCurtain();

        String typed = session().snapshot().hand(PlayerId.P1).get(0).id();
        cli.execute("play 1 5");
        waitUntil(() -> node(n -> "zone-P1-4".equals(n.getId())).map(zone -> find(zone,
                        n -> n instanceof CardView cv && cv.card() != null && cv.card().id().equals(typed)).isPresent())
                        .orElse(false),
                10_000, "the card played on the command line does not appear on Stone 5 in the window");

        onFxRun(() -> button(b -> "end-turn".equals(b.getId())).orElseThrow().fire());
        waitUntil(() -> printed.toString(StandardCharsets.UTF_8).contains("Fergus draws"), 10_000,
                "the command line does not report the turn ended in the window");

        liftCurtain();
        String clicked = onFx(() -> session().snapshot().hand(PlayerId.P2).get(0).id());
        onFxRun(() -> click(handCard(clicked).orElseThrow()));
        waitUntil(() -> ((GameScreen) sm.current()).awaitingField().isPresent(), 10_000, "no Stone is offered");
        onFxRun(() -> click(stone(2)));
        waitUntil(() -> printed.toString(StandardCharsets.UTF_8).contains("Isla plays"), 10_000,
                "the command line does not report the card played in the window");
        assertTrue(session().snapshot().board().stone(2).side(PlayerId.P2).contains(clicked));
    }

    private void liftCurtain() throws Exception {
        waitUntil(() -> button(b -> b.getText() != null && b.getText().startsWith("I am ")).isPresent(), 10_000,
                "the hand-over screen does not appear");
        onFxRun(() -> button(b -> b.getText() != null && b.getText().startsWith("I am ")).orElseThrow().fire());
    }

    private List<CardView> faceUpHandCards() {
        GameState s = session().snapshot();
        List<CardView> found = new ArrayList<>();
        collect(sm.scene().getRoot(), n -> n instanceof CardView cv && cv.card() != null
                && (s.hand(PlayerId.P1).contains(cv.card()) || s.hand(PlayerId.P2).contains(cv.card())), found);
        return found;
    }

    private static void collect(Node node, Predicate<Node> test, List<CardView> into) {
        if (!node.isVisible()) {
            return;
        }
        if (test.test(node)) {
            into.add((CardView) node);
        }
        if (node instanceof Parent p) {
            p.getChildrenUnmodifiable().forEach(child -> collect(child, test, into));
        }
    }

    private ToggleButton toggle(String text) {
        return node(n -> n instanceof ToggleButton t && text.equals(t.getText())).map(n -> (ToggleButton) n)
                .orElseThrow(() -> new AssertionError("No '" + text + "' switch"));
    }

    private void waitUntil(Supplier<Boolean> condition, long millis, String failure) throws Exception {
        long deadline = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < deadline) {
            if (onFx(condition)) {
                return;
            }
            Thread.sleep(100);
        }
        fail(failure);
    }

    private void open(GameConfiguration config) {
        onFx(() -> {
            context = AppContext.create();
            describer = new TextDescriber(context.engine().catalog());
            stage = new Stage();
            sm = new ScreenManager(stage, context, new SoundManager());
            stage.setScene(sm.scene());
            stage.setWidth(1440);
            stage.setHeight(900);
            stage.show();
            sm.showMainMenu();
            context.controller().startNewGame(config);
            return null;
        });
    }

    private GameState play(GameConfiguration config) throws Exception {
        open(config);
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        long lastVersion = -1;
        long lastProgress = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(60);
            Optional<GameState> finished = onFx(this::step);
            if (!errors.isEmpty()) {
                fail("The interface refused a move: " + errors);
            }
            if (finished.isPresent()) {
                assertTrue(uiActions > 10, "too few interface actions: " + uiActions);
                System.out.println("GUI playthrough finished: " + uiActions + " interface actions, special cards "
                        + "played through the interface: " + specialsPlayed);
                return finished.get();
            }
            long version = onFx(() -> session().snapshot().version());
            if (version != lastVersion) {
                lastVersion = version;
                lastProgress = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - lastProgress > 15_000) {
                fail("No progress for 15 s at " + onFx(() -> describeSituation()));
            }
        }
        fail("Match not finished in time");
        return null;
    }

    private GameSession session() {
        return context.controller().hub().current().orElseThrow();
    }

    /** Performs at most one interface action. Returns the final state once the victory screen is up. */
    private Optional<GameState> step() {
        collectErrors();
        GameState s = session().snapshot();
        if (s.phase() == TurnPhase.MATCH_OVER) {
            return button(b -> "Play Again".equals(b.getText())).map(b -> s);
        }
        if (s.phase() == TurnPhase.ROUND_OVER) {
            button(b -> "Next Round".equals(b.getText())).ifPresent(this::fire);
            return Optional.empty();
        }
        Optional<Button> curtain = button(b -> b.getText() != null && b.getText().startsWith("I am "));
        if (curtain.isPresent()) {
            assertTrue(node(n -> n instanceof CardView cv && cv.card() != null
                    && (s.hand(PlayerId.P1).contains(cv.card()) || s.hand(PlayerId.P2).contains(cv.card()))).isEmpty(),
                    "a hand is visible behind the curtain");
            fire(curtain.get());
            curtainsLifted++;
            return Optional.empty();
        }
        Optional<PlayerId> actor = context.engine().playerToAct(s);
        if (actor.isEmpty() || s.config().profile(actor.get()).isVirtual()) {
            return Optional.empty();
        }
        if (pending == null || pendingVersion != s.version()) {
            pending = context.controller().hint().orElse(null);
            pendingVersion = s.version();
            if (pending == null) {
                return Optional.empty();
            }
            startAction(pending, s);
        } else {
            continueAction(pending, s);
        }
        return Optional.empty();
    }

    private void startAction(Action action, GameState s) {
        idleSteps = 0;
        switch (action) {
            case PlayCardAction pc -> {
                Optional<Node> card = handCard(pc.cardId());
                if (card.isEmpty()) {
                    return;
                }
                click(card.get());
                if (context.engine().catalog().get(pc.cardId()).isSpecial()) {
                    specialsPlayed.add(context.engine().catalog().get(pc.cardId()).displayName());
                }
            }
            case ClaimAction c -> button(b -> ("claim-" + c.stone()).equals(b.getId())).ifPresent(this::fire);
            case EndTurnAction e -> {
                if (e.drawFrom() == null) {
                    button(b -> "end-turn".equals(b.getId())).ifPresent(this::fire);
                } else {
                    node(n -> ("deck-" + e.drawFrom().name()).equals(n.getId())).ifPresent(GuiPlaythroughTest::click);
                }
            }
            case PassAction p -> button(b -> "pass".equals(b.getId())).ifPresent(this::fire);
            case ResolveEffectAction r -> continueAction(action, s);
            default -> throw new AssertionError("Unexpected hint " + action);
        }
        uiActions++;
    }

    private void continueAction(Action action, GameState s) {
        EffectParameters params = switch (action) {
            case PlayCardAction pc -> pc.parameters();
            case ResolveEffectAction r -> r.parameters();
            default -> null;
        };
        Optional<Button> identities = button(b -> "Claim".equals(b.getText()) && b.getId() == null);
        if (identities.isPresent()) {
            fire(identities.get());
            return;
        }
        if (params == null) {
            if (++idleSteps > 8) {
                startAction(action, s);
            }
            return;
        }
        for (ParameterField field : List.of(ParameterField.DECKS, ParameterField.DECK, ParameterField.CARDS)) {
            Object value = field.read(params);
            if (value != null) {
                String label = describer.value(field, value, s);
                Optional<Button> option = button(b -> label.equals(b.getText()));
                if (option.isPresent()) {
                    fire(option.get());
                    uiActions++;
                    return;
                }
            }
        }
        GameScreen screen = (GameScreen) sm.current();
        Optional<ParameterField> awaiting = screen.awaitingField();
        if (awaiting.isEmpty()) {
            if (++idleSteps > 8) {
                startAction(action, s);
            }
            return;
        }
        idleSteps = 0;
        Object value = awaiting.get().read(params);
        switch (awaiting.get()) {
            case STONE -> click(stone((Integer) value));
            case DESTINATION -> click((Integer) value == EffectParameters.DISCARD
                    ? node(n -> "discard-pile".equals(n.getId())).orElseThrow() : stone((Integer) value));
            case TARGET_CARD, SECOND_CARD -> click(boardCard((String) value));
            default -> {
            }
        }
        uiActions++;
    }

    private void collectErrors() {
        node(n -> n instanceof Label l && l.getStyleClass().contains("toast-error")).ifPresent(n -> {
            String text = ((Label) n).getText();
            if (!errors.contains(text)) {
                errors.add(text);
            }
        });
    }

    private String describeSituation() {
        GameState s = session().snapshot();
        return "phase " + s.phase() + ", turn " + s.turn() + ", pending " + pending + ", awaiting "
                + (sm.current() instanceof GameScreen g ? g.awaitingField() : "-");
    }

    // ------------------------------------------------------------------ node helpers

    private Optional<Node> handCard(String id) {
        return node(n -> n instanceof CardView cv && cv.card() != null && cv.card().id().equals(id));
    }

    private Node boardCard(String id) {
        return node(n -> n instanceof CardView cv && cv.card() != null && cv.card().id().equals(id) && cv.isTargeted())
                .orElseThrow(() -> new AssertionError("Card " + id + " not highlighted"));
    }

    private Node stone(int index) {
        return node(n -> ("stone-" + index).equals(n.getId())).orElseThrow();
    }

    private Optional<Button> button(Predicate<Button> test) {
        return node(n -> n instanceof Button b && test.test(b) && !b.isDisabled()).map(n -> (Button) n);
    }

    private Optional<Node> node(Predicate<Node> test) {
        return find(sm.scene().getRoot(), test);
    }

    private static Optional<Node> find(Node node, Predicate<Node> test) {
        if (!node.isVisible()) {
            return Optional.empty();
        }
        if (test.test(node)) {
            return Optional.of(node);
        }
        if (node instanceof Parent p) {
            List<Node> children = new ArrayList<>(p.getChildrenUnmodifiable());
            for (int i = children.size() - 1; i >= 0; i--) {
                Optional<Node> found = find(children.get(i), test);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    private void fire(Button b) {
        b.fire();
    }

    private static void click(Node node) {
        node.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.PRIMARY, 1,
                false, false, false, false, true, false, false, true, false, false, null));
    }

    private static void onFxRun(Runnable task) {
        onFx(() -> {
            task.run();
            return null;
        });
    }

    private static <T> T onFx(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException e) {
            if (e.getCause() instanceof AssertionError a) {
                throw a;
            }
            throw new AssertionError(e.getCause());
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
}
