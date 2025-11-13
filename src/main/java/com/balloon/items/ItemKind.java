package com.balloon.items;

/** 게임 아이템의 종류(그림 규칙 100% 반영) */
public enum ItemKind {
    // RED = TIME (1P)
    TIME_PLUS5,
    TIME_MINUS5,

    // BLUE = Balloon (1P / 2P)
    BAL_MY_P3,
    BAL_MY_M3,
    BAL_ENEMY_P3,
    BAL_ENEMY_M3,

    // GREEN = Trick (2P, 지속효과)
    BLIND,
    REVERSE,

    // ✅ 아이템 없음(기본 상태에서 필요)
    NONE;

    /** 색 카테고리 키(스폰 결정에 사용) */
    public enum ColorKey { RED, BLUE, GREEN }

    public static ColorKey colorOf(ItemKind k) {
        switch (k) {
            case TIME_PLUS5:
            case TIME_MINUS5:   return ColorKey.RED;
            case BAL_MY_P3:
            case BAL_MY_M3:
            case BAL_ENEMY_P3:
            case BAL_ENEMY_M3:  return ColorKey.BLUE;
            case BLIND:
            case REVERSE:       return ColorKey.GREEN;
            case NONE:          // ✅ 추가된 NONE 처리
                return null;    // NONE은 색이 없음
        }
        throw new IllegalArgumentException("unknown kind: " + k);
    }
}
