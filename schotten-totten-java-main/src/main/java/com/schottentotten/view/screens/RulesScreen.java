package com.schottentotten.view.screens;

import com.schottentotten.controller.RulesDigest;
import com.schottentotten.model.Card;
import com.schottentotten.model.CardCatalog;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ClanColor;
import com.schottentotten.model.ExpansionCard;
import com.schottentotten.model.TacticCard;
import com.schottentotten.model.TacticType;
import com.schottentotten.view.Screen;
import com.schottentotten.view.ScreenManager;
import com.schottentotten.view.SoundManager;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.CardView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rules and tutorial: the rules digest by section, illustrated with real card faces (a
 * formation gallery and the full list of Tactic and expansion cards).
 */
public final class RulesScreen implements Screen {

    private final ScreenManager sm;
    private final List<RulesDigest.Section> sections;
    private final CardCatalog catalog;
    private final VBox page = new VBox(16);
    private final ScrollPane scroll = new ScrollPane(page);
    private final ToggleGroup nav = new ToggleGroup();
    private final Parent root;

    /**
     * Builds the screen.
     *
     * @param sm           screen manager
     * @param returnToGame {@code true} to go back to the board when closed
     */
    public RulesScreen(ScreenManager sm, boolean returnToGame) {
        this.sm = sm;
        this.sections = sm.context().rules().sections();
        this.catalog = sm.context().engine().catalog();
        VBox menu = new VBox(6);
        menu.setPrefWidth(250);
        menu.setMinWidth(250);
        for (int i = 0; i < sections.size(); i++) {
            ToggleButton b = new ToggleButton(shortTitle(sections.get(i).title()));
            b.getStyleClass().setAll("toggle-button", "choice-tile", "section-tab");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setAlignment(Pos.CENTER_LEFT);
            b.setToggleGroup(nav);
            b.setUserData(i);
            menu.getChildren().add(b);
        }
        nav.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null) {
                nav.selectToggle(oldT);
            } else {
                sm.sound().play(SoundManager.Sfx.CLICK);
                showSection((Integer) newT.getUserData());
            }
        });
        page.setPadding(new Insets(4, 18, 12, 6));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(scroll, Priority.ALWAYS);
        HBox body = new HBox(24, menu, scroll);
        VBox.setVgrow(body, Priority.ALWAYS);

        Button back = Styles.plaque(returnToGame ? "Back to the Game" : "Back", "plaque-primary");
        back.setCancelButton(true);
        back.setOnAction(e -> sm.back(returnToGame));
        HBox footer = new HBox(back);
        footer.setAlignment(Pos.CENTER_RIGHT);
        VBox content = new VBox(18, body, footer);
        content.setPrefHeight(640);
        root = Backdrop.framed("Rules & Tutorial", content, 1140, 1220, 800);
        nav.selectToggle(nav.getToggles().get(0));
    }

    private static String shortTitle(String title) {
        int cut = title.indexOf(" (");
        String t = cut > 0 ? title.substring(0, cut) : title;
        int colon = t.indexOf(':');
        return colon > 0 ? t.substring(0, colon) : t;
    }

    private void showSection(int index) {
        RulesDigest.Section s = sections.get(index);
        page.getChildren().clear();
        page.getChildren().add(Styles.label(s.title(), "heading"));
        String title = s.title();
        if (title.equals("Tactic cards")) {
            page.getChildren().add(Styles.label("Ten Tactic cards, in three families. Elite Troops join a "
                    + "formation, Combat Modes change how a Stone is won, Ruses act once and are discarded.",
                    "body-text"));
            for (TacticType type : TacticType.values()) {
                Card card = CardCatalog.tacticCards().stream().filter(c -> c.type() == type).findFirst()
                        .map(c -> (Card) c).orElseThrow();
                page.getChildren().add(cardRow(card, type.displayName(), type.category().displayName()
                        + (type.copies() > 1 ? " - " + type.copies() + " copies" : ""), type.rulesText()));
            }
        } else if (title.equals("Expansion cards")) {
            page.getChildren().add(Styles.label("Original cards created for this edition. They are only shuffled in "
                    + "when you answer Yes to \"Include expansion cards?\" when setting up a game.", "body-text"));
            Map<String, ExpansionCard> unique = new LinkedHashMap<>();
            catalog.all().stream().filter(c -> c instanceof ExpansionCard).map(c -> (ExpansionCard) c)
                    .forEach(c -> unique.putIfAbsent(c.definition().key(), c));
            for (ExpansionCard c : unique.values()) {
                page.getChildren().add(cardRow(c, c.definition().displayName(), c.definition().category()
                        + " - " + c.definition().copies() + " copies", c.definition().rulesText()));
            }
        } else {
            page.getChildren().add(Styles.label(s.body(), "body-text"));
            if (title.startsWith("Formations")) {
                page.getChildren().add(formations());
            } else if (title.startsWith("Tactic variant")) {
                page.getChildren().add(gallery(CardCatalog.tacticCards(), 70));
            }
        }
        scroll.setVvalue(0);
    }

    private Node formations() {
        FlowPane grid = new FlowPane(26, 18);
        grid.setPadding(new Insets(8, 0, 0, 0));
        grid.getChildren().addAll(
                example("Color-Run", "Red 3-4-5", clan(ClanColor.RED, 3), clan(ClanColor.RED, 4), clan(ClanColor.RED, 5)),
                example("Three of a Kind", "8-8-8", clan(ClanColor.BLUE, 8), clan(ClanColor.GREEN, 8),
                        clan(ClanColor.PURPLE, 8)),
                example("Color", "three Yellow cards", clan(ClanColor.YELLOW, 2), clan(ClanColor.YELLOW, 6),
                        clan(ClanColor.YELLOW, 9)),
                example("Run", "6-7-8 of any colours", clan(ClanColor.ORANGE, 6), clan(ClanColor.GREEN, 7),
                        clan(ClanColor.BLUE, 8)),
                example("Sum", "anything else: 9+5+2 = 16", clan(ClanColor.PURPLE, 9), clan(ClanColor.RED, 5),
                        clan(ClanColor.ORANGE, 2)));
        return grid;
    }

    private static Card clan(ClanColor color, int value) {
        return new ClanCard(color, value);
    }

    private static Node example(String name, String detail, Card... cards) {
        HBox row = new HBox(5);
        row.setAlignment(Pos.CENTER);
        for (Card c : cards) {
            row.getChildren().add(new CardView(c, 64));
        }
        Label title = Styles.label(name, "subheading");
        Label hint = Styles.label(detail, "hint-text");
        VBox box = new VBox(4, row, title, hint);
        box.setAlignment(Pos.TOP_CENTER);
        box.setPrefWidth(230);
        title.setAlignment(Pos.CENTER);
        hint.setAlignment(Pos.CENTER);
        return box;
    }

    private static Node gallery(List<? extends Card> cards, double width) {
        FlowPane flow = new FlowPane(10, 10);
        cards.forEach(c -> flow.getChildren().add(new CardView(c, width)));
        return flow;
    }

    private static Node cardRow(Card card, String name, String family, String text) {
        CardView view = new CardView(card, 84);
        Label title = Styles.label(name, "subheading");
        Label kind = Styles.label(family, "hint-text");
        Label rules = Styles.label(text, "body-text");
        VBox info = new VBox(4, title, kind, rules);
        info.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(info, Priority.ALWAYS);
        HBox row = new HBox(18, view, info);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("parchment-card");
        row.setPadding(new Insets(10, 14, 10, 10));
        row.setMinHeight(Region.USE_PREF_SIZE);
        return row;
    }

    @Override
    public Parent root() {
        return root;
    }
}
