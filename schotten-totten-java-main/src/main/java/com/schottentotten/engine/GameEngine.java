package com.schottentotten.engine;

import com.schottentotten.engine.action.Action;
import com.schottentotten.engine.action.ClaimAction;
import com.schottentotten.engine.action.EndTurnAction;
import com.schottentotten.engine.action.NextRoundAction;
import com.schottentotten.engine.action.PassAction;
import com.schottentotten.engine.action.PlayCardAction;
import com.schottentotten.engine.action.ResolveEffectAction;
import com.schottentotten.model.Card;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PendingEffect;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.RoundResult;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.model.VictoryReason;
import com.schottentotten.rules.ClaimCheck;
import com.schottentotten.rules.ClaimResolver;
import com.schottentotten.rules.FormationEvaluator;
import com.schottentotten.rules.RuleSet;
import com.schottentotten.rules.VictoryChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The rules engine: validates and applies actions on a {@link GameState} and lists legal
 * actions. The engine itself is stateless and contains no user-interface code, so the same
 * instance serves the GUI, the CLI, the virtual players and the tests.
 *
 * <p><b>Turn state machine</b> ({@link TurnPhase}):</p>
 * <ul>
 *   <li>{@code PLAY}: the current player plays one card ({@link PlayCardAction}) or passes when
 *       allowed ({@link PassAction}).</li>
 *   <li>{@code RESOLVE_EFFECT}: a multi-step effect waits for its decision
 *       ({@link ResolveEffectAction}).</li>
 *   <li>{@code CLAIM}: the player claims any number of Stones ({@link ClaimAction}) and ends the
 *       turn, drawing one card ({@link EndTurnAction}).</li>
 *   <li>{@code ROUND_OVER}: waiting for {@link NextRoundAction}; {@code MATCH_OVER}: final.</li>
 * </ul>
 *
 * <p>Every action is fully validated before the state is touched, so a rejected action leaves
 * the state unchanged. {@link com.schottentotten.controller.GameSession} additionally applies
 * actions on a copy and commits atomically.</p>
 */
public final class GameEngine {

    private final EffectRegistry registry;
    private final CardCatalog catalog;
    private final GameSetup setup;
    private final ClaimResolver claimResolver;
    private final VictoryChecker victoryChecker = new VictoryChecker();

    /**
     * Creates an engine.
     *
     * @param registry card effects
     * @param catalog  card catalogue
     */
    public GameEngine(EffectRegistry registry, CardCatalog catalog) {
        this.registry = Objects.requireNonNull(registry);
        this.catalog = Objects.requireNonNull(catalog);
        this.setup = new GameSetup(catalog);
        this.claimResolver = new ClaimResolver(new FormationEvaluator(), catalog);
    }

    /**
     * Creates a new game.
     *
     * @param config session configuration
     * @return the initial state
     */
    public GameState newGame(GameConfiguration config) {
        return setup.newGame(config);
    }

    /**
     * Returns the card catalogue.
     *
     * @return catalogue
     */
    public CardCatalog catalog() {
        return catalog;
    }

    /**
     * Returns the effect registry.
     *
     * @return registry
     */
    public EffectRegistry registry() {
        return registry;
    }

    /**
     * Returns the effect of a card.
     *
     * @param card the card
     * @return its effect
     */
    public CardEffect effectOf(Card card) {
        return registry.get(card.effectId());
    }

    /**
     * Returns the rules for a state.
     *
     * @param state game state
     * @return rule set
     */
    public RuleSet rules(GameState state) {
        return new RuleSet(state.config());
    }

    /**
     * Returns the player expected to act, or empty when the round/match is over (in
     * {@code ROUND_OVER} any participant may start the next round).
     *
     * @param state game state
     * @return the player to act
     */
    public Optional<PlayerId> playerToAct(GameState state) {
        return switch (state.phase()) {
            case PLAY, CLAIM -> Optional.of(state.currentPlayer());
            case RESOLVE_EFFECT -> Optional.of(state.pending().map(PendingEffect::player)
                    .orElse(state.currentPlayer()));
            case ROUND_OVER, MATCH_OVER -> Optional.empty();
        };
    }

    /**
     * Checks a claim without applying it.
     *
     * @param state    game state
     * @param claimant claiming player
     * @param stone    Stone index
     * @return the claim check
     */
    public ClaimCheck checkClaim(GameState state, PlayerId claimant, int stone) {
        return claimResolver.check(state, claimant, stone);
    }

    /**
     * Returns the claim resolver.
     *
     * @return claim resolver
     */
    public ClaimResolver claimResolver() {
        return claimResolver;
    }

    /**
     * Tells whether an action would be accepted, without modifying the state.
     *
     * @param state  game state
     * @param actor  acting player
     * @param action the action
     * @return {@code true} if legal
     */
    public boolean isLegal(GameState state, PlayerId actor, Action action) {
        try {
            apply(state.copy(), actor, action);
            return true;
        } catch (IllegalActionException e) {
            return false;
        }
    }

    /**
     * Validates and applies an action.
     *
     * @param state  the state to modify
     * @param actor  the player submitting the action
     * @param action the action
     * @return public events describing what happened
     * @throws IllegalActionException if the action is illegal (the state is then unchanged)
     */
    public List<GameEvent> apply(GameState state, PlayerId actor, Action action) throws IllegalActionException {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(action, "action");
        List<GameEvent> events = new ArrayList<>();
        EffectContext ctx = new EffectContext(state, actor, rules(state), events);
        switch (action) {
            case PlayCardAction a -> play(ctx, a);
            case PassAction a -> pass(ctx);
            case ClaimAction a -> claim(ctx, a);
            case EndTurnAction a -> endTurn(ctx, a);
            case ResolveEffectAction a -> resolve(ctx, a);
            case NextRoundAction a -> nextRound(ctx);
        }
        state.bumpVersion();
        return events;
    }

    /**
     * Lists every legal action of the player who must act. In {@code ROUND_OVER} the only action
     * is {@link NextRoundAction}.
     *
     * @param state game state
     * @return legal actions (empty when the match is over)
     */
    public List<Action> legalActions(GameState state) {
        List<Action> actions = new ArrayList<>();
        PlayerId actor = playerToAct(state).orElse(state.currentPlayer());
        EffectContext ctx = new EffectContext(state, actor, rules(state), new ArrayList<>());
        switch (state.phase()) {
            case PLAY -> {
                for (Card card : state.hand(actor)) {
                    if (card.isSpecial() && !ctx.rules().maySpecialBePlayed(state, actor)) {
                        continue;
                    }
                    for (EffectParameters p : effectOf(card).enumerate(ctx, card)) {
                        actions.add(new PlayCardAction(card.id(), p));
                    }
                }
                if (ctx.rules().mayPass(state, actor)) {
                    actions.add(new PassAction());
                }
            }
            case RESOLVE_EFFECT -> state.pending().ifPresent(pending -> {
                MultiStepEffect effect = (MultiStepEffect) registry.get(pending.effectId());
                for (EffectParameters p : effect.enumerateResolutions(ctx, pending)) {
                    actions.add(new ResolveEffectAction(p));
                }
            });
            case CLAIM -> {
                for (StoneState stone : state.board().stones()) {
                    if (!stone.isClaimed() && stone.isComplete(actor)
                            && claimResolver.check(state, actor, stone.index()).allowed()) {
                        actions.add(new ClaimAction(stone.index()));
                    }
                }
                List<DeckType> drawable = drawableDecks(state, actor);
                if (drawable.isEmpty()) {
                    actions.add(new EndTurnAction(null));
                } else {
                    drawable.forEach(d -> actions.add(new EndTurnAction(d)));
                }
            }
            case ROUND_OVER -> actions.add(new NextRoundAction());
            case MATCH_OVER -> {
                // No action is possible once the match is over.
            }
        }
        return actions;
    }

    /**
     * Returns the decks the player may draw from at the end of the turn (empty if the hand is
     * full or every deck is empty).
     *
     * @param state  game state
     * @param player the player
     * @return drawable decks
     */
    public List<DeckType> drawableDecks(GameState state, PlayerId player) {
        if (state.handSize(player) >= rules(state).handSize()) {
            return List.of();
        }
        return state.nonEmptyDecks();
    }

    private void requireTurn(EffectContext ctx, TurnPhase expected, String what) throws IllegalActionException {
        GameState state = ctx.state();
        if (state.phase() == TurnPhase.MATCH_OVER) {
            throw new IllegalActionException("The match is over.");
        }
        if (state.phase() == TurnPhase.ROUND_OVER) {
            throw new IllegalActionException("The round is over. Start the next round first.");
        }
        if (ctx.actor() != state.currentPlayer()) {
            throw new IllegalActionException("It is " + ctx.name(state.currentPlayer()) + "'s turn.");
        }
        if (state.phase() != expected) {
            String hint = switch (state.phase()) {
                case PLAY -> "You must play a card (or pass) first.";
                case RESOLVE_EFFECT -> "Finish resolving the current card effect first.";
                case CLAIM -> "You have already played this turn. Claim Stones or end your turn.";
                default -> "";
            };
            throw new IllegalActionException("You cannot " + what + " now. " + hint);
        }
    }

    private void play(EffectContext ctx, PlayCardAction action) throws IllegalActionException {
        requireTurn(ctx, TurnPhase.PLAY, "play a card");
        GameState state = ctx.state();
        PlayerId actor = ctx.actor();
        Card card = state.findInHand(actor, action.cardId())
                .orElseThrow(() -> new IllegalActionException("You do not hold that card."));
        if (card.isSpecial() && !ctx.rules().maySpecialBePlayed(state, actor)) {
            throw new IllegalActionException("You cannot play another special card until your opponent has played"
                    + " as many as you (" + state.specialPlayed(actor) + " vs "
                    + state.specialPlayed(actor.opponent()) + ").");
        }
        CardEffect effect = effectOf(card);
        effect.validate(ctx, card, action.parameters());
        state.removeFromHand(actor, card.id());
        if (card.isSpecial()) {
            state.incrementSpecialPlayed(actor);
        }
        state.nextSequence();
        state.setConsecutivePasses(0);
        effect.apply(ctx, card, action.parameters());
        state.setPhase(state.pending().isPresent() ? TurnPhase.RESOLVE_EFFECT : TurnPhase.CLAIM);
    }

    private void pass(EffectContext ctx) throws IllegalActionException {
        requireTurn(ctx, TurnPhase.PLAY, "pass");
        GameState state = ctx.state();
        if (!ctx.rules().mayPass(state, ctx.actor())) {
            throw new IllegalActionException("You can only pass when none of your Clan cards can be placed.");
        }
        state.setConsecutivePasses(state.consecutivePasses() + 1);
        state.setPhase(TurnPhase.CLAIM);
        ctx.emit(GameEventType.PASSED, ctx.name(ctx.actor()) + " passes.", -1, null);
    }

    private void resolve(EffectContext ctx, ResolveEffectAction action) throws IllegalActionException {
        GameState state = ctx.state();
        PendingEffect pending = state.pending()
                .orElseThrow(() -> new IllegalActionException("There is no effect to resolve."));
        if (state.phase() != TurnPhase.RESOLVE_EFFECT) {
            throw new IllegalActionException("There is no effect to resolve.");
        }
        if (pending.player() != ctx.actor()) {
            throw new IllegalActionException("Only " + ctx.name(pending.player()) + " can resolve this effect.");
        }
        MultiStepEffect effect = (MultiStepEffect) registry.get(pending.effectId());
        effect.validateResolution(ctx, pending, action.parameters());
        effect.resolve(ctx, pending, action.parameters());
        state.setPending(null);
        state.setPhase(TurnPhase.CLAIM);
    }

    private void claim(EffectContext ctx, ClaimAction action) throws IllegalActionException {
        requireTurn(ctx, TurnPhase.CLAIM, "claim a Stone");
        GameState state = ctx.state();
        ClaimCheck check = claimResolver.check(state, ctx.actor(), action.stone(), action.identities());
        if (!check.allowed()) {
            throw new IllegalActionException(check.reason());
        }
        StoneState stone = state.board().stone(action.stone());
        stone.claim(ctx.actor());
        state.setConsecutivePasses(0);
        String identities = check.claimant().identities().isEmpty() ? ""
                : " Elite Troops count as " + check.claimant().identities().values() + ".";
        ctx.emit(GameEventType.STONE_CLAIMED, ctx.name(ctx.actor()) + " claims Stone " + (action.stone() + 1)
                + ": " + check.reason() + identities, action.stone(), null);
        for (LifecycleHook hook : registry.hooks()) {
            hook.onStoneClaimed(ctx, stone);
        }
        victoryChecker.check(state.board()).ifPresent(v -> endRound(ctx, v.winner(), v.reason()));
    }

    private void endTurn(EffectContext ctx, EndTurnAction action) throws IllegalActionException {
        requireTurn(ctx, TurnPhase.CLAIM, "end your turn");
        GameState state = ctx.state();
        PlayerId actor = ctx.actor();
        List<DeckType> drawable = drawableDecks(state, actor);
        DeckType deck = action.drawFrom();
        if (drawable.isEmpty()) {
            if (deck != null) {
                throw new IllegalActionException(state.handSize(actor) >= ctx.rules().handSize()
                        ? "Your hand is full; you cannot draw." : "Every deck is empty; you cannot draw.");
            }
        } else {
            if (deck == null) {
                if (drawable.size() > 1) {
                    throw new IllegalActionException("Choose which deck to draw from: " + drawable + ".");
                }
                deck = drawable.get(0);
            }
            if (!drawable.contains(deck)) {
                throw new IllegalActionException("You cannot draw from the " + deck.displayName() + " deck.");
            }
            Card drawn = state.deck(deck).orElseThrow().draw().orElseThrow();
            state.addToHand(actor, drawn);
            state.setConsecutivePasses(0);
            ctx.emit(GameEventType.CARD_DRAWN, ctx.name(actor) + " draws from the " + deck.displayName() + " deck.",
                    -1, null);
        }
        for (LifecycleHook hook : registry.hooks()) {
            hook.onTurnEnd(ctx, actor);
        }
        if (state.consecutivePasses() >= 2) {
            int mine = state.board().stonesOwnedBy(actor);
            int theirs = state.board().stonesOwnedBy(actor.opponent());
            PlayerId winner = mine == theirs ? null : (mine > theirs ? actor : actor.opponent());
            endRound(ctx, winner, winner == null ? VictoryReason.DEADLOCK_DRAW : VictoryReason.DEADLOCK_MORE_STONES);
            return;
        }
        state.advanceTurn();
        state.setPhase(TurnPhase.PLAY);
        ctx.events().add(GameEvent.of(GameEventType.TURN_STARTED, state.currentPlayer(),
                ctx.name(state.currentPlayer()) + "'s turn."));
    }

    private void endRound(EffectContext ctx, PlayerId winner, VictoryReason reason) {
        GameState state = ctx.state();
        RoundResult result = RoundResult.of(state.round(), winner, reason, state.board());
        state.match().record(result);
        state.setLastRoundResult(result);
        String text = winner == null ? "Round " + state.round() + " ends in a draw: " + reason.description() + "."
                : ctx.name(winner) + " wins round " + state.round() + ": " + reason.description() + ".";
        ctx.emit(GameEventType.ROUND_OVER, text, -1, null);
        if (state.match().isComplete()) {
            state.setPhase(TurnPhase.MATCH_OVER);
            String matchText = state.match().winner()
                    .map(p -> ctx.name(p) + " wins the match " + state.match().points(p) + " to "
                            + state.match().points(p.opponent()) + "!")
                    .orElse("The match is drawn.");
            ctx.emit(GameEventType.MATCH_OVER, matchText, -1, null);
        } else {
            state.setPhase(TurnPhase.ROUND_OVER);
        }
    }

    private void nextRound(EffectContext ctx) throws IllegalActionException {
        GameState state = ctx.state();
        if (state.phase() != TurnPhase.ROUND_OVER) {
            throw new IllegalActionException(state.phase() == TurnPhase.MATCH_OVER
                    ? "The match is over." : "The current round is not finished.");
        }
        RoundResult last = state.lastRoundResult().orElseThrow();
        PlayerId starter = last.winner() == null ? state.roundStarter().opponent() : last.winner().opponent();
        setup.dealRound(state, state.round() + 1, starter);
        ctx.events().add(GameEvent.of(GameEventType.ROUND_STARTED, starter,
                "Round " + state.round() + " begins. " + ctx.name(starter) + " plays first."));
    }
}
