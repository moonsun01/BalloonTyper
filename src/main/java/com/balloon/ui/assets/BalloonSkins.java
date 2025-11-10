package com.balloon.ui.assets;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

import static com.balloon.ui.assets.ImageAssets.load;

public final class BalloonSkins {

    // 준비해둔 다섯 색 이름을 그대로 enum으로
    public enum Skin { GREEN, ORANGE, PINK, PURPLE, YELLOW }

    private static final Map<Skin, BufferedImage> cache = new EnumMap<>(Skin.class);

    /** 색상 스킨 PNG 로드(캐시) */
    public static BufferedImage of(Skin s) {
        return cache.computeIfAbsent(s, k -> switch (k) {
            case GREEN  -> load("balloon_green.png");
            case ORANGE -> load("balloon_orange.png");
            case PINK   -> load("balloon_pink.png");
            case PURPLE -> load("balloon_purple.png");
            case YELLOW -> load("balloon_yellow.png");
        });
    }
}
