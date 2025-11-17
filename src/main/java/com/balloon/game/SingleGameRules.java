package com.balloon.game;
/*
1인용 규칙
 */

import com.balloon.game.GameRules;
import com.balloon.game.model.Balloon;
import java.util.List;

public class SingleGameRules implements GameRules {
    private final GameState state;

    public SingleGameRules(GameState state) {
        this.state = state;
    }

    @Override
    public void onTick() {           //1초감소
        state.decreaseTime();
    }

    @Override
    public void onPop(List<Balloon> balloons) {
        // 모든 풍선 터지면 레벨 클리어
        if (allCleared(balloons)) {
            state.addRemainingTimeAsScore();    // 남은시간 점수로 누적
            state.nextLevel();                  // 다음 레벨 시간 초기화
        }
    }

    @Override
    public void onMiss() {
        //오답일 때 life 감소
        state.loseLife();
    }

    @Override
    public boolean isGameOver() {
        return state.isGameOver();
    }

   /* @Override
    public int getRemainingTime() {     //남은시간
        return state.getTimeLeft();
    }
*/
    private boolean allCleared(List<Balloon> balloons) {    //풍선 다 터졌는지 확인
        for (Balloon b : balloons) {
            if (b.isActive()) return false;
        }
        return true;
    }
}
