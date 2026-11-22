package com.schottentotten.view;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ExpansionCard;
import com.schottentotten.model.TacticCard;
import javafx.scene.image.Image;
import javafx.scene.text.Font;

import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads and caches the bundled artwork and fonts. Every asset is packaged in the application,
 * so the game works fully offline.
 */
public final class Assets {

    /** Title font family (Celtic uncial). */
    public static final String TITLE_FONT = "Uncial Antiqua";
    /** Heading and numeral font family. */
    public static final String HEADING_FONT = "Cinzel Decorative";
    /** Body text font family. */
    public static final String BODY_FONT = "Alegreya Sans";

    private static final String ROOT = "/com/schottentotten/view/";
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private Assets() {
    }

    /**
     * Loads the bundled fonts (call once before building the scene).
     */
    public static void loadFonts() {
        for (String name : new String[]{"UncialAntiqua-Regular", "CinzelDecorative-Regular", "CinzelDecorative-Bold",
                "AlegreyaSans-Regular", "AlegreyaSans-Medium", "AlegreyaSans-Bold"}) {
            try (InputStream in = Assets.class.getResourceAsStream(ROOT + "fonts/" + name + ".ttf")) {
                if (in != null) {
                    Font.loadFont(in, 14);
                }
            } catch (Exception e) {
                System.err.println("Font not loaded: " + name + " (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Returns the URL of a bundled resource.
     *
     * @param path path relative to the view resource folder
     * @return URL
     * @throws IllegalStateException if the resource is missing
     */
    public static URL url(String path) {
        URL url = Assets.class.getResource(ROOT + path);
        if (url == null) {
            throw new IllegalStateException("Missing resource " + path);
        }
        return url;
    }

    /**
     * Returns the stylesheet URL.
     *
     * @return stylesheet location
     */
    public static String stylesheet() {
        return url("style.css").toExternalForm();
    }

    /**
     * Returns a cached image.
     *
     * @param path path relative to the images folder
     * @return image
     */
    public static Image image(String path) {
        return CACHE.computeIfAbsent(path, p -> new Image(url("images/" + p).toExternalForm(), false));
    }

    /**
     * Returns the illustration of a card.
     *
     * @param card the card
     * @return image
     */
    public static Image cardArt(Card card) {
        String name = switch (card) {
            case ClanCard c -> "clan-" + c.color().name().toLowerCase(Locale.ROOT);
            case TacticCard t -> "tactic-" + t.type().name().toLowerCase(Locale.ROOT).replace("_", "-");
            case ExpansionCard x -> "expansion-" + x.definition().key().toLowerCase(Locale.ROOT);
        };
        URL url = Assets.class.getResource(ROOT + "images/cards/" + name + ".jpg");
        return url == null ? cardBack() : image("cards/" + name + ".jpg");
    }

    /**
     * Returns the card back.
     *
     * @return image
     */
    public static Image cardBack() {
        return image("cards/card-back.jpg");
    }
}
