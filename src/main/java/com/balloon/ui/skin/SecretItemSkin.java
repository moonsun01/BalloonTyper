package com.balloon.ui.skin;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * SecretItemSkin
 * - 텍스트 색(카테고리) + 배경색(아이템 키) 매핑 유틸.
 * - Day 8: 정의만, Day 9에서 GamePanel에 연결.
 */
public final class SecretItemSkin {

    /** 텍스트 색을 정할 카테고리 */
    public enum ItemCategory { TIME, BALLOON, TRICK, NONE }

    private static final Map<ItemCategory, Color> CATEGORY_TEXT_COLOR;
    static {
        Map<ItemCategory, Color> m = new HashMap<>();
        m.put(ItemCategory.TIME,    new Color(0xE53935)); // 빨강
        m.put(ItemCategory.BALLOON, new Color(0x1E88E5)); // 파랑
        m.put(ItemCategory.TRICK,   new Color(0x2E7D32)); // 초록
        m.put(ItemCategory.NONE,    new Color(0x111111)); // 기본
        CATEGORY_TEXT_COLOR = Collections.unmodifiableMap(m);
    }

    public static Color textColorOf(ItemCategory c) {
        if (c == null) return CATEGORY_TEXT_COLOR.get(ItemCategory.NONE);
        return CATEGORY_TEXT_COLOR.getOrDefault(c, CATEGORY_TEXT_COLOR.get(ItemCategory.NONE));
    }

    // 아이템 키 → 배경색 (게임 로직의 Keys와 문자열만 공유)
    private static final Map<String, Color> ITEM_BG_COLOR;
    static {
        Map<String, Color> m = new HashMap<>();
        m.put("TIME_PLUS5",     new Color(0x66BB6A));
        m.put("TIME_MINUS5",    new Color(0xEF5350));
        m.put("BAL_SELF_P3",    new Color(0x42A5F5));
        m.put("BAL_SELF_M3",    new Color(0x7E57C2));
        m.put("BAL_OPP_P3",     new Color(0x90CAF9));
        m.put("BAL_OPP_M3",     new Color(0x9575CD));
        m.put("TRICK_BLIND",    new Color(0x2E7D32));
        m.put("TRICK_REVERSE",  new Color(0x00897B));
        m.put("DEFAULT",        new Color(0xB3E5FC));
        ITEM_BG_COLOR = Collections.unmodifiableMap(m);
    }

    public static Color backgroundOf(String itemKey) {
        if (itemKey == null) return ITEM_BG_COLOR.get("DEFAULT");
        return ITEM_BG_COLOR.getOrDefault(itemKey, ITEM_BG_COLOR.get("DEFAULT"));
    }

    /** 배경 대비로 흑/백 중 더 잘 보이는 텍스트 */
    public static Color bestTextColorFor(Color bg) {
        if (bg == null) return Color.BLACK;
        double y = 0.299*bg.getRed() + 0.587*bg.getGreen() + 0.114*bg.getBlue();
        return (y > 160) ? Color.BLACK : Color.WHITE;
    }

    private SecretItemSkin() {}
}
