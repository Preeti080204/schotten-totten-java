package com.schottentotten.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;

/**
 * Small factory helpers applying the design system to nodes (textured backgrounds that CSS
 * cannot clip to rounded corners, and pre-styled labels and buttons).
 */
public final class Styles {

    private Styles() {
    }

    /**
     * Gives a region the parchment panel look (gold-trimmed rounded parchment).
     *
     * @param region region
     * @param <T>    region type
     * @return the region
     */
    public static <T extends Region> T parchment(T region) {
        region.getStyleClass().add("parchment-panel");
        ImagePattern paper = new ImagePattern(Assets.image("parchment.jpg"), 0, 0, 640, 640, false);
        region.setBackground(new Background(
                new BackgroundFill(Color.web("#6B4A2B"), new CornerRadii(16), Insets.EMPTY),
                new BackgroundFill(Palette.GOLD, new CornerRadii(14), new Insets(3)),
                new BackgroundFill(paper, new CornerRadii(12), new Insets(5))));
        return region;
    }

    /**
     * Gives a region the dark oak texture.
     *
     * @param region region
     * @param <T>    region type
     * @return the region
     */
    public static <T extends Region> T wood(T region) {
        region.getStyleClass().add("wood-panel");
        ImagePattern wood = new ImagePattern(Assets.image("wood.jpg"), 0, 0, 300, 300, false);
        region.setBackground(new Background(new BackgroundFill(wood, CornerRadii.EMPTY, Insets.EMPTY)));
        return region;
    }

    /**
     * Creates a carved wooden plaque button.
     *
     * @param text    label
     * @param classes extra style classes (e.g. {@code plaque-primary})
     * @return button
     */
    public static Button plaque(String text, String... classes) {
        Button b = new Button(text);
        b.getStyleClass().setAll("button", "plaque");
        b.getStyleClass().addAll(classes);
        b.setMnemonicParsing(false);
        return b;
    }

    /**
     * Creates a label with style classes.
     *
     * @param text    text
     * @param classes style classes
     * @return label
     */
    public static Label label(String text, String... classes) {
        Label l = new Label(text);
        l.getStyleClass().addAll(classes);
        l.setWrapText(true);
        return l;
    }
}
