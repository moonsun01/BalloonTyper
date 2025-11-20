package com.balloon.game;


public class VersusGameRules {

    // 승자
    public enum Winner {
        NONE, P1, P2, DRAW
    }

    // 한 플레이어의 런타임 상태
    public static class PlayerState {
        private int score;
        private int timeLeft;
        private boolean cleared;
        private boolean dead;

        private int typedTotal;
        private int typedCorrect;

        public int getScore()      { return score; }
        public int getTimeLeft()   { return timeLeft; }
        public boolean isCleared() { return cleared; }
        public boolean isDead()    { return dead; }

        public double getAccuracy() {
            if (typedTotal == 0) return 1.0;  // 아무것도 안 치면 100%
            return (double) typedCorrect / typedTotal;
        }

        // 내부 업데이트 메서드들

        /** 생성 시 초기 시간 설정 (지금은 듀얼에서 승패에는 사용 안 함) */
        void setInitialTime(int seconds) {
            this.timeLeft = Math.max(0, seconds);
        }

        /** 1초 틱마다 호출됨 (듀얼에서는 승패에 영향 X) */
        void tickTime() {}

        void markCleared() {
            this.cleared = true;
        }

        void markDead() {
            this.dead = true;
        }

        void onCorrectType(int scoreDelta) {
            typedTotal++;
            typedCorrect++;
            score += scoreDelta;
        }

        void onMissType() {
            typedTotal++;
        }
    }

    private final PlayerState p1 = new PlayerState();
    private final PlayerState p2 = new PlayerState();
    private Winner winner = Winner.NONE;

    // 블라인드/리버스 상태 (밀리초 기준)
    private long p1BlindUntilMs   = 0L;
    private long p2BlindUntilMs   = 0L;
    private long p1ReverseUntilMs = 0L;
    private long p2ReverseUntilMs = 0L;

    // 생성 시 각 플레이어의 시작 시간 설정
    public VersusGameRules(int initialTimeSeconds) {
        p1.setInitialTime(initialTimeSeconds);
        p2.setInitialTime(initialTimeSeconds);
    }

    public PlayerState getP1() { return p1; }
    public PlayerState getP2() { return p2; }
    public Winner getWinner()  { return winner; }

    public boolean isFinished() {
        return winner != Winner.NONE;
    }

    /** 1초 틱마다 호출 (타이머에서) */
    public void onTick() {
        if (isFinished()) return;

        // 시간 경과는 기록만 하고, 승패 판정에는 사용하지 않는다.
        p1.tickTime();
        p2.tickTime();

        // 시간만으로는 winner를 결정하지 않으므로 여기서 winner가 바뀌지 않는다.
        decideWinnerIfNeeded();
    }

    /**
     * @param playerIndex 1 또는 2
     * @param scoreDelta  정답 시 얻는 점수
     * @param allCleared  그 플레이어 필드가 전부 터졌는지 여부
     */
    public void onPop(int playerIndex, int scoreDelta, boolean allCleared) {
        PlayerState self = (playerIndex == 1) ? p1 : p2;

        if (isFinished() || self.isDead()) return;

        self.onCorrectType(scoreDelta);

        if (allCleared) {
            self.markCleared();
        }

        decideWinnerIfNeeded();
    }

    /** 오타 / 실패 입력 */
    public void onMiss(int playerIndex) {
        PlayerState self = (playerIndex == 1) ? p1 : p2;
        if (isFinished() || self.isDead()) return;
        self.onMissType();
    }

    // ================== 블라인드 / 리버스 관련 API ==================

    // 공격자(attackerRole)가 BLIND 아이템 사용 → 상대에게 durationSec 동안 블라인드
    public void applyBlindToOpponent(String attackerRole, int durationSec) {
        long until = System.currentTimeMillis() + durationSec * 1000L;
        if ("P1".equals(attackerRole)) {
            p2BlindUntilMs = Math.max(p2BlindUntilMs, until);
        } else if ("P2".equals(attackerRole)) {
            p1BlindUntilMs = Math.max(p1BlindUntilMs, until);
        }
    }

    // 공격자(attackerRole)가 REVERSE 아이템 사용 → 상대에게 durationSec 동안 리버스
    public void applyReverseToOpponent(String attackerRole, int durationSec) {
        long until = System.currentTimeMillis() + durationSec * 1000L;
        if ("P1".equals(attackerRole)) {
            p2ReverseUntilMs = Math.max(p2ReverseUntilMs, until);
        } else if ("P2".equals(attackerRole)) {
            p1ReverseUntilMs = Math.max(p1ReverseUntilMs, until);
        }
    }

    // role("P1"/"P2") 기준으로 블라인드 남은 시간(초)
    public int getBlindRemainingSecondsFor(String role) {
        long now = System.currentTimeMillis();
        long until;
        if ("P1".equals(role)) {
            until = p1BlindUntilMs;
        } else {
            until = p2BlindUntilMs;
        }
        long diff = until - now;
        if (diff <= 0) return 0;
        return (int) Math.ceil(diff / 1000.0);
    }

    // role("P1"/"P2") 기준으로 리버스 활성 여부
    public boolean isReverseActiveFor(String role) {
        long now = System.currentTimeMillis();
        long until;
        if ("P1".equals(role)) {
            until = p1ReverseUntilMs;
        } else {
            until = p2ReverseUntilMs;
        }
        return now < until;
    }

    // 승패 판정
    private void decideWinnerIfNeeded() {
        if (winner != Winner.NONE) return;

        // 1) 둘 다 올클리어 → 무승부
        if (p1.isCleared() && p2.isCleared()) {
            winner = Winner.DRAW;
            return;
        }

        // 2) 한쪽만 올클리어 → 즉시 승리
        if (p1.isCleared() && !p2.isCleared()) {
            winner = Winner.P1;
            return;
        }
        if (!p1.isCleared() && p2.isCleared()) {
            winner = Winner.P2;
            return;
        }
    }
}
