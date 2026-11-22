package com.schottentotten.view.widgets;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ExpansionCard;
import com.schottentotten.model.TacticCard;
import com.schottentotten.model.TacticCategory;
import com.schottentotten.view.Assets;
import com.schottentotten.view.Palette;
import javafx.animation.FadeTransition;
import javafx.animation.Animation;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * An illustrated card: cream border, coloured frame, hand-painted illustration and corner
 * badges. Clan cards show their value in two opposite corners, so a card stays readable when it
 * is partially covered by the cards stacked above or below it. Special cards show a category emblem and
 * their name.
 */
public final class CardView extends Pane {

    private static final String STAR = "M12 2 L14.9 8.6 L22 9.3 L16.6 14 L18.2 21 L12 17.3 L5.8 21 L7.4 14 L2 9.3 L9.1 8.6 Z";
    private static final String SWORDS = "M5 5 L19 19 M19 5 L5 19 M4 15 L9 20 M15 20 L20 15";
    private static final String EYE = "M2 12 C6 5 18 5 22 12 C18 19 6 19 2 12 Z M12 8.5 A3.5 3.5 0 1 0 12.01 8.5 Z";
    private static final String KNOT = "M12 2 L21 12 L12 22 L3 12 Z M12 7 L16.5 12 L12 17 L7.5 12 Z";

    private final Card card;
    private final double w;
    private final double h;
    private final Group face = new Group();
    private final Rectangle ring;
    private Animation pulse;
    private boolean targeted;

    /**
     * Creates a face-up card.
     *
     * @param card  the card
     * @param width card width (height is 1.4 times the width)
     */
    public CardView(Card card, double width) {
        this(card, width, true);
    }

    private CardView(Card card, double width, boolean faceUp) {
        this.card = card;
        this.w = width;
        this.h = width * 1.4;
        setPrefSize(w, h);
        setMinSize(w, h);
        setMaxSize(w, h);
        double arc = w * 0.16;
        ring = new Rectangle(-4, -4, w + 8, h + 8);
        ring.setArcWidth(arc + 8);
        ring.setArcHeight(arc + 8);
        ring.setFill(Color.TRANSPARENT);
        ring.setStrokeWidth(Math.max(2.5, w * 0.04));
        ring.setVisible(false);
        ring.setMouseTransparent(true);
        if (faceUp && card != null) {
            buildFace(arc);
            Tooltip tip = new Tooltip(card.displayName() + describe(card));
            tip.setWrapText(true);
            tip.setMaxWidth(320);
            tip.setShowDelay(Duration.millis(450));
            Tooltip.install(this, tip);
        } else {
            buildBack(arc);
        }
        face.setEffect(new DropShadow(w * 0.09, 0, w * 0.035, Color.rgb(20, 12, 4, 0.55)));
        getChildren().addAll(face, ring);
        getStyleClass().add("card-view");
    }

    /**
     * Creates a face-down card.
     *
     * @param width card width
     * @return card back
     */
    public static CardView back(double width) {
        return new CardView(null, width, false);
    }

    /**
     * Returns the card shown, or {@code null} for a face-down card.
     *
     * @return card
     */
    public Card card() {
        return card;
    }

    /**
     * Returns the card height.
     *
     * @return height
     */
    public double cardHeight() {
        return h;
    }

    private static String describe(Card card) {
        return switch (card) {
            case ClanCard c -> " - Clan card";
            case TacticCard t -> " (" + t.type().category().displayName() + ")\n" + t.type().rulesText();
            case ExpansionCard x -> " (expansion, " + x.definition().category() + ")\n" + x.definition().rulesText();
        };
    }

    private Color frameColor() {
        return switch (card) {
            case ClanCard c -> Palette.clan(c.color());
            case TacticCard t -> switch (t.type().category()) {
                case ELITE_TROOP -> Color.web("#14968E");
                case COMBAT_MODE -> Color.web("#9A5A1C");
                case RUSE -> Color.web("#8E1B2A");
            };
            case ExpansionCard x -> Color.web("#B07D12");
        };
    }

    private void buildFace(double arc) {
        Color frame = frameColor();
        Rectangle base = new Rectangle(w, h);
        base.setArcWidth(arc);
        base.setArcHeight(arc);
        base.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FFF9E8")), new Stop(1, Palette.PARCHMENT_DARK)));
        base.setStroke(Color.rgb(59, 38, 20, 0.55));
        base.setStrokeWidth(Math.max(1, w * 0.012));

        double m = w * 0.05;
        Rectangle frameRect = new Rectangle(m, m, w - 2 * m, h - 2 * m);
        frameRect.setArcWidth(arc * 0.75);
        frameRect.setArcHeight(arc * 0.75);
        frameRect.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, frame.deriveColor(0, 1, 1.15, 1)), new Stop(1, frame.deriveColor(0, 1, 0.7, 1))));

        boolean special = !(card instanceof ClanCard);
        double inset = w * 0.09;
        double strip = special ? h * 0.2 : 0;
        double artW = w - 2 * inset;
        double artH = h - 2 * inset - strip;
        ImageView art = coverImage(Assets.cardArt(card), artW, artH);
        art.setLayoutX(inset);
        art.setLayoutY(inset);
        Rectangle clip = new Rectangle(artW, artH);
        clip.setArcWidth(arc * 0.5);
        clip.setArcHeight(arc * 0.5);
        art.setClip(clip);
        art.setEffect(new InnerShadow(w * 0.06, Color.rgb(0, 0, 0, 0.45)));
        face.getChildren().addAll(base, frameRect, art);

        if (special) {
            Rectangle band = new Rectangle(inset, h - inset - strip + w * 0.02, artW, strip - w * 0.02);
            band.setArcWidth(arc * 0.4);
            band.setArcHeight(arc * 0.4);
            band.setFill(Palette.PARCHMENT);
            Text name = new Text(card.displayName());
            double size = w * 0.115;
            name.setFont(Font.font(Assets.BODY_FONT, FontWeight.BOLD, size));
            name.setFill(Palette.INK);
            name.setTextAlignment(TextAlignment.CENTER);
            name.setWrappingWidth(artW - w * 0.04);
            while (name.getLayoutBounds().getHeight() > band.getHeight() * 0.98 && size > 5) {
                size *= 0.9;
                name.setFont(Font.font(Assets.BODY_FONT, FontWeight.BOLD, size));
            }
            StackPane label = new StackPane(name);
            label.setAlignment(Pos.CENTER);
            label.setPrefSize(artW, band.getHeight());
            label.setLayoutX(inset);
            label.setLayoutY(band.getY());
            face.getChildren().addAll(band, label);
            face.getChildren().add(badge(frame, w * 0.06, w * 0.06));
        } else {
            face.getChildren().add(badge(frame, w * 0.04, w * 0.04));
            face.getChildren().add(badge(frame, w - w * 0.04 - badgeSize(), h - w * 0.04 - badgeSize()));
        }
    }

    private double badgeSize() {
        return w * 0.36;
    }

    private Node badge(Color frame, double x, double y) {
        double d = badgeSize();
        Circle disc = new Circle(d / 2);
        disc.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, frame.deriveColor(0, 1, 1.1, 1)), new Stop(1, frame.deriveColor(0, 1, 0.65, 1))));
        disc.setStroke(Palette.PARCHMENT);
        disc.setStrokeWidth(Math.max(1.2, w * 0.03));
        StackPane badge = new StackPane(disc, emblem(d));
        badge.setPrefSize(d, d);
        badge.setLayoutX(x);
        badge.setLayoutY(y);
        badge.setEffect(new DropShadow(w * 0.03, Color.rgb(0, 0, 0, 0.5)));
        return badge;
    }

    private Node emblem(double d) {
        String text = switch (card) {
            case ClanCard c -> String.valueOf(c.strength());
            case TacticCard t when t.type().category() == TacticCategory.ELITE_TROOP && !t.isJoker() ->
                    t.type().eliteStrengths().size() == 1 ? String.valueOf(t.type().eliteStrengths().iterator().next())
                            : "1-3";
            default -> null;
        };
        if (text != null) {
            Text t = new Text(text);
            t.setFont(Font.font(Assets.HEADING_FONT, FontWeight.BOLD, text.length() > 1 ? d * 0.42 : d * 0.62));
            t.setFill(Color.WHITE);
            t.setStroke(Color.rgb(40, 20, 5, 0.55));
            t.setStrokeWidth(Math.max(0.5, d * 0.02));
            return t;
        }
        SVGPath icon = new SVGPath();
        boolean stroked = false;
        if (card instanceof TacticCard t) {
            switch (t.type().category()) {
                case ELITE_TROOP -> icon.setContent(STAR);
                case COMBAT_MODE -> {
                    icon.setContent(SWORDS);
                    stroked = true;
                }
                case RUSE -> icon.setContent(EYE);
            }
        } else {
            icon.setContent(KNOT);
        }
        if (stroked) {
            icon.setFill(Color.TRANSPARENT);
            icon.setStroke(Color.WHITE);
            icon.setStrokeWidth(2.6);
            icon.setStrokeLineCap(StrokeLineCap.ROUND);
        } else {
            icon.setFill(Color.WHITE);
            icon.setFillRule(javafx.scene.shape.FillRule.EVEN_ODD);
        }
        double s = d * 0.62 / 24.0;
        icon.setScaleX(s);
        icon.setScaleY(s);
        return new Group(icon);
    }

    private void buildBack(double arc) {
        Rectangle base = new Rectangle(w, h);
        base.setArcWidth(arc);
        base.setArcHeight(arc);
        base.setFill(Palette.PARCHMENT);
        base.setStroke(Color.rgb(59, 38, 20, 0.6));
        base.setStrokeWidth(Math.max(1, w * 0.012));
        double m = w * 0.045;
        ImageView img = coverImage(Assets.cardBack(), w - 2 * m, h - 2 * m);
        img.setLayoutX(m);
        img.setLayoutY(m);
        Rectangle clip = new Rectangle(w - 2 * m, h - 2 * m);
        clip.setArcWidth(arc * 0.75);
        clip.setArcHeight(arc * 0.75);
        img.setClip(clip);
        face.getChildren().addAll(base, img);
    }

    /**
     * Creates an image view that covers the given box, cropping the image if needed.
     *
     * @param img    image
     * @param width  box width
     * @param height box height
     * @return image view
     */
    public static ImageView coverImage(Image img, double width, double height) {
        ImageView view = new ImageView(img);
        view.setSmooth(true);
        view.setFitWidth(width);
        view.setFitHeight(height);
        double iw = img.getWidth();
        double ih = img.getHeight();
        if (iw > 0 && ih > 0) {
            double target = width / height;
            double ratio = iw / ih;
            if (ratio > target) {
                double vw = ih * target;
                view.setViewport(new Rectangle2D((iw - vw) / 2, 0, vw, ih));
            } else {
                double vh = iw / target;
                view.setViewport(new Rectangle2D(0, (ih - vh) * 0.25, iw, vh));
            }
        }
        return view;
    }

    /**
     * Shows or hides the golden selection ring.
     *
     * @param selected selection flag
     */
    public void setSelected(boolean selected) {
        stopPulse();
        targeted = false;
        ring.setStroke(Palette.GOLD);
        ring.setEffect(new DropShadow(14, Palette.GOLD));
        ring.setOpacity(1);
        ring.setVisible(selected);
    }

    /**
     * Shows or hides the pulsing turquoise ring marking a valid target.
     *
     * @param targeted target flag
     * @param animate  {@code false} when reduced motion is requested
     */
    public void setTargeted(boolean targeted, boolean animate) {
        stopPulse();
        ring.setStroke(Palette.TURQUOISE);
        ring.setEffect(new DropShadow(16, Color.web("#3FE0D0")));
        ring.setVisible(targeted);
        ring.setOpacity(1);
        if (targeted && animate) {
            FadeTransition ft = new FadeTransition(Duration.millis(700), ring);
            ft.setFromValue(1);
            ft.setToValue(0.35);
            ft.setAutoReverse(true);
            ft.setCycleCount(Animation.INDEFINITE);
            ft.play();
            pulse = ft;
        }
        this.targeted = targeted;
    }

    /**
     * Tells whether the card is currently marked as a valid target.
     *
     * @return target flag
     */
    public boolean isTargeted() {
        return targeted;
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.stop();
            pulse = null;
        }
    }

    /**
     * Greys the card out (for example when it cannot be played now).
     *
     * @param dimmed dim flag
     */
    public void setDimmed(boolean dimmed) {
        DropShadow shadow = (DropShadow) face.getEffect();
        if (dimmed) {
            ColorAdjust adjust = new ColorAdjust(0, -0.55, -0.12, 0);
            shadow.setInput(adjust);
        } else {
            shadow.setInput(null);
        }
    }
}
