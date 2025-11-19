// [L1] 패키지 선언: core 계층(전역 상태/라우팅/식별자 등) 모음
package com.balloon.core;

// [L4] 필요한 클래스 임포트: 속성 변경 이벤트를 구독/발행하기 위해 사용
import com.balloon.game.VersusGameRules;
import java.beans.PropertyChangeListener;           // [L5] 리스너 인터페이스
import java.beans.PropertyChangeSupport;            // [L6] 옵저버 패턴 도우미

// [★추가] 아이템 카테고리(enum) - Day8에서 사용한 SecretItemSkin과 동일 소스 경로
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;

/**
 * GameContext
 * - 플레이어 이름/선택 모드 등 전역 상태를 보관/브로드캐스트하는 싱글톤
 * - Day10: 현재 활성 아이템(종류/라벨/만료시각)을 전역으로 관리 (HUD에서 표시)
 */
public final class GameContext {

    // ====== [싱글톤 보일러플레이트] =========================================================

    private static volatile GameContext INSTANCE;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // ====== [기존 전역 상태] ===============================================================

    private String playerName = "";
    private GameMode mode = GameMode.UNSPECIFIED;

    // ====== [★추가: 활성 아이템 전역 상태] =================================================
    // - HUD에서 현재 아이템 배지를 그릴 때 사용
    // - activate/clear 시에만 바뀌고, 남은 시간 카운트 다운은 paint주기(repaint)로 처리
    private volatile ItemCategory activeItemCategory = null; // TIME, BALLOON, (기타)
    private volatile long activeItemExpiresAtMs = 0L;        // 이 시각(밀리초)까지 유효
    private volatile String activeItemLabel = null;          // "+5s", "-1" 등

    // Property 이름 상수(옵저버가 구독할 때 사용 가능)
    public static final String PROP_PLAYER_NAME = "playerName";
    public static final String PROP_MODE        = "mode";
    public static final String PROP_ACTIVE_ITEM = "activeItem"; // 활성 아이템 스냅샷

    // 활성 아이템 스냅샷(불변) – 이벤트 페이로드로 넘겨주기 위함(옵션)
    public static final class ActiveItemSnapshot {
        public final ItemCategory category;
        public final String label;
        public final long expiresAtMs;

        public ActiveItemSnapshot(ItemCategory category, String label, long expiresAtMs) {
            this.category = category;
            this.label = label;
            this.expiresAtMs = expiresAtMs;
        }

        @Override public String toString() {
            return "ActiveItemSnapshot{category=" + category +
                    ", label='" + label + '\'' +
                    ", expiresAtMs=" + expiresAtMs + '}';
        }
    }

    private GameContext() { /* 기본값은 필드 선언부에서 설정 */ }

    public static GameContext getInstance() {
        if (INSTANCE == null) {
            synchronized (GameContext.class) {
                if (INSTANCE == null) {
                    INSTANCE = new GameContext();
                }
            }
        }
        return INSTANCE;
    }

    // ====== [게터/세터: 기존 상태] ==========================================================

    public String getPlayerName() { return playerName; }

    public void setPlayerName(String newName) {
        String old = this.playerName;
        this.playerName = (newName == null) ? "" : newName.trim();
        if (!old.equals(this.playerName)) {
            pcs.firePropertyChange(PROP_PLAYER_NAME, old, this.playerName);
        }
    }

    public GameMode getMode() { return mode; }

    public void setMode(GameMode newMode) {
        GameMode old = this.mode;
        this.mode = (newMode == null) ? GameMode.UNSPECIFIED : newMode;
        if (old != this.mode) {
            pcs.firePropertyChange(PROP_MODE, old, this.mode);
        }
    }

    // ====== [★추가: 활성 아이템 API] ========================================================

    /** 활성 아이템 적용(없애려면 clearActiveItem 호출) */
    public synchronized void activateItem(ItemCategory category, String label, long durationMs) {
        if (category == null || durationMs <= 0) {
            clearActiveItem();
            return;
        }
        ActiveItemSnapshot oldSnap = getActiveItemSnapshot();
        this.activeItemCategory    = category;
        this.activeItemLabel       = (label == null || label.isBlank()) ? null : label.trim();
        this.activeItemExpiresAtMs = System.currentTimeMillis() + durationMs;
        ActiveItemSnapshot newSnap = getActiveItemSnapshot();
        pcs.firePropertyChange(PROP_ACTIVE_ITEM, oldSnap, newSnap);
    }

    /** 활성 아이템 제거 */
    public synchronized void clearActiveItem() {
        ActiveItemSnapshot oldSnap = getActiveItemSnapshot();
        this.activeItemCategory = null;
        this.activeItemLabel = null;
        this.activeItemExpiresAtMs = 0L;
        ActiveItemSnapshot newSnap = getActiveItemSnapshot();
        pcs.firePropertyChange(PROP_ACTIVE_ITEM, oldSnap, newSnap);
    }

    /** 현재 활성 아이템의 남은 시간(ms). 없으면 0 이하 */
    public long getActiveItemRemainingMs() {
        if (activeItemCategory == null || activeItemExpiresAtMs <= 0) return 0L;
        return activeItemExpiresAtMs - System.currentTimeMillis();
    }

    /** 활성 아이템 카테고리(만료 또는 없음이면 null) */
    public ItemCategory getActiveItemCategory() {
        if (getActiveItemRemainingMs() <= 0) return null;
        return activeItemCategory;
    }

    /** HUD 표시에 쓸 라벨(만료 또는 없음이면 null) */
    public String getActiveItemLabel() {
        if (getActiveItemRemainingMs() <= 0) return null;
        return activeItemLabel;
    }

    /** 스냅샷(옵저버/디버그 용) */
    public ActiveItemSnapshot getActiveItemSnapshot() {
        return new ActiveItemSnapshot(activeItemCategory, activeItemLabel, activeItemExpiresAtMs);
    }

    // ====== [유틸] =========================================================================

    /** 전체 초기화(타이틀 화면 복귀, 새 게임 전 초기화 등에 사용) */
    public void reset() {
        setPlayerName("");
        setMode(GameMode.UNSPECIFIED);
        clearActiveItem();
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        if (l != null) pcs.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        if (l != null) pcs.removePropertyChangeListener(l);
    }

    @Override
    public String toString() {
        return "GameContext{" +
                "playerName='" + playerName + '\'' +
                ", mode=" + mode +
                ", activeItem=" + getActiveItemSnapshot() +
                '}';
    }

    // ====== [지원 enum] ====================================================================

    public enum GameMode {
        UNSPECIFIED,
        SINGLE,
        VERSUS
    }

    public enum VersusWinner {
        NONE, P1, P2, DRAW
    }

    public static final class VersusSnapshot {
        public final int p1Score;
        public final int p2Score;
        public final double p1Accuracy;
        public final double p2Accuracy;
        public final boolean p1Cleared;
        public final boolean p2Cleared;
        public final VersusWinner winner;

        public VersusSnapshot(int p1Score,
                              int p2Score,
                              double p1Accuracy,
                              double p2Accuracy,
                              boolean p1Cleared,
                              boolean p2Cleared,
                              VersusWinner winner) {
            this.p1Score   = p1Score;
            this.p2Score   = p2Score;
            this.p1Accuracy = p1Accuracy;
            this.p2Accuracy = p2Accuracy;
            this.p1Cleared  = p1Cleared;
            this.p2Cleared  = p2Cleared;
            this.winner     = winner;
        }
    }

    private VersusSnapshot versusSnapshot;

    public VersusSnapshot getVersusSnapshot() {
        return versusSnapshot;
    }

    public void setVersusSnapshot(VersusSnapshot snapshot) {
        this.versusSnapshot = snapshot;
    }

    /**
     * VersusGameRules(런타임 룰/상태)에서
     * HUD/결과 화면에서 쓸 요약 스냅샷을 만들어 GameContext에 저장.
     *
     * - GamePanel에서 틱마다 호출해도 되고,
     *   결과 화면 들어가기 전에 한 번만 호출해도 됨.
     */
    public void updateVersusSnapshotFromRules(VersusGameRules rules) {
        if (rules == null) {
            this.versusSnapshot = null;
            return;
        }

        VersusGameRules.PlayerState p1 = rules.getP1();
        VersusGameRules.PlayerState p2 = rules.getP2();

        // VersusGameRules.Winner -> GameContext.VersusWinner 매핑
        VersusGameRules.Winner gw = rules.getWinner();
        VersusWinner vw;

        if (gw == VersusGameRules.Winner.P1) {
            vw = VersusWinner.P1;
        } else if (gw == VersusGameRules.Winner.P2) {
            vw = VersusWinner.P2;
        } else if (gw == VersusGameRules.Winner.DRAW) {
            vw = VersusWinner.DRAW;
        } else {
            vw = VersusWinner.NONE;
        }

        this.versusSnapshot = new VersusSnapshot(
                p1.getScore(),
                p2.getScore(),
                p1.getAccuracy(),   // 0.0 ~ 1.0
                p2.getAccuracy(),
                p1.isCleared(),
                p2.isCleared(),
                vw
        );
    }



}
