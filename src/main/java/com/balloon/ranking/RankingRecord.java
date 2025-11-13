package com.balloon.ranking;

/**
 * RankingRecord
 * - ranking.csv의 "한 행"을 보관하는 순수 데이터 클래스.
 * - 화면(UI)과 저장소(CSV 리포지토리) 사이에서 데이터를 전달하는 역할만 한다.
 * - 불변(immutable)로 설계: 생성자에서 값이 정해지면 이후 변경하지 않는다.
 */
public final class RankingRecord {

    private int rank; //화면용 순위

    // 플레이어 이름(표시용)
    private final String name;

    // 최종 점수(정렬 우선순위 1순위: 내림차순)
    private final int score;

    // 정확도(%) 예: 93.5 (정렬 우선순위 2순위: 내림차순)
    private final double accuracy;

    // 남은 시간(초) 예: 12 (정렬 우선순위 3순위: 내림차순)
    private final int timeLeft;

    // 플레이한 시각(문자열 그대로 보관/표시; 정렬은 나중에 원하면 파서로 처리)
    private final String playedAt;

    /**
     * 모든 필드를 한 번에 설정하는 생성자.
     * CSV를 읽어 파싱한 값들을 그대로 넘겨서 객체를 만든다.
     */
    public RankingRecord(String name, int score, double accuracy, int timeLeft, String playedAt) {
        this.name = name;
        this.score = score;
        this.accuracy = accuracy;
        this.timeLeft = timeLeft;
        this.playedAt = playedAt;
    }

    // --- Getter들: JTable 모델이 getValueAt()에서 이 값들을 꺼내 쓴다. ---

    /** 플레이어 이름 반환 */
    public String getName() {
        return name;
    }

    /** 최종 점수 반환 */
    public int getScore() {
        return score;
    }

    /** 정확도(%) 반환 (소수점 포함 가능) */
    public double getAccuracy() {
        return accuracy;
    }

    /** 남은 시간(초) 반환 */
    public int getTimeLeft() {
        return timeLeft;
    }

    /** 플레이 시각(문자열) 반환 */
    public String getPlayedAt() {
        return playedAt;
    }
}
