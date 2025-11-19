// 결과 데이터를 보관하고 화면 간에 전달하는 단순 컨텍스트
package com.balloon.core;

public final class ResultContext {

    // 마지막 게임 결과 (ResultScreen에서 사용)
    private static ResultData lastResult;

    // 외부에서 객체를 만들지 못하게 막기
    private ResultContext() {
    }

    // 이미 만들어진 ResultData를 그대로 저장
    public static void set(ResultData data) {
        lastResult = data;
    }

    // 저장된 ResultData 읽기
    public static ResultData get() {
        return lastResult;
    }

    // 결과 비우기 (재시작 등에서 사용)
    public static void clear() {
        lastResult = null;
    }

    /**
     * 게임 종료 시 “원시 값들”만 넘기면
     * 정확도 계산 + ScoreCalculator로 점수 계산까지 한 뒤
     * ResultData를 만들어 lastResult에 저장한다.
     *
     * @param mode        게임 모드 (SINGLE / VERSUS)
     * @param timeLeftSec 남은 시간(초)
     * @param correctCount 맞힌 개수
     * @param wrongCount   틀린 개수
     */
    public static void setFromRaw(GameMode mode,
                                  int timeLeftSec,
                                  int correctCount,
                                  int wrongCount) {

        int totalTyped = correctCount + wrongCount;

        // 정확도(0.0 ~ 1.0)
        double accuracy = (totalTyped <= 0)
                ? 0.0
                : (double) correctCount / totalTyped;

        // ScoreCalculator로 최종 점수 계산
        int score = ScoreCalculator.calculate(
                mode,
                correctCount,
                totalTyped,
                timeLeftSec
        );

        // ResultData 생성자 순서에 맞게 생성
        ResultData data = new ResultData(
                score,
                timeLeftSec,
                accuracy,
                correctCount,
                wrongCount
        );

        lastResult = data;
    }
}
