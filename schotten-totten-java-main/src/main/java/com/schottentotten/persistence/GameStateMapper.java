package com.schottentotten.persistence;

import com.schottentotten.model.ActiveWard;
import com.schottentotten.model.Board;
import com.schottentotten.model.Card;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.Deck;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.LogEntry;
import com.schottentotten.model.MatchScore;
import com.schottentotten.model.PendingEffect;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.PlayerType;
import com.schottentotten.model.RoundResult;
import com.schottentotten.model.StoneSide;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticCard;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.model.VictoryReason;
import com.schottentotten.persistence.SaveFileDto.ConfigDto;
import com.schottentotten.persistence.SaveFileDto.GameDto;
import com.schottentotten.persistence.SaveFileDto.LogDto;
import com.schottentotten.persistence.SaveFileDto.PendingDto;
import com.schottentotten.persistence.SaveFileDto.PlayerDto;
import com.schottentotten.persistence.SaveFileDto.RoundDto;
import com.schottentotten.persistence.SaveFileDto.SideDto;
import com.schottentotten.persistence.SaveFileDto.StoneDto;
import com.schottentotten.util.SeededRandom;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts between {@link GameState} and its JSON data transfer objects, validating everything
 * on the way back: unknown cards, duplicated cards, missing cards and impossible values are
 * reported as {@link SaveFormatException}s.
 */
public final class GameStateMapper {

    private final CardCatalog catalog;

    /**
     * Creates a mapper.
     *
     * @param catalog card catalogue used to resolve identifiers
     */
    public GameStateMapper(CardCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Converts a state to its DTO.
     *
     * @param s the state
     * @return game DTO
     */
    public GameDto toDto(GameState s) {
        GameConfiguration c = s.config();
        ConfigDto config = new ConfigDto(player(c.player1()), player(c.player2()), c.tacticVariant(), c.expansions(),
                c.rounds(), c.firstPlayerRule().name(), c.seed());
        Map<String, List<String>> hands = new LinkedHashMap<>();
        Map<String, Integer> special = new LinkedHashMap<>();
        List<String> jokers = new ArrayList<>();
        for (PlayerId p : PlayerId.values()) {
            hands.put(p.name(), ids(s.hand(p)));
            special.put(p.name(), s.specialPlayed(p));
            if (s.hasPlayedJoker(p)) {
                jokers.add(p.name());
            }
        }
        Map<String, List<String>> decks = new LinkedHashMap<>();
        for (DeckType t : DeckType.values()) {
            s.deck(t).ifPresent(d -> decks.put(t.name(), ids(d.cards())));
        }
        List<StoneDto> stones = s.board().stones().stream().map(st -> new StoneDto(side(st.side(PlayerId.P1)),
                side(st.side(PlayerId.P2)), ids(st.combatModes()), st.owner().map(Enum::name).orElse(null))).toList();
        PendingDto pending = s.pending().map(p -> new PendingDto(p.effectId(), p.sourceCardId(), p.player().name(),
                p.deck() == null ? null : p.deck().name(), p.cardIds())).orElse(null);
        return new GameDto(config, s.round(), s.turn(), s.currentPlayer().name(), s.roundStarter().name(),
                s.phase().name(), hands, decks, ids(s.discard()), stones, special, jokers, pending, s.sequence(),
                s.consecutivePasses(), s.lastRoundResult().map(GameStateMapper::round).orElse(null),
                s.match().results().stream().map(GameStateMapper::round).toList(), s.rng().state(),
                s.log().stream().map(l -> new LogDto(l.round(), l.turn(),
                        l.player() == null ? null : l.player().name(), l.text())).toList(), s.version());
    }

    /**
     * Rebuilds and validates a state from its DTO.
     *
     * @param d game DTO
     * @return the state
     * @throws SaveFormatException if the content is invalid or inconsistent
     */
    public GameState fromDto(GameDto d) throws SaveFormatException {
        if (d == null || d.config() == null) {
            throw new SaveFormatException("The save file contains no game.");
        }
        try {
            GameConfiguration config = new GameConfiguration(profile(d.config().player1()),
                    profile(d.config().player2()), d.config().tacticVariant(), d.config().expansions(),
                    d.config().rounds(), FirstPlayerRule.valueOf(d.config().firstPlayerRule()), d.config().seed());
            Set<String> seen = new HashSet<>();
            Map<PlayerId, List<Card>> hands = new EnumMap<>(PlayerId.class);
            for (PlayerId p : PlayerId.values()) {
                hands.put(p, cards(d.hands().get(p.name()), seen));
            }
            Map<DeckType, Deck> decks = new EnumMap<>(DeckType.class);
            for (Map.Entry<String, List<String>> e : d.decks().entrySet()) {
                DeckType type = DeckType.valueOf(e.getKey());
                List<Card> content = cards(e.getValue(), seen);
                for (Card card : content) {
                    if (card.deckType() != type) {
                        throw new SaveFormatException("Card " + card.id() + " is in the wrong deck.");
                    }
                }
                decks.put(type, new Deck(type, content));
            }
            List<Card> discard = cards(d.discard(), seen);
            if (d.stones() == null || d.stones().size() != Board.STONE_COUNT) {
                throw new SaveFormatException("A game needs exactly 9 Stones.");
            }
            List<StoneState> stones = new ArrayList<>();
            for (int i = 0; i < d.stones().size(); i++) {
                StoneDto st = d.stones().get(i);
                List<TacticCard> modes = new ArrayList<>();
                for (Card c : cards(st.combatModes(), seen)) {
                    if (!(c instanceof TacticCard t)) {
                        throw new SaveFormatException("Invalid combat mode card " + c.id());
                    }
                    modes.add(t);
                }
                stones.add(new StoneState(i, side(st.p1(), seen), side(st.p2(), seen), modes,
                        st.owner() == null ? null : PlayerId.valueOf(st.owner())));
            }
            checkCompleteness(config, seen);
            Map<PlayerId, Integer> special = new EnumMap<>(PlayerId.class);
            d.specialPlayed().forEach((k, v) -> special.put(PlayerId.valueOf(k), v));
            Set<PlayerId> jokers = EnumSet.noneOf(PlayerId.class);
            d.jokerPlayed().forEach(j -> jokers.add(PlayerId.valueOf(j)));
            PendingEffect pending = d.pending() == null ? null : new PendingEffect(d.pending().effectId(),
                    d.pending().sourceCardId(), PlayerId.valueOf(d.pending().player()),
                    d.pending().deck() == null ? null : DeckType.valueOf(d.pending().deck()), d.pending().cardIds());
            TurnPhase phase = TurnPhase.valueOf(d.phase());
            if ((phase == TurnPhase.RESOLVE_EFFECT) != (pending != null)) {
                throw new SaveFormatException("The pending effect does not match the turn phase.");
            }
            List<RoundResult> results = new ArrayList<>();
            for (RoundDto r : d.results()) {
                results.add(round(r));
            }
            List<LogEntry> log = d.log() == null ? List.of() : d.log().stream().map(l -> new LogEntry(l.round(),
                    l.turn(), l.player() == null ? null : PlayerId.valueOf(l.player()), l.text())).toList();
            return new GameState(config, new Board(stones), hands, decks, discard, d.round(), d.turn(),
                    PlayerId.valueOf(d.currentPlayer()), PlayerId.valueOf(d.roundStarter()), phase, special, jokers,
                    pending, d.sequence(), d.consecutivePasses(), d.lastRound() == null ? null : round(d.lastRound()),
                    new MatchScore(config.rounds(), results), new SeededRandom(d.rngState()), log, d.version());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new SaveFormatException("The save file is damaged or incompatible: " + e.getMessage(), e);
        }
    }

    private void checkCompleteness(GameConfiguration config, Set<String> seen) throws SaveFormatException {
        for (Card c : catalog.all()) {
            boolean inGame = switch (c.deckType()) {
                case CLAN -> true;
                case TACTIC -> config.tacticVariant();
                case EXPANSION -> config.expansions();
            };
            if (inGame != seen.contains(c.id())) {
                throw new SaveFormatException(inGame ? "Card " + c.id() + " is missing from the save file."
                        : "Card " + c.id() + " is not part of this game's rules.");
            }
        }
    }

    private List<Card> cards(List<String> ids, Set<String> seen) throws SaveFormatException {
        List<Card> result = new ArrayList<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            Card c = catalog.find(id).orElseThrow(() -> new SaveFormatException("Unknown card '" + id + "'."));
            if (!seen.add(id)) {
                throw new SaveFormatException("Card " + id + " appears twice in the save file.");
            }
            result.add(c);
        }
        return result;
    }

    private StoneSide side(SideDto dto, Set<String> seen) throws SaveFormatException {
        if (dto == null) {
            throw new SaveFormatException("A Stone side is missing.");
        }
        ActiveWard ward = null;
        if (dto.wardCardId() != null) {
            ward = new ActiveWard(cards(List.of(dto.wardCardId()), seen).get(0), dto.wardTurns());
        }
        return new StoneSide(cards(dto.cards(), seen), dto.completedAt(), ward);
    }

    private static SideDto side(StoneSide side) {
        return new SideDto(ids(side.cards()), side.completedAt(),
                side.ward().map(w -> w.card().id()).orElse(null),
                side.ward().map(ActiveWard::remainingOpponentTurns).orElse(0));
    }

    private static PlayerDto player(PlayerProfile p) {
        return new PlayerDto(p.name(), p.type().name(), p.difficulty().name());
    }

    private static PlayerProfile profile(PlayerDto p) {
        return new PlayerProfile(p.name(), PlayerType.valueOf(p.type()), Difficulty.valueOf(p.difficulty()));
    }

    private static RoundDto round(RoundResult r) {
        return new RoundDto(r.round(), r.winner() == null ? null : r.winner().name(), r.reason().name(),
                r.stonesP1(), r.stonesP2(), r.pointsP1(), r.pointsP2());
    }

    private static RoundResult round(RoundDto r) {
        return new RoundResult(r.round(), r.winner() == null ? null : PlayerId.valueOf(r.winner()),
                VictoryReason.valueOf(r.reason()), r.stonesP1(), r.stonesP2(), r.pointsP1(), r.pointsP2());
    }

    private static List<String> ids(List<? extends Card> cards) {
        return cards.stream().map(Card::id).toList();
    }
}
