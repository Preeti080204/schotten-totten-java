package com.schottentotten.view.screens;

import com.schottentotten.app.GameSettings;
import com.schottentotten.controller.ActionOutcome;
import com.schottentotten.controller.GameController;
import com.schottentotten.controller.GameSession;
import com.schottentotten.controller.ParameterChooser;
import com.schottentotten.controller.SessionListener;
import com.schottentotten.controller.SessionUpdate;
import com.schottentotten.controller.TextDescriber;
import com.schottentotten.engine.EffectParameters;
import com.schottentotten.engine.GameEngine;
import com.schottentotten.engine.GameEvent;
import com.schottentotten.engine.GameEventType;
import com.schottentotten.engine.MultiStepEffect;
import com.schottentotten.engine.ParameterField;
import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.LogEntry;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.RoundResult;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveManager;
import com.schottentotten.rules.ClaimCheck;
import com.schottentotten.rules.EliteIdentity;
import com.schottentotten.view.Assets;
import com.schottentotten.view.Dialogs;
import com.schottentotten.view.Screen;
import com.schottentotten.view.ScreenManager;
import com.schottentotten.view.SoundManager;
import com.schottentotten.view.game.BoardView;
import com.schottentotten.view.game.GameOverlays;
import com.schottentotten.view.game.SideRail;
import com.schottentotten.view.widgets.CoverImage;
import com.schottentotten.view.widgets.FitHeightPane;
import com.schottentotten.view.widgets.ScalingPane;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The game board screen. It observes the shared session (exactly like the command line does),
 * renders every snapshot and turns clicks, drags and keys into actions submitted through the
 * session, so every move gets the engine's validation.
 *
 * <p>Information hiding: a hand is shown face up only to its owner. In a two-human game the
 * board hides both hands behind a curtain until the next player confirms they are at the
 * screen.</p>
 */
public final class GameScreen implements Screen, SessionListener, BoardView.Handler, SideRail.Handler {

    private static final ExecutorService AUTOSAVE = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "autosave");
        t.setDaemon(true);
        return t;
    });

    /** Selection in progress: a card being played (or a pending effect) and its remaining choices. */
    private static final class Selection {
        private final Card card;
        private final ParameterChooser chooser;
        private final long version;
        private ParameterField awaiting;

        private Selection(Card card, ParameterChooser chooser, long version) {
            this.card = card;
            this.chooser = chooser;
            this.version = version;
        }
    }

    private final ScreenManager sm;
    private final GameController controller;
    private final GameEngine engine;
    private final TextDescriber describer;
    private final BorderPane root = new BorderPane();
    private final BoardView board = new BoardView(this);
    private final SideRail rail = new SideRail(this);
    private final EventHandler<KeyEvent> keys = this::onKey;
    private final Set<String> newCards = new HashSet<>();
    private final Set<Integer> claimedNow = new HashSet<>();

    private GameSession session;
    private GameState state;
    private Selection selection;
    private PlayerId revealed;
    private ScreenManager.Overlay curtain;
    private ScreenManager.Overlay victory;
    private String victoryKey;
    private boolean revealHands;
    private boolean dialogOpen;
    private Action hint;

    /**
     * Builds the board screen and attaches it to the active session.
     *
     * @param sm screen manager
     */
    public GameScreen(ScreenManager sm) {
        this.sm = sm;
        this.controller = sm.context().controller();
        this.engine = controller.engine();
        this.describer = new TextDescriber(engine.catalog());
        CoverImage meadow = new CoverImage(Assets.image("board-meadow.jpg"), 0.5);
        ScalingPane scaling = new ScalingPane(board, BoardView.WIDTH, BoardView.HEIGHT);
        StackPane center = new StackPane(meadow, scaling);
        center.setMinSize(0, 0);
        root.setCenter(center);
        root.setRight(new FitHeightPane(rail, 780));
        root.getStyleClass().add("screen");
        controller.hub().addSessionChangeListener(s -> Platform.runLater(() -> attach(s)));
        controller.hub().current().ifPresent(this::attach);
    }

    /**
     * Returns the location of the automatic save used by "Continue".
     *
     * @return autosave file
     */
    public static Path autosavePath() {
        return SaveManager.defaultDirectory().resolve("autosave" + SaveManager.EXTENSION);
    }

    @Override
    public Parent root() {
        return root;
    }

    /**
     * Returns the choice the screen waits for on the board (a Stone, a destination or a card),
     * if a card is being played.
     *
     * @return awaited field
     */
    public Optional<ParameterField> awaitingField() {
        return selection == null ? Optional.empty() : Optional.ofNullable(selection.awaiting);
    }

    @Override
    public void onShow() {
        sm.scene().addEventFilter(KeyEvent.KEY_PRESSED, keys);
        dialogOpen = false;
        selection = null;
        if (state != null) {
            updateCurtain();
            render();
            maybeResolve();
        }
    }

    @Override
    public void onHide() {
        sm.scene().removeEventFilter(KeyEvent.KEY_PRESSED, keys);
    }

    // ---------------------------------------------------------------- session

    private void attach(GameSession s) {
        if (s == session) {
            return;
        }
        if (session != null) {
            session.removeListener(this);
        }
        session = s;
        state = null;
        selection = null;
        revealed = null;
        victoryKey = null;
        hint = null;
        dialogOpen = false;
        closeCurtain();
        if (victory != null) {
            victory.close();
            victory = null;
        }
        if (s != null) {
            s.addListener(this);
        }
    }

    @Override
    public void onUpdate(SessionUpdate update) {
        Platform.runLater(() -> apply(update));
    }

    private void apply(SessionUpdate update) {
        if (update.session() != session) {
            return;
        }
        state = update.snapshot();
        boolean placed = false;
        boolean effect = false;
        for (GameEvent e : update.events()) {
            switch (e.type()) {
                case CARD_PLACED, CARD_MOVED -> {
                    if (e.cardId() != null) {
                        newCards.add(e.cardId());
                    }
                    placed = true;
                }
                case STONE_CLAIMED -> {
                    claimedNow.add(e.stone());
                    sm.sound().play(SoundManager.Sfx.CLAIM);
                }
                case COMBAT_MODE_PLAYED, EFFECT_PLAYED, CARD_DISCARDED, EFFECT_RESOLVED, WARD_CHANGED -> effect = true;
                case CARD_DRAWN -> sm.sound().play(SoundManager.Sfx.DRAW);
                default -> {
                }
            }
        }
        if (placed) {
            sm.sound().play(SoundManager.Sfx.PLACE);
        }
        if (effect) {
            sm.sound().play(SoundManager.Sfx.EFFECT);
        }
        if (selection != null && selection.version != state.version()) {
            selection = null;
        }
        if (hint != null && update.events().stream().anyMatch(e -> e.type() != GameEventType.GAME_STARTED)) {
            hint = null;
        }
        if (victory != null && state.phase() != TurnPhase.ROUND_OVER && state.phase() != TurnPhase.MATCH_OVER) {
            victory.close();
            victory = null;
        }
        autosave(update);
        updateCurtain();
        if (update.events().stream().anyMatch(e -> e.type() == GameEventType.TURN_STARTED) && canAct()) {
            sm.sound().play(SoundManager.Sfx.TURN);
        }
        render();
        newCards.clear();
        claimedNow.clear();
        maybeShowVictory();
        maybeResolve();
    }

    private void autosave(SessionUpdate update) {
        GameState snapshot = update.snapshot();
        if (snapshot.phase() == TurnPhase.MATCH_OVER) {
            AUTOSAVE.submit(() -> {
                try {
                    Files.deleteIfExists(autosavePath());
                } catch (IOException ignored) {
                    // A stale autosave only offers to continue a finished game, which is refused.
                }
            });
            return;
        }
        boolean milestone = update.events().stream().anyMatch(e -> e.type() == GameEventType.TURN_STARTED
                || e.type() == GameEventType.ROUND_STARTED || e.type() == GameEventType.ROUND_OVER);
        if (milestone) {
            AUTOSAVE.submit(() -> {
                try {
                    controller.saves().save(snapshot, autosavePath());
                } catch (IOException e) {
                    System.err.println("Autosave failed: " + e.getMessage());
                }
            });
        }
    }

    // ---------------------------------------------------------------- viewer

    private Set<PlayerId> humans() {
        Set<PlayerId> set = EnumSet.noneOf(PlayerId.class);
        for (PlayerId p : PlayerId.values()) {
            if (!state.config().profile(p).isVirtual()) {
                set.add(p);
            }
        }
        return set;
    }

    private PlayerId viewer() {
        Set<PlayerId> humans = humans();
        if (humans.size() == 1) {
            return humans.iterator().next();
        }
        return humans.isEmpty() ? null : revealed;
    }

    private Optional<PlayerId> toAct() {
        return engine.playerToAct(state);
    }

    private boolean canAct() {
        if (state == null || curtain != null) {
            return false;
        }
        Optional<PlayerId> p = toAct();
        return p.isPresent() && !state.config().profile(p.get()).isVirtual() && p.get() == viewer();
    }

    private String name(PlayerId p) {
        return state.config().profile(p).name();
    }

    private void updateCurtain() {
        if (humans().size() < 2 || state.phase() == TurnPhase.ROUND_OVER || state.phase() == TurnPhase.MATCH_OVER) {
            closeCurtain();
            return;
        }
        Optional<PlayerId> actor = toAct();
        if (actor.isEmpty() || actor.get() == revealed) {
            closeCurtain();
            return;
        }
        if (curtain == null) {
            PlayerId next = actor.get();
            selection = null;
            ScreenManager.Overlay opened = sm.openOverlay(GameOverlays.curtain(name(next), name(next.opponent()), () -> {
                revealed = next;
                closeCurtain();
                render();
                maybeResolve();
            }), false);
            opened.setOnClose(() -> {
                if (curtain == opened) {
                    curtain = null;
                }
            });
            curtain = opened;
        }
    }

    private void closeCurtain() {
        if (curtain != null) {
            ScreenManager.Overlay c = curtain;
            curtain = null;
            c.close();
        }
    }

    // ---------------------------------------------------------------- rendering

    private void render() {
        if (state == null) {
            return;
        }
        PlayerId viewer = viewer();
        boolean act = canAct();
        Set<PlayerId> faceUp = EnumSet.noneOf(PlayerId.class);
        if (viewer != null && curtain == null) {
            faceUp.add(viewer);
        }
        if (humans().isEmpty() && revealHands) {
            faceUp.addAll(EnumSet.allOf(PlayerId.class));
        }
        List<Action> legal = act ? engine.legalActions(state) : List.of();
        Set<String> playable = new HashSet<>();
        boolean passLegal = false;
        for (Action a : legal) {
            if (a instanceof PlayCardAction pc) {
                playable.add(pc.cardId());
            } else if (a instanceof PassAction) {
                passLegal = true;
            }
        }
        Set<Integer> targetStones = new HashSet<>();
        Set<String> targetCards = new HashSet<>();
        boolean discardTarget = false;
        if (selection != null && selection.awaiting != null) {
            for (Object v : selection.chooser.values(selection.awaiting)) {
                switch (selection.awaiting) {
                    case STONE -> targetStones.add((Integer) v);
                    case DESTINATION -> {
                        if ((Integer) v == EffectParameters.DISCARD) {
                            discardTarget = true;
                        } else {
                            targetStones.add((Integer) v);
                        }
                    }
                    case TARGET_CARD, SECOND_CARD -> targetCards.add((String) v);
                    default -> {
                    }
                }
            }
        }
        String hintCard = null;
        if (hint instanceof PlayCardAction pc && selection == null) {
            hintCard = pc.cardId();
            if (pc.parameters().stone() != null) {
                targetStones.add(pc.parameters().stone());
            }
        }
        Set<Integer> claimable = new HashSet<>();
        if (act && state.phase() == TurnPhase.CLAIM) {
            for (Action a : legal) {
                if (a instanceof ClaimAction c) {
                    claimable.add(c.stone());
                }
            }
        }
        board.render(state, new BoardView.Decorations(faceUp, playable,
                selection != null && selection.card != null ? selection.card.id() : null, targetStones, targetCards,
                claimable, viewer != PlayerId.P2, toAct().orElse(null), hintCard, Set.copyOf(newCards),
                Set.copyOf(claimedNow), !sm.reduceMotion()));
        rail.render(railView(act, legal, playable, passLegal, claimable, discardTarget));
    }

    private SideRail.View railView(boolean act, List<Action> legal, Set<String> playable, boolean passLegal,
                                   Set<Integer> claimable, boolean discardTarget) {
        String round = "Round " + state.round() + (state.config().isMultiRound() ? " of " + state.config().rounds() : "")
                + "  |  Turn " + state.turn();
        String[] banner = instruction(act, playable, passLegal, claimable, discardTarget);
        String endLabel = "End Turn";
        boolean endEnabled = act && state.phase() == TurnPhase.CLAIM;
        if (state.phase() == TurnPhase.ROUND_OVER) {
            endLabel = "Next Round";
            endEnabled = true;
        } else if (state.phase() == TurnPhase.MATCH_OVER) {
            endLabel = "Results";
            endEnabled = true;
        }
        Map<DeckType, Integer> decks = new LinkedHashMap<>();
        for (DeckType t : DeckType.values()) {
            if (state.deck(t).isPresent()) {
                decks.put(t, state.deckSize(t));
            }
        }
        Set<DeckType> drawable = act && state.phase() == TurnPhase.CLAIM
                ? Set.copyOf(engine.drawableDecks(state, viewer())) : Set.of();
        List<String> log = new ArrayList<>();
        List<LogEntry> entries = state.log();
        for (LogEntry e : entries.subList(Math.max(0, entries.size() - 80), entries.size())) {
            log.add("R" + e.round() + " T" + e.turn() + "  " + e.text());
        }
        boolean anyVirtual = state.config().player1().isVirtual() || state.config().player2().isVirtual();
        boolean paused = controller.hub().driver().map(d -> d.isPaused()).orElse(false);
        boolean hintEnabled = act && state.phase() != TurnPhase.RESOLVE_EFFECT
                && GameSettings.getInstance().isShowHints();
        return new SideRail.View(round, banner[0], banner[1], endLabel, endEnabled, act && passLegal, hintEnabled,
                decks, drawable, state.discard(), discardTarget, log, anyVirtual, humans().isEmpty(), paused);
    }

    private String[] instruction(boolean act, Set<String> playable, boolean passLegal, Set<Integer> claimable,
                                 boolean discardTarget) {
        if (state.phase() == TurnPhase.ROUND_OVER || state.phase() == TurnPhase.MATCH_OVER) {
            String result = state.lastRoundResult().map(this::resultText).orElse("");
            return state.phase() == TurnPhase.ROUND_OVER
                    ? new String[]{"Round over", result + " Press Next Round to continue."}
                    : new String[]{"Match over", result};
        }
        PlayerId actor = toAct().orElse(state.currentPlayer());
        if (curtain != null) {
            return new String[]{name(actor) + "'s turn", "Waiting for " + name(actor) + " to take the cards."};
        }
        if (state.config().profile(actor).isVirtual()) {
            return new String[]{name(actor) + "'s turn", name(actor) + " is thinking..."};
        }
        if (!act) {
            return new String[]{name(actor) + "'s turn", ""};
        }
        String title = "Your turn, " + name(actor);
        if (selection != null && selection.awaiting != null) {
            String what = selection.card != null ? selection.card.displayName() + ": " : "";
            String how = switch (selection.awaiting) {
                case STONE -> "click a glowing Stone (or drag the card onto it).";
                case DESTINATION -> "click a glowing Stone" + (discardTarget ? " or the discard pile." : ".");
                default -> "click one of the glowing cards.";
            };
            return new String[]{title, what + describer.fieldPrompt(selection.awaiting) + " - " + how
                    + " Right-click or Esc cancels."};
        }
        return switch (state.phase()) {
            case PLAY -> playable.isEmpty() && passLegal
                    ? new String[]{title, "You cannot play any card. Press Pass."}
                    : new String[]{title, "Pick a card from your hand, then a glowing Stone. You can also drag it "
                    + "or use the number keys."};
            case CLAIM -> {
                List<DeckType> drawable = engine.drawableDecks(state, actor);
                String draw = drawable.isEmpty() ? "press End Turn."
                        : "click a glowing draw pile" + (drawable.size() > 1 ? " (your choice of deck)." : ".");
                yield claimable.isEmpty() ? new String[]{title, "To end your turn, " + draw}
                        : new String[]{title, "Claim the Stones with a golden ribbon, then " + draw};
            }
            case RESOLVE_EFFECT -> new String[]{title, pendingPrompt()};
            default -> new String[]{title, ""};
        };
    }

    private String resultText(RoundResult r) {
        if (r.winner() == null) {
            return "Round " + r.round() + " is a draw.";
        }
        return name(r.winner()) + " " + r.reason().description() + ".";
    }

    private String pendingPrompt() {
        return state.pending().map(p -> ((MultiStepEffect) engine.registry().get(p.effectId())).resolutionPrompt(p))
                .orElse("");
    }

    // ---------------------------------------------------------------- playing cards

    private boolean beginPlay(Card card) {
        List<EffectParameters> options = engine.legalActions(state).stream()
                .filter(a -> a instanceof PlayCardAction pc && pc.cardId().equals(card.id()))
                .map(a -> ((PlayCardAction) a).parameters()).toList();
        if (options.isEmpty()) {
            ActionOutcome refused = session.submitAsHuman(new PlayCardAction(card.id(), EffectParameters.NONE));
            sm.toast(refused.accepted() ? "" : refused.message(), true);
            return false;
        }
        sm.sound().play(SoundManager.Sfx.PICK);
        hint = null;
        selection = new Selection(card, new ParameterChooser(options), state.version());
        advance();
        return true;
    }

    private void advance() {
        Selection sel = selection;
        if (sel == null) {
            return;
        }
        while (!sel.chooser.isResolved()) {
            Optional<ParameterField> next = sel.chooser.nextField();
            if (next.isEmpty()) {
                break;
            }
            ParameterField field = next.get();
            List<Object> values = sel.chooser.values(field);
            if (values.size() == 1) {
                sel.chooser.choose(field, values.get(0));
                continue;
            }
            switch (field) {
                case STONE, DESTINATION, TARGET_CARD, SECOND_CARD -> {
                    sel.awaiting = field;
                    render();
                }
                default -> openChoice(sel, field, values);
            }
            return;
        }
        selection = null;
        Action action = sel.card != null ? new PlayCardAction(sel.card.id(), sel.chooser.result())
                : new ResolveEffectAction(sel.chooser.result());
        submit(action);
    }

    private void openChoice(Selection sel, ParameterField field, List<Object> values) {
        List<Dialogs.Option> options = new ArrayList<>();
        for (Object v : values) {
            List<Card> preview = new ArrayList<>();
            if (field == ParameterField.CARDS && v instanceof List<?> ids) {
                ids.forEach(id -> engine.catalog().find((String) id).ifPresent(preview::add));
            }
            options.add(new Dialogs.Option(describer.value(field, v, state), preview));
        }
        String title = sel.card != null ? sel.card.displayName() : state.pending()
                .map(p -> describer.cardName(p.sourceCardId())).orElse("Card effect");
        String message = describer.fieldPrompt(field) + (sel.card == null ? ". " + pendingPrompt() : ".");
        if (field == ParameterField.CARDS && sel.card == null && state.pending().map(p -> p.deck() != null).orElse(false)) {
            message += " The first card listed will be on top.";
        }
        dialogOpen = true;
        boolean cancellable = sel.card != null;
        Dialogs.choose(sm, title, message, options, cancellable, index -> {
            dialogOpen = false;
            if (selection != sel) {
                return;
            }
            sel.chooser.choose(field, values.get(index));
            advance();
        }, () -> {
            dialogOpen = false;
            if (selection == sel) {
                selection = null;
                render();
            }
        });
    }

    private void submit(Action action) {
        if (session == null) {
            return;
        }
        ActionOutcome outcome = session.submitAsHuman(action);
        if (!outcome.accepted()) {
            sm.toast(outcome.message(), true);
            render();
        }
    }

    private void maybeResolve() {
        if (!canAct() || state.phase() != TurnPhase.RESOLVE_EFFECT || selection != null || dialogOpen) {
            return;
        }
        List<EffectParameters> options = engine.legalActions(state).stream()
                .filter(a -> a instanceof ResolveEffectAction).map(a -> ((ResolveEffectAction) a).parameters()).toList();
        if (options.isEmpty()) {
            return;
        }
        selection = new Selection(null, new ParameterChooser(options), state.version());
        advance();
    }

    // ---------------------------------------------------------------- board handler

    @Override
    public void handCardClicked(Card card) {
        if (state == null) {
            return;
        }
        if (!canAct()) {
            toAct().filter(p -> state.config().profile(p).isVirtual())
                    .ifPresent(p -> sm.toast(name(p) + " is playing. Please wait for your turn.", false));
            return;
        }
        switch (state.phase()) {
            case CLAIM -> sm.toast("You have already played a card this turn. Claim Stones or draw to end it.", false);
            case RESOLVE_EFFECT -> maybeResolve();
            case PLAY -> {
                if (selection != null && selection.card != null && selection.card.id().equals(card.id())) {
                    cancel();
                } else {
                    selection = null;
                    beginPlay(card);
                }
            }
            default -> {
            }
        }
    }

    @Override
    public boolean handCardDragStarted(Card card) {
        if (!canAct() || state.phase() != TurnPhase.PLAY) {
            return false;
        }
        if (selection == null || selection.card == null || !selection.card.id().equals(card.id())) {
            selection = null;
            if (!beginPlay(card)) {
                return false;
            }
        }
        return selection != null && (selection.awaiting == ParameterField.STONE
                || selection.awaiting == ParameterField.DESTINATION);
    }

    @Override
    public boolean acceptsDrop(int stone) {
        return selection != null && (selection.awaiting == ParameterField.STONE
                || selection.awaiting == ParameterField.DESTINATION)
                && selection.chooser.values(selection.awaiting).contains(stone);
    }

    @Override
    public void stoneChosen(int stone, boolean dropped) {
        if (selection == null || (selection.awaiting != ParameterField.STONE
                && selection.awaiting != ParameterField.DESTINATION)) {
            return;
        }
        if (!selection.chooser.values(selection.awaiting).contains(stone)) {
            sm.toast("Stone " + (stone + 1) + " is not a valid choice for this card.", true);
            return;
        }
        selection.chooser.choose(selection.awaiting, stone);
        selection.awaiting = null;
        advance();
    }

    @Override
    public boolean boardCardClicked(String cardId) {
        if (selection == null || (selection.awaiting != ParameterField.TARGET_CARD
                && selection.awaiting != ParameterField.SECOND_CARD)) {
            return false;
        }
        if (!selection.chooser.values(selection.awaiting).contains(cardId)) {
            sm.toast(describer.cardName(cardId) + " cannot be chosen for this card.", true);
            return true;
        }
        selection.chooser.choose(selection.awaiting, cardId);
        selection.awaiting = null;
        advance();
        return true;
    }

    @Override
    public void claimPressed(int stone) {
        if (!canAct() || state.phase() != TurnPhase.CLAIM) {
            return;
        }
        PlayerId me = viewer();
        List<Card> elites = state.board().stone(stone).side(me).cards().stream().filter(Card::hasWildColor).toList();
        if (elites.isEmpty()) {
            submit(new ClaimAction(stone));
            return;
        }
        ClaimCheck check = engine.checkClaim(state, me, stone);
        Map<String, EliteIdentity> defaults = check.claimant() != null ? check.claimant().identities() : Map.of();
        ScreenManager.Overlay[] overlay = new ScreenManager.Overlay[1];
        overlay[0] = sm.openOverlay(GameOverlays.identities(stone + 1, elites, defaults, ids -> {
            overlay[0].close();
            submit(new ClaimAction(stone, ids));
        }, () -> overlay[0].close()), true);
    }

    @Override
    public void cancel() {
        if (selection != null && selection.card != null) {
            selection = null;
            render();
        }
    }

    // ---------------------------------------------------------------- rail handler

    @Override
    public void menu() {
        sm.sound().play(SoundManager.Sfx.CLICK);
        ScreenManager.Overlay[] overlay = new ScreenManager.Overlay[1];
        overlay[0] = sm.openOverlay(GameOverlays.menu(new GameOverlays.MenuActions(
                () -> overlay[0].close(),
                () -> {
                    overlay[0].close();
                    saveDialog();
                },
                () -> {
                    overlay[0].close();
                    sm.showLoad();
                },
                () -> {
                    overlay[0].close();
                    sm.showRules(true);
                },
                () -> {
                    overlay[0].close();
                    sm.showSettings(true);
                },
                () -> {
                    overlay[0].close();
                    Dialogs.confirm(sm, "Restart?", "Start a new game with the same players and options?",
                            "Restart", "Cancel", this::playAgain);
                },
                () -> {
                    overlay[0].close();
                    leave();
                })), true);
    }

    private void saveDialog() {
        if (state == null) {
            return;
        }
        String suggestion = (name(PlayerId.P1) + "-vs-" + name(PlayerId.P2)).replaceAll("[^A-Za-z0-9-]", "");
        Dialogs.input(sm, "Save game", "Name of the saved game:", suggestion, "Save", name -> {
            String file = name.replaceAll("[^A-Za-z0-9 _-]", "").strip();
            if (file.isEmpty()) {
                file = "saved-game";
            }
            Path path = SaveManager.defaultDirectory().resolve(file + SaveManager.EXTENSION);
            try {
                controller.save(path);
                sm.toast("Game saved as \"" + file + "\".", false);
            } catch (IOException | IllegalStateException e) {
                sm.toast("The game could not be saved: " + e.getMessage(), true);
            }
        });
    }

    private void playAgain() {
        GameConfiguration c = state.config();
        controller.startNewGame(new GameConfiguration(c.player1(), c.player2(), c.tacticVariant(), c.expansions(),
                c.rounds(), c.firstPlayerRule(), System.nanoTime()));
    }

    private void leave() {
        Runnable go = () -> {
            controller.hub().clear();
            sm.showMainMenu();
        };
        if (state == null || state.phase() == TurnPhase.MATCH_OVER) {
            go.run();
        } else {
            Dialogs.confirm(sm, "Leave the game?", "Your game is saved automatically: choose Continue on the main "
                    + "menu to resume it.", "Leave", "Stay", go);
        }
    }

    @Override
    public void endTurn() {
        if (state == null) {
            return;
        }
        if (state.phase() == TurnPhase.ROUND_OVER) {
            submit(new NextRoundAction());
            return;
        }
        if (state.phase() == TurnPhase.MATCH_OVER) {
            showVictory();
            return;
        }
        if (!canAct()) {
            return;
        }
        if (state.phase() != TurnPhase.CLAIM) {
            sm.toast(state.phase() == TurnPhase.PLAY ? "Play a card (or pass) before ending your turn."
                    : "Finish the card effect first.", true);
            return;
        }
        List<DeckType> drawable = engine.drawableDecks(state, viewer());
        if (drawable.size() <= 1) {
            submit(new EndTurnAction(drawable.isEmpty() ? null : drawable.get(0)));
            return;
        }
        List<Dialogs.Option> options = drawable.stream()
                .map(d -> new Dialogs.Option(d.displayName() + " deck\n" + state.deckSize(d) + " cards left")).toList();
        Dialogs.choose(sm, "Draw a card", "Choose the deck you draw from.", options, true,
                i -> submit(new EndTurnAction(drawable.get(i))), null);
    }

    @Override
    public void pass() {
        submit(new PassAction());
    }

    @Override
    public void hint() {
        Optional<Action> suggestion = controller.hint();
        if (suggestion.isEmpty()) {
            return;
        }
        hint = suggestion.get();
        selection = null;
        sm.toast("Hint: " + describer.action(hint, state) + ".", false);
        render();
    }

    @Override
    public void deckClicked(DeckType deck) {
        if (!canAct()) {
            return;
        }
        if (state.phase() == TurnPhase.CLAIM) {
            if (engine.drawableDecks(state, viewer()).contains(deck)) {
                submit(new EndTurnAction(deck));
            } else {
                sm.toast("You cannot draw from the " + deck.displayName() + " deck now.", true);
            }
        } else if (state.phase() == TurnPhase.PLAY) {
            sm.toast("Play a card first: you draw at the end of your turn.", false);
        }
    }

    @Override
    public void discardClicked() {
        if (selection != null && selection.awaiting == ParameterField.DESTINATION
                && selection.chooser.values(ParameterField.DESTINATION).contains(EffectParameters.DISCARD)) {
            selection.chooser.choose(ParameterField.DESTINATION, EffectParameters.DISCARD);
            selection.awaiting = null;
            advance();
            return;
        }
        if (state == null) {
            return;
        }
        ScreenManager.Overlay[] overlay = new ScreenManager.Overlay[1];
        overlay[0] = sm.openOverlay(GameOverlays.discard(state.discard(), () -> overlay[0].close()), true);
    }

    @Override
    public void speedChanged(GameSettings.Speed speed) {
        GameSettings.getInstance().setSpeed(speed);
        controller.setAiDelay(speed.aiDelayMillis());
    }

    @Override
    public void pauseChanged(boolean paused) {
        controller.hub().driver().ifPresent(d -> d.setPaused(paused));
        sm.toast(paused ? "Computer players paused." : "Computer players resumed.", false);
    }

    @Override
    public void revealChanged(boolean reveal) {
        revealHands = reveal;
        render();
    }

    // ---------------------------------------------------------------- victory

    private void maybeShowVictory() {
        if (state.phase() != TurnPhase.ROUND_OVER && state.phase() != TurnPhase.MATCH_OVER) {
            return;
        }
        String key = state.round() + "-" + state.phase();
        if (key.equals(victoryKey)) {
            return;
        }
        victoryKey = key;
        PauseTransition delay = new PauseTransition(Duration.millis(sm.reduceMotion() ? 50 : 900));
        GameSession expected = session;
        delay.setOnFinished(e -> {
            if (session == expected && state != null && (state.phase() == TurnPhase.ROUND_OVER
                    || state.phase() == TurnPhase.MATCH_OVER)) {
                celebrate();
                showVictory();
            }
        });
        delay.play();
    }

    private void celebrate() {
        RoundResult r = state.lastRoundResult().orElse(null);
        if (r == null) {
            return;
        }
        Set<PlayerId> humans = humans();
        boolean lost = humans.size() == 1 && r.winner() != null && !humans.contains(r.winner());
        sm.sound().play(lost ? SoundManager.Sfx.DEFEAT : SoundManager.Sfx.VICTORY);
        sm.sound().duckMusic(4);
        if (r.winner() != null && !lost) {
            sm.confetti();
        }
    }

    private void showVictory() {
        if (victory != null || state == null || state.lastRoundResult().isEmpty()) {
            return;
        }
        boolean matchOver = state.phase() == TurnPhase.MATCH_OVER;
        GameOverlays.VictoryActions actions = new GameOverlays.VictoryActions(
                this::closeVictory,
                matchOver ? null : () -> {
                    closeVictory();
                    submit(new NextRoundAction());
                },
                () -> {
                    closeVictory();
                    playAgain();
                },
                () -> {
                    closeVictory();
                    leave();
                });
        ScreenManager.Overlay opened = sm.openOverlay(GameOverlays.victory(state, actions), false);
        opened.setOnClose(() -> {
            if (victory == opened) {
                victory = null;
            }
        });
        victory = opened;
    }

    private void closeVictory() {
        if (victory != null) {
            ScreenManager.Overlay v = victory;
            victory = null;
            v.close();
        }
    }

    // ---------------------------------------------------------------- keyboard

    private void onKey(KeyEvent e) {
        if (sm.hasOverlay() || state == null) {
            return;
        }
        KeyCode code = e.getCode();
        int digit = digit(code);
        if (digit > 0) {
            if (selection != null && (selection.awaiting == ParameterField.STONE
                    || selection.awaiting == ParameterField.DESTINATION)) {
                stoneChosen(digit - 1, false);
            } else if (canAct() && state.phase() == TurnPhase.PLAY && viewer() != null) {
                List<Card> hand = state.hand(viewer());
                if (digit <= hand.size()) {
                    handCardClicked(hand.get(digit - 1));
                }
            }
            e.consume();
            return;
        }
        if (e.isShortcutDown() && code == KeyCode.S) {
            saveDialog();
            e.consume();
            return;
        }
        switch (code) {
            case ESCAPE -> cancel();
            case C -> claimAll();
            case E, ENTER -> endTurn();
            case P -> {
                if (canAct()) {
                    pass();
                }
            }
            case H -> {
                if (canAct()) {
                    hint();
                }
            }
            case D -> discardClicked();
            case M -> rail.toggleMute();
            case N -> {
                if (state.phase() == TurnPhase.ROUND_OVER) {
                    submit(new NextRoundAction());
                }
            }
            default -> {
                return;
            }
        }
        e.consume();
    }

    private void claimAll() {
        if (!canAct() || state.phase() != TurnPhase.CLAIM) {
            return;
        }
        PlayerId me = viewer();
        List<Integer> stones = new ArrayList<>();
        for (StoneState s : state.board().stones()) {
            if (!s.isClaimed() && s.isComplete(me) && engine.checkClaim(state, me, s.index()).allowed()) {
                stones.add(s.index());
            }
        }
        if (stones.isEmpty()) {
            sm.toast("No Stone can be claimed right now.", false);
        }
        for (int stone : stones) {
            submit(new ClaimAction(stone));
        }
    }

    private static int digit(KeyCode code) {
        return switch (code) {
            case DIGIT1, NUMPAD1 -> 1;
            case DIGIT2, NUMPAD2 -> 2;
            case DIGIT3, NUMPAD3 -> 3;
            case DIGIT4, NUMPAD4 -> 4;
            case DIGIT5, NUMPAD5 -> 5;
            case DIGIT6, NUMPAD6 -> 6;
            case DIGIT7, NUMPAD7 -> 7;
            case DIGIT8, NUMPAD8 -> 8;
            case DIGIT9, NUMPAD9 -> 9;
            default -> 0;
        };
    }
}
