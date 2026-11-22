package com.schottentotten.view.game;

import com.schottentotten.app.GameSettings;
import javafx.application.Platform;
import com.schottentotten.model.Card;
import com.schottentotten.model.DeckType;
import com.schottentotten.view.Palette;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.CardView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wooden side rail of the board: turn banner with instructions, action buttons, draw piles,
 * discard pile, the chronicle (game log) and the computer-pace controls.
 */
public final class SideRail extends VBox {

    /** Rail width. */
    public static final double WIDTH = 310;

    /**
     * Receives the rail interactions.
     */
    public interface Handler {
        /** Opens the game menu. */
        void menu();

        /** Ends the turn (asks for a deck if needed). */
        void endTurn();

        /** Passes. */
        void pass();

        /** Shows a hint. */
        void hint();

        /**
         * A draw pile was clicked.
         *
         * @param deck deck type
         */
        void deckClicked(DeckType deck);

        /** The discard pile was clicked. */
        void discardClicked();

        /**
         * The computer pace changed.
         *
         * @param speed new pace
         */
        void speedChanged(GameSettings.Speed speed);

        /**
         * The computer players were paused or resumed.
         *
         * @param paused pause flag
         */
        void pauseChanged(boolean paused);

        /**
         * Spectator hand reveal toggled.
         *
         * @param reveal reveal flag
         */
        void revealChanged(boolean reveal);
    }

    /**
     * Everything the rail displays.
     *
     * @param round          round and turn line
     * @param title          banner title
     * @param text           banner instruction
     * @param endTurnLabel   label of the main action button
     * @param endTurnEnabled main action enabled
     * @param passVisible    {@code true} when passing is allowed
     * @param hintEnabled    {@code true} when a hint is available
     * @param decks          remaining cards per deck in play
     * @param drawable       decks the viewer may draw from now
     * @param discard        discard pile, oldest first
     * @param discardTarget  {@code true} if the discard pile is a valid target
     * @param log            log lines, oldest first
     * @param showAi         {@code true} to show the computer-pace controls
     * @param spectator      {@code true} in computer-versus-computer games
     * @param paused         computer players paused
     */
    public record View(String round, String title, String text, String endTurnLabel, boolean endTurnEnabled,
                       boolean passVisible, boolean hintEnabled, Map<DeckType, Integer> decks, Set<DeckType> drawable,
                       List<Card> discard, boolean discardTarget, List<String> log, boolean showAi, boolean spectator,
                       boolean paused) {
    }

    private final Handler handler;
    private final Label round = Styles.label("", "rail-title");
    private final Label title = Styles.label("", "turn-title");
    private final Label text = Styles.label("", "turn-text");
    private final Button endTurn = Styles.plaque("End Turn", "plaque-primary", "plaque-small");
    private final Button pass = Styles.plaque("Pass", "plaque-small");
    private final Button hint = Styles.plaque("Hint", "plaque-small");
    private final HBox decks = new HBox(12);
    private final Label discardTitle = Styles.label("Discard pile", "rail-title");
    private final StackPane discardSpot = new StackPane();
    private final ObservableList<String> logItems = FXCollections.observableArrayList();
    private final ListView<String> log = new ListView<>(logItems);
    private final VBox aiBox;
    private final Slider speed = new Slider(0, 3, 1);
    private final ToggleButton pause = new ToggleButton("Pause");
    private final ToggleButton reveal = new ToggleButton("Show hands");
    private final Button mute = new Button();
    private boolean updating;

    /**
     * Builds the rail.
     *
     * @param handler interaction handler
     */
    public SideRail(Handler handler) {
        super(10);
        this.handler = handler;
        Styles.wood(this);
        setPadding(new Insets(12, 12, 12, 16));
        setPrefWidth(WIDTH);
        setMinWidth(WIDTH);
        setMaxWidth(WIDTH);

        Button menu = new Button("Menu");
        menu.getStyleClass().setAll("button", "icon-button");
        menu.setOnAction(e -> handler.menu());
        mute.setId("mute");
        mute.getStyleClass().setAll("button", "icon-button");
        mute.setTooltip(new Tooltip("Mute or restore sound effects and music (M)"));
        mute.setOnAction(e -> toggleMute());
        GameSettings.getInstance().addListener(() -> Platform.runLater(this::renderMute));
        renderMute();
        round.setStyle("-fx-text-fill: #FBF3DC; -fx-font-size: 14px;");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(8, round, gap, mute, menu);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox banner = new VBox(4, title, text);
        banner.getStyleClass().add("turn-banner");
        banner.setMinHeight(96);

        endTurn.setId("end-turn");
        pass.setId("pass");
        hint.setId("hint");
        discardSpot.setId("discard-pile");
        endTurn.setOnAction(e -> handler.endTurn());
        pass.setOnAction(e -> handler.pass());
        hint.setOnAction(e -> handler.hint());
        endTurn.setTooltip(new Tooltip("End your turn and draw (E or Enter)"));
        pass.setTooltip(new Tooltip("Pass when you cannot play any card (P)"));
        hint.setTooltip(new Tooltip("Suggest a move (H)"));
        HBox actions = new HBox(8, endTurn, pass, hint);
        actions.setAlignment(Pos.CENTER_LEFT);

        VBox deckSection = section("Draw piles", decks);
        decks.setAlignment(Pos.CENTER_LEFT);

        discardSpot.setPrefSize(62, 86);
        discardSpot.setCursor(Cursor.HAND);
        discardSpot.setOnMouseClicked(e -> handler.discardClicked());
        Label discardHelp = Styles.label("Click to see every discarded card.", "hint-text");
        HBox discardRow = new HBox(12, discardSpot, discardHelp);
        discardRow.setAlignment(Pos.CENTER_LEFT);
        VBox discardSection = new VBox(6, discardTitle, discardRow);
        discardSection.getStyleClass().add("rail-section");

        log.setFocusTraversable(false);
        log.setCellFactory(lv -> {
            javafx.scene.control.ListCell<String> cell = new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.prefWidthProperty().bind(lv.widthProperty().subtract(18));
            return cell;
        });
        VBox logSection = section("Chronicle", log);
        VBox.setVgrow(log, Priority.ALWAYS);
        VBox.setVgrow(logSection, Priority.ALWAYS);
        logSection.setMinHeight(90);

        speed.setMajorTickUnit(1);
        speed.setMinorTickCount(0);
        speed.setSnapToTicks(true);
        speed.setShowTickMarks(true);
        speed.setShowTickLabels(true);
        speed.setLabelFormatter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Double v) {
                return switch (v.intValue()) {
                    case 0 -> "Slow";
                    case 1 -> "Normal";
                    case 2 -> "Fast";
                    default -> "Instant";
                };
            }

            @Override
            public Double fromString(String s) {
                return 1.0;
            }
        });
        speed.valueProperty().addListener((o, a, b) -> {
            if (!updating && Math.abs(b.doubleValue() - Math.rint(b.doubleValue())) < 1e-6) {
                handler.speedChanged(GameSettings.Speed.values()[(int) Math.rint(b.doubleValue())]);
            }
        });
        for (ToggleButton t : new ToggleButton[]{pause, reveal}) {
            t.getStyleClass().setAll("toggle-button", "switch");
            t.setPrefWidth(Region.USE_COMPUTED_SIZE);
        }
        pause.setOnAction(e -> handler.pauseChanged(pause.isSelected()));
        reveal.setOnAction(e -> handler.revealChanged(reveal.isSelected()));
        HBox aiButtons = new HBox(8, pause, reveal);
        aiBox = section("Computer pace", speed, aiButtons);

        getChildren().addAll(header, banner, actions, deckSection, discardSection, logSection, aiBox);
        title.setMinHeight(Region.USE_PREF_SIZE);
        text.setMinHeight(Region.USE_PREF_SIZE);
        for (Region fixed : new Region[]{header, banner, actions, deckSection, discardSection, aiBox}) {
            fixed.setMinHeight(Region.USE_PREF_SIZE);
        }
    }

    /**
     * Mutes every sound (effects and music) or, when everything is already silent, turns both back
     * on. The choice is saved like any other preference.
     */
    public void toggleMute() {
        GameSettings settings = GameSettings.getInstance();
        boolean silence = settings.isSoundEnabled() || settings.isMusicEnabled();
        settings.setSoundEnabled(!silence);
        settings.setMusicEnabled(!silence);
    }

    private void renderMute() {
        GameSettings settings = GameSettings.getInstance();
        mute.setText(settings.isSoundEnabled() || settings.isMusicEnabled() ? "Mute" : "Unmute");
    }

    private static VBox section(String name, javafx.scene.Node... content) {
        VBox box = new VBox(6);
        box.getChildren().add(Styles.label(name, "rail-title"));
        box.getChildren().addAll(content);
        box.getStyleClass().add("rail-section");
        return box;
    }

    /**
     * Updates the rail.
     *
     * @param v content
     */
    public void render(View v) {
        updating = true;
        round.setText(v.round());
        title.setText(v.title());
        text.setText(v.text());
        endTurn.setText(v.endTurnLabel());
        endTurn.setDisable(!v.endTurnEnabled());
        pass.setVisible(v.passVisible());
        pass.setManaged(v.passVisible());
        hint.setDisable(!v.hintEnabled());
        renderDecks(v);
        renderDiscard(v);
        if (!logItems.equals(v.log())) {
            logItems.setAll(v.log());
            if (!logItems.isEmpty()) {
                log.scrollTo(logItems.size() - 1);
            }
        }
        aiBox.setVisible(v.showAi());
        aiBox.setManaged(v.showAi());
        reveal.setVisible(v.spectator());
        reveal.setManaged(v.spectator());
        pause.setSelected(v.paused());
        speed.setValue(GameSettings.getInstance().speed().ordinal());
        updating = false;
    }

    private void renderDecks(View v) {
        decks.getChildren().clear();
        for (Map.Entry<DeckType, Integer> e : v.decks().entrySet()) {
            DeckType type = e.getKey();
            int count = e.getValue();
            Pane stack = new Pane();
            stack.setPrefSize(58, 80);
            stack.setMinSize(58, 80);
            int layers = count == 0 ? 0 : Math.min(3, 1 + count / 12);
            for (int i = 0; i < layers; i++) {
                CardView back = CardView.back(48);
                back.relocate(2 + i * 2.5, 6 - i * 2.5);
                stack.getChildren().add(back);
            }
            if (count == 0) {
                Rectangle empty = new Rectangle(4, 4, 48, 67);
                empty.setArcWidth(8);
                empty.setArcHeight(8);
                empty.setFill(Color.rgb(59, 38, 20, 0.12));
                empty.setStroke(Color.rgb(59, 38, 20, 0.4));
                empty.getStrokeDashArray().addAll(5.0, 4.0);
                stack.getChildren().add(empty);
            }
            Text band = new Text(type.displayName());
            band.setFont(Font.font("Alegreya Sans", FontWeight.BOLD, 11));
            band.setFill(Color.WHITE);
            Rectangle bandBg = new Rectangle(52, 16, deckColor(type));
            bandBg.setArcWidth(6);
            bandBg.setArcHeight(6);
            StackPane label = new StackPane(bandBg, band);
            label.relocate(2, 56);
            stack.getChildren().add(label);
            Label countLabel = Styles.label(count + (count == 1 ? " card" : " cards"), "deck-count");
            VBox box = new VBox(2, stack, countLabel);
            box.setId("deck-" + type.name());
            box.setAlignment(Pos.CENTER);
            boolean drawable = v.drawable().contains(type);
            if (drawable) {
                stack.setEffect(new DropShadow(18, Palette.GOLD));
                box.setCursor(Cursor.HAND);
                Tooltip.install(box, new Tooltip("Draw from the " + type.displayName() + " deck and end your turn"));
            } else {
                Tooltip.install(box, new Tooltip(type.displayName() + " deck: " + count + " card(s) left"));
            }
            box.setOnMouseClicked(ev -> handler.deckClicked(type));
            decks.getChildren().add(box);
        }
    }

    private static Color deckColor(DeckType type) {
        return switch (type) {
            case CLAN -> Palette.EMERALD;
            case TACTIC -> Palette.TURQUOISE.darker();
            case EXPANSION -> Color.web("#B07D12");
        };
    }

    private void renderDiscard(View v) {
        discardTitle.setText("Discard pile (" + v.discard().size() + ")");
        discardSpot.getChildren().clear();
        if (v.discard().isEmpty()) {
            Rectangle empty = new Rectangle(58, 81);
            empty.setArcWidth(9);
            empty.setArcHeight(9);
            empty.setFill(Color.rgb(59, 38, 20, 0.1));
            empty.setStroke(Color.rgb(59, 38, 20, 0.4));
            empty.getStrokeDashArray().addAll(5.0, 4.0);
            discardSpot.getChildren().add(empty);
        } else {
            CardView top = new CardView(v.discard().get(v.discard().size() - 1), 58);
            if (v.discardTarget()) {
                top.setTargeted(true, true);
            }
            discardSpot.getChildren().add(top);
        }
        discardSpot.setEffect(v.discardTarget() ? new DropShadow(18, Color.web("#3FE0D0")) : null);
    }
}
