package com.balloon.game.render;

import com.balloon.game.GameState;

/**
 * 아이템 효과 적용 여부를 눈에 확실하게 보기 위한 디버그 전용 Runtime
 * 실제 UI 반영은 안 하고 콘솔 출력 중심으로 확인용.
 */
public class DebugRuntime implements GameRuntime {

    private final GameState gs;

    public DebugRuntime(GameState gs) {
        this.gs = gs;
    }

    @Override
    public void addTimeSeconds(int delta) {
        int before = gs.getTimeLeft();
        gs.decreaseTime(); // 원래 구조에 맞게 1초 감소 함수가 있으니 조절
        System.out.println("[Item] TIME 변화 → " + delta + "초 / (before=" + before + ", after=" + gs.getTimeLeft() + ")");
    }

    @Override
    public void addMyBalloons(int delta) {
        System.out.println("[Item] MY 풍선 변화 → " + delta + "개");
    }

    @Override
    public void addEnemyBalloons(int delta) {
        System.out.println("[Item] ENEMY 풍선 변화 → " + delta + "개");
    }

    @Override
    public void applyBlindToOpponent(int durationSec) {
        System.out.println("[Item] BLIND 적용! (" + durationSec + "초)");
    }

    @Override
    public void applyReverseToOpponent(int durationSec) {
        System.out.println("[Item] REVERSE 적용! (" + durationSec + "초)");
    }
}
