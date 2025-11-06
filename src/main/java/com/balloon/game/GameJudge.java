package com.balloon.game;

import com.balloon.game.model.Balloon;
import java.util.List;

public class GameJudge {

    /** 입력 단어와 일치하는 풍선 찾기 (정확 일치, 한글 기준) */
    public Balloon findBestMatch(List<Balloon> balloons, String input) {
        if (balloons == null || balloons.isEmpty()) return null;
        if (input == null || input.isEmpty()) return null;

        for (Balloon b : balloons) {
            if (!b.isActive()) continue;           // 이미 터진 풍선은 제외
            if (b.getWord().equals(input)) {       // 정확히 같은 단어면
                return b;                          // 바로 반환
            }
        }
        return null; // 못 찾으면 null
    }

    /** 입력 판정: 맞으면 pop, 틀리면 onMiss */
    public boolean submit(List<Balloon> balloons, String input, GameRules rules) {
        Balloon matched = findBestMatch(balloons, input);

        if (matched != null) {                     // 정답
            matched.pop();                         // 풍선 터뜨림
            rules.onPop(balloons);                 // 규칙 처리 (점수/다음레벨)
            return true;
        }

        rules.onMiss();                            // 오답
        return false;
    }
}
