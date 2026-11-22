package com.schottentotten.engine;

import com.schottentotten.model.Board;
import com.schottentotten.model.Card;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Deck;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.MatchScore;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.rules.RuleSet;
import com.schottentotten.util.SeededRandom;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Creates new games and deals new rounds: builds and shuffles the decks in play and deals the
 * starting hands.
 */
public final class GameSetup {

    private final CardCatalog catalog;

    /**
     * Creates the set-up service.
     *
     * @param catalog card catalogue
     */
    public GameSetup(CardCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Creates a new game ready for the first turn.
     *
     * @param config session configuration
     * @return the initial state
     */
    public GameState newGame(GameConfiguration config) {
        SeededRandom rng = new SeededRandom(config.seed());
        PlayerId starter = switch (config.firstPlayerRule()) {
            case PLAYER_ONE -> PlayerId.P1;
            case PLAYER_TWO -> PlayerId.P2;
            case RANDOM -> rng.nextInt(2) == 0 ? PlayerId.P1 : PlayerId.P2;
        };
        Map<PlayerId, List<Card>> emptyHands = new EnumMap<>(PlayerId.class);
        GameState state = new GameState(config, new Board(), emptyHands, Map.of(), List.of(), 1, 1, starter,
                starter, TurnPhase.PLAY, Map.of(), EnumSet.noneOf(PlayerId.class), null, 0, 0, null,
                new MatchScore(config.rounds()), rng, List.of(), 0);
        dealRound(state, 1, starter);
        return state;
    }

    /**
     * Resets the board, rebuilds and shuffles every deck in play and deals fresh hands.
     *
     * @param state   the state to reset
     * @param round   round number
     * @param starter first player of the round
     */
    public void dealRound(GameState state, int round, PlayerId starter) {
        RuleSet rules = new RuleSet(state.config());
        Map<DeckType, Deck> decks = new EnumMap<>(DeckType.class);
        for (DeckType type : rules.decks()) {
            List<Card> cards = new ArrayList<>(catalog.all().stream()
                    .filter(c -> c.deckType() == type)
                    .sorted(Comparator.comparing(Card::id))
                    .toList());
            state.rng().shuffle(cards);
            decks.put(type, new Deck(type, cards));
        }
        Map<PlayerId, List<Card>> hands = new EnumMap<>(PlayerId.class);
        hands.put(PlayerId.P1, new ArrayList<>());
        hands.put(PlayerId.P2, new ArrayList<>());
        Deck clan = decks.get(DeckType.CLAN);
        for (int i = 0; i < rules.handSize(); i++) {
            hands.get(starter).add(clan.draw().orElseThrow());
            hands.get(starter.opponent()).add(clan.draw().orElseThrow());
        }
        state.resetRound(new Board(), hands, decks, round, starter);
        state.log(null, "Round " + round + " begins. " + state.config().profile(starter).name() + " plays first.");
    }

    /**
     * Returns the catalogue used by this set-up.
     *
     * @return catalogue
     */
    public CardCatalog catalog() {
        return catalog;
    }
}
