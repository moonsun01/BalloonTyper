package com.balloon.items;

import java.util.concurrent.ThreadLocalRandom;

/** 아이템 효과(Effect) 선택 규칙과 DTO */
public final class ItemRules {

    /** 적용 대상 */
    public enum Target { SELF, OPPONENT, NONE }

    /** UI/로그에서 쓸 문자열 키 */
    public static final class Keys {
        public static final String TIME_PLUS5   = "TIME_PLUS5";
        public static final String TIME_MINUS5  = "TIME_MINUS5";
        public static final String BAL_MY_P3    = "BAL_MY_P3";
        public static final String BAL_MY_M3    = "BAL_MY_M3";
        public static final String BAL_ENEMY_P3 = "BAL_ENEMY_P3";
        public static final String BAL_ENEMY_M3 = "BAL_ENEMY_M3";
        public static final String BLIND        = "BLIND";
        public static final String REVERSE      = "REVERSE";
        private Keys() {}
    }

    /** 실제로 게임에 적용할 데이터 묶음 */
    public static final class Effect {
        public final ItemKind kind;   // 어떤 아이템인지
        public final Target target;   // 누구에게 적용?
        public final int amount;      // +5s, -3개 같은 수치
        public final int durationSec; // BLIND/REVERSE 지속시간
        public final String key;      // UI 색상/아이콘 매핑용

        public Effect(ItemKind kind, Target target, int amount, int durationSec, String key) {
            this.kind = kind;
            this.target = target;
            this.amount = amount;
            this.durationSec = durationSec;
            this.key = key;
        }
    }

    private ItemRules() {}

    /* ------------------ 1P 규칙 ------------------ */

    public static Effect pickFor1P(ColorKey color) {
        switch (color) {
            case RED:   return pick1PTime();
            case BLUE:  return pick1PBalloon();
            default:    return none();
        }
    }

    private static Effect pick1PTime() {
        boolean plus = ThreadLocalRandom.current().nextBoolean();
        return plus
                ? new Effect(ItemKind.TIME_PLUS5,  Target.SELF, +5, 0, Keys.TIME_PLUS5)
                : new Effect(ItemKind.TIME_MINUS5, Target.SELF, -5, 0, Keys.TIME_MINUS5);
    }

    private static Effect pick1PBalloon() {
        boolean plus = ThreadLocalRandom.current().nextBoolean();
        return plus
                ? new Effect(ItemKind.BAL_MY_P3, Target.SELF, +3, 0, Keys.BAL_MY_P3)
                : new Effect(ItemKind.BAL_MY_M3, Target.SELF, -3, 0, Keys.BAL_MY_M3);
    }

    /* ------------------ 2P 규칙 ------------------ */

    public static Effect pickFor2P(ColorKey color) {
        switch (color) {
            case BLUE:  return pick2PBalloon();
            case GREEN: return pick2PTrick();
            default:    return none();
        }
    }

    private static Effect pick2PBalloon() {
        // 0~3 : 내+3 / 내-3 / 상대+3 / 상대-3
        switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0:  return new Effect(ItemKind.BAL_MY_P3,     Target.SELF,     +3, 0, Keys.BAL_MY_P3);
            case 1:  return new Effect(ItemKind.BAL_MY_M3,     Target.SELF,     -3, 0, Keys.BAL_MY_M3);
            case 2:  return new Effect(ItemKind.BAL_ENEMY_P3,  Target.OPPONENT, +3, 0, Keys.BAL_ENEMY_P3);
            default: return new Effect(ItemKind.BAL_ENEMY_M3,  Target.OPPONENT, -3, 0, Keys.BAL_ENEMY_M3);
        }
    }

    private static Effect pick2PTrick() {
        boolean blind = ThreadLocalRandom.current().nextBoolean();
        int dur = 5; // 기본 5초
        return blind
                ? new Effect(ItemKind.BLIND,   Target.OPPONENT, 0, dur, Keys.BLIND)
                : new Effect(ItemKind.REVERSE, Target.OPPONENT, 0, dur, Keys.REVERSE);
    }

    /* ------------------ 공통 ------------------ */
    private static Effect none() {
        return new Effect(ItemKind.NONE, Target.NONE, 0, 0, "NONE");
    }
}
