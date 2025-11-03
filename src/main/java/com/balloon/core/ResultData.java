// [L1]  패키지 선언: core 하위에 결과 데이터 보관용 POJO
package com.balloon.core;                      // [L1]

// [L2]  결과 화면에 전달할 데이터를 담는 불변(immutable) 모델 클래스
public class ResultData {                      // [L2]
    // [L3]  최종 점수
    private final int score;                   // [L3]
    // [L4]  남은 시간(초)
    private final int timeLeftSec;             // [L4]
    // [L5]  정확도(0.0 ~ 1.0)
    private final double accuracy;             // [L5]
    // [L6]  맞힌 개수
    private final int correctCount;            // [L6]
    // [L7]  틀린 개수
    private final int wrongCount;              // [L7]

    // [L8]  모든 필드를 채우는 생성자
    public ResultData(int score, int timeLeftSec, double accuracy,
                      int correctCount, int wrongCount) { // [L8]
        this.score = score;                   // [L9]
        this.timeLeftSec = timeLeftSec;       // [L10]
        this.accuracy = accuracy;             // [L11]
        this.correctCount = correctCount;     // [L12]
        this.wrongCount = wrongCount;         // [L13]
    }                                         // [L14]

    // [L15]  getter들: 화면에서 읽을 수 있도록 공개
    public int getScore() {                   // [L15]
        return score;                         // [L16]
    }                                         // [L17]
    public int getTimeLeftSec() {             // [L18]
        return timeLeftSec;                   // [L19]
    }                                         // [L20]
    public double getAccuracy() {             // [L21]
        return accuracy;                      // [L22]
    }                                         // [L23]
    public int getCorrectCount() {            // [L24]
        return correctCount;                  // [L25]
    }                                         // [L26]
    public int getWrongCount() {              // [L27]
        return wrongCount;                    // [L28]
    }                                         // [L29]
}                                             // [L30]
