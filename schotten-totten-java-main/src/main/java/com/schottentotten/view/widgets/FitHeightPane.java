package com.schottentotten.view.widgets;

import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

/**
 * Gives its content the full available space, and scales the content down uniformly when the
 * height drops below a minimum design height, so a tall column (the side rail) never clips on
 * short laptop screens while keeping its natural layout on larger ones.
 */
public final class FitHeightPane extends Region {

    private final Region content;
    private final double minDesignHeight;
    private final Scale scale = new Scale(1, 1, 0, 0);

    /**
     * Creates the pane.
     *
     * @param content         content
     * @param minDesignHeight height below which the content is scaled down
     */
    public FitHeightPane(Region content, double minDesignHeight) {
        this.content = content;
        this.minDesignHeight = minDesignHeight;
        content.getTransforms().add(scale);
        getChildren().add(content);
    }

    @Override
    protected void layoutChildren() {
        double h = getHeight();
        double s = Math.max(0.5, Math.min(1, h / minDesignHeight));
        scale.setX(s);
        scale.setY(s);
        content.resizeRelocate(0, 0, getWidth() / s, h / s);
    }

    @Override
    protected double computePrefWidth(double height) {
        return content.prefWidth(-1);
    }

    @Override
    protected double computeMinWidth(double height) {
        return content.minWidth(-1);
    }

    @Override
    protected double computeMaxWidth(double height) {
        return content.prefWidth(-1);
    }

    @Override
    protected double computePrefHeight(double width) {
        return minDesignHeight;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }
}
