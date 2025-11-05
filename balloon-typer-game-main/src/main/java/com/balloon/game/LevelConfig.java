package com.balloon.game;

public class LevelConfig {

    public int getInitialTime(int level) {
        if (level == 1) return 90;
        if (level == 2) return 80;
        if (level == 3) return 70;
        return 70;
    }
}
