// src/main/java/com/balloon/game/render/UiRuntime.java
package com.balloon.game.render;

import com.balloon.game.render.GameRuntime;    // 인터페이스 (네가 만든 위치 기준으로 import)
import com.balloon.game.GameState;

public class UiRuntime implements GameRuntime {

    private final GameState gs;

    public UiRuntime(GameState gs) {
        this.gs = gs;
    }

    @Override
    public void addTimeSeconds(int delta) {
        // TIME +5 / -5 같은 효과가 실제 남은시간에 반영됨
        gs.addTimeSeconds(delta);
        System.out.println("[Item] time += " + delta + " -> now=" + gs.getTimeLeft());
    }

    @Override
    public void addMyBalloons(int delta) {
        // 현재 GameState에는 '풍선 개수' 개념이 없으므로 일단 no-op.
        // 추후 네가 풍선 수/난이도 조절 지점(예: LevelInitializer, WordProvider 등)
        // 을 정하면 여기에 반영 로직을 연결하면 됨.
        System.out.println("[Item] my balloons " + (delta>=0?"+":"") + delta + " (no-op for now)");
    }

    @Override
    public void addEnemyBalloons(int delta) {
        // 2P 모드가 생기면 상대 쪽에 반영. 지금은 싱글이라 no-op.
        System.out.println("[Item] enemy balloons " + (delta>=0?"+":"") + delta + " (no-op for now)");
    }

    @Override
    public void applyBlindToOpponent(int durationSec) {
        // 1P에선 시각효과가 없다면 no-op. 2P/효과 UI가 생기면 여기서 플래그 on/off.
        System.out.println("[Item] BLIND " + durationSec + "s (no-op for now)");
    }

    @Override
    public void applyReverseToOpponent(int durationSec) {
        // 1P에선 no-op. 2P/키 반전 구현 시 여기서 토글.
        System.out.println("[Item] REVERSE " + durationSec + "s (no-op for now)");
    }
}
