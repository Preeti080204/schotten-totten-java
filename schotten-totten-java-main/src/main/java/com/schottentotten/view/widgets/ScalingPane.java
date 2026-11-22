package com.schottentotten.view.widgets;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Lays out its content at a fixed design size and scales it uniformly to fit the available
 * space, centred. The board therefore keeps its proportions and never clips, from small laptop
 * screens to large monitors.
 */
public final class ScalingPane extends Region {

    private final Node content;
    private final double designWidth;
    private final double designHeight;
    private final Scale scale = new Scale(1, 1, 0, 0);

    /**
     * Creates the pane.
     *
     * @param content      content laid out at the design size
     * @param designWidth  design width
     * @param designHeight design height
     */
    public ScalingPane(Region content, double designWidth, double designHeight) {
        this.content = content;
        this.designWidth = designWidth;
        this.designHeight = designHeight;
        content.setPrefSize(designWidth, designHeight);
        content.setMinSize(designWidth, designHeight);
        content.setMaxSize(designWidth, designHeight);
        content.getTransforms().add(scale);
        getChildren().add(content);
        setMinSize(0, 0);
    }

    /**
     * Returns the current scale factor.
     *
     * @return scale
     */
    public double scaleFactor() {
        return scale.getX();
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        double s = Math.max(0.2, Math.min(w / designWidth, h / designHeight));
        scale.setX(s);
        scale.setY(s);
        content.resizeRelocate((w - designWidth * s) / 2, (h - designHeight * s) / 2, designWidth, designHeight);
    }

    @Override
    protected double computePrefWidth(double height) {
        return designWidth;
    }

    @Override
    protected double computePrefHeight(double width) {
        return designHeight;
    }
}
