package com.github.diplombmstu.httpstreamer;

import java.awt.image.BufferedImage;

/**
 * TODO add comment
 */
public interface FrameProvider
{
    BufferedImage getNextFrame();

    int getFrameWidth();

    int getFrameHeight();
}
