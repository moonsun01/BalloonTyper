package com.balloon.data;

import com.balloon.core.GameMode;

public class ScoreEntry {

    private final String name;
    private final int score;
    private final GameMode mode;   // SINGLE / VERSUS

    // 새 생성자: name, score, mode
    public ScoreEntry(String name, int score, GameMode mode) {
        this.name = name;
        this.score = score;
        this.mode = mode;
    }

    // 예전 코드 호환용: mode 안 주면 SINGLE로 처리
    public ScoreEntry(String name, int score) {
        this(name, score, GameMode.SINGLE);
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public GameMode getMode() {
        return mode;
    }
}
