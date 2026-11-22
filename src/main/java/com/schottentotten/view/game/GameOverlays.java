package com.schottentotten.view.game;

import com.schottentotten.model.Card;
import com.schottentotten.model.ClanColor;
import com.schottentotten.model.GameState;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.RoundResult;
import com.schottentotten.rules.EliteIdentity;
import com.schottentotten.view.Assets;
import com.schottentotten.view.Palette;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.CardView;
import com.schottentotten.view.widgets.Ribbon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builders of the board's modal panels: victory scroll, hot-seat curtain, discard viewer,
 * Elite Troop identity chooser and game menu.
 */
public final class GameOverlays {

    private GameOverlays() {
    }

    /**
     * Actions offered on the victory panel.
     *
     * @param viewBoard close the panel and look at the final border
     * @param nextRound start the next round ({@code null} when the match is over)
     * @param playAgain new game with the same settings
     * @param mainMenu  back to the main menu
     */
    public record VictoryActions(Runnable viewBoard, Runnable nextRound, Runnable playAgain, Runnable mainMenu) {
    }

    /**
     * Builds the victory panel of a finished round or match.
     *
     * @param s       final state
     * @param actions actions
     * @return panel
     */
    public static VBox victory(GameState s, VictoryActions actions) {
        RoundResult r = s.lastRoundResult().orElseThrow();
        boolean matchOver = actions.nextRound() == null;
        String headline;
        Color color;
        if (matchOver && s.config().isMultiRound()) {
            headline = s.match().winner().map(w -> s.config().profile(w).name() + " wins the match!")
                    .orElse("The match is a draw!");
            color = s.match().winner().map(Palette::player).orElse(Palette.BURGUNDY);
        } else if (r.winner() != null) {
            headline = s.config().profile(r.winner()).name() + (s.config().isMultiRound()
                    ? " wins round " + r.round() + "!" : " wins the border!");
            color = Palette.player(r.winner());
        } else {
            headline = "Round " + r.round() + " is a draw";
            color = Palette.BURGUNDY;
        }
        Ribbon ribbon = new Ribbon(matchOver ? "Victory" : "Round " + r.round() + " is over", 30, color);
        Text big = new Text(headline);
        big.setFont(Font.font(Assets.TITLE_FONT, 40));
        big.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, color.deriveColor(0, 1, 1.25, 1)), new Stop(1, color.darker())));
        big.setStroke(Color.web("#3B2614", 0.4));
        big.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.3)));
        Label reason = Styles.label(r.winner() == null ? capitalize(r.reason().description()) + "."
                : s.config().profile(r.winner()).name() + " " + r.reason().description() + ".", "body-text");
        reason.setStyle("-fx-font-size: 18px;");
        Label stones = Styles.label("Stones: " + s.config().player1().name() + " " + r.stonesP1() + "  -  "
                + s.config().player2().name() + " " + r.stonesP2(), "subheading");

        VBox box = Styles.parchment(new VBox(14));
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(30, 40, 28, 40));
        box.setPrefWidth(700);
        box.getChildren().addAll(ribbon, big, reason, stones);
        if (s.config().isMultiRound()) {
            box.getChildren().add(scoreTable(s));
        }
        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);
        Button view = Styles.plaque("View the Border");
        view.setOnAction(e -> actions.viewBoard().run());
        buttons.getChildren().add(view);
        if (!matchOver) {
            Button next = Styles.plaque("Next Round", "plaque-primary");
            next.setDefaultButton(true);
            next.setOnAction(e -> actions.nextRound().run());
            buttons.getChildren().add(next);
        } else {
            Button again = Styles.plaque("Play Again", "plaque-primary");
            again.setDefaultButton(true);
            again.setOnAction(e -> actions.playAgain().run());
            buttons.getChildren().add(again);
        }
        Button menu = Styles.plaque("Main Menu");
        menu.setOnAction(e -> actions.mainMenu().run());
        buttons.getChildren().add(menu);
        box.getChildren().add(buttons);
        return box;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static GridPane scoreTable(GameState s) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("score-table");
        grid.setHgap(6);
        grid.setVgap(2);
        grid.setAlignment(Pos.CENTER);
        grid.add(cell("Round", true), 0, 0);
        grid.add(cell(s.config().player1().name(), true), 1, 0);
        grid.add(cell(s.config().player2().name(), true), 2, 0);
        int row = 1;
        for (RoundResult r : s.match().results()) {
            grid.add(cell(String.valueOf(r.round()), false), 0, row);
            grid.add(cell(String.valueOf(r.points(PlayerId.P1)), false), 1, row);
            grid.add(cell(String.valueOf(r.points(PlayerId.P2)), false), 2, row);
            row++;
        }
        grid.add(cell("Total (" + s.match().results().size() + "/" + s.config().rounds() + ")", true), 0, row);
        grid.add(cell(String.valueOf(s.match().points(PlayerId.P1)), true), 1, row);
        grid.add(cell(String.valueOf(s.match().points(PlayerId.P2)), true), 2, row);
        return grid;
    }

    private static Label cell(String text, boolean bold) {
        Label l = Styles.label(text, "cell-label", bold ? "subheading" : "body-text");
        l.setMinWidth(90);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    /**
     * Builds the hot-seat curtain hiding the hands between two human players.
     *
     * @param name     name of the player to call
     * @param other    name of the player who must look away
     * @param onReveal action when the called player is ready
     * @return panel
     */
    public static VBox curtain(String name, String other, Runnable onReveal) {
        VBox box = Styles.parchment(new VBox(16));
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(34, 46, 30, 46));
        box.setPrefWidth(560);
        Ribbon ribbon = new Ribbon("Pass the cards", 28);
        CardView back = CardView.back(110);
        back.setRotate(-6);
        Label text = Styles.label(name + ", it is your turn.", "heading");
        Label sub = Styles.label(other + ", please look away while " + name + " plays.", "body-text");
        Button reveal = Styles.plaque("I am " + name + " - show my hand", "plaque-primary");
        reveal.setDefaultButton(true);
        reveal.setOnAction(e -> onReveal.run());
        box.getChildren().addAll(ribbon, back, text, sub, reveal);
        return box;
    }

    /**
     * Builds the discard pile viewer.
     *
     * @param cards   discarded cards, oldest first
     * @param onClose close action
     * @return panel
     */
    public static VBox discard(List<Card> cards, Runnable onClose) {
        VBox box = Styles.parchment(new VBox(14));
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(26, 32, 24, 32));
        box.setPrefWidth(820);
        box.getChildren().add(Styles.label("Discard pile", "heading"));
        if (cards.isEmpty()) {
            box.getChildren().add(Styles.label("No card has been discarded yet.", "body-text"));
        } else {
            FlowPane flow = new FlowPane(12, 12);
            flow.setAlignment(Pos.CENTER);
            flow.setPrefWrapLength(740);
            cards.forEach(c -> flow.getChildren().add(new CardView(c, 96)));
            ScrollPane scroll = new ScrollPane(flow);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportHeight(Math.min(440, 150.0 * ((cards.size() + 5) / 6)));
            box.getChildren().addAll(Styles.label("Oldest first. Played Ruses and discarded cards stay visible to "
                    + "both players.", "hint-text"), scroll);
        }
        Button close = Styles.plaque("Close", "plaque-primary");
        close.setDefaultButton(true);
        close.setOnAction(e -> onClose.run());
        box.getChildren().add(close);
        return box;
    }

    /**
     * Builds the Elite Troop identity chooser shown before a claim.
     *
     * @param stone     Stone number (1-based)
     * @param elites    the claimant's Elite Troops on that Stone
     * @param defaults  suggested identities (strongest formation)
     * @param onClaim   receives the chosen identities
     * @param onCancel  cancel action
     * @return panel
     */
    public static VBox identities(int stone, List<Card> elites, Map<String, EliteIdentity> defaults,
                                  Consumer<Map<String, EliteIdentity>> onClaim, Runnable onCancel) {
        VBox box = Styles.parchment(new VBox(14));
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(26, 32, 24, 32));
        box.setPrefWidth(760);
        box.getChildren().addAll(Styles.label("Claim Stone " + stone, "heading"),
                Styles.label("Choose the colour and value of each Elite Troop. The strongest choice is preselected.",
                        "body-text"));
        Map<String, ToggleGroup[]> groups = new LinkedHashMap<>();
        for (Card elite : elites) {
            EliteIdentity preset = defaults.get(elite.id());
            ToggleGroup colors = new ToggleGroup();
            ToggleGroup values = new ToggleGroup();
            FlowPane colorRow = new FlowPane(6, 6);
            for (ClanColor c : ClanColor.values()) {
                ToggleButton t = new ToggleButton();
                Circle dot = new Circle(11, Palette.clan(c));
                dot.setStroke(Color.WHITE);
                dot.setStrokeWidth(2);
                t.setGraphic(dot);
                t.setUserData(c);
                t.setToggleGroup(colors);
                t.getStyleClass().setAll("toggle-button", "choice-tile");
                t.setStyle("-fx-padding: 4 6 4 6;");
                t.setAccessibleText(c.displayName());
                colorRow.getChildren().add(t);
                if (preset != null && preset.color() == c) {
                    t.setSelected(true);
                }
            }
            FlowPane valueRow = new FlowPane(6, 6);
            elite.allowedStrengths().stream().sorted().forEach(v -> {
                ToggleButton t = new ToggleButton(String.valueOf(v));
                t.setUserData(v);
                t.setToggleGroup(values);
                t.getStyleClass().setAll("toggle-button", "choice-tile");
                t.setStyle("-fx-padding: 4 10 4 10; -fx-font-family: 'Cinzel Decorative';");
                valueRow.getChildren().add(t);
                if (preset != null && preset.strength() == v) {
                    t.setSelected(true);
                }
            });
            VBox pickers = new VBox(8, Styles.label(elite.displayName(), "subheading"), colorRow, valueRow);
            HBox row = new HBox(16, new CardView(elite, 80), pickers);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
            groups.put(elite.id(), new ToggleGroup[]{colors, values});
        }
        Label error = Styles.label("", "hint-text");
        Button cancel = Styles.plaque("Cancel");
        cancel.setOnAction(e -> onCancel.run());
        Button claim = Styles.plaque("Claim", "plaque-primary");
        claim.setDefaultButton(true);
        claim.setOnAction(e -> {
            Map<String, EliteIdentity> chosen = new LinkedHashMap<>();
            for (Map.Entry<String, ToggleGroup[]> g : groups.entrySet()) {
                if (g.getValue()[0].getSelectedToggle() == null || g.getValue()[1].getSelectedToggle() == null) {
                    error.setText("Choose a colour and a value for every Elite Troop.");
                    return;
                }
                chosen.put(g.getKey(), new EliteIdentity((ClanColor) g.getValue()[0].getSelectedToggle().getUserData(),
                        (Integer) g.getValue()[1].getSelectedToggle().getUserData()));
            }
            onClaim.accept(chosen);
        });
        HBox buttons = new HBox(14, cancel, claim);
        buttons.setAlignment(Pos.CENTER);
        box.getChildren().addAll(error, buttons);
        return box;
    }

    /**
     * Entries of the game menu.
     *
     * @param resume   close the menu
     * @param save     save the game
     * @param load     load a game
     * @param rules    rules and tutorial
     * @param settings settings
     * @param restart  new game with the same settings
     * @param quit     back to the main menu
     */
    public record MenuActions(Runnable resume, Runnable save, Runnable load, Runnable rules, Runnable settings,
                              Runnable restart, Runnable quit) {
    }

    /**
     * Builds the in-game menu.
     *
     * @param a actions
     * @return panel
     */
    public static VBox menu(MenuActions a) {
        VBox box = Styles.parchment(new VBox(10));
        box.setAlignment(Pos.TOP_CENTER);
        box.setPadding(new Insets(26, 40, 26, 40));
        box.setPrefWidth(400);
        box.getChildren().add(Styles.label("Game menu", "heading"));
        Object[][] entries = {{"Resume", a.resume(), "plaque-primary"}, {"Save Game", a.save(), ""},
                {"Load Game", a.load(), ""}, {"Rules & Tutorial", a.rules(), ""}, {"Settings", a.settings(), ""},
                {"Restart", a.restart(), ""}, {"Main Menu", a.quit(), ""}};
        for (Object[] entry : entries) {
            String extra = (String) entry[2];
            Button b = extra.isEmpty() ? Styles.plaque((String) entry[0]) : Styles.plaque((String) entry[0], extra);
            b.setMaxWidth(Double.MAX_VALUE);
            Runnable action = (Runnable) entry[1];
            b.setOnAction(e -> action.run());
            box.getChildren().add(b);
        }
        return box;
    }
}
