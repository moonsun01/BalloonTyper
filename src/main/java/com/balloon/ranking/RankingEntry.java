package com.balloon.ranking;                         // 랭킹 한 줄을 담는 DTO가 위치할 패키지

// 한 기록(플레이 결과)을 보관하는 단순 데이터 클래스
public class RankingEntry {                           // 클래스 시작
    public final String name;                         // 플레이어 이름
    public final int score;                           // 점수
    public final double accuracy;                     // 정확도(0~100)
    public final int timeLeft;                        // 남은 시간(초)
    public final long epochMillis;                    // 저장 시각(정렬/표시용)

    public RankingEntry(// 생성자: 모든 필드 초기화
                        String name, //이름
                        int score, //점수
                        double accuracy, //정확도
                        int timeLeft, //남은 시간
                        long epochMillis //시간 (ms)

    ) {
        this.name = name;                             // 필드 대입
        this.score = score;                           // 필드 대입
        this.accuracy = accuracy;                     // 필드 대입
        this.timeLeft = timeLeft;                     // 필드 대입
        this.epochMillis = epochMillis;               // 필드 대입
    }                                                 // 생성자 끝
}                                                     // 클래스 끝
