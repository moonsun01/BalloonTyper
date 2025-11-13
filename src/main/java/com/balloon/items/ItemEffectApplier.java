package com.balloon.items;

import com.balloon.game.render.GameRuntime; // ← 너 지금 render에 둔 상태면 이 임포트 그대로
// 만약 GameRuntime을 game 최상단으로 옮겼다면:
// import com.balloon.game.GameRuntime;

public final class ItemEffectApplier {
    private ItemEffectApplier() {}

    public static void apply(Item item, GameRuntime rt) {
        // Item에 getKind()/getDurationSec()가 없다면 추가해줘
        switch (item.getKind()) {
            // 1P TIME
            case TIME_PLUS5:   rt.addTimeSeconds(+5);  break;
            case TIME_MINUS5:  rt.addTimeSeconds(-5);  break;

            // 1P/2P BALLOON
            case BAL_MY_P3:     rt.addMyBalloons(+3);   break;
            case BAL_MY_M3:     rt.addMyBalloons(-3);   break;
            case BAL_ENEMY_P3:  rt.addEnemyBalloons(+3); break;
            case BAL_ENEMY_M3:  rt.addEnemyBalloons(-3); break;

            // 2P TRICK
            case BLIND:   rt.applyBlindToOpponent(item.getDurationSec());   break;
            case REVERSE: rt.applyReverseToOpponent(item.getDurationSec()); break;
        }
    }
}
