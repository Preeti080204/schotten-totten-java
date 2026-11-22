package com.schottentotten.view;

import javafx.scene.Parent;

/**
 * A full-window screen of the application (main menu, set-up, board...).
 */
public interface Screen {

    /**
     * Returns the root node of the screen.
     *
     * @return root
     */
    Parent root();

    /**
     * Called when the screen becomes visible.
     */
    default void onShow() {
    }

    /**
     * Called when the screen is replaced by another one.
     */
    default void onHide() {
    }
}
