package com.balloon.game;

/**
 * VersusGameRules
 * - 듀얼 모드 한 판의 룰/상태/승패 판정을 담당
 * - 네트워크/UI랑은 분리된 순수 로직
 */
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

        void setInitialTime(int seconds) {
            this.timeLeft = Math.max(0, seconds);
        }

        void tickTime() {
            if (dead || cleared) return;
            timeLeft = Math.max(0, timeLeft - 1);
            if (timeLeft == 0) {
                dead = true;
            }
        }

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
        if (isFinished()) return; // 시간 0 이후 무한루프 방지

        p1.tickTime();
        p2.tickTime();
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

        // 3) 시간/목숨으로 종료되는 케이스
        boolean p1Dead = p1.isDead() || p1.getTimeLeft() <= 0;
        boolean p2Dead = p2.isDead() || p2.getTimeLeft() <= 0;

        if (!p1Dead && !p2Dead) return; // 둘 다 살아있으면 아직 끝 아님

        // 둘 다 동시에 죽음 → 점수/정확도로 승패 or 무승부
        if (p1Dead && p2Dead) {
            if (p1.getScore() > p2.getScore()) {
                winner = Winner.P1;
            } else if (p1.getScore() < p2.getScore()) {
                winner = Winner.P2;
            } else {
                double acc1 = p1.getAccuracy();
                double acc2 = p2.getAccuracy();
                if (acc1 > acc2) winner = Winner.P1;
                else if (acc1 < acc2) winner = Winner.P2;
                else winner = Winner.DRAW;
            }
            return;
        }

        // 4) 한쪽만 죽었으면 → 살아있는 쪽 승리
        if (p1Dead && !p2Dead) {
            winner = Winner.P2;
        } else if (!p1Dead && p2Dead) {
            winner = Winner.P1;
        }
    }
}
