package com.schottentotten.view.widgets;

import com.schottentotten.model.ActiveWard;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.StoneState;
import com.schottentotten.model.TacticCard;
import com.schottentotten.view.Assets;
import com.schottentotten.view.Palette;
import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.function.IntConsumer;

/**
 * A carved border Stone on its grass tile, with its number, the owner's clan pennant once
 * claimed, Combat Mode tokens, Stone Ward glow and a "Claim" ribbon when it can be claimed.
 * Like in the board game, a claimed Stone moves towards its owner's side.
 */
public final class StoneView extends Pane {

    private final int index;
    private final double w;
    private final double h;
    private final StackPane stone = new StackPane();
    private final ImageView image;
    private final Pane decorations = new Pane();
    private final Button claimButton = new Button("Claim");
    private Animation pulse;

    /**
     * Creates a Stone view.
     *
     * @param index   Stone index (0-based)
     * @param width   view width
     * @param height  view height
     * @param onClaim called with the Stone index when the Claim ribbon is pressed
     */
    public StoneView(int index, double width, double height, IntConsumer onClaim) {
        this.index = index;
        this.w = width;
        this.h = height;
        setPrefSize(w, h);
        setMinSize(w, h);
        setMaxSize(w, h);
        image = new ImageView(Assets.image("stone-tile.png"));
        image.setFitHeight(h * 0.92);
        image.setPreserveRatio(true);
        image.setSmooth(true);
        Text number = new Text(String.valueOf(index + 1));
        number.setFont(Font.font(Assets.HEADING_FONT, FontWeight.BOLD, h * 0.17));
        number.setFill(Palette.PARCHMENT);
        Rectangle plaque = new Rectangle(h * 0.28, h * 0.22);
        plaque.setArcWidth(8);
        plaque.setArcHeight(8);
        plaque.setFill(Palette.BURGUNDY);
        plaque.setStroke(Palette.GOLD);
        plaque.setStrokeWidth(1.5);
        StackPane numberPlate = new StackPane(plaque, number);
        numberPlate.setTranslateY(h * 0.33);
        numberPlate.setTranslateX(w * 0.30);
        stone.getChildren().addAll(image, numberPlate);
        stone.setPrefSize(w, h);
        stone.setAlignment(Pos.CENTER);
        claimButton.getStyleClass().add("claim-button");
        claimButton.setVisible(false);
        claimButton.setManaged(false);
        claimButton.setOnAction(e -> onClaim.accept(index));
        claimButton.setId("claim-" + index);
        setId("stone-" + index);
        decorations.setPickOnBounds(false);
        decorations.setPrefSize(w, h);
        getChildren().addAll(stone, decorations, claimButton);
        getStyleClass().add("stone-view");
    }

    /**
     * Returns the Stone index.
     *
     * @return index
     */
    public int index() {
        return index;
    }

    /**
     * Refreshes the view.
     *
     * @param state     the Stone
     * @param names     player names (P1, P2)
     * @param claimable {@code true} if the viewer may claim it now
     * @param claimDown {@code true} to put the Claim ribbon below the Stone (viewer at the bottom)
     * @param targeted  {@code true} if the Stone is a valid target of the current selection
     * @param animate   {@code false} when reduced motion is requested
     */
    public void update(StoneState state, String[] names, boolean claimable, boolean claimDown,
                       boolean targeted, boolean animate) {
        stopPulse();
        decorations.getChildren().clear();
        stone.setScaleX(1);
        stone.setScaleY(1);
        double shift = state.owner().map(o -> o == PlayerId.P1 ? h * 0.16 : -h * 0.16).orElse(0.0);
        stone.setTranslateY(shift);
        Tooltip.install(this, new Tooltip(describe(state, names)));
        state.owner().ifPresent(owner -> decorations.getChildren().add(pennant(owner, shift)));
        double tokenX = w * 0.02;
        for (TacticCard mode : state.combatModes()) {
            Circle token = new Circle(h * 0.14);
            token.setFill(new ImagePattern(Assets.cardArt(mode)));
            token.setStroke(Color.web("#9A5A1C"));
            token.setStrokeWidth(2.5);
            token.setEffect(new DropShadow(5, Color.rgb(0, 0, 0, 0.6)));
            token.setCenterX(tokenX + h * 0.14);
            token.setCenterY(h * 0.22 + shift);
            Tooltip.install(token, new Tooltip(mode.displayName() + ": " + mode.type().rulesText()));
            decorations.getChildren().add(token);
            tokenX += h * 0.3;
        }
        for (PlayerId p : PlayerId.values()) {
            state.side(p).ward().ifPresent(ward -> decorations.getChildren().add(wardBadge(p, ward, shift)));
        }
        if (targeted) {
            image.setEffect(new DropShadow(22, Color.web("#3FE0D0")));
        } else if (claimable) {
            image.setEffect(new DropShadow(24, Palette.GOLD));
        } else {
            image.setEffect(new DropShadow(10, 0, 4, Color.rgb(0, 0, 0, 0.45)));
        }
        claimButton.setVisible(claimable);
        if (claimable) {
            claimButton.resizeRelocate((w - 84) / 2, claimDown ? h - 24 : -2, 84, 28);
            if (animate) {
                ScaleTransition st = new ScaleTransition(Duration.millis(650), claimButton);
                st.setFromX(1);
                st.setFromY(1);
                st.setToX(1.08);
                st.setToY(1.08);
                st.setAutoReverse(true);
                st.setCycleCount(Animation.INDEFINITE);
                st.play();
                pulse = st;
            }
        }
        if (targeted && animate) {
            ScaleTransition st = new ScaleTransition(Duration.millis(600), stone);
            st.setToX(1.06);
            st.setToY(1.06);
            st.setAutoReverse(true);
            st.setCycleCount(Animation.INDEFINITE);
            st.play();
            pulse = st;
        }
    }

    private static String describe(StoneState state, String[] names) {
        StringBuilder sb = new StringBuilder("Stone ").append(state.index() + 1);
        state.owner().ifPresent(o -> sb.append(" - claimed by ").append(names[o.ordinal()]));
        if (state.isSumOnly()) {
            sb.append("\nBlind-Man's Bluff: only the sum counts.");
        }
        if (state.requiredSize() == StoneState.MUD_SIZE) {
            sb.append("\nMud Fight: formations need 4 cards.");
        }
        return sb.toString();
    }

    private Node pennant(PlayerId owner, double shift) {
        Color c = Palette.player(owner);
        double poleX = w * 0.82;
        double top = h * 0.08 + shift;
        Line pole = new Line(poleX, top, poleX, top + h * 0.62);
        pole.setStroke(Color.web("#5A3A1A"));
        pole.setStrokeWidth(3);
        Polygon flag = new Polygon(poleX, top, poleX + w * 0.2, top + h * 0.1, poleX, top + h * 0.2);
        flag.setFill(c);
        flag.setStroke(Palette.GOLD);
        flag.setStrokeWidth(1.2);
        Pane p = new Pane(pole, flag);
        p.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.5)));
        p.setMouseTransparent(true);
        return p;
    }

    private Node wardBadge(PlayerId side, ActiveWard ward, double shift) {
        Circle glow = new Circle(h * 0.13);
        glow.setFill(Color.web("#1BA9A0", 0.85));
        glow.setStroke(Color.web("#BFFCF6"));
        glow.setStrokeWidth(2);
        glow.setEffect(new DropShadow(14, Color.web("#3FE0D0")));
        Text n = new Text(String.valueOf(ward.remainingOpponentTurns()));
        n.setFont(Font.font(Assets.HEADING_FONT, FontWeight.BOLD, h * 0.15));
        n.setFill(Color.WHITE);
        StackPane badge = new StackPane(glow, n);
        badge.setLayoutX(w * 0.02);
        badge.setLayoutY((side == PlayerId.P1 ? h * 0.62 : h * 0.02) + shift);
        Tooltip.install(badge, new Tooltip("Stone Ward: Tactic and expansion cards cannot affect this side "
                + "for " + ward.remainingOpponentTurns() + " more opponent turn(s)."));
        return badge;
    }

    /**
     * Plays the claim celebration (Stone pulse).
     */
    public void celebrateClaim() {
        ScaleTransition st = new ScaleTransition(Duration.millis(260), stone);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(1.18);
        st.setToY(1.18);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
        TranslateTransition drop = new TranslateTransition(Duration.millis(380), decorations);
        drop.setFromY(-18);
        drop.setToY(0);
        drop.play();
    }

    private void stopPulse() {
        if (pulse != null) {
            pulse.stop();
            pulse = null;
            claimButton.setScaleX(1);
            claimButton.setScaleY(1);
        }
    }
}
