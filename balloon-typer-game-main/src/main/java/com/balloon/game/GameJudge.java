package com.balloon.game;

import java.util.List;

public class GameJudge {

    //입력 단어와 일치하는 풍선 찾기, 여러 개면 y가 큰 풍선 우선 제거
    public Balloon findBestMatch(List<Balloon> balloons, String input) {
        if (balloons == null || balloons.isEmpty()) return null;    //풍선없으면 null
        if (input == null) return null;                             //입력이 null이면 매칭x

        for (Balloon b : balloons) {                                //풍선확인
            if (!b.isActive()) continue;                            //터진풍선제외
            if (b.getWord().equals(input)) {
                return b;
            }
        }
        return null;
    }

    //입력 판정, 맞으면pop 틀리면 onMiss
    public boolean submit(List<Balloon> balloons, String input, GameRules rules) {
        Balloon m = findBestMatch(balloons, input);
        if (m != null) {            //정답
            m.pop();                //풍선터짐
            rules.onPop(balloons);  //풍선있는지 확인->점수 계산->다음레벨
            return true;
        }
        else {                      //오답
            rules.onMiss();
            return false;
        }
    }

}
