package com.balloon.ui.assets;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;

public final class ImageAssets {
    private ImageAssets() {}

    public static BufferedImage load(String name) {
        try {
            URL url = ImageAssets.class.getResource("/images/" + name);
            if (url == null) throw new IllegalArgumentException("Missing image: " + name);
            return ImageIO.read(url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load image: " + name, e);
        }
    }
}
