package com.schottentotten.cli;

import com.schottentotten.TestSupport;
import com.schottentotten.controller.GameController;
import com.schottentotten.controller.GameSession;
import com.schottentotten.controller.RulesDigest;
import com.schottentotten.controller.SessionHub;
import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.GameState;
import com.schottentotten.model.TurnPhase;
import com.schottentotten.persistence.SaveManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.schottentotten.TestSupport.ENGINE;
import static com.schottentotten.TestSupport.card;
import static com.schottentotten.TestSupport.clan;
import static com.schottentotten.TestSupport.hand;
import static com.schottentotten.TestSupport.place;
import static com.schottentotten.model.PlayerId.P1;
import static com.schottentotten.model.PlayerId.P2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineViewTest {

    private static final RulesDigest RULES = new RulesDigest(ENGINE.catalog());

    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private final GameController controller = new GameController(ENGINE, new SessionHub(),
            new SaveManager(ENGINE.catalog()), () -> 0L);

    @AfterEach
    void tearDown() {
        controller.hub().shutdown();
        System.clearProperty("schotten.saves");
    }

    private CommandLineView cli(String input) {
        return cli(input, bytes);
    }

    private CommandLineView cli(String input, ByteArrayOutputStream sink) {
        CommandLineView view = new CommandLineView(controller, RULES, new StringReader(input),
                new PrintStream(sink, true, StandardCharsets.UTF_8), false, false);
        view.connect();
        return view;
    }

    private String output() {
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private GameSession session() {
        return controller.hub().current().orElseThrow();
    }

    private void activate(GameState state) {
        controller.hub().activate(new GameSession(ENGINE, state), 0);
    }

    @Test
    void humanVsHumanGameIsPlayableToTheEndThroughCommands() {
        CommandLineView view = cli("1\nAlice\nBob\nno\nno\n1\n1\n7\n");
        view.execute("new");
        GameState s = session().snapshot();
        assertEquals("Alice", s.config().player1().name());
        assertFalse(s.config().tacticVariant());
        view.execute("hand");
        view.execute("board");
        assertTrue(output().contains("Alice's hand (6 cards)"));
        for (int i = 0; i < 2000 && session().snapshot().phase() != TurnPhase.MATCH_OVER; i++) {
            view.execute("auto");
        }
        assertEquals(TurnPhase.MATCH_OVER, session().snapshot().phase());
        assertTrue(output().contains("The match is over"), output());
    }

    @Test
    void tacticGameAsksForTheDeckAndPlaysByLabel() {
        CommandLineView view = cli("1\nAlice\nBob\nyes\nyes\n1\n1\n11\n");
        view.execute("new");
        GameState s = session().snapshot();
        assertTrue(s.config().tacticVariant());
        assertTrue(s.config().expansions());
        assertEquals(7, s.handSize(P1));
        Card clanCard = s.hand(P1).stream().filter(c -> c instanceof ClanCard).findFirst().orElseThrow();
        view.execute("play " + clanCard.shortLabel() + " 1");
        assertTrue(session().snapshot().board().stone(0).side(P1).contains(clanCard.id()), output());
        view.execute("claim");
        view.execute("end tactic");
        GameState after = session().snapshot();
        assertEquals(P2, after.currentPlayer());
        assertEquals(7, after.handSize(P1));
        assertTrue(output().contains("draws from the Tactic deck"), output());
    }

    @Test
    void claimWithEliteIdentityAndRefusals() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P1, 0, card("T-JOKER-1"), clan("R8"), clan("R7"));
        place(s, P2, 0, clan("B1"), clan("Y2"), clan("G4"));
        s.setPhase(TurnPhase.CLAIM);
        activate(s);
        CommandLineView view = cli("");
        view.execute("claim 1 as R8=G5");
        assertTrue(output().contains("not one of your Elite Troops"), output());
        view.execute("claim 3");
        assertFalse(session().snapshot().board().stone(2).isClaimed());
        view.execute("claim 1 as joker=R9");
        assertEquals(P1, session().snapshot().board().stone(0).owner().orElseThrow());
        assertTrue(output().contains("R9") || output().contains("Red 9"), output());
    }

    @Test
    void wizardAsksForTraitorDestination() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P2, 1, clan("R5"));
        hand(s, P1, card("T-TRAITOR"), clan("G1"), clan("G2"), clan("G3"), clan("G4"), clan("G5"), clan("G6"));
        activate(s);
        CommandLineView view = cli("1\n");
        view.execute("play traitor");
        GameState after = session().snapshot();
        assertTrue(output().contains("Choose where it goes"), output());
        assertFalse(after.board().stone(1).side(P2).contains("RED-5"));
        assertTrue(after.board().stone(0).side(P1).contains("RED-5"), output());
    }

    @Test
    void wizardCanBeCancelled() {
        GameState s = TestSupport.newGame(true, false);
        place(s, P2, 1, clan("R5"));
        hand(s, P1, card("T-TRAITOR"), clan("G1"), clan("G2"), clan("G3"), clan("G4"), clan("G5"), clan("G6"));
        activate(s);
        long version = session().version();
        cli("cancel\n").execute("play traitor");
        assertEquals(version, session().version());
        assertTrue(output().contains("Cancelled"));
    }

    @Test
    void mistakesGetFriendlyMessages() {
        activate(TestSupport.newGame(false, false));
        CommandLineView view = cli("");
        view.execute("play 99");
        view.execute("frobnicate");
        view.execute("end");
        view.execute("claim 12");
        view.execute("next");
        String out = output();
        assertTrue(out.contains("There is no card number 99"), out);
        assertTrue(out.contains("Unknown command 'frobnicate'"), out);
        assertTrue(out.contains("You cannot end your turn now"), out);
        assertTrue(out.contains("between 1 and 9"), out);
        assertEquals(TurnPhase.PLAY, session().snapshot().phase());
    }

    @Test
    void saveAndLoadRoundTrip(@TempDir Path dir) {
        System.setProperty("schotten.saves", dir.toString());
        activate(TestSupport.newGame(false, false));
        CommandLineView view = cli("");
        view.execute("save my game");
        assertTrue(Files.exists(dir.resolve("my-game" + SaveManager.EXTENSION)), output());
        String saved = TestSupport.json(session().snapshot());
        view.execute("auto");
        view.execute("saves");
        view.execute("load 1");
        assertEquals(saved, TestSupport.json(session().snapshot()));
        view.execute("load missing");
        assertTrue(output().contains("Game loaded"), output());
    }

    @Test
    void twoViewsShareTheSameLiveGame() {
        activate(TestSupport.newGame(false, false));
        ByteArrayOutputStream other = new ByteArrayOutputStream();
        CommandLineView first = cli("");
        cli("", other);
        first.execute("play 1 4");
        String seen = other.toString(StandardCharsets.UTF_8);
        assertTrue(seen.contains("Alice plays"), seen);
        assertTrue(seen.contains("on Stone 4"), seen);
    }

    @Test
    void rulesAreBrowsable() {
        CommandLineView view = cli("");
        view.execute("rules");
        view.execute("rules 1");
        view.execute("rules 99");
        view.execute("help");
        String out = output();
        assertTrue(out.contains("Rules sections"));
        assertTrue(out.contains("three adjacent"));
        assertTrue(out.contains("There is no rules section 99"));
        assertTrue(out.contains("Tactic cards"));
    }

    @Test
    void informationCommandsWorkBeforeAndDuringAGame(@TempDir Path dir) {
        System.setProperty("schotten.saves", dir.toString());
        CommandLineView view = cli("");
        view.execute("board");
        view.execute("pause");
        view.execute("saves");
        view.execute("frobnicate");
        view.execute("");
        activate(TestSupport.newGame(true, true));
        view.execute("status");
        view.execute("legal");
        view.execute("card 1");
        view.execute("card Joker");
        view.execute("card");
        view.execute("discard");
        view.execute("log 3");
        view.execute("hint");
        view.execute("speed fast");
        view.execute("speed 120");
        view.execute("speed warp");
        view.execute("pause");
        view.execute("resume");
        view.execute("next");
        String out = output();
        assertTrue(out.contains("No game in progress"), out);
        assertTrue(out.contains("No saved game in"), out);
        assertTrue(out.contains("Unknown command 'frobnicate'"), out);
        assertTrue(out.contains("Legal actions ("), out);
        assertTrue(out.contains("Joker"), out);
        assertTrue(out.contains("Usage: card"), out);
        assertTrue(out.contains("Usage: speed"), out);
        assertTrue(out.contains("Computer players paused"), out);
        assertTrue(out.contains("Computer players resumed"), out);
        assertTrue(out.contains("Suggestion: "), out);
        assertTrue(out.contains("The current round is not finished"), out);
    }

    @Test
    void runReadsCommandsUntilQuit() {
        CommandLineView view = cli("help\nstatus\nquit\nboard\n");
        view.run();
        String out = output();
        assertTrue(out.contains("play"), out);
        assertFalse(out.contains("Stone 9 |"), out);
    }

    @Test
    void computerVsComputerRunsUnattended() throws InterruptedException {
        CommandLineView view = cli("3\nAnna\n1\nBert\n2\nno\nno\n1\n1\n5\n");
        view.execute("new");
        for (int i = 0; i < 400 && session().snapshot().phase() != TurnPhase.MATCH_OVER; i++) {
            Thread.sleep(50);
        }
        assertEquals(TurnPhase.MATCH_OVER, session().snapshot().phase());
        assertTrue(output().contains("The match is over"), output());
    }
}
