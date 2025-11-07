package com.balloon.data;

import java.util.List;

public interface RankingRepository {
    void save(ScoreEntry entry);
    //점수 1개를 저정하는 기능 ScoreEntry 타입의 entry라는 데이터를 받아라
    List<ScoreEntry> loadAll();
    //저장된 점수를 모두 불러오는 기능 List에 들어갈 타입 결정
    List<ScoreEntry> topN(int n);
    //저장된 점수들 중 상위n개 보여줌
}
