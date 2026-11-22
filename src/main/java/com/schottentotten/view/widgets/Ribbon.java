package com.schottentotten.view.widgets;

import com.schottentotten.view.Assets;
import com.schottentotten.view.Palette;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * A burgundy ribbon banner with folded, notched tails and gold stitching, like the section
 * banners of the rulebook.
 */
public final class Ribbon extends StackPane {

    private final Text text;

    /**
     * Creates a ribbon.
     *
     * @param label    text
     * @param fontSize font size
     * @param color    ribbon colour
     */
    public Ribbon(String label, double fontSize, Color color) {
        text = new Text(label);
        text.setFont(Font.font(Assets.TITLE_FONT, fontSize));
        text.setFill(Palette.PARCHMENT);
        text.setEffect(new DropShadow(3, 0, 1, Color.rgb(0, 0, 0, 0.6)));
        double tw = text.getLayoutBounds().getWidth();
        double bh = fontSize * 1.75;
        double bw = tw + fontSize * 2.4;
        double tail = bh * 0.9;
        double drop = bh * 0.28;
        Color dark = color.deriveColor(0, 1, 0.62, 1);
        Polygon leftTail = new Polygon(0, drop, tail, drop, tail, bh + drop, 0, bh + drop, tail * 0.38, drop + bh / 2);
        Polygon rightTail = new Polygon(bw + 2 * tail * 0.62, drop, bw + 2 * tail * 0.62 - tail, drop,
                bw + 2 * tail * 0.62 - tail, bh + drop, bw + 2 * tail * 0.62, bh + drop,
                bw + 2 * tail * 0.62 - tail * 0.38, drop + bh / 2);
        leftTail.setFill(dark);
        rightTail.setFill(dark);
        double x0 = tail * 0.62;
        Polygon body = new Polygon(x0, 0, x0 + bw, 0, x0 + bw, bh, x0, bh);
        body.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 1.2, 1)), new Stop(0.55, color),
                new Stop(1, color.deriveColor(0, 1, 0.75, 1))));
        body.setStroke(dark);
        Line stitchTop = new Line(x0 + 6, 5, x0 + bw - 6, 5);
        Line stitchBottom = new Line(x0 + 6, bh - 5, x0 + bw - 6, bh - 5);
        for (Line l : new Line[]{stitchTop, stitchBottom}) {
            l.setStroke(Palette.GOLD);
            l.getStrokeDashArray().addAll(6.0, 4.0);
            l.setStrokeWidth(1.4);
        }
        Pane shape = new Pane(leftTail, rightTail, body, stitchTop, stitchBottom);
        shape.setEffect(new DropShadow(10, 0, 4, Color.rgb(0, 0, 0, 0.45)));
        StackPane caption = new StackPane(text);
        caption.setPrefSize(bw, bh);
        caption.setLayoutX(x0);
        shape.getChildren().add(caption);
        double total = bw + 2 * tail * 0.62;
        shape.setPrefSize(total, bh + drop);
        shape.setMaxSize(total, bh + drop);
        getChildren().add(shape);
        setAlignment(Pos.CENTER);
        setMaxSize(total, bh + drop);
        setPickOnBounds(false);
    }

    /**
     * Creates a burgundy ribbon.
     *
     * @param label    text
     * @param fontSize font size
     */
    public Ribbon(String label, double fontSize) {
        this(label, fontSize, Palette.BURGUNDY);
    }

    /**
     * Returns the text node (for styling).
     *
     * @return text
     */
    public Text textNode() {
        return text;
    }
}
