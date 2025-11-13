// src/main/java/com/balloon/game/GameRuntime.java
package com.balloon.game.render;

/** 플레이 중 상태에 아이템 효과를 적용하기 위한 최소 포트 */
public interface GameRuntime {
    void addTimeSeconds(int delta);
    void addMyBalloons(int delta);
    void addEnemyBalloons(int delta);
    void applyBlindToOpponent(int durationSec);
    void applyReverseToOpponent(int durationSec);
}
