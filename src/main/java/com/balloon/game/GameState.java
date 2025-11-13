package com.balloon.game;
/*
레벨, 생명, 남은시간, 점수 보관/갱신
레벨 전환, 시간초기화 수행
게임종료 판정
 */

public class GameState {
    private int level;
    private int life;
    private int timeLeft;
    private int totalScore;

    private final LevelConfig config;   //레벨별 초기 시간

    public GameState(LevelConfig config) {
        this.level = 1;                                     // 시작레벨 1
        this.totalScore = 0;                                // 시작 점수 0점
        this.life = 3;                                      // 시작 생명 3개
        this.config = config;
        this.timeLeft = config.getInitialTime(level);
    }

    //레벨
    public int getLevel() {return level;}

    public void nextLevel() {
        level++;
        timeLeft = config.getInitialTime(level);
    }

    //점수 (남은시간->점수 누적)
    public void addRemainingTimeAsScore() {
        totalScore += Math.max(0, timeLeft);            // 0이랑 timeleft중 큰 값 반환 후 더하기
    }
    public int getTotalScore() { return totalScore; }

    //생명
    public void loseLife() { if (life > 0) life--; }    // 오답 -> 생명-1
    public int getLife() { return life; }

    //시간
    public void decreaseTime() { if (timeLeft > 0) timeLeft--; }    // 1초씩 감소
    public int getTimeLeft() { return timeLeft; }                   // 남은시간

    //게임종료
    public boolean isGameOver() {
        boolean timeOut = (timeLeft <= 0);
        boolean noLife = (life <= 0);
        boolean clearedAll = (level > 3);
        return timeOut || noLife || clearedAll;
    }
    // com/balloon/game/GameState.java
    public void addTimeSeconds(int delta) {
        // 음수/양수 모두 허용, 0 미만은 0으로 바운딩
        timeLeft = Math.max(0, timeLeft + delta);
    }

}
