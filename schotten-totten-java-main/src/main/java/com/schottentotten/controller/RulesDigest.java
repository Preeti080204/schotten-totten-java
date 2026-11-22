package com.schottentotten.controller;

import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.ExpansionCard;
import com.schottentotten.model.TacticType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Player-facing summary of the rules, loaded from a bundled text resource and completed with
 * the rules text of every Tactic and expansion card, so card descriptions are never
 * duplicated by hand.
 */
public final class RulesDigest {

    /**
     * One section of the rules.
     *
     * @param title section title
     * @param body  section text
     */
    public record Section(String title, String body) {
    }

    private static final String RESOURCE = "/com/schottentotten/text/rules.txt";

    private final List<Section> sections;

    /**
     * Loads the digest.
     *
     * @param catalog catalogue providing the special cards
     */
    public RulesDigest(CardCatalog catalog) {
        List<Section> list = new ArrayList<>(parse(readResource()));
        StringBuilder tactic = new StringBuilder();
        for (TacticType type : TacticType.values()) {
            tactic.append(type.displayName()).append(" (").append(type.category().displayName())
                    .append(type.copies() > 1 ? ", " + type.copies() + " copies" : "").append(")\n  ")
                    .append(type.rulesText()).append("\n");
        }
        list.add(new Section("Tactic cards", tactic.toString().strip()));
        StringBuilder expansion = new StringBuilder();
        Map<String, ExpansionCard> unique = new LinkedHashMap<>();
        catalog.all().stream().filter(c -> c instanceof ExpansionCard).map(c -> (ExpansionCard) c)
                .forEach(c -> unique.putIfAbsent(c.definition().key(), c));
        unique.values().forEach(c -> expansion.append(c.displayName()).append(" (")
                .append(c.definition().category()).append(")\n  ").append(c.definition().rulesText()).append("\n"));
        if (!unique.isEmpty()) {
            list.add(new Section("Expansion cards", expansion.toString().strip()));
        }
        this.sections = List.copyOf(list);
    }

    private static String readResource() {
        try (InputStream in = RulesDigest.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<Section> parse(String text) {
        List<Section> result = new ArrayList<>();
        String title = null;
        StringBuilder body = new StringBuilder();
        for (String line : text.split("\n", -1)) {
            if (line.startsWith("## ")) {
                if (title != null) {
                    result.add(new Section(title, body.toString().strip()));
                }
                title = line.substring(3).strip();
                body.setLength(0);
            } else {
                body.append(line).append('\n');
            }
        }
        if (title != null) {
            result.add(new Section(title, body.toString().strip()));
        }
        return result;
    }

    /**
     * Returns all sections in reading order.
     *
     * @return sections
     */
    public List<Section> sections() {
        return sections;
    }
}
