// [L1] 패키지 선언: core 계층(전역 상태/라우팅/식별자 등) 모음
package com.balloon.core;

// [L4] 필요한 클래스 임포트: 속성 변경 이벤트를 구독/발행하기 위해 사용
import java.beans.PropertyChangeListener;           // [L5] 리스너 인터페이스
import java.beans.PropertyChangeSupport;            // [L6] 옵저버 패턴 도우미

// [L8] GameContext: 플레이어 이름/선택 모드 등 전역 상태를 보관/브로드캐스트하는 싱글톤
public final class GameContext {

    // [L11] 싱글톤 인스턴스 보관용(초기 지연 로딩, 멀티스레드 안전 위해 volatile)
    private static volatile GameContext INSTANCE;

    // [L14] 상태 변경을 구독자(화면/컴포넌트)에게 알리기 위한 헬퍼
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    // [L17] 전역 상태 1: 플레이어 이름(빈 문자열이면 미입력으로 간주)
    private String playerName = "";

    // [L20] 전역 상태 2: 게임 모드(싱글/버스/미지정)
    private GameMode mode = GameMode.UNSPECIFIED;

    // [L23] 외부에서 new 금지(싱글톤)
    private GameContext() {
        // [L25] 생성 시 기본값은 위 필드 선언부에서 이미 부여
    }

    // [L28] 싱글톤 인스턴스를 얻는 정적 메서드(더블 체크 락)
    public static GameContext getInstance() {
        // [L30] 1차 널 체크(성능상 이점, 동기화 비용 절감)
        if (INSTANCE == null) {
            // [L32] 동기화 블록 진입
            synchronized (GameContext.class) {
                // [L34] 2차 널 체크(다중 스레드 경합 대비)
                if (INSTANCE == null) {
                    // [L36] 실제 인스턴스 생성
                    INSTANCE = new GameContext();
                }
            }
        }
        // [L40] 준비된 인스턴스 반환
        return INSTANCE;
    }

    // [L43] 현재 플레이어 이름을 조회
    public String getPlayerName() {
        // [L45] null 방지: 항상 non-null 문자열 보장
        return playerName;
    }

    // [L48] 플레이어 이름을 설정(변경 사항을 구독자에게 알림)
    public void setPlayerName(String newName) {
        // [L50] 이전 값 백업(이벤트에 실어 보낼 용도)
        String old = this.playerName;
        // [L52] null 이 들어오면 빈 문자열로 정규화
        this.playerName = (newName == null) ? "" : newName.trim();
        // [L54] 값이 실제로 변했을 때만 이벤트 발행
        if (!old.equals(this.playerName)) {
            // [L56] "playerName" 프로퍼티가 old→new 로 바뀌었다고 알림
            pcs.firePropertyChange("playerName", old, this.playerName);
        }
    }

    // [L60] 현재 선택된 게임 모드를 조회
    public GameMode getMode() {
        // [L62] null 방지: 항상 유효한 enum 값 반환
        return mode;
    }

    // [L65] 게임 모드를 설정(변경 사항을 구독자에게 알림)
    public void setMode(GameMode newMode) {
        // [L67] 이전 값 백업
        GameMode old = this.mode;
        // [L69] null 이면 UNSPECIFIED 로 정규화
        this.mode = (newMode == null) ? GameMode.UNSPECIFIED : newMode;
        // [L71] 값이 실제로 변했을 때만 이벤트 발행
        if (old != this.mode) {
            // [L73] "mode" 프로퍼티 변경을 알림
            pcs.firePropertyChange("mode", old, this.mode);
        }
    }

    // [L77] 컨텍스트 전체 초기화(타이틀 화면으로 돌아가거나, 새 게임 시작 전 사용)
    public void reset() {
        // [L79] 필드 별로 set 호출해서 변경 이벤트도 함께 발행되도록 구성
        setPlayerName("");
        setMode(GameMode.UNSPECIFIED);
    }

    // [L83] 외부(UI, 라우터)가 프로퍼티 변경 이벤트를 구독할 때 사용
    public void addPropertyChangeListener(PropertyChangeListener l) {
        // [L85] 널 체크 후 등록
        if (l != null) pcs.addPropertyChangeListener(l);
    }

    // [L88] 더 이상 구독하지 않을 때 호출
    public void removePropertyChangeListener(PropertyChangeListener l) {
        // [L90] 널 체크 후 해제
        if (l != null) pcs.removePropertyChangeListener(l);
    }

    // [L93] 문자열 디버그용 출력(로그에서 현재 전역 상태를 한눈에 확인)
    @Override
    public String toString() {
        // [L95] 사람이 읽기 좋은 포맷으로 구성
        return "GameContext{" +
                "playerName='" + playerName + '\'' +
                ", mode=" + mode +
                '}';
    }

    // [L101] GameMode: 지원하는 게임 모드 집합
    public enum GameMode {
        // [L103] 아직 선택되지 않은 상태(초기값)
        UNSPECIFIED,
        // [L105] 싱글 플레이
        SINGLE,
        // [L107] 1:1 등 대전 모드(후속 과제 확장 여지)
        VERSUS
    }
}
