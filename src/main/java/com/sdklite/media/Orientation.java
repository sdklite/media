package com.sdklite.media;

/**
 * Defines an enumeration for screen orientation
 */
public enum Orientation {

    /**
     * Unknown orientation
     */
    UNDEFINED,

    /**
     * Portrait screen
     */
    PORTRAIT,

    /**
     * Landscape screen
     */
    LANDSCAPE;

    /**
     * Returns an orientation mapping the degree to
     *
     * @param degree
     * @return
     */
    public static Orientation valueOf(final int degree) {
        if (degree < 0) {
            return UNDEFINED;
        }

        final int angle = degree % 360;
        if ((angle >= 0 && angle <= 45) || (angle >= 135 && angle <= 225) || (angle >= 315 && angle <= 360)) {
            return PORTRAIT;
        }

        return LANDSCAPE;
    }
}
