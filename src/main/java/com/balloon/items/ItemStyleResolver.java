package com.balloon.items;

import java.awt.Color;

/** 아이템 라벨/색상 등 UI 표시용 간단 매핑 */
public final class ItemStyleResolver {
    private ItemStyleResolver() {}

    public static String label(ItemKind k) {
        switch (k) {
            case TIME_PLUS5:   return "TIME +5";
            case TIME_MINUS5:  return "TIME -5";
            case BAL_MY_P3:    return "My Balloon +3";
            case BAL_MY_M3:    return "My Balloon -3";
            case BAL_ENEMY_P3: return "Enemy Balloon +3";
            case BAL_ENEMY_M3: return "Enemy Balloon -3";
            case BLIND:        return "BLIND";
            case REVERSE:      return "REVERSE";
        }
        return k.name();
    }

    /** 배경색(그림 규칙: RED/TIME, BLUE/Balloon, GREEN/Trick) */
    public static Color background(ItemKind k) {
        switch (ItemKind.colorOf(k)) {
            case RED:   return new Color(0xF4,0x88,0x66); // 연한 빨강
            case BLUE:  return new Color(0xC8,0xD4,0xFF); // 연한 파랑
            case GREEN: return new Color(0xE7,0xF7,0xA9); // 연한 연두
            default:    return Color.LIGHT_GRAY;
        }
    }
}
