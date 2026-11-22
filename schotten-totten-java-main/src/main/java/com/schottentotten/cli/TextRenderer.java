package com.schottentotten.cli;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.ExpansionCard;
import com.schottentotten.model.GameState;
import com.schottentotten.model.LogEntry;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.RoundResult;
import com.schottentotten.model.StoneSide;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticCard;

import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Renders game snapshots as plain (optionally ANSI-coloured) text for the command-line view.
 * It only reads {@link GameState} copies and never modifies them.
 */
public final class TextRenderer {

    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;]*m");
    private static final String RESET = "\u001B[0m";
    private static final int SIDE_WIDTH = 30;

    private final boolean color;

    /**
     * Creates a renderer.
     *
     * @param color {@code true} to use ANSI colours
     */
    public TextRenderer(boolean color) {
        this.color = color;
    }

    private String paint(String text, String code) {
        return color ? "\u001B[" + code + "m" + text + RESET : text;
    }

    /**
     * Returns bold text (when colours are enabled).
     *
     * @param text text
     * @return decorated text
     */
    public String bold(String text) {
        return paint(text, "1");
    }

    /**
     * Returns dimmed text (when colours are enabled).
     *
     * @param text text
     * @return decorated text
     */
    public String dim(String text) {
        return paint(text, "2");
    }

    /**
     * Returns text highlighted as a warning or error.
     *
     * @param text text
     * @return decorated text
     */
    public String warn(String text) {
        return paint(text, "38;5;203");
    }

    /**
     * Returns text highlighted as good news.
     *
     * @param text text
     * @return decorated text
     */
    public String good(String text) {
        return paint(text, "38;5;35");
    }

    /**
     * Returns the compact label of a card: "R5" for Clan cards, the name for special cards.
     *
     * @param card the card
     * @return label
     */
    public String label(Card card) {
        return switch (card) {
            case ClanCard c -> paint(c.shortLabel(), switch (c.color()) {
                case RED -> "1;38;5;196";
                case ORANGE -> "1;38;5;208";
                case YELLOW -> "1;38;5;220";
                case GREEN -> "1;38;5;34";
                case BLUE -> "1;38;5;33";
                case PURPLE -> "1;38;5;129";
            });
            case TacticCard t -> paint(t.displayName(), "1;38;5;44");
            case ExpansionCard x -> paint(x.displayName(), "1;38;5;178");
        };
    }

    /**
     * Returns the category of a card for hand listings.
     *
     * @param card the card
     * @return category text
     */
    public String category(Card card) {
        return switch (card) {
            case ClanCard c -> "Clan card, " + c.displayName();
            case TacticCard t -> "Tactic, " + t.type().category().displayName() + ": " + t.type().rulesText();
            case ExpansionCard x -> "Expansion, " + x.definition().category() + ": " + x.definition().rulesText();
        };
    }

    private static int visibleLength(String s) {
        return ANSI.matcher(s).replaceAll("").length();
    }

    private static String pad(String s, int width) {
        int missing = width - visibleLength(s);
        return missing > 0 ? s + " ".repeat(missing) : s;
    }

    private String side(StoneState stone, PlayerId player) {
        StoneSide side = stone.side(player);
        StringJoiner j = new StringJoiner(" ");
        side.cards().forEach(c -> j.add(label(c)));
        for (int i = side.size(); i < stone.requiredSize() && !stone.isClaimed(); i++) {
            j.add(dim("__"));
        }
        side.ward().ifPresent(w -> j.add(paint("[Ward " + w.remainingOpponentTurns() + "]", "38;5;178")));
        return j.toString();
    }

    /**
     * Renders the header line: round, turn, rules and scores.
     *
     * @param s state
     * @return header text
     */
    public String header(GameState s) {
        String p1 = s.config().player1().name();
        String p2 = s.config().player2().name();
        StringBuilder sb = new StringBuilder();
        sb.append(bold("Schotten Totten")).append(" - Round ").append(s.round());
        if (s.config().isMultiRound()) {
            sb.append(" of ").append(s.config().rounds());
        }
        sb.append(" - Turn ").append(s.turn()).append(" - ").append(s.config().describeRules());
        if (s.config().isMultiRound()) {
            sb.append(" - Score: ").append(p1).append(' ').append(s.match().points(PlayerId.P1))
                    .append(", ").append(p2).append(' ').append(s.match().points(PlayerId.P2));
        }
        return sb.toString();
    }

    /**
     * Renders the whole border: one row per Stone with both sides, owner and combat modes.
     *
     * @param s state
     * @return multi-line board text
     */
    public String board(GameState s) {
        String n1 = playerTitle(s, PlayerId.P1);
        String n2 = playerTitle(s, PlayerId.P2);
        StringBuilder sb = new StringBuilder();
        sb.append(header(s)).append('\n');
        StringJoiner decks = new StringJoiner(", ");
        s.decks().keySet().stream().sorted().forEach(d -> decks.add(d.displayName() + " " + s.deckSize(d)));
        sb.append("Decks: ").append(decks).append(" - Discard pile: ").append(s.discard().size())
                .append(" - Hands: ").append(s.config().player1().name()).append(' ').append(s.handSize(PlayerId.P1))
                .append(", ").append(s.config().player2().name()).append(' ').append(s.handSize(PlayerId.P2))
                .append('\n');
        String line = "-".repeat(7) + "+" + "-".repeat(SIDE_WIDTH + 2) + "+" + "-".repeat(10) + "+"
                + "-".repeat(SIDE_WIDTH + 2) + "+" + "-".repeat(14);
        sb.append(" Stone | ").append(pad(n2, SIDE_WIDTH)).append(" | Owner    | ").append(pad(n1, SIDE_WIDTH))
                .append(" | Combat modes\n").append(line).append('\n');
        for (StoneState stone : s.board().stones()) {
            String owner = stone.owner().map(o -> s.config().profile(o).name()).orElse("");
            if (owner.length() > 8) {
                owner = owner.substring(0, 8);
            }
            StringJoiner modes = new StringJoiner(", ");
            stone.combatModes().forEach(m -> modes.add(m.displayName()));
            sb.append(String.format("   %d   | ", stone.index() + 1))
                    .append(pad(side(stone, PlayerId.P2), SIDE_WIDTH)).append(" | ")
                    .append(pad(owner.isEmpty() ? "" : good(owner), 8)).append(" | ")
                    .append(pad(side(stone, PlayerId.P1), SIDE_WIDTH)).append(" | ")
                    .append(modes).append('\n');
        }
        sb.append(status(s));
        return sb.toString();
    }

    private String playerTitle(GameState s, PlayerId p) {
        PlayerProfile profile = s.config().profile(p);
        return profile.name() + " (" + (p == PlayerId.P1 ? "P1" : "P2")
                + (profile.isVirtual() ? ", " + profile.difficulty().displayName() + " AI" : "") + ")";
    }

    /**
     * Describes whose turn it is and what they must do.
     *
     * @param s state
     * @return status line
     */
    public String status(GameState s) {
        String name = s.config().profile(s.currentPlayer()).name();
        return switch (s.phase()) {
            case PLAY -> "Stones: " + stoneCount(s) + ". " + bold(name) + " must play a card.";
            case RESOLVE_EFFECT -> "Stones: " + stoneCount(s) + ". "
                    + bold(s.config().profile(s.pending().map(p -> p.player()).orElse(s.currentPlayer())).name())
                    + " must finish a card effect.";
            case CLAIM -> "Stones: " + stoneCount(s) + ". " + bold(name) + " may claim Stones, then ends the turn.";
            case ROUND_OVER -> "The round is over. " + s.lastRoundResult().map(r -> roundResult(s, r)).orElse("");
            case MATCH_OVER -> "The match is over. " + matchResult(s);
        };
    }

    private String stoneCount(GameState s) {
        return s.config().player1().name() + " " + s.board().stonesOwnedBy(PlayerId.P1) + ", "
                + s.config().player2().name() + " " + s.board().stonesOwnedBy(PlayerId.P2);
    }

    /**
     * Describes the result of a round.
     *
     * @param s state
     * @param r round result
     * @return text
     */
    public String roundResult(GameState s, RoundResult r) {
        if (r.winner() == null) {
            return "Round " + r.round() + " is a draw (" + r.reason().description() + ").";
        }
        return s.config().profile(r.winner()).name() + " wins round " + r.round() + ": "
                + r.reason().description() + ".";
    }

    /**
     * Describes the final result of the match.
     *
     * @param s state
     * @return text
     */
    public String matchResult(GameState s) {
        if (!s.config().isMultiRound()) {
            return s.lastRoundResult().map(r -> roundResult(s, r)).orElse("");
        }
        String score = s.config().player1().name() + " " + s.match().points(PlayerId.P1) + " points, "
                + s.config().player2().name() + " " + s.match().points(PlayerId.P2) + " points.";
        return s.match().winner().map(w -> s.config().profile(w).name() + " wins the match! ")
                .orElse("The match is a draw! ") + score;
    }

    /**
     * Renders a player's hand, numbered from 1.
     *
     * @param s      state
     * @param player the player
     * @return hand text
     */
    public String hand(GameState s, PlayerId player) {
        List<Card> hand = s.hand(player);
        StringBuilder sb = new StringBuilder(s.config().profile(player).name()).append("'s hand (")
                .append(hand.size()).append(" cards):\n");
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.get(i);
            sb.append(String.format("  %d) ", i + 1)).append(pad(label(c), 16)).append(dim(category(c))).append('\n');
        }
        if (s.config().tacticVariant() || s.config().expansions()) {
            sb.append(dim("  Special cards played this round: " + s.specialPlayed(player) + " (opponent "
                    + s.specialPlayed(player.opponent()) + ")")).append('\n');
        }
        return sb.toString();
    }

    /**
     * Renders the discard pile.
     *
     * @param s state
     * @return text
     */
    public String discard(GameState s) {
        if (s.discard().isEmpty()) {
            return "The discard pile is empty.";
        }
        StringJoiner j = new StringJoiner(", ", "Discard pile (oldest first): ", "");
        s.discard().forEach(c -> j.add(label(c)));
        return j.toString();
    }

    /**
     * Renders the last entries of the public log.
     *
     * @param s     state
     * @param count number of entries
     * @return text
     */
    public String log(GameState s, int count) {
        List<LogEntry> log = s.log();
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : log.subList(Math.max(0, log.size() - count), log.size())) {
            sb.append(dim(String.format("[R%d T%d] ", e.round(), e.turn()))).append(e.text()).append('\n');
        }
        return sb.length() == 0 ? "The log is empty." : sb.toString().stripTrailing();
    }

    /**
     * Describes the decks available for drawing.
     *
     * @param s     state
     * @param decks deck types
     * @return text
     */
    public String deckChoice(GameState s, List<DeckType> decks) {
        StringJoiner j = new StringJoiner(", ");
        decks.forEach(d -> j.add(d.displayName() + " (" + s.deckSize(d) + " left)"));
        return j.toString();
    }
}
