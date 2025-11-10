package com.balloon.game.items;

import java.util.concurrent.ThreadLocalRandom;

/**
 * ItemRules
 * - 1P/2P 아이템 효과를 '랜덤 규칙'으로 결정하는 중앙 유틸.
 * - UI 의존 없음: UI는 Effect.itemKey만 받아 색상 칠하면 됨.
 */
public final class ItemRules {

    /** 게임 상태 갱신 시 분기용 타입 */
    public enum EffectType {
        TIME_PLUS5, TIME_MINUS5,          // 시간 +5 / -5 (1P, 2P 공용)
        BAL_SELF_P3, BAL_SELF_M3,         // 내 풍선 +3 / -3
        BAL_OPP_P3,  BAL_OPP_M3,          // 상대 풍선 +3 / -3 (2P)
        TRICK_BLIND, TRICK_REVERSE,       // 상대 BLIND/REVERSE (2P)
        NONE
    }

    /** 효과 적용 대상 */
    public enum Target { SELF, OPPONENT, GLOBAL, NONE }

    /** UI 스킨에서 쓰는 키 문자열(색 매핑용) */
    public static final class Keys {
        public static final String TIME_PLUS5     = "TIME_PLUS5";
        public static final String TIME_MINUS5    = "TIME_MINUS5";
        public static final String BAL_SELF_P3    = "BAL_SELF_P3";
        public static final String BAL_SELF_M3    = "BAL_SELF_M3";
        public static final String BAL_OPP_P3     = "BAL_OPP_P3";
        public static final String BAL_OPP_M3     = "BAL_OPP_M3";
        public static final String TRICK_BLIND    = "TRICK_BLIND";
        public static final String TRICK_REVERSE  = "TRICK_REVERSE";
        public static final String DEFAULT        = "DEFAULT";
        private Keys() {}
    }

    /** 규칙 적용 결과 DTO */
    public static final class Effect {
        public final EffectType type;     // 어떤 효과인지
        public final Target target;       // 누구에게 적용되는지
        public final int amount;          // +초/-초, +개/-개 (부호 포함)
        public final int durationSec;     // 지속시간(초) — BLIND/REVERSE 등에 사용
        public final String itemKey;      // UI 스킨 매핑용 키

        public Effect(EffectType type, Target target, int amount, int durationSec, String itemKey) {
            this.type = type;
            this.target = target;
            this.amount = amount;
            this.durationSec = durationSec;
            this.itemKey = itemKey;
        }
    }

    private ItemRules() {}

    // ----------------- 1P -----------------

    /** 1P: TIME — +5 또는 -5 랜덤 */
    public static Effect pickTimeFor1P() {
        boolean plus = ThreadLocalRandom.current().nextBoolean();
        return plus
                ? new Effect(EffectType.TIME_PLUS5,  Target.SELF, +5, 0, Keys.TIME_PLUS5)
                : new Effect(EffectType.TIME_MINUS5, Target.SELF, -5, 0, Keys.TIME_MINUS5);
    }

    /** 1P: BALLOON — +3 또는 -3 랜덤 */
    public static Effect pickBalloonFor1P() {
        boolean plus = ThreadLocalRandom.current().nextBoolean();
        return plus
                ? new Effect(EffectType.BAL_SELF_P3, Target.SELF, +3, 0, Keys.BAL_SELF_P3)
                : new Effect(EffectType.BAL_SELF_M3, Target.SELF, -3, 0, Keys.BAL_SELF_M3);
    }

    // ----------------- 2P -----------------

    /** 2P: BALLOON — 내±3 / 상대±3 4지선다 랜덤 */
    public static Effect pickBalloonFor2P() {
        switch (ThreadLocalRandom.current().nextInt(4)) { // 0~3
            case 0:  return new Effect(EffectType.BAL_SELF_P3, Target.SELF,     +3, 0, Keys.BAL_SELF_P3);
            case 1:  return new Effect(EffectType.BAL_SELF_M3, Target.SELF,     -3, 0, Keys.BAL_SELF_M3);
            case 2:  return new Effect(EffectType.BAL_OPP_P3,  Target.OPPONENT, +3, 0, Keys.BAL_OPP_P3);
            default: return new Effect(EffectType.BAL_OPP_M3,  Target.OPPONENT, -3, 0, Keys.BAL_OPP_M3);
        }
    }

    /** 2P: TRICK — BLIND 또는 REVERSE 랜덤(기본 5초) */
    public static Effect pickTrickFor2P() {
        boolean blind = ThreadLocalRandom.current().nextBoolean();
        int duration = 5;
        return blind
                ? new Effect(EffectType.TRICK_BLIND,   Target.OPPONENT, 0, duration, Keys.TRICK_BLIND)
                : new Effect(EffectType.TRICK_REVERSE, Target.OPPONENT, 0, duration, Keys.TRICK_REVERSE);
    }
}
