package com.balloon.game;

/**
 * 정확도(맞춘 글자/전체 입력)를 계산하고 점수 가중치를 산출.
 */
public final class ScoreCalculator {

    private ScoreCalculator() {}

    /** 0.0 ~ 1.0 로 정확도 반환 (total<=0면 1.0로 처리) */
    public static double accuracy(int correctChars, int totalChars) {
        if (totalChars <= 0) return 1.0;
        double acc = (double) correctChars / (double) totalChars;
        if (acc < 0) acc = 0;
        if (acc > 1) acc = 1;
        return acc;
    }

    /**
     * 정확도 기반 점수 가중치.
     * 예: base=100, acc=0.7 → 70점. (원하면 가중식을 바꿔도 됨)
     */
    public static int weightedScore(int baseScore, double accuracy) {
        double w = baseScore * accuracy;
        return (int)Math.round(w);
    }
}
