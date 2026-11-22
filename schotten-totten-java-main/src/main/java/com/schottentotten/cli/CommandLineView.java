package com.schottentotten.cli;

import com.schottentotten.controller.ActionOutcome;
import com.schottentotten.controller.GameController;
import com.schottentotten.controller.GameSession;
import com.schottentotten.controller.ParameterChooser;
import com.schottentotten.controller.RulesDigest;
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
import com.schottentotten.model.ClanColor;
import com.schottentotten.model.DeckType;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PendingEffect;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveFormatException;
import com.schottentotten.persistence.SaveManager;
import com.schottentotten.rules.ClaimCheck;
import com.schottentotten.rules.EliteIdentity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Command-line view of the game. It observes the shared session exactly like the GUI does and
 * submits the same actions through it, so a game can be followed and played from the terminal
 * and the window at the same time.
 *
 * <p>Every command goes through the engine's validation: the CLI only builds actions and prints
 * the engine's explanation when one is refused.</p>
 */
public final class CommandLineView implements SessionListener {

    private static final String HELP = """
            Game:
              new                     start a new game (asks the options, including expansion cards)
              load [name|number]      resume a saved game; 'saves' lists them
              save [name]             save the current game
              quit                    leave the command line
            Your turn:
              hand (h)                show your hand
              board (b)               show the border
              play (p) <card> [stone] play a card: <card> is its number in your hand or its name
                                      (e.g. 'play 2 5', 'play R7 3', 'play traitor'); the CLI asks
                                      for any other target
              resolve (r)             finish a card effect that needs a second step (Recruiter, Scout)
              claim (c) [stone|all] [as <card>=<colour><value> ...]
                                      claim a Stone; without a number, lists what you may claim.
                                      'as' chooses Elite Troop identities, e.g. 'claim 4 as joker=R9'
              end (e) [clan|tactic|expansion]
                                      end the turn and draw (asks the deck if several are allowed)
              pass                    pass when you cannot play any card
              next (n)                start the next round of a match
            Information:
              discard  log [n]  status  legal  card <number|name>  rules [section]
              hint                    suggest a move;  auto  plays the suggested move
              speed slow|normal|fast|instant   pace of virtual players;  pause / resume
            """;

    private final GameController controller;
    private final GameEngine engine;
    private final TextDescriber describer;
    private final TextRenderer renderer;
    private final RulesDigest rules;
    private final BufferedReader in;
    private final PrintStream out;
    private final boolean shared;
    private final Object outputLock = new Object();
    private volatile GameSession attached;
    private volatile boolean running = true;

    /**
     * Creates the command-line view.
     *
     * @param controller application controller shared with the GUI
     * @param rules      rules digest
     * @param in         command input
     * @param out        output
     * @param color      {@code true} to use ANSI colours
     * @param shared     {@code true} when the GUI shows the same session
     */
    public CommandLineView(GameController controller, RulesDigest rules, Reader in, PrintStream out,
                           boolean color, boolean shared) {
        this.controller = controller;
        this.engine = controller.engine();
        this.describer = new TextDescriber(engine.catalog());
        this.renderer = new TextRenderer(color);
        this.rules = rules;
        this.in = in instanceof BufferedReader b ? b : new BufferedReader(in);
        this.out = out;
        this.shared = shared;
    }

    /**
     * Runs the command loop until {@code quit} or end of input.
     */
    public void run() {
        connect();
        println(renderer.bold("Schotten Totten - command line") + (shared ? " (sharing the game with the window)" : ""));
        if (attached == null) {
            println("Type 'new' to start a game, 'load' to resume one, or 'help' for all commands.");
        }
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                execute(line);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            GameSession s = attached;
            if (s != null) {
                s.removeListener(this);
            }
        }
    }

    /**
     * Tells whether the loop is still running.
     *
     * @return running flag
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Starts following the shared session hub (called by {@link #run()}; tests call it directly
     * and then use {@link #execute(String)}).
     */
    public void connect() {
        controller.hub().addSessionChangeListener(this::attach);
        controller.hub().current().ifPresent(this::attach);
    }

    private void attach(GameSession session) {
        GameSession previous = attached;
        if (previous == session) {
            return;
        }
        if (previous != null) {
            previous.removeListener(this);
        }
        attached = session;
        if (session == null) {
            println("The game was closed. Type 'new' to start one or 'load' to resume one.");
            return;
        }
        session.addListener(this);
    }

    private void println(String text) {
        synchronized (outputLock) {
            out.println(text);
            out.flush();
        }
    }

    // ------------------------------------------------------------------ session events

    @Override
    public void onUpdate(SessionUpdate update) {
        if (update.session() != attached) {
            return;
        }
        GameState s = update.snapshot();
        boolean attachedNow = update.events().size() == 1 && update.events().get(0).type() == GameEventType.GAME_STARTED;
        StringBuilder sb = new StringBuilder();
        if (attachedNow) {
            sb.append(renderer.board(s)).append('\n');
        } else {
            for (GameEvent e : update.events()) {
                sb.append(renderer.dim("  > ")).append(e.message()).append('\n');
            }
        }
        boolean turnStarted = attachedNow || update.events().stream().anyMatch(e ->
                e.type() == GameEventType.TURN_STARTED || e.type() == GameEventType.ROUND_STARTED);
        Optional<PlayerId> toAct = engine.playerToAct(s);
        if (s.phase() == TurnPhase.ROUND_OVER || s.phase() == TurnPhase.MATCH_OVER) {
            if (!attachedNow) {
                sb.append(renderer.board(s)).append('\n');
            }
            if (s.phase() == TurnPhase.ROUND_OVER) {
                sb.append(bothVirtual(s) ? "The next round starts shortly.\n" : "Type 'next' to start the next round.\n");
            } else {
                sb.append(renderer.bold(renderer.matchResult(s))).append(" Type 'new' to play again.\n");
            }
        } else if (toAct.isPresent() && !s.config().profile(toAct.get()).isVirtual()) {
            PlayerId p = toAct.get();
            if (turnStarted && s.phase() == TurnPhase.PLAY) {
                if (!attachedNow) {
                    sb.append(renderer.board(s)).append('\n');
                }
                if (singleHuman(s).isPresent()) {
                    sb.append(renderer.hand(s, p));
                    sb.append("Your turn: 'play <card> <stone>' (or 'help').\n");
                } else {
                    sb.append(renderer.bold(s.config().profile(p).name())).append(", it is your turn. Type 'hand' to see your cards (")
                            .append(s.config().profile(p.opponent()).name()).append(", look away).\n");
                }
            } else if (s.phase() == TurnPhase.CLAIM && update.events().stream().anyMatch(e -> e.player() == p
                    && e.type() != GameEventType.STONE_CLAIMED)) {
                sb.append(claimHint(s, p)).append('\n');
            } else if (s.phase() == TurnPhase.RESOLVE_EFFECT) {
                sb.append(pendingPrompt(s)).append(" Type 'resolve'.\n");
            }
        }
        if (sb.length() > 0) {
            println(sb.toString().stripTrailing());
        }
    }

    private boolean bothVirtual(GameState s) {
        return s.config().player1().isVirtual() && s.config().player2().isVirtual();
    }

    private Optional<PlayerId> singleHuman(GameState s) {
        boolean h1 = !s.config().player1().isVirtual();
        boolean h2 = !s.config().player2().isVirtual();
        if (h1 == h2) {
            return Optional.empty();
        }
        return Optional.of(h1 ? PlayerId.P1 : PlayerId.P2);
    }

    private String claimHint(GameState s, PlayerId p) {
        List<Integer> claimable = claimableStones(s, p);
        String draw = engine.drawableDecks(s, p).isEmpty() ? "'end' your turn"
                : "'end' your turn to draw" + (s.nonEmptyDecks().size() > 1 ? " (e.g. 'end tactic')" : "");
        if (claimable.isEmpty()) {
            return "No Stone can be claimed right now. " + capitalize(draw) + ".";
        }
        StringBuilder sb = new StringBuilder("You may claim Stone");
        sb.append(claimable.size() > 1 ? "s " : " ");
        claimable.forEach(i -> sb.append(i + 1).append(' '));
        return sb.append("('claim <n>' or 'claim all'), then ").append(draw).append('.').toString();
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private List<Integer> claimableStones(GameState s, PlayerId p) {
        List<Integer> result = new ArrayList<>();
        for (StoneState stone : s.board().stones()) {
            if (!stone.isClaimed() && stone.isComplete(p) && engine.checkClaim(s, p, stone.index()).allowed()) {
                result.add(stone.index());
            }
        }
        return result;
    }

    private String pendingPrompt(GameState s) {
        return s.pending().map(p -> ((MultiStepEffect) engine.registry().get(p.effectId())).resolutionPrompt(p))
                .orElse("");
    }

    // ------------------------------------------------------------------ commands

    /**
     * Executes one command line (also used by tests).
     *
     * @param line the command
     */
    public void execute(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return;
        }
        String[] parts = trimmed.split("\\s+");
        String cmd = parts[0].toLowerCase(Locale.ROOT);
        String[] args = Arrays.copyOfRange(parts, 1, parts.length);
        try {
            switch (cmd) {
                case "help", "?" -> println(HELP.stripTrailing());
                case "rules" -> rules(args);
                case "new" -> newGame();
                case "board", "b" -> withGame(s -> renderer.board(s));
                case "hand", "h" -> hand();
                case "play", "p" -> play(args);
                case "resolve", "r" -> resolve();
                case "claim", "c" -> claim(args);
                case "end", "e", "draw", "d" -> end(args);
                case "pass" -> submit(new PassAction());
                case "next", "n" -> submit(new NextRoundAction());
                case "discard" -> withGame(renderer::discard);
                case "log" -> withGame(s -> renderer.log(s, args.length > 0 ? parseInt(args[0], 15) : 15));
                case "status" -> withGame(s -> renderer.header(s) + "\n" + renderer.status(s));
                case "legal" -> legal();
                case "card", "info" -> cardInfo(args);
                case "hint" -> hint(false);
                case "auto" -> hint(true);
                case "save" -> save(args);
                case "load" -> load(args);
                case "saves" -> listSaves();
                case "speed" -> speed(args);
                case "pause" -> pause(true);
                case "resume" -> pause(false);
                case "quit", "exit", "q" -> {
                    running = false;
                    println(shared ? "Command line closed; the window keeps running." : "Goodbye!");
                }
                default -> println(renderer.warn("Unknown command '" + cmd + "'.") + " Type 'help' for the list of commands.");
            }
        } catch (IllegalArgumentException e) {
            println(renderer.warn(e.getMessage()));
        }
    }

    private Optional<GameSession> session() {
        GameSession s = attached;
        if (s == null) {
            println("No game in progress. Type 'new' to start one or 'load' to resume one.");
        }
        return Optional.ofNullable(s);
    }

    private void withGame(Function<GameState, String> render) {
        session().ifPresent(s -> println(render.apply(s.snapshot())));
    }

    private void submit(Action action) {
        session().ifPresent(s -> report(s.submitAsHuman(action)));
    }

    private void report(ActionOutcome outcome) {
        if (!outcome.accepted()) {
            println(renderer.warn(outcome.message()));
        }
    }

    private static int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private Optional<String> ask(String prompt) {
        synchronized (outputLock) {
            out.print(prompt + " ");
            out.flush();
        }
        try {
            String line = in.readLine();
            if (line == null) {
                running = false;
                return Optional.empty();
            }
            return Optional.of(line.strip());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String askDefault(String prompt, String fallback) {
        String answer = ask(prompt + " [" + fallback + "]").orElse("");
        return answer.isEmpty() ? fallback : answer;
    }

    private boolean askYesNo(String prompt, boolean fallback) {
        while (running) {
            String a = askDefault(prompt, fallback ? "yes" : "no").toLowerCase(Locale.ROOT);
            if (a.startsWith("y")) {
                return true;
            }
            if (a.startsWith("n")) {
                return false;
            }
            println("Please answer yes or no.");
        }
        return fallback;
    }

    private int askNumber(String prompt, int min, int max, int fallback) {
        while (running) {
            String a = askDefault(prompt, String.valueOf(fallback));
            int n = parseInt(a, Integer.MIN_VALUE);
            if (n >= min && n <= max) {
                return n;
            }
            println("Please enter a number from " + min + " to " + max + ".");
        }
        return fallback;
    }

    private void rules(String[] args) {
        List<RulesDigest.Section> sections = rules.sections();
        if (args.length == 0) {
            StringBuilder sb = new StringBuilder("Rules sections (type 'rules <number>'):\n");
            for (int i = 0; i < sections.size(); i++) {
                sb.append(String.format("  %d) %s%n", i + 1, sections.get(i).title()));
            }
            println(sb.toString().stripTrailing());
            return;
        }
        int n = parseInt(args[0], -1);
        if (n < 1 || n > sections.size()) {
            throw new IllegalArgumentException("There is no rules section " + args[0] + ".");
        }
        RulesDigest.Section sec = sections.get(n - 1);
        println(renderer.bold(sec.title()) + "\n" + sec.body());
    }

    private void newGame() {
        println(renderer.bold("New game"));
        int mode = askNumber("Players: 1) Human vs Human  2) Human vs Computer  3) Computer vs Computer", 1, 3, 2);
        String n1 = askDefault("Name of Player 1 (bottom side)", mode == 3 ? "Fergus" : "Fergus");
        PlayerProfile p1 = mode == 3 ? PlayerProfile.virtual(n1, askDifficulty(n1)) : PlayerProfile.human(n1);
        String n2 = askDefault("Name of Player 2 (top side)", "Isla");
        PlayerProfile p2 = mode == 1 ? PlayerProfile.human(n2) : PlayerProfile.virtual(n2, askDifficulty(n2));
        boolean tactic = askYesNo("Play the Tactic variant?", false);
        boolean expansion = askYesNo("Include expansion cards?", false);
        int rounds = askNumber("Number of rounds (1 = single round, up to " + GameConfiguration.MAX_ROUNDS + ")",
                1, GameConfiguration.MAX_ROUNDS, 1);
        int first = askNumber("Who starts? 1) " + n1 + "  2) " + n2 + "  3) Random", 1, 3, 1);
        String seedText = askDefault("Random seed (any number, for a reproducible deal)", "random");
        if (!running) {
            return;
        }
        long seed = seedText.equalsIgnoreCase("random") ? System.nanoTime() : parseSeed(seedText);
        FirstPlayerRule rule = switch (first) {
            case 2 -> FirstPlayerRule.PLAYER_TWO;
            case 3 -> FirstPlayerRule.RANDOM;
            default -> FirstPlayerRule.PLAYER_ONE;
        };
        controller.startNewGame(new GameConfiguration(p1, p2, tactic, expansion, rounds, rule, seed));
    }

    private static long parseSeed(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            return text.hashCode();
        }
    }

    private Difficulty askDifficulty(String name) {
        int d = askNumber("Difficulty of " + name + ": 1) Easy  2) Hard", 1, 2, 1);
        return d == 2 ? Difficulty.HARD : Difficulty.EASY;
    }

    private Optional<PlayerId> humanToAct(GameState s) {
        Optional<PlayerId> p = engine.playerToAct(s);
        if (p.isPresent() && s.config().profile(p.get()).isVirtual()) {
            println("It is " + s.config().profile(p.get()).name() + "'s turn (computer). Please wait.");
            return Optional.empty();
        }
        if (p.isEmpty()) {
            println(s.phase() == TurnPhase.MATCH_OVER ? "The match is over. Type 'new' to play again."
                    : "The round is over. Type 'next' to start the next round.");
        }
        return p;
    }

    private void hand() {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        GameState s = session.get().snapshot();
        Optional<PlayerId> who = singleHuman(s);
        if (who.isEmpty()) {
            Optional<PlayerId> toAct = engine.playerToAct(s);
            if (toAct.isPresent() && !s.config().profile(toAct.get()).isVirtual()) {
                who = toAct;
            } else if (bothVirtual(s)) {
                println(renderer.hand(s, PlayerId.P1) + renderer.hand(s, PlayerId.P2));
                return;
            }
        }
        if (who.isEmpty()) {
            println("No human player is to act.");
            return;
        }
        println(renderer.hand(s, who.get()).stripTrailing());
    }

    private Card findCard(List<Card> cards, String ref) {
        int n = parseInt(ref, -1);
        if (n >= 1 && n <= cards.size()) {
            return cards.get(n - 1);
        }
        if (n != -1) {
            throw new IllegalArgumentException("There is no card number " + ref + " (you hold " + cards.size() + " cards).");
        }
        String key = normalize(ref);
        for (Card c : cards) {
            if (normalize(c.id()).equals(key) || normalize(c.shortLabel()).equals(key)
                    || normalize(c.displayName()).equals(key)) {
                return c;
            }
        }
        for (Card c : cards) {
            if (normalize(c.displayName()).startsWith(key) || normalize(c.displayName()).contains(key)) {
                return c;
            }
        }
        throw new IllegalArgumentException("No card matches '" + ref + "'.");
    }

    private static String normalize(String s) {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private void play(String[] args) {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        GameState s = session.get().snapshot();
        Optional<PlayerId> actor = humanToAct(s);
        if (actor.isEmpty()) {
            return;
        }
        if (s.phase() == TurnPhase.CLAIM) {
            println("You have already played this turn. " + claimHint(s, actor.get()));
            return;
        }
        if (s.phase() == TurnPhase.RESOLVE_EFFECT) {
            println("Finish the current effect first: " + pendingPrompt(s) + " Type 'resolve'.");
            return;
        }
        if (args.length == 0) {
            println("Which card? " + renderer.hand(s, actor.get()).stripTrailing() + "\nUsage: play <card> [stone]");
            return;
        }
        Card card = findCard(s.hand(actor.get()), args[0]);
        List<EffectParameters> options = engine.legalActions(s).stream()
                .filter(a -> a instanceof PlayCardAction pc && pc.cardId().equals(card.id()))
                .map(a -> ((PlayCardAction) a).parameters()).toList();
        if (options.isEmpty()) {
            ActionOutcome refused = session.get().submitAsHuman(new PlayCardAction(card.id(), EffectParameters.NONE));
            println(renderer.warn(refused.accepted() ? "" : refused.message()));
            return;
        }
        ParameterChooser chooser = new ParameterChooser(options);
        if (args.length > 1) {
            preselect(chooser, args[1]);
        }
        if (card.isSpecial() && !chooser.isResolved()) {
            println(renderer.label(card) + ": " + renderer.dim(renderer.category(card)));
        }
        Optional<EffectParameters> params = runWizard(chooser, s);
        params.ifPresent(p -> report(session.get().submitAsHuman(new PlayCardAction(card.id(), p))));
    }

    private void preselect(ParameterChooser chooser, String arg) {
        Optional<ParameterField> field = chooser.nextField();
        if (field.isEmpty()) {
            return;
        }
        int n = parseInt(arg, -1);
        Object value = switch (field.get()) {
            case STONE, DESTINATION -> n - 1;
            case DECK -> DeckType.parse(arg);
            default -> null;
        };
        if (value != null) {
            try {
                chooser.choose(field.get(), value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("You cannot play that card on Stone " + arg + ". "
                        + "Allowed: " + describeValues(chooser, field.get()));
            }
        }
    }

    private String describeValues(ParameterChooser chooser, ParameterField field) {
        GameSession s = attached;
        GameState state = s.snapshot();
        List<String> list = chooser.values(field).stream().map(v -> describer.value(field, v, state)).toList();
        return String.join(", ", list) + ".";
    }

    private Optional<EffectParameters> runWizard(ParameterChooser chooser, GameState s) {
        while (!chooser.isResolved()) {
            Optional<ParameterField> next = chooser.nextField();
            if (next.isEmpty()) {
                break;
            }
            ParameterField field = next.get();
            List<Object> values = chooser.values(field);
            StringBuilder sb = new StringBuilder(describer.fieldPrompt(field)).append(":\n");
            for (int i = 0; i < values.size(); i++) {
                sb.append(String.format("  %d) %s%n", i + 1, describer.value(field, values.get(i), s)));
            }
            println(sb.toString().stripTrailing());
            int choice = -1;
            while (choice < 1 || choice > values.size()) {
                Optional<String> answer = ask("Your choice (1-" + values.size() + ", or 'cancel'):");
                if (answer.isEmpty() || answer.get().equalsIgnoreCase("cancel")) {
                    println("Cancelled.");
                    return Optional.empty();
                }
                choice = parseInt(answer.get(), -1);
            }
            chooser.choose(field, values.get(choice - 1));
        }
        return Optional.of(chooser.result());
    }

    private void resolve() {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        GameState s = session.get().snapshot();
        if (humanToAct(s).isEmpty()) {
            return;
        }
        Optional<PendingEffect> pending = s.pending();
        if (s.phase() != TurnPhase.RESOLVE_EFFECT || pending.isEmpty()) {
            println("There is no effect waiting to be resolved.");
            return;
        }
        println(pendingPrompt(s));
        if (!pending.get().cardIds().isEmpty()) {
            List<String> names = pending.get().cardIds().stream().map(describer::cardName).toList();
            println("Cards involved: " + String.join(", ", names));
        }
        List<EffectParameters> options = engine.legalActions(s).stream()
                .filter(a -> a instanceof ResolveEffectAction).map(a -> ((ResolveEffectAction) a).parameters()).toList();
        if (options.isEmpty()) {
            println(renderer.warn("No resolution is possible."));
            return;
        }
        runWizard(new ParameterChooser(options), s)
                .ifPresent(p -> report(session.get().submitAsHuman(new ResolveEffectAction(p))));
    }

    private void claim(String[] args) {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        GameState s = session.get().snapshot();
        Optional<PlayerId> actor = humanToAct(s);
        if (actor.isEmpty()) {
            return;
        }
        PlayerId p = actor.get();
        if (args.length == 0) {
            StringBuilder sb = new StringBuilder();
            for (StoneState stone : s.board().stones()) {
                if (!stone.isClaimed() && stone.isComplete(p)) {
                    ClaimCheck check = engine.checkClaim(s, p, stone.index());
                    sb.append("  Stone ").append(stone.index() + 1).append(": ")
                            .append(check.allowed() ? renderer.good("can be claimed") : "not yet").append(" - ")
                            .append(check.reason()).append('\n');
                }
            }
            println(sb.length() == 0 ? "You have no complete formation on an unclaimed Stone."
                    : "Your complete formations:\n" + sb.toString().stripTrailing());
            return;
        }
        if (args[0].equalsIgnoreCase("all")) {
            List<Integer> stones = claimableStones(s, p);
            if (stones.isEmpty()) {
                println("No Stone can be claimed right now.");
            }
            for (int i : stones) {
                report(session.get().submitAsHuman(new ClaimAction(i)));
            }
            return;
        }
        int stone = parseInt(args[0], 0) - 1;
        if (stone < 0 || stone > 8) {
            throw new IllegalArgumentException("Choose a Stone between 1 and 9.");
        }
        Map<String, EliteIdentity> identities = new LinkedHashMap<>();
        if (args.length > 1) {
            if (!args[1].equalsIgnoreCase("as")) {
                throw new IllegalArgumentException("Usage: claim <stone> [as <card>=<colour><value> ...]");
            }
            List<Card> mine = s.board().stone(stone).side(p).cards();
            for (int i = 2; i < args.length; i++) {
                String[] kv = args[i].split("=");
                if (kv.length != 2 || kv[1].length() < 2) {
                    throw new IllegalArgumentException("Write identities like joker=R9 (colour letter and value).");
                }
                Card elite = findCard(mine, kv[0]);
                ClanColor color = ClanColor.parse(kv[1].substring(0, 1));
                int value = parseInt(kv[1].substring(1), -1);
                identities.put(elite.id(), new EliteIdentity(color, value));
            }
        }
        report(session.get().submitAsHuman(new ClaimAction(stone, identities)));
    }

    private void end(String[] args) {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        GameState s = session.get().snapshot();
        Optional<PlayerId> actor = humanToAct(s);
        if (actor.isEmpty()) {
            return;
        }
        if (s.phase() != TurnPhase.CLAIM) {
            report(session.get().submitAsHuman(new EndTurnAction(null)));
            return;
        }
        List<DeckType> drawable = engine.drawableDecks(s, actor.get());
        DeckType deck = null;
        if (args.length > 0) {
            deck = DeckType.parse(args[0]);
        } else if (drawable.size() == 1) {
            deck = drawable.get(0);
        } else if (drawable.size() > 1) {
            println("Draw from: " + renderer.deckChoice(s, drawable));
            for (int i = 0; i < drawable.size(); i++) {
                println(String.format("  %d) %s", i + 1, drawable.get(i).displayName()));
            }
            int choice = askNumber("Deck", 1, drawable.size(), 1);
            deck = drawable.get(choice - 1);
        }
        report(session.get().submitAsHuman(new EndTurnAction(deck)));
    }

    private void legal() {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        GameState s = session.get().snapshot();
        List<Action> actions = engine.legalActions(s);
        StringBuilder sb = new StringBuilder("Legal actions (" + actions.size() + "):\n");
        actions.stream().limit(40).forEach(a -> sb.append("  - ").append(describer.action(a, s)).append('\n'));
        if (actions.size() > 40) {
            sb.append("  ... and ").append(actions.size() - 40).append(" more\n");
        }
        println(sb.toString().stripTrailing());
    }

    private void cardInfo(String[] args) {
        Optional<GameSession> session = session();
        if (session.isEmpty()) {
            return;
        }
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: card <number in hand | name>");
        }
        GameState s = session.get().snapshot();
        PlayerId p = singleHuman(s).or(() -> engine.playerToAct(s)).orElse(PlayerId.P1);
        List<Card> pool = new ArrayList<>(s.hand(p));
        Card card;
        try {
            card = findCard(pool, args[0]);
        } catch (IllegalArgumentException e) {
            card = findCard(new ArrayList<>(engine.catalog().all()), args[0]);
        }
        println(renderer.label(card) + " - " + renderer.category(card));
    }

    private void hint(boolean play) {
        if (session().isEmpty()) {
            return;
        }
        Optional<Action> hint = controller.hint();
        if (hint.isEmpty()) {
            println("No hint available: it is not a human player's turn.");
            return;
        }
        GameState s = attached.snapshot();
        println((play ? "Playing: " : "Suggestion: ") + describer.action(hint.get(), s) + ".");
        if (play) {
            submit(hint.get());
        }
    }

    private Path savePath(String name) {
        Path dir = SaveManager.defaultDirectory();
        String file = name.endsWith(SaveManager.EXTENSION) ? name : name + SaveManager.EXTENSION;
        Path p = Path.of(file);
        return p.isAbsolute() || file.contains("/") ? p : dir.resolve(file);
    }

    private void save(String[] args) {
        if (session().isEmpty()) {
            return;
        }
        String name = args.length > 0 ? String.join("-", args) : "quicksave";
        Path path = savePath(name);
        try {
            controller.save(path);
            println(renderer.good("Game saved to " + path));
        } catch (IOException e) {
            println(renderer.warn("The game could not be saved: " + e.getMessage()));
        }
    }

    private void listSaves() {
        List<Path> saves = SaveManager.listSaves(SaveManager.defaultDirectory());
        if (saves.isEmpty()) {
            println("No saved game in " + SaveManager.defaultDirectory());
            return;
        }
        StringBuilder sb = new StringBuilder("Saved games:\n");
        for (int i = 0; i < saves.size(); i++) {
            sb.append(String.format("  %d) %s%n", i + 1, saves.get(i).getFileName()));
        }
        println(sb.toString().stripTrailing());
    }

    private void load(String[] args) {
        Path path;
        List<Path> saves = SaveManager.listSaves(SaveManager.defaultDirectory());
        if (args.length == 0) {
            if (saves.isEmpty()) {
                println("No saved game in " + SaveManager.defaultDirectory());
                return;
            }
            listSaves();
            int n = askNumber("Load which game?", 1, saves.size(), 1);
            path = saves.get(n - 1);
        } else {
            int n = parseInt(args[0], -1);
            path = n >= 1 && n <= saves.size() ? saves.get(n - 1) : savePath(String.join("-", args));
        }
        try {
            controller.load(path);
            println(renderer.good("Game loaded from " + path.getFileName()));
        } catch (SaveFormatException e) {
            println(renderer.warn(e.getMessage()));
        }
    }

    private void speed(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Usage: speed slow|normal|fast|instant");
        }
        long delay = switch (args[0].toLowerCase(Locale.ROOT)) {
            case "slow" -> 1400;
            case "normal" -> 750;
            case "fast" -> 300;
            case "instant" -> 40;
            default -> {
                int ms = parseInt(args[0], -1);
                if (ms < 0) {
                    throw new IllegalArgumentException("Usage: speed slow|normal|fast|instant (or milliseconds)");
                }
                yield ms;
            }
        };
        controller.setAiDelay(delay);
        println("Computer players now wait " + delay + " ms before each action.");
    }

    private void pause(boolean pause) {
        if (controller.hub().driver().isEmpty()) {
            println("No game in progress.");
            return;
        }
        controller.hub().driver().get().setPaused(pause);
        println(pause ? "Computer players paused. Type 'resume' to continue." : "Computer players resumed.");
    }
}
