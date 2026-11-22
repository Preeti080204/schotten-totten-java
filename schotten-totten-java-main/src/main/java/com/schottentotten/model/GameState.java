package com.schottentotten.model;

import com.schottentotten.util.SeededRandom;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The complete, authoritative state of one game session: board, hands, decks, discard, turn
 * phase, special-card counters, pending effect, match score and random generator.
 *
 * <p>Only the engine mutates a {@code GameState}, and only after validating the action. Views
 * never hold a live reference: they receive deep copies through {@link #copy()} so that rendering
 * can never race with the engine.</p>
 */
public final class GameState {

    private static final int LOG_LIMIT = 300;

    private final GameConfiguration config;
    private final Map<PlayerId, List<Card>> hands = new EnumMap<>(PlayerId.class);
    private final Map<DeckType, Deck> decks = new EnumMap<>(DeckType.class);
    private final List<Card> discard = new ArrayList<>();
    private final Map<PlayerId, Integer> specialPlayed = new EnumMap<>(PlayerId.class);
    private final Set<PlayerId> jokerPlayed = EnumSet.noneOf(PlayerId.class);
    private final List<LogEntry> log = new ArrayList<>();
    private final MatchScore match;
    private final SeededRandom rng;
    private Board board;
    private int round;
    private int turn;
    private PlayerId currentPlayer;
    private PlayerId roundStarter;
    private TurnPhase phase;
    private PendingEffect pending;
    private long sequence;
    private int consecutivePasses;
    private RoundResult lastRoundResult;
    private long version;

    /**
     * Creates a state from all of its components. Used by game set-up, copies and save-file
     * loading. Collections are copied.
     *
     * @param config            session configuration
     * @param board             the border
     * @param hands             hand of each player
     * @param decks             draw piles (only those in play)
     * @param discard           public discard pile, oldest first
     * @param round             current round number (1-based)
     * @param turn              current turn number within the round (1-based)
     * @param currentPlayer     player whose turn it is
     * @param roundStarter      player who started the current round
     * @param phase             current turn phase
     * @param specialPlayed     number of special cards played by each player this round
     * @param jokerPlayed       players who already played a Joker this round
     * @param pending           pending multi-step effect or {@code null}
     * @param sequence          monotonic action counter used for completion order
     * @param consecutivePasses number of consecutive passes
     * @param lastRoundResult   result of the last finished round or {@code null}
     * @param match             match score sheet
     * @param rng               random generator
     * @param log               public log entries
     * @param version           state version, incremented on every applied action
     */
    public GameState(GameConfiguration config, Board board, Map<PlayerId, List<Card>> hands,
                     Map<DeckType, Deck> decks, List<Card> discard, int round, int turn,
                     PlayerId currentPlayer, PlayerId roundStarter, TurnPhase phase,
                     Map<PlayerId, Integer> specialPlayed, Set<PlayerId> jokerPlayed, PendingEffect pending,
                     long sequence, int consecutivePasses, RoundResult lastRoundResult, MatchScore match,
                     SeededRandom rng, List<LogEntry> log, long version) {
        this.config = config;
        this.board = board;
        for (PlayerId p : PlayerId.values()) {
            this.hands.put(p, new ArrayList<>(hands.getOrDefault(p, List.of())));
            this.specialPlayed.put(p, specialPlayed.getOrDefault(p, 0));
        }
        decks.forEach((type, deck) -> this.decks.put(type, deck.copy()));
        this.discard.addAll(discard);
        this.round = round;
        this.turn = turn;
        this.currentPlayer = currentPlayer;
        this.roundStarter = roundStarter;
        this.phase = phase;
        this.jokerPlayed.addAll(jokerPlayed);
        this.pending = pending;
        this.sequence = sequence;
        this.consecutivePasses = consecutivePasses;
        this.lastRoundResult = lastRoundResult;
        this.match = match;
        this.rng = rng;
        this.log.addAll(log);
        this.version = version;
    }

    /**
     * Returns the session configuration.
     *
     * @return configuration
     */
    public GameConfiguration config() {
        return config;
    }

    /**
     * Returns the border.
     *
     * @return board
     */
    public Board board() {
        return board;
    }

    /**
     * Returns a player's hand.
     *
     * @param player the player
     * @return unmodifiable copy of the hand
     */
    public List<Card> hand(PlayerId player) {
        return List.copyOf(hands.get(player));
    }

    /**
     * Returns the number of cards in a player's hand (public information).
     *
     * @param player the player
     * @return hand size
     */
    public int handSize(PlayerId player) {
        return hands.get(player).size();
    }

    /**
     * Finds a card in a player's hand.
     *
     * @param player the player
     * @param cardId card identifier
     * @return the card if the player holds it
     */
    public Optional<Card> findInHand(PlayerId player, String cardId) {
        return hands.get(player).stream().filter(c -> c.id().equals(cardId)).findFirst();
    }

    /**
     * Adds a card to a hand.
     *
     * @param player the player
     * @param card   the card
     */
    public void addToHand(PlayerId player, Card card) {
        hands.get(player).add(card);
    }

    /**
     * Removes a card from a hand.
     *
     * @param player the player
     * @param cardId card identifier
     * @return the removed card
     * @throws IllegalArgumentException if the player does not hold the card
     */
    public Card removeFromHand(PlayerId player, String cardId) {
        Card card = findInHand(player, cardId)
                .orElseThrow(() -> new IllegalArgumentException("Card not in hand: " + cardId));
        hands.get(player).remove(card);
        return card;
    }

    /**
     * Returns a deck if it is part of this game.
     *
     * @param type deck type
     * @return the deck, or empty if this deck is not used
     */
    public Optional<Deck> deck(DeckType type) {
        return Optional.ofNullable(decks.get(type));
    }

    /**
     * Returns the decks used in this game.
     *
     * @return unmodifiable view of the decks by type
     */
    public Map<DeckType, Deck> decks() {
        return Map.copyOf(decks);
    }

    /**
     * Returns the size of a deck, 0 if the deck is not used.
     *
     * @param type deck type
     * @return remaining cards
     */
    public int deckSize(DeckType type) {
        return deck(type).map(Deck::size).orElse(0);
    }

    /**
     * Returns the decks that still contain cards.
     *
     * @return list of non-empty deck types
     */
    public List<DeckType> nonEmptyDecks() {
        return decks.values().stream().filter(d -> !d.isEmpty()).map(Deck::type).toList();
    }

    /**
     * Returns the public discard pile, oldest first.
     *
     * @return unmodifiable copy
     */
    public List<Card> discard() {
        return List.copyOf(discard);
    }

    /**
     * Puts a card face-up on the discard pile.
     *
     * @param card card to discard
     */
    public void addToDiscard(Card card) {
        discard.add(card);
    }

    /**
     * Returns the current round number.
     *
     * @return round, starting at 1
     */
    public int round() {
        return round;
    }

    /**
     * Returns the current turn number within the round.
     *
     * @return turn, starting at 1
     */
    public int turn() {
        return turn;
    }

    /**
     * Returns the player whose turn it is.
     *
     * @return current player
     */
    public PlayerId currentPlayer() {
        return currentPlayer;
    }

    /**
     * Returns the player who started the current round.
     *
     * @return round starter
     */
    public PlayerId roundStarter() {
        return roundStarter;
    }

    /**
     * Returns the current phase.
     *
     * @return turn phase
     */
    public TurnPhase phase() {
        return phase;
    }

    /**
     * Sets the turn phase.
     *
     * @param phase new phase
     */
    public void setPhase(TurnPhase phase) {
        this.phase = phase;
    }

    /**
     * Passes the turn to the next player and increments the turn counter.
     */
    public void advanceTurn() {
        currentPlayer = currentPlayer.opponent();
        turn++;
    }

    /**
     * Returns the number of special cards (Tactic and expansion) a player has played this round.
     *
     * @param player the player
     * @return special cards played
     */
    public int specialPlayed(PlayerId player) {
        return specialPlayed.get(player);
    }

    /**
     * Increments a player's special-card counter.
     *
     * @param player the player
     */
    public void incrementSpecialPlayed(PlayerId player) {
        specialPlayed.merge(player, 1, Integer::sum);
    }

    /**
     * Tells whether a player has played a Joker this round.
     *
     * @param player the player
     * @return {@code true} once a Joker was played
     */
    public boolean hasPlayedJoker(PlayerId player) {
        return jokerPlayed.contains(player);
    }

    /**
     * Records that a player played a Joker.
     *
     * @param player the player
     */
    public void markJokerPlayed(PlayerId player) {
        jokerPlayed.add(player);
    }

    /**
     * Returns the pending multi-step effect.
     *
     * @return pending effect, if any
     */
    public Optional<PendingEffect> pending() {
        return Optional.ofNullable(pending);
    }

    /**
     * Sets or clears the pending effect.
     *
     * @param pending effect or {@code null}
     */
    public void setPending(PendingEffect pending) {
        this.pending = pending;
    }

    /**
     * Returns the last used action sequence number.
     *
     * @return sequence
     */
    public long sequence() {
        return sequence;
    }

    /**
     * Advances and returns the action sequence (used to order formation completions).
     *
     * @return the new sequence number
     */
    public long nextSequence() {
        return ++sequence;
    }

    /**
     * Returns the number of consecutive passes.
     *
     * @return pass counter
     */
    public int consecutivePasses() {
        return consecutivePasses;
    }

    /**
     * Sets the consecutive-pass counter.
     *
     * @param value new value
     */
    public void setConsecutivePasses(int value) {
        this.consecutivePasses = value;
    }

    /**
     * Returns the result of the last finished round.
     *
     * @return last round result, if any
     */
    public Optional<RoundResult> lastRoundResult() {
        return Optional.ofNullable(lastRoundResult);
    }

    /**
     * Records the result of the round that just ended.
     *
     * @param result the result
     */
    public void setLastRoundResult(RoundResult result) {
        this.lastRoundResult = result;
    }

    /**
     * Returns the match score sheet.
     *
     * @return match score
     */
    public MatchScore match() {
        return match;
    }

    /**
     * Returns the random generator of the session.
     *
     * @return generator
     */
    public SeededRandom rng() {
        return rng;
    }

    /**
     * Returns the public log.
     *
     * @return unmodifiable copy, oldest first
     */
    public List<LogEntry> log() {
        return List.copyOf(log);
    }

    /**
     * Appends a public log entry, discarding the oldest when the log is full.
     *
     * @param player acting player or {@code null}
     * @param text   message
     */
    public void log(PlayerId player, String text) {
        log.add(new LogEntry(round, turn, player, text));
        if (log.size() > LOG_LIMIT) {
            log.remove(0);
        }
    }

    /**
     * Returns the state version, incremented on every applied action.
     *
     * @return version
     */
    public long version() {
        return version;
    }

    /**
     * Increments the version after an action has been applied.
     */
    public void bumpVersion() {
        version++;
    }

    /**
     * Resets the round-scoped data for a new round.
     *
     * @param newBoard   empty board
     * @param newHands   freshly dealt hands
     * @param newDecks   shuffled decks
     * @param newRound   new round number
     * @param starter    player who starts the round
     */
    public void resetRound(Board newBoard, Map<PlayerId, List<Card>> newHands, Map<DeckType, Deck> newDecks,
                           int newRound, PlayerId starter) {
        this.board = newBoard;
        for (PlayerId p : PlayerId.values()) {
            hands.put(p, new ArrayList<>(newHands.get(p)));
            specialPlayed.put(p, 0);
        }
        decks.clear();
        decks.putAll(newDecks);
        discard.clear();
        jokerPlayed.clear();
        pending = null;
        consecutivePasses = 0;
        round = newRound;
        turn = 1;
        currentPlayer = starter;
        roundStarter = starter;
        phase = TurnPhase.PLAY;
    }

    /**
     * Creates a deep copy that shares only immutable objects (cards, configuration).
     *
     * @return independent copy
     */
    public GameState copy() {
        return new GameState(config, board.copy(), hands, decks, discard, round, turn, currentPlayer,
                roundStarter, phase, specialPlayed, jokerPlayed, pending, sequence, consecutivePasses,
                lastRoundResult, match.copy(), rng.copy(), log, version);
    }

    /**
     * Replaces the content of a hand (used by information hiding for virtual players).
     *
     * @param player the player
     * @param cards  new hand content
     */
    public void replaceHand(PlayerId player, List<Card> cards) {
        hands.put(player, new ArrayList<>(cards));
    }

    /**
     * Replaces a deck (used by information hiding for virtual players).
     *
     * @param deck new deck
     */
    public void replaceDeck(Deck deck) {
        decks.put(deck.type(), deck);
    }
}
