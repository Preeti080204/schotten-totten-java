package com.schottentotten.view;

import com.schottentotten.model.ClanColor;
import com.schottentotten.model.PlayerId;
import javafx.scene.paint.Color;

/**
 * Colour design system of the interface, taken from the rulebook's palette: emerald grass,
 * turquoise Celtic carvings, burgundy ribbons, gold accents, cream parchment and earthy browns.
 */
public final class Palette {

    /** Cream parchment. */
    public static final Color PARCHMENT = Color.web("#FBF3DC");
    /** Darker parchment for borders. */
    public static final Color PARCHMENT_DARK = Color.web("#E7D6AE");
    /** Burgundy ribbon. */
    public static final Color BURGUNDY = Color.web("#8E1B2A");
    /** Turquoise carving. */
    public static final Color TURQUOISE = Color.web("#1BA9A0");
    /** Gold accent. */
    public static final Color GOLD = Color.web("#D9A520");
    /** Emerald green. */
    public static final Color EMERALD = Color.web("#2F8F3A");
    /** Ink brown for text. */
    public static final Color INK = Color.web("#3B2614");
    /** Clan colour of Player 1 (burgundy banner). */
    public static final Color PLAYER_ONE = Color.web("#A3212F");
    /** Clan colour of Player 2 (royal blue banner). */
    public static final Color PLAYER_TWO = Color.web("#1F5AA6");

    private Palette() {
    }

    /**
     * Returns the colour of a clan.
     *
     * @param color clan colour
     * @return display colour
     */
    public static Color clan(ClanColor color) {
        return switch (color) {
            case RED -> Color.web("#D32F3A");
            case ORANGE -> Color.web("#EE7A1C");
            case YELLOW -> Color.web("#E9B714");
            case GREEN -> Color.web("#2E9E44");
            case BLUE -> Color.web("#2168C9");
            case PURPLE -> Color.web("#8546A8");
        };
    }

    /**
     * Returns the banner colour of a player.
     *
     * @param player the player
     * @return colour
     */
    public static Color player(PlayerId player) {
        return player == PlayerId.P1 ? PLAYER_ONE : PLAYER_TWO;
    }

    /**
     * Returns a CSS hex string for a colour.
     *
     * @param c colour
     * @return {@code #RRGGBB}
     */
    public static String hex(Color c) {
        return String.format("#%02X%02X%02X", (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }
}
