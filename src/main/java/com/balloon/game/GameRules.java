package com.balloon.game;

import com.balloon.game.model.Balloon;
import java.util.List;

public interface GameRules {
    void onTick();                              //1초에 한번 호출(시간1초씩감소)
    void onPop(List<Balloon> balloons);         //정답처리
    void onMiss();                              //오답처리
    boolean isGameOver();                       //종료여부
}
