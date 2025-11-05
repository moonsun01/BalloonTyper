package game;

public class GameState {
    private int level;
    private int life;
    private int timeLeft;
    private int totalScore;

    // 레벨 시작시간? ()
    private int LV1_TIME = 90;
    private int LV2_TIME = 80;
    private int LV3_TIME = 60;

    public GameState(int life) {
        this.level = 1;
        this.totalScore = 0;
        this.life = life;
        this.timeLeft = LV1_TIME;
    }

    // 레벨
    public int getLevel() {
        return level;
    }

    public void nextLevel() {
        level++;
        if (level == 2) timeLeft = LV2_TIME;
        else if (level == 3) timeLeft = LV3_TIME;
    }

    // 점수(누적)
    public void addRemainingTimeAsScore() {
        totalScore += timeLeft*10;
    }

    public int getTotalScore() {
        return totalScore;
    }

    // 생명
    public void loseLife() {
        if (life > 0) life--;
    }

    public int getLife() {
        return life;
    }

    // 시간
    public void decreaseTime() {
        if (timeLeft > 0) timeLeft--;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    // 게임 종료
    public boolean isGameOver() {
        boolean timeOut = (timeLeft <= 0);
        boolean noLife = (life <= 0);
        boolean clearedAll = (level > 3);
        return timeOut || noLife || clearedAll;
    }

}
