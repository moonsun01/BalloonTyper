package com.balloon.core;

/**
 * 게임 점수 계산 유틸리티.
 * 모든 점수 계산 규칙은 이 클래스 한 곳에서 관리한다.
 */
public final class ScoreCalculator {

    private ScoreCalculator() {
    }

    /**
     * 점수 계산.
     *
     * @param mode        게임 모드 (SINGLE / VERSUS)
     * @param correct     맞힌 개수
     * @param totalTyped  총 입력 수(맞힌 + 틀린)
     * @param timeValue   시간 관련 값(남은 시간 or 경과 시간 등, 규칙에 맞게 사용)
     * @return 최종 점수 (0 이상)
     */
    public static int calculate(GameMode mode,
                                int correct,
                                int totalTyped,
                                int timeValue) {

        int wrong = Math.max(0, totalTyped - correct);

        // 정확도(0.0 ~ 1.0)
        double accuracy = (totalTyped <= 0)
                ? 0.0
                : (double) correct / totalTyped;

        // 기본 점수: 맞힌 개수와 정확도를 반영
        int base = correct * 10;

        // 오타 패널티
        int penalty = wrong * 5;

        // 시간 보너스 예시: 값이 클수록 보너스 증가 (원하는 규칙으로 바꿔도 됨)
        int timeBonus = Math.max(0, timeValue);

        double raw = (base - penalty + timeBonus) * (0.5 + accuracy / 2.0);
        int score = (int) Math.round(raw);

        // VS 모드에 약간 가산점 (규칙에 따라 조정 가능)
        if (mode == GameMode.VERSUS) {
            score = (int) Math.round(score * 1.1);
        }

        return Math.max(score, 0);
    }
}
