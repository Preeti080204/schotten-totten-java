package com.schottentotten.view.screens;

import com.schottentotten.app.GameSettings;
import com.schottentotten.model.ClanCard;
import com.schottentotten.model.ClanColor;
import com.schottentotten.model.Difficulty;
import com.schottentotten.model.FirstPlayerRule;
import com.schottentotten.model.GameConfiguration;
import com.schottentotten.model.PlayerId;
import com.schottentotten.model.PlayerProfile;
import com.schottentotten.view.Assets;
import com.schottentotten.view.Palette;
import com.schottentotten.view.Screen;
import com.schottentotten.view.ScreenManager;
import com.schottentotten.view.SoundManager;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.Ribbon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
/**
 * New-game screen: players, variant, the explicit "Include expansion cards?" question, number
 * of rounds, first player and an optional seed.
 */
public final class SetupScreen implements Screen {

    private enum Mode { HOT_SEAT, VS_COMPUTER, COMPUTERS }

    private final ScreenManager sm;
    private final StackPane root;
    private final TextField name1 = new TextField();
    private final TextField name2 = new TextField();
    private final ToggleGroup modeGroup = new ToggleGroup();
    private final ToggleGroup diff1 = new ToggleGroup();
    private final ToggleGroup diff2 = new ToggleGroup();
    private final ToggleGroup variant = new ToggleGroup();
    private final ToggleGroup expansion = new ToggleGroup();
    private final ToggleGroup first = new ToggleGroup();
    private final TextField seed = new TextField();
    private final Label roundsLabel = new Label();
    private final Label roundsHint = Styles.label("", "hint-text");
    private final HBox difficulty1;
    private final HBox difficulty2;
    private final Label type1 = Styles.label("", "player-meta");
    private final Label type2 = Styles.label("", "player-meta");
    private int rounds = 1;

    /**
     * Builds the screen.
     *
     * @param sm screen manager
     */
    public SetupScreen(ScreenManager sm) {
        this.sm = sm;
        GameSettings settings = GameSettings.getInstance();
        String[] names = settings.lastPlayerNames();
        name1.setText(names[0]);
        name2.setText(names[1]);
        name1.setPromptText("Name of Player 1");
        name2.setPromptText("Name of Player 2");

        HBox modes = new HBox(14,
                tile("Two players\n(hot-seat)", modeGroup, Mode.HOT_SEAT, portraits(false, false)),
                tile("Against the\ncomputer", modeGroup, Mode.VS_COMPUTER, portraits(false, true)),
                tile("Watch two\ncomputers", modeGroup, Mode.COMPUTERS, portraits(true, true)));
        modes.setAlignment(Pos.CENTER_LEFT);

        difficulty1 = difficultyChooser(diff1, settings.defaultDifficulty());
        difficulty2 = difficultyChooser(diff2, settings.defaultDifficulty());
        HBox players = new HBox(18, playerCard(PlayerId.P1, name1, type1, difficulty1),
                playerCard(PlayerId.P2, name2, type2, difficulty2));

        HBox variants = new HBox(14,
                tile("Base game\n54 Clan cards, hands of 6", variant, Boolean.FALSE, null),
                tile("Tactic variant\n+10 Tactic cards, hands of 7", variant, Boolean.TRUE, null));
        HBox expansions = new HBox(14,
                tile("No", expansion, Boolean.FALSE, null),
                tile("Yes - Highland Muster\nScout, Stone Ward, Clan Exchange", expansion, Boolean.TRUE, null));
        HBox firsts = new HBox(14,
                tile("Player 1", first, FirstPlayerRule.PLAYER_ONE, null),
                tile("Player 2", first, FirstPlayerRule.PLAYER_TWO, null),
                tile("Random", first, FirstPlayerRule.RANDOM, null));

        Button minus = Styles.plaque("-", "plaque-small");
        Button plus = Styles.plaque("+", "plaque-small");
        roundsLabel.getStyleClass().add("subheading");
        roundsLabel.setMinWidth(150);
        roundsLabel.setAlignment(Pos.CENTER);
        minus.setOnAction(e -> setRounds(rounds - 1));
        plus.setOnAction(e -> setRounds(rounds + 1));
        HBox roundsBox = new HBox(10, minus, roundsLabel, plus, roundsHint);
        roundsBox.setAlignment(Pos.CENTER_LEFT);
        roundsHint.setMaxWidth(400);

        seed.setPromptText("random");
        seed.setPrefColumnCount(12);
        Label seedHint = Styles.label("Optional: the same seed always deals the same cards.", "hint-text");
        HBox seedBox = new HBox(12, seed, seedHint);
        seedBox.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane();
        grid.setHgap(22);
        grid.setVgap(14);
        ColumnConstraints c0 = new ColumnConstraints(190);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c0, c1);
        addRow(grid, 0, "Who is playing?", modes);
        addRow(grid, 1, "The clans", players);
        addRow(grid, 2, "Variant", variants);
        addRow(grid, 3, "Include expansion cards?", expansions);
        addRow(grid, 4, "Rounds", roundsBox);
        addRow(grid, 5, "Who starts?", firsts);
        addRow(grid, 6, "Seed", seedBox);

        Button back = Styles.plaque("Back");
        back.setOnAction(e -> sm.showMainMenu());
        back.setCancelButton(true);
        Button start = Styles.plaque("Start the Battle", "plaque-primary");
        start.setDefaultButton(true);
        start.setOnAction(e -> start());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox buttons = new HBox(16, back, gap, start);

        VBox panel = Styles.parchment(new VBox(16, grid, buttons));
        panel.setPadding(new Insets(56, 40, 28, 40));
        panel.setPrefWidth(1000);
        Ribbon ribbon = new Ribbon("Muster Your Clans", 30);
        VBox column = new VBox(-34, ribbon, panel);
        column.setAlignment(Pos.TOP_CENTER);
        ribbon.setViewOrder(-1);
        column.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        root = Backdrop.build(column, 1080, 860);

        modeGroup.selectedToggleProperty().addListener((o, a, b) -> refreshMode());
        select(modeGroup, Mode.VS_COMPUTER);
        select(variant, Boolean.FALSE);
        select(expansion, Boolean.FALSE);
        select(first, FirstPlayerRule.PLAYER_ONE);
        keepOneSelected(modeGroup);
        keepOneSelected(variant);
        keepOneSelected(expansion);
        keepOneSelected(first);
        keepOneSelected(diff1);
        keepOneSelected(diff2);
        setRounds(1);
    }

    private static void addRow(GridPane grid, int row, String title, Node content) {
        Label l = Styles.label(title, "subheading");
        l.setMinHeight(40);
        GridPane.setValignment(l, javafx.geometry.VPos.CENTER);
        grid.add(l, 0, row);
        grid.add(content, 1, row);
    }

    private ToggleButton tile(String text, ToggleGroup group, Object value, Node graphic) {
        ToggleButton t = new ToggleButton(text);
        t.getStyleClass().setAll("toggle-button", "choice-tile");
        t.setToggleGroup(group);
        t.setUserData(value);
        t.setWrapText(true);
        t.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        t.setMinWidth(150);
        if (graphic != null) {
            t.setGraphic(graphic);
        }
        t.setOnAction(e -> sm.sound().play(SoundManager.Sfx.CLICK));
        return t;
    }

    private static Node portraits(boolean leftComputer, boolean rightComputer) {
        HBox box = new HBox(-8, portrait(ClanColor.RED, Palette.PLAYER_ONE, leftComputer),
                portrait(ClanColor.BLUE, Palette.PLAYER_TWO, rightComputer));
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static Node portrait(ClanColor clan, Color ring, boolean computer) {
        Circle c = new Circle(22);
        c.setFill(new ImagePattern(Assets.cardArt(new ClanCard(clan, 5)), 0, -0.05, 1, 0.75, true));
        c.setStroke(ring);
        c.setStrokeWidth(3);
        c.setEffect(new DropShadow(4, Color.rgb(0, 0, 0, 0.5)));
        if (!computer) {
            return c;
        }
        Text badge = new Text("AI");
        badge.setFont(Font.font(Assets.BODY_FONT, FontWeight.BOLD, 11));
        badge.setFill(Color.WHITE);
        Circle dot = new Circle(10, Palette.TURQUOISE);
        dot.setStroke(Color.WHITE);
        StackPane tag = new StackPane(dot, badge);
        tag.setTranslateX(16);
        tag.setTranslateY(14);
        return new StackPane(c, tag);
    }

    private VBox playerCard(PlayerId p, TextField name, Label type, HBox difficulty) {
        Label title = Styles.label(p == PlayerId.P1 ? "Player 1 - bottom side" : "Player 2 - top side", "subheading");
        title.setStyle("-fx-text-fill: " + Palette.hex(Palette.player(p)) + ";");
        VBox card = new VBox(8, title, name, type, difficulty);
        card.getStyleClass().add("parchment-card");
        card.setPadding(new Insets(10, 14, 12, 14));
        card.setStyle("-fx-border-color: " + Palette.hex(Palette.player(p)) + "; -fx-border-width: 0 0 0 6;"
                + " -fx-border-radius: 11;");
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setMaxWidth(Double.MAX_VALUE);
        return card;
    }

    private HBox difficultyChooser(ToggleGroup group, Difficulty initial) {
        ToggleButton easy = new ToggleButton("Easy");
        ToggleButton hard = new ToggleButton("Hard");
        for (ToggleButton t : new ToggleButton[]{easy, hard}) {
            t.getStyleClass().setAll("toggle-button", "switch");
            t.setToggleGroup(group);
        }
        easy.setUserData(Difficulty.EASY);
        hard.setUserData(Difficulty.HARD);
        group.selectToggle(initial == Difficulty.HARD ? hard : easy);
        Label l = Styles.label("Difficulty", "player-meta");
        HBox box = new HBox(8, l, easy, hard);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static void select(ToggleGroup group, Object value) {
        group.getToggles().stream().filter(t -> value.equals(t.getUserData())).findFirst().ifPresent(group::selectToggle);
    }

    private static void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((o, oldT, newT) -> {
            if (newT == null && oldT != null) {
                group.selectToggle(oldT);
            }
        });
    }

    private Mode mode() {
        return (Mode) modeGroup.getSelectedToggle().getUserData();
    }

    private void refreshMode() {
        if (modeGroup.getSelectedToggle() == null) {
            return;
        }
        Mode m = mode();
        boolean c1 = m == Mode.COMPUTERS;
        boolean c2 = m != Mode.HOT_SEAT;
        type1.setText(c1 ? "Computer player" : "Human player");
        type2.setText(c2 ? "Computer player" : "Human player");
        show(difficulty1, c1);
        show(difficulty2, c2);
    }

    private static void show(Node n, boolean visible) {
        n.setVisible(visible);
        n.setManaged(true);
        n.setOpacity(visible ? 1 : 0);
    }

    private void setRounds(int n) {
        rounds = Math.max(1, Math.min(GameConfiguration.MAX_ROUNDS, n));
        roundsLabel.setText(rounds == 1 ? "Single round" : rounds + " rounds");
        roundsHint.setText(rounds == 1 ? "The first clan to win the border wins the game."
                : "Round winner scores 5 points, the loser 1 point per Stone. Highest total wins.");
    }

    private void start() {
        sm.sound().play(SoundManager.Sfx.CLICK);
        Mode m = mode();
        String n1 = name1.getText().isBlank() ? "Player 1" : name1.getText().strip();
        String n2 = name2.getText().isBlank() ? "Player 2" : name2.getText().strip();
        if (n1.equalsIgnoreCase(n2)) {
            n2 = n2 + " II";
        }
        Difficulty d1 = (Difficulty) diff1.getSelectedToggle().getUserData();
        Difficulty d2 = (Difficulty) diff2.getSelectedToggle().getUserData();
        PlayerProfile p1 = m == Mode.COMPUTERS ? PlayerProfile.virtual(n1, d1) : PlayerProfile.human(n1);
        PlayerProfile p2 = m == Mode.HOT_SEAT ? PlayerProfile.human(n2) : PlayerProfile.virtual(n2, d2);
        long s = parseSeed(seed.getText());
        GameConfiguration config = new GameConfiguration(p1, p2, (Boolean) variant.getSelectedToggle().getUserData(),
                (Boolean) expansion.getSelectedToggle().getUserData(), rounds,
                (FirstPlayerRule) first.getSelectedToggle().getUserData(), s);
        GameSettings.getInstance().setLastPlayerNames(n1, name2.getText().strip());
        sm.context().controller().startNewGame(config);
        sm.showGame();
    }

    private static long parseSeed(String text) {
        String t = text.strip();
        if (t.isEmpty()) {
            return System.nanoTime();
        }
        try {
            return Long.parseLong(t);
        } catch (NumberFormatException e) {
            return t.hashCode();
        }
    }

    @Override
    public Parent root() {
        return root;
    }
}
