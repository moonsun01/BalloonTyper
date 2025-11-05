package com.balloon.game;

import java.util.ArrayList;
import java.util.List;

public class LevelInitializer {
    public List<Balloon> createInitialBalloons(WordProvider provider, int panelWidth, int fixedY) {
        final int COUNT = 30;   //풍선개수
        final int margin = 24;  //화면 여백
        int usable = Math.max(1, panelWidth - margin * 2);  //단어 배치 가능한 너비
        int step = Math.max(1, usable / COUNT);     //풍선 간 간격

        List<Balloon> list = new ArrayList<>(COUNT);    //풍선 객체 리스트
        int x = margin;     //첫 풍선 시작위치
        for (int i = 0; i < COUNT; i++) {   //풍선 30개 생성 반복
            String w = provider.nextWord(); //단어 랜덤으로 가져오기
            //item 아직
            Balloon b = new Balloon(w, x, fixedY);
            list.add(b);    //풍선객체 리스트에 추가
            x += step;      //다음 풍선 x좌표 이동
        }
        return list;
    }
}
