package com.schottentotten.view.widgets;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;

/**
 * Shows an image that always covers the whole region (like CSS {@code background-size: cover}),
 * cropping the excess instead of stretching.
 */
public final class CoverImage extends Region {

    private final ImageView view;
    private final double focusY;

    /**
     * Creates the region.
     *
     * @param image  image
     * @param focusY vertical focus between 0 (keep the top) and 1 (keep the bottom)
     */
    public CoverImage(Image image, double focusY) {
        this.view = new ImageView(image);
        this.focusY = focusY;
        view.setSmooth(true);
        getChildren().add(view);
        setMinSize(0, 0);
    }

    /**
     * Returns the inner image view (for animations).
     *
     * @return image view
     */
    public ImageView imageView() {
        return view;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        Image img = view.getImage();
        if (w <= 0 || h <= 0 || img.getWidth() <= 0) {
            return;
        }
        double iw = img.getWidth();
        double ih = img.getHeight();
        double target = w / h;
        if (iw / ih > target) {
            double vw = ih * target;
            view.setViewport(new Rectangle2D((iw - vw) / 2, 0, vw, ih));
        } else {
            double vh = iw / target;
            view.setViewport(new Rectangle2D(0, (ih - vh) * focusY, iw, vh));
        }
        view.setFitWidth(w);
        view.setFitHeight(h);
        view.relocate(0, 0);
    }
}
