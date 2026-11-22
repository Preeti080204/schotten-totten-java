package com.schottentotten.view.screens;

import com.schottentotten.view.Assets;
import com.schottentotten.view.Styles;
import com.schottentotten.view.widgets.CoverImage;
import com.schottentotten.view.widgets.Ribbon;
import com.schottentotten.view.widgets.ScalingPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Shared layout of the secondary screens: the blurred Highlands panorama behind a parchment
 * panel designed at a fixed size and scaled to fit the window.
 */
final class Backdrop {

    private Backdrop() {
    }

    /**
     * Builds a screen whose content sits in a parchment panel under a title ribbon.
     *
     * @param title        ribbon text
     * @param content      panel content
     * @param panelWidth   panel width
     * @param designWidth  design width of the panel area
     * @param designHeight design height of the panel area
     * @return root pane
     */
    static StackPane framed(String title, Region content, double panelWidth, double designWidth,
                            double designHeight) {
        VBox panel = Styles.parchment(new VBox(content));
        panel.setPadding(new Insets(56, 40, 28, 40));
        panel.setPrefWidth(panelWidth);
        VBox.setVgrow(content, Priority.ALWAYS);
        Ribbon ribbon = new Ribbon(title, 30);
        ribbon.setViewOrder(-1);
        VBox column = new VBox(-34, ribbon, panel);
        column.setAlignment(Pos.TOP_CENTER);
        column.setMaxSize(panelWidth, designHeight - 40);
        VBox.setVgrow(panel, Priority.ALWAYS);
        return build(column, designWidth, designHeight);
    }

    /**
     * Builds the screen root.
     *
     * @param panel        content panel
     * @param designWidth  design width of the panel area
     * @param designHeight design height of the panel area
     * @return root pane
     */
    static StackPane build(Region panel, double designWidth, double designHeight) {
        CoverImage bg = new CoverImage(Assets.image("menu-landscape.jpg"), 0.6);
        bg.setEffect(new GaussianBlur(14));
        Region veil = new Region();
        veil.setStyle("-fx-background-color: rgba(20, 30, 15, 0.35);");
        StackPane holder = new StackPane(panel);
        ScalingPane scaling = new ScalingPane(holder, designWidth, designHeight);
        StackPane root = new StackPane(bg, veil, scaling);
        root.getStyleClass().add("screen");
        return root;
    }
}
