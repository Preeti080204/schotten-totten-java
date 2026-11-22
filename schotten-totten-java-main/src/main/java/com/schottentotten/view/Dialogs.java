package com.schottentotten.view;

import com.schottentotten.model.Card;
import com.schottentotten.view.widgets.CardView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Custom parchment dialogs (no native or default JavaFX dialogs are used).
 */
public final class Dialogs {

    /**
     * One option of a choice dialog.
     *
     * @param label   text
     * @param preview cards illustrated next to the text (may be empty)
     */
    public record Option(String label, List<Card> preview) {
        /**
         * Creates a text-only option.
         *
         * @param label text
         */
        public Option(String label) {
            this(label, List.of());
        }
    }

    private Dialogs() {
    }

    private static VBox panel(String title, String message, double width) {
        VBox box = Styles.parchment(new VBox(14));
        box.setPadding(new Insets(26, 32, 26, 32));
        box.setPrefWidth(width);
        box.setAlignment(Pos.TOP_CENTER);
        Label t = Styles.label(title, "heading");
        box.getChildren().add(t);
        if (message != null && !message.isEmpty()) {
            Label m = Styles.label(message, "body-text");
            m.setMaxWidth(width - 70);
            box.getChildren().add(m);
        }
        return box;
    }

    /**
     * Shows a confirmation dialog.
     *
     * @param sm      screen manager
     * @param title   title
     * @param message message
     * @param ok      confirm label
     * @param cancel  cancel label
     * @param onOk    action on confirmation
     */
    public static void confirm(ScreenManager sm, String title, String message, String ok, String cancel,
                               Runnable onOk) {
        VBox box = panel(title, message, 520);
        Button yes = Styles.plaque(ok, "plaque-primary");
        Button no = Styles.plaque(cancel);
        HBox buttons = new HBox(16, no, yes);
        buttons.setAlignment(Pos.CENTER);
        box.getChildren().add(buttons);
        ScreenManager.Overlay overlay = sm.openOverlay(box, true);
        yes.setDefaultButton(true);
        yes.setOnAction(e -> {
            sm.sound().play(SoundManager.Sfx.CLICK);
            overlay.close();
            onOk.run();
        });
        no.setCancelButton(true);
        no.setOnAction(e -> overlay.close());
        yes.requestFocus();
    }

    /**
     * Shows an information dialog.
     *
     * @param sm      screen manager
     * @param title   title
     * @param message message
     */
    public static void info(ScreenManager sm, String title, String message) {
        VBox box = panel(title, message, 520);
        Button ok = Styles.plaque("OK", "plaque-primary");
        box.getChildren().add(ok);
        ScreenManager.Overlay overlay = sm.openOverlay(box, true);
        ok.setDefaultButton(true);
        ok.setOnAction(e -> overlay.close());
        ok.requestFocus();
    }

    /**
     * Shows a text input dialog.
     *
     * @param sm       screen manager
     * @param title    title
     * @param message  prompt
     * @param initial  initial text
     * @param okLabel  confirm label
     * @param onResult receives the entered text
     */
    public static void input(ScreenManager sm, String title, String message, String initial, String okLabel,
                             Consumer<String> onResult) {
        VBox box = panel(title, message, 520);
        TextField field = new TextField(initial);
        field.setMaxWidth(420);
        Button ok = Styles.plaque(okLabel, "plaque-primary");
        Button cancel = Styles.plaque("Cancel");
        HBox buttons = new HBox(16, cancel, ok);
        buttons.setAlignment(Pos.CENTER);
        box.getChildren().addAll(field, buttons);
        ScreenManager.Overlay overlay = sm.openOverlay(box, true);
        ok.setDefaultButton(true);
        ok.setOnAction(e -> {
            String text = field.getText().strip();
            if (!text.isEmpty()) {
                overlay.close();
                onResult.accept(text);
            }
        });
        cancel.setOnAction(e -> overlay.close());
        field.requestFocus();
        field.selectAll();
    }

    /**
     * Shows a choice among options, optionally illustrated with cards.
     *
     * @param sm        screen manager
     * @param title     title
     * @param message   prompt
     * @param options   options
     * @param cancel    {@code true} to offer a Cancel button
     * @param onChoice  receives the chosen index
     * @param onCancel  run when cancelled (may be {@code null})
     */
    public static void choose(ScreenManager sm, String title, String message, List<Option> options, boolean cancel,
                              IntConsumer onChoice, Runnable onCancel) {
        boolean illustrated = options.stream().anyMatch(o -> !o.preview().isEmpty());
        VBox box = panel(title, message, illustrated ? 820 : 560);
        FlowPane grid = new FlowPane(14, 14);
        grid.setAlignment(Pos.CENTER);
        grid.setPrefWrapLength(illustrated ? 760 : 500);
        ScreenManager.Overlay[] overlay = new ScreenManager.Overlay[1];
        for (int i = 0; i < options.size(); i++) {
            Option o = options.get(i);
            Button b = new Button(o.label());
            b.getStyleClass().setAll("button", "choice-tile");
            b.setWrapText(true);
            if (!o.preview().isEmpty()) {
                HBox cards = new HBox(6);
                cards.setAlignment(Pos.CENTER);
                for (Card c : o.preview()) {
                    cards.getChildren().add(new CardView(c, o.preview().size() > 2 ? 58 : 70));
                }
                b.setGraphic(cards);
            } else {
                b.setMinWidth(220);
            }
            int index = i;
            b.setOnAction(e -> {
                sm.sound().play(SoundManager.Sfx.CLICK);
                overlay[0].setOnClose(null);
                overlay[0].close();
                onChoice.accept(index);
            });
            grid.getChildren().add(b);
        }
        Node content = grid;
        if (options.size() > 6 && illustrated) {
            ScrollPane scroll = new ScrollPane(grid);
            scroll.setFitToWidth(true);
            scroll.setPrefViewportHeight(430);
            scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            content = scroll;
        }
        box.getChildren().add(content);
        VBox.setVgrow(content, Priority.ALWAYS);
        if (cancel) {
            Button c = Styles.plaque("Cancel");
            box.getChildren().add(c);
            c.setOnAction(e -> overlay[0].close());
        }
        overlay[0] = sm.openOverlay(box, cancel);
        if (onCancel != null) {
            overlay[0].setOnClose(onCancel);
        }
        box.setMaxHeight(Region.USE_PREF_SIZE);
    }
}
