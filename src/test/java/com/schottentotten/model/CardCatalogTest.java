package com.schottentotten.model;

import com.schottentotten.TestSupport;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardCatalogTest {

    @Test
    void thereAreExactly54UniqueClanCards() {
        List<ClanCard> cards = CardCatalog.clanCards();
        assertEquals(54, cards.size());
        assertEquals(54, new HashSet<>(cards).size());
    }

    @Test
    void everyColourStrengthCombinationExistsOnce() {
        Set<String> seen = new HashSet<>();
        for (ClanCard c : CardCatalog.clanCards()) {
            assertTrue(seen.add(c.color() + "/" + c.strength()));
        }
        for (ClanColor color : ClanColor.values()) {
            for (int s = 1; s <= 9; s++) {
                assertTrue(seen.contains(color + "/" + s));
            }
        }
        assertEquals(6, ClanColor.values().length);
    }

    @Test
    void tacticDeckHasTheTenOfficialCards() {
        List<TacticCard> tactics = CardCatalog.tacticCards();
        assertEquals(10, tactics.size());
        assertEquals(2, tactics.stream().filter(TacticCard::isJoker).count());
        for (TacticType t : TacticType.values()) {
            assertEquals(t.copies(), tactics.stream().filter(c -> c.type() == t).count(), t.name());
        }
    }

    @Test
    void identifiersAreUniqueAndResolvable() {
        CardCatalog catalog = TestSupport.ENGINE.catalog();
        assertEquals(54 + 10 + 6, catalog.all().size());
        for (Card c : catalog.all()) {
            assertEquals(c, catalog.get(c.id()));
        }
        assertThrows(IllegalArgumentException.class, () -> catalog.get("NOPE"));
    }

    @Test
    void clanCardRejectsInvalidStrength() {
        assertThrows(IllegalArgumentException.class, () -> new ClanCard(ClanColor.RED, 0));
        assertThrows(IllegalArgumentException.class, () -> new ClanCard(ClanColor.RED, 10));
    }

    @Test
    void eliteTroopsAreFormationCardsWithWildColour() {
        TacticCard spy = (TacticCard) TestSupport.card("T-SPY");
        assertTrue(spy.isFormationCard());
        assertTrue(spy.hasWildColor());
        assertEquals(Set.of(7), spy.allowedStrengths());
        assertEquals(Set.of(1, 2, 3), TestSupport.card("T-SHIELD_BEARER").allowedStrengths());
        assertEquals(9, TestSupport.card("T-JOKER-1").allowedStrengths().size());
    }
}
