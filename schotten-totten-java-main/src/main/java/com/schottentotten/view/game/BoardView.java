package com.schottentotten.view.game;

import com.schottentotten.model.Board;
import com.schottentotten.model.Card;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.model.StoneState;
import com.schottentotten.view.Palette;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.CardView;
import com.schottentotten.view.widgets.StoneView;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The border, laid out at a fixed design size: Player 2's hand and formations at the top, the
 * nine Stones across the middle, Player 1's formations and hand at the bottom.
 */
public final class BoardView extends Pane {

    /** Design width. */
    public static final double WIDTH = 1180;
    /** Design height. */
    public static final double HEIGHT = 830;

    private static final double COL_W = 112;
    private static final double CARD_W = 78;
    private static final double ZONE_H = 200;
    private static final double STONE_H = 104;
    private static final double HAND_CARD_W = 90;
    private static final double BACK_W = 62;
    private static final double TOP_HAND_Y = 6;
    private static final double TOP_ZONE_Y = 140;
    private static final double STONE_Y = TOP_ZONE_Y + ZONE_H + 4;
    private static final double BOTTOM_ZONE_Y = STONE_Y + STONE_H + 4;
    private static final double BOTTOM_HAND_Y = BOTTOM_ZONE_Y + ZONE_H + 6;
    private static final double COLUMNS_X = (WIDTH - COL_W * Board.STONE_COUNT) / 2;
    private static final double TRAY_X = 236;
    private static final double TRAY_W = WIDTH - TRAY_X - 10;

    /**
     * Receives the player's interactions with the board.
     */
    public interface Handler {
        /**
         * A card of the viewer's hand was clicked.
         *
         * @param card the card
         */
        void handCardClicked(Card card);

        /**
         * A drag started on a card of the viewer's hand.
         *
         * @param card the card
         * @return {@code true} if the card may be dragged
         */
        boolean handCardDragStarted(Card card);

        /**
         * A Stone column (either side) was clicked or a card was dropped on it.
         *
         * @param stone   Stone index
         * @param dropped {@code true} for a drop
         */
        void stoneChosen(int stone, boolean dropped);

        /**
         * A card on the board was clicked.
         *
         * @param cardId card identifier
         * @return {@code true} if the click was used
         */
        boolean boardCardClicked(String cardId);

        /**
         * The Claim ribbon of a Stone was pressed.
         *
         * @param stone Stone index
         */
        void claimPressed(int stone);

        /**
         * Right click anywhere on the board (cancel).
         */
        void cancel();

        /**
         * Tells whether a Stone may receive the current drag.
         *
         * @param stone Stone index
         * @return {@code true} if dropping is allowed
         */
        boolean acceptsDrop(int stone);
    }

    /**
     * What the board must display beyond the game state itself.
     *
     * @param faceUp         players whose hand is shown face up
     * @param playable       identifiers of the hand cards that can be played now
     * @param selectedCard   identifier of the selected hand card (or {@code null})
     * @param targetStones   Stones that are valid targets of the current selection
     * @param targetCards    board cards that are valid targets of the current selection
     * @param claimable      Stones the viewer may claim now
     * @param claimDown      {@code true} if the Claim ribbons face the bottom player
     * @param active         player whose turn it is (highlighted plaque), or {@code null}
     * @param hintCard       card suggested by the hint (or {@code null})
     * @param newCards       cards that just arrived on the board (animated)
     * @param claimedNow     Stones that were just claimed (celebrated)
     * @param animate        {@code false} when reduced motion is requested
     */
    public record Decorations(Set<PlayerId> faceUp, Set<String> playable, String selectedCard, Set<Integer> targetStones,
                              Set<String> targetCards, Set<Integer> claimable, boolean claimDown, PlayerId active,
                              String hintCard, Set<String> newCards, Set<Integer> claimedNow, boolean animate) {
    }

    private final Handler handler;
    private final Pane[][] zones = new Pane[2][Board.STONE_COUNT];
    private final Rectangle[][] zoneGlow = new Rectangle[2][Board.STONE_COUNT];
    private final StoneView[] stones = new StoneView[Board.STONE_COUNT];
    private final Pane[] hands = {new Pane(), new Pane()};
    private final VBox[] plaques = {new VBox(2), new VBox(2)};
    private final List<Set<String>> shownHands = List.of(new HashSet<>(), new HashSet<>());

    /**
     * Builds the empty board.
     *
     * @param handler interaction handler
     */
    public BoardView(Handler handler) {
        this.handler = handler;
        setPrefSize(WIDTH, HEIGHT);
        for (PlayerId p : PlayerId.values()) {
            for (int i = 0; i < Board.STONE_COUNT; i++) {
                Pane zone = new Pane();
                zone.setPrefSize(COL_W, ZONE_H);
                zone.relocate(COLUMNS_X + i * COL_W, p == PlayerId.P2 ? TOP_ZONE_Y : BOTTOM_ZONE_Y);
                Rectangle glow = new Rectangle(4, 2, COL_W - 8, ZONE_H - 4);
                glow.setArcWidth(18);
                glow.setArcHeight(18);
                glow.setFill(Color.web("#3FE0D0", 0.16));
                glow.setStroke(Color.web("#3FE0D0", 0.9));
                glow.setStrokeWidth(2.5);
                glow.getStrokeDashArray().addAll(10.0, 6.0);
                glow.setVisible(false);
                glow.setMouseTransparent(true);
                zone.getChildren().add(glow);
                int stone = i;
                zone.setOnMouseClicked(e -> {
                    if (e.getButton() == MouseButton.SECONDARY) {
                        handler.cancel();
                    } else {
                        handler.stoneChosen(stone, false);
                    }
                });
                zone.setOnDragOver(e -> {
                    if (handler.acceptsDrop(stone)) {
                        e.acceptTransferModes(TransferMode.MOVE);
                    }
                    e.consume();
                });
                zone.setOnDragDropped(e -> {
                    handler.stoneChosen(stone, true);
                    e.setDropCompleted(true);
                    e.consume();
                });
                zone.setId("zone-" + p.name() + "-" + i);
                zones[p.ordinal()][i] = zone;
                zoneGlow[p.ordinal()][i] = glow;
                getChildren().add(zone);
            }
        }
        for (int i = 0; i < Board.STONE_COUNT; i++) {
            StoneView sv = new StoneView(i, COL_W, STONE_H, handler::claimPressed);
            sv.relocate(COLUMNS_X + i * COL_W, STONE_Y);
            int stone = i;
            sv.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    handler.cancel();
                } else {
                    handler.stoneChosen(stone, false);
                }
            });
            sv.setOnDragOver(e -> {
                if (handler.acceptsDrop(stone)) {
                    e.acceptTransferModes(TransferMode.MOVE);
                }
                e.consume();
            });
            sv.setOnDragDropped(e -> {
                handler.stoneChosen(stone, true);
                e.setDropCompleted(true);
                e.consume();
            });
            stones[i] = sv;
            getChildren().add(sv);
        }
        hands[PlayerId.P2.ordinal()].relocate(TRAY_X, TOP_HAND_Y);
        hands[PlayerId.P1.ordinal()].relocate(TRAY_X, BOTTOM_HAND_Y);
        for (Pane hand : hands) {
            hand.setPrefSize(TRAY_W, 128);
            hand.setPickOnBounds(false);
        }
        plaques[PlayerId.P2.ordinal()].relocate(14, TOP_HAND_Y + 6);
        plaques[PlayerId.P1.ordinal()].relocate(14, BOTTOM_HAND_Y + 12);
        for (VBox plaque : plaques) {
            plaque.getStyleClass().add("player-plaque");
            plaque.setPrefWidth(200);
            plaque.setMaxWidth(200);
        }
        getChildren().addAll(hands);
        getChildren().addAll(plaques);
        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                handler.cancel();
            }
        });
    }

    /**
     * Returns the view of a Stone.
     *
     * @param index Stone index
     * @return Stone view
     */
    public StoneView stone(int index) {
        return stones[index];
    }

    /**
     * Redraws the whole board from a snapshot.
     *
     * @param s state snapshot
     * @param d decorations
     */
    public void render(GameState s, Decorations d) {
        String[] names = {s.config().player1().name(), s.config().player2().name()};
        for (PlayerId p : PlayerId.values()) {
            renderPlaque(s, p, d);
            renderHand(s, p, d);
            for (int i = 0; i < Board.STONE_COUNT; i++) {
                renderZone(s.board().stone(i), p, d);
            }
        }
        for (int i = 0; i < Board.STONE_COUNT; i++) {
            StoneState stone = s.board().stone(i);
            stones[i].update(stone, names, d.claimable().contains(i), d.claimDown(),
                    d.targetStones().contains(i), d.animate());
            stones[i].setCursor(d.targetStones().contains(i) ? Cursor.HAND : Cursor.DEFAULT);
            if (d.claimedNow().contains(i) && d.animate()) {
                stones[i].celebrateClaim();
            }
        }
    }

    private void renderPlaque(GameState s, PlayerId p, Decorations d) {
        VBox plaque = plaques[p.ordinal()];
        PlayerProfile profile = s.config().profile(p);
        Label name = Styles.label(profile.name(), "player-name");
        name.setWrapText(false);
        name.setStyle("-fx-text-fill: " + Palette.hex(Palette.player(p).darker()) + ";");
        String kind = profile.isVirtual() ? "Computer - " + profile.difficulty().displayName() : "Human";
        Label meta = Styles.label(kind + "  |  " + (p == PlayerId.P1 ? "Player 1" : "Player 2"), "player-meta");
        Label stonesLabel = Styles.label("Stones: " + s.board().stonesOwnedBy(p) + "    Hand: " + s.handSize(p),
                "player-meta");
        plaque.getChildren().setAll(name, meta, stonesLabel);
        if (s.config().tacticVariant() || s.config().expansions()) {
            plaque.getChildren().add(Styles.label("Special cards played: " + s.specialPlayed(p), "player-meta"));
        }
        if (s.config().isMultiRound()) {
            plaque.getChildren().add(Styles.label("Match points: " + s.match().points(p), "player-meta"));
        }
        plaque.setBorder(new javafx.scene.layout.Border(new javafx.scene.layout.BorderStroke(Palette.player(p),
                javafx.scene.layout.BorderStrokeStyle.SOLID, new CornerRadii(12), new javafx.scene.layout.BorderWidths(0, 0, 0, 6))));
        plaque.getStyleClass().remove("player-plaque-active");
        if (p == d.active()) {
            plaque.getStyleClass().add("player-plaque-active");
        }
        plaque.setPadding(new Insets(8, 12, 8, 14));
    }

    private void renderHand(GameState s, PlayerId p, Decorations d) {
        Pane tray = hands[p.ordinal()];
        tray.getChildren().clear();
        List<Card> cards = s.hand(p);
        Set<String> before = Set.copyOf(shownHands.get(p.ordinal()));
        shownHands.get(p.ordinal()).clear();
        cards.forEach(c -> shownHands.get(p.ordinal()).add(c.id()));
        boolean faceUp = d.faceUp().contains(p);
        double w = faceUp ? HAND_CARD_W : BACK_W;
        int n = cards.size();
        if (n == 0) {
            return;
        }
        int dealt = 0;
        double step = n == 1 ? 0 : Math.min(w + 12, (TRAY_W - 40 - w) / (n - 1));
        double total = w + step * (n - 1);
        double x0 = (TRAY_W - total) / 2;
        boolean top = p == PlayerId.P2;
        for (int i = 0; i < n; i++) {
            Card card = cards.get(i);
            CardView view = faceUp ? new CardView(card, w) : CardView.back(w);
            double rel = i - (n - 1) / 2.0;
            double arc = rel * rel * (faceUp ? 1.6 : 0.8);
            double y = top ? 4 + (128 - view.cardHeight()) / 2 - arc : 10 + arc;
            view.relocate(x0 + i * step, y);
            view.setRotate((top ? -1 : 1) * rel * (faceUp ? 2.6 : 1.8));
            if (faceUp) {
                decorateHandCard(view, card, d, top);
            }
            tray.getChildren().add(view);
            if (d.animate() && !before.contains(card.id())) {
                deal(view, top, dealt++);
            }
        }
    }

    /** Slides a card that just entered a hand in from the draw piles, one after the other. */
    private static void deal(CardView view, boolean top, int order) {
        Duration delay = Duration.millis(order * 70);
        view.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(380), view);
        tt.setFromX(280);
        tt.setFromY(top ? -70 : 70);
        tt.setToX(0);
        tt.setToY(0);
        tt.setDelay(delay);
        tt.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.3, 1));
        FadeTransition ft = new FadeTransition(Duration.millis(220), view);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(delay);
        new ParallelTransition(tt, ft).play();
    }

    private void decorateHandCard(CardView view, Card card, Decorations d, boolean top) {
        boolean playable = d.playable().contains(card.id());
        boolean selected = card.id().equals(d.selectedCard());
        view.setDimmed(!playable && !d.playable().isEmpty());
        view.setSelected(selected || card.id().equals(d.hintCard()));
        double lift = top ? 14 : -18;
        if (selected) {
            view.setTranslateY(lift * 1.3);
            view.setViewOrder(-2);
        }
        view.setCursor(playable ? Cursor.HAND : Cursor.DEFAULT);
        view.setOnMouseEntered(e -> {
            if (!selected) {
                slide(view, lift, d.animate());
                view.setViewOrder(-1);
            }
        });
        view.setOnMouseExited(e -> {
            if (!selected) {
                slide(view, 0, d.animate());
                view.setViewOrder(0);
            }
        });
        view.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                handler.cancel();
            } else {
                handler.handCardClicked(card);
            }
            e.consume();
        });
        view.setOnDragDetected(e -> {
            if (!handler.handCardDragStarted(card)) {
                return;
            }
            // beginPlay() re-renders the hand, so use the live node for this card id
            CardView source = findHandCard(card.id()).orElse(null);
            if (source == null || source.getScene() == null) {
                return;
            }
            Dragboard db = source.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(card.id());
            db.setContent(content);
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            db.setDragView(source.snapshot(sp, null), source.getWidth() / 2, source.getHeight() / 2);
            e.consume();
        });
    }

    private Optional<CardView> findHandCard(String cardId) {
        for (Pane tray : hands) {
            for (javafx.scene.Node child : tray.getChildren()) {
                if (child instanceof CardView cv && cv.card() != null && cardId.equals(cv.card().id())) {
                    return Optional.of(cv);
                }
            }
        }
        return Optional.empty();
    }

    private static void slide(CardView view, double y, boolean animate) {
        if (!animate) {
            view.setTranslateY(y);
            return;
        }
        TranslateTransition tt = new TranslateTransition(Duration.millis(120), view);
        tt.setToY(y);
        tt.play();
    }

    private void renderZone(StoneState stone, PlayerId p, Decorations d) {
        Pane zone = zones[p.ordinal()][stone.index()];
        Rectangle glow = zoneGlow[p.ordinal()][stone.index()];
        zone.getChildren().setAll(glow);
        boolean target = d.targetStones().contains(stone.index());
        glow.setVisible(target);
        zone.setCursor(target ? Cursor.HAND : Cursor.DEFAULT);
        List<Card> cards = stone.side(p).cards();
        int slots = Math.max(3, Math.max(cards.size(), stone.requiredSize()));
        double h = CARD_W * 1.4;
        double offset = Math.min(36, (ZONE_H - 8 - h) / (slots - 1));
        boolean top = p == PlayerId.P2;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            CardView view = new CardView(card, CARD_W);
            double y = top ? ZONE_H - 4 - h - i * offset : 4 + i * offset;
            view.relocate((COL_W - CARD_W) / 2, y);
            if (d.targetCards().contains(card.id())) {
                view.setTargeted(true, d.animate());
                view.setCursor(Cursor.HAND);
            }
            view.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.SECONDARY) {
                    handler.cancel();
                } else if (!handler.boardCardClicked(card.id())) {
                    handler.stoneChosen(stone.index(), false);
                }
                e.consume();
            });
            zone.getChildren().add(view);
            if (d.newCards().contains(card.id()) && d.animate()) {
                arrive(view, top);
            }
        }
    }

    private static void arrive(CardView view, boolean top) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(420), view);
        tt.setFromY(top ? -220 : 220);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.SPLINE(0.2, 0.8, 0.3, 1));
        FadeTransition ft = new FadeTransition(Duration.millis(260), view);
        ft.setFromValue(0.15);
        ft.setToValue(1);
        new ParallelTransition(tt, ft).play();
    }
}
