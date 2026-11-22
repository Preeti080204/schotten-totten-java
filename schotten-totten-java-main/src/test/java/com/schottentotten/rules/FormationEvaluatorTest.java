package com.schottentotten.rules;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanColor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.schottentotten.TestSupport.card;
import static com.schottentotten.TestSupport.clan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormationEvaluatorTest {

    private final FormationEvaluator evaluator = new FormationEvaluator();

    private FormationValue eval(String... codes) {
        return evaluator.evaluate(Arrays.stream(codes).map(c -> (Card) clan(c)).toList(), false);
    }

    @ParameterizedTest
    @CsvSource({
            "R4 R5 R6, COLOR_RUN, 15",
            "R6 R4 R5, COLOR_RUN, 15",
            "G8 B8 P8, SAME_STRENGTH, 24",
            "Y1 Y5 Y9, COLOR, 15",
            "R3 G4 B5, RUN, 12",
            "B5 R3 G4, RUN, 12",
            "R9 G1 B2, SUM, 12",
            "G7 P4 B3, SUM, 14",
            "R1 R2 G9, SUM, 12"
    })
    void classifiesEveryCategory(String cards, FormationRank rank, int sum) {
        FormationValue v = eval(cards.split(" "));
        assertEquals(rank, v.rank());
        assertEquals(sum, v.sum());
    }

    @Test
    void runsDoNotWrapAround() {
        assertEquals(FormationRank.SUM, eval("R8", "G9", "B1").rank());
        assertEquals(FormationRank.COLOR, eval("R8", "R9", "R1").rank());
    }

    @Test
    void rankingOrderIsOfficial() {
        FormationValue colorRun = eval("R1", "R2", "R3");
        FormationValue kind = eval("R9", "G9", "B9");
        FormationValue color = eval("R9", "R7", "R5");
        FormationValue run = eval("R9", "G8", "B7");
        FormationValue sum = eval("R9", "G9", "B8");
        assertTrue(colorRun.beats(kind));
        assertTrue(kind.beats(color));
        assertTrue(color.beats(run));
        assertTrue(run.beats(sum));
    }

    @Test
    void sameCategoryComparesSumAndEqualSumsTie() {
        assertTrue(eval("R7", "R8", "R9").beats(eval("G6", "G7", "G8")));
        assertEquals(0, eval("R7", "R8", "R9").compareTo(eval("G7", "G8", "G9")));
    }

    @Test
    void rulebookExampleThreeOfAKindBeatsSum() {
        FormationValue mine = eval("G5", "R5", "B5");
        FormationValue theirs = eval("G7", "P4", "B3");
        assertEquals(FormationRank.SAME_STRENGTH, mine.rank());
        assertTrue(mine.beats(theirs));
    }

    @Test
    void mudFightUsesFourCards() {
        assertEquals(FormationRank.COLOR_RUN, eval("B2", "B3", "B4", "B5").rank());
        assertEquals(FormationRank.SAME_STRENGTH, eval("B2", "R2", "G2", "Y2").rank());
        assertEquals(FormationRank.COLOR, eval("B2", "B9", "B4", "B5").rank());
        assertEquals(FormationRank.RUN, eval("B2", "R3", "G4", "Y5").rank());
        assertEquals(FormationRank.SUM, eval("B2", "R2", "G2", "Y3").rank());
        assertEquals("Four of a Kind", FormationRank.SAME_STRENGTH.displayName(4));
    }

    @Test
    void sumOnlyIgnoresCategories() {
        List<Card> run = List.of(clan("R1"), clan("R2"), clan("R3"));
        FormationValue v = evaluator.evaluate(run, true);
        assertEquals(FormationRank.SUM, v.rank());
        assertEquals(6, v.sum());
        assertTrue(evaluator.evaluate(List.of(clan("G9"), clan("B9"), clan("P1")), true).beats(v));
    }

    @Test
    void jokerTakesTheBestIdentity() {
        FormationValue v = evaluator.evaluate(List.of(clan("R7"), clan("R8"), card("T-JOKER-1")), false);
        assertEquals(FormationRank.COLOR_RUN, v.rank());
        assertEquals(24, v.sum());
        assertEquals(new EliteIdentity(ClanColor.RED, 9), v.identities().get("T-JOKER-1"));
    }

    @Test
    void spyIsAlwaysSeven() {
        FormationValue v = evaluator.evaluate(List.of(clan("G5"), clan("G6"), card("T-SPY")), false);
        assertEquals(FormationRank.COLOR_RUN, v.rank());
        assertEquals(18, v.sum());
        FormationValue kind = evaluator.evaluate(List.of(clan("G7"), clan("B7"), card("T-SPY")), false);
        assertEquals(FormationRank.SAME_STRENGTH, kind.rank());
    }

    @Test
    void shieldBearerIsOneToThree() {
        FormationValue v = evaluator.evaluate(List.of(clan("Y1"), clan("Y2"), card("T-SHIELD_BEARER")), false);
        assertEquals(FormationRank.COLOR_RUN, v.rank());
        assertEquals(6, v.sum());
        FormationValue high = evaluator.evaluate(List.of(clan("Y8"), clan("Y9"), card("T-SHIELD_BEARER")), false);
        assertEquals(FormationRank.COLOR, high.rank());
        assertEquals(20, high.sum());
    }

    @Test
    void severalWildCardsAreOptimisedTogether() {
        FormationValue v = evaluator.evaluate(List.of(card("T-JOKER-1"), card("T-SPY"), card("T-SHIELD_BEARER")),
                false);
        assertEquals(FormationRank.COLOR, v.rank());
        assertEquals(9 + 7 + 3, v.sum());
        FormationValue two = evaluator.evaluate(List.of(clan("B6"), card("T-JOKER-1"), card("T-SPY")), false);
        assertEquals(FormationRank.COLOR_RUN, two.rank());
        assertEquals(6 + 7 + 8, two.sum());
    }

    @Test
    void overridesAreRespectedAndValidated() {
        List<Card> cards = List.of(clan("R7"), clan("R8"), card("T-JOKER-1"));
        FormationValue forced = evaluator.evaluate(cards, false,
                Map.of("T-JOKER-1", new EliteIdentity(ClanColor.BLUE, 1)));
        assertEquals(FormationRank.SUM, forced.rank());
        assertEquals(16, forced.sum());
        assertThrows(IllegalArgumentException.class, () -> evaluator.evaluate(List.of(clan("R7"), clan("R8"),
                card("T-SPY")), false, Map.of("T-SPY", new EliteIdentity(ClanColor.RED, 9))));
    }
}
