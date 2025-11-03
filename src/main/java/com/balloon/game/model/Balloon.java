package com.balloon.game.model;

import java.awt.Color;

/**
 * Balloon: 화면에 떠다니는 '풍선' 데이터(엔티티).
 * - 렌더러는 이 객체가 가진 상태만 읽어서 그려준다(단방향).
 * - 게임 로직(이동/소멸 판정 등)은 update() 등의 메서드로 수행한다.
 *
 *  핵심 개념
 *  1) 데이터(단어/좌표/반지름/종류)를 보관
 *  2) 매 프레임 update()로 y좌표를 살짝 올려 '떠오르는' 느낌
 *  3) 화면 밖으로 나갔는지 판정(isOffscreen)
 *  4) 색상/표시용 정보(getFillColor 등)
 */
public class Balloon {

    /** 풍선 종류: 게임 규칙과 연결 (RED=time, BLUE=balloon, GREEN=방해) */
    public enum Kind { RED, BLUE, GREEN }

    // ====== 표시/판정에 필요한 상태값 ======
    private String word;   // 풍선 안에 적힐 단어
    private float x;       // 중심 X 좌표 (픽셀)
    private float y;       // 중심 Y 좌표 (픽셀)
    private float radius;  // 반지름 (픽셀)
    private Kind kind;     // 풍선 종류(색/아이템 의미)

    // ====== 이동 관련 ======
    private float vy = -0.6f; // 기본 위로 이동 속도(음수: 위로 씩 올라감)

    /**
     * 생성자: 필수 상태를 모두 받아 초기화한다.
     */
    public Balloon(String word, float x, float y, float radius, Kind kind) {
        this.word = word;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.kind = kind;
    }

    // ====== 게임 루프에서 매 프레임 호출되어 '위로 이동'시키는 메서드 ======
    public void update() {
        y += vy;
    }

    // ====== 화면 밖으로 나갔는지(위로 완전히 사라졌는지) 판정 ======
    // - 패널 높이를 알고 있어야 하므로, 호출하는 쪽에서 height를 넘겨준다.
    public boolean isOffscreenTop(int panelHeight) {
        // 풍선의 가장 아래가 화면 맨 위보다 위(음수)로 올라갔다면 화면 밖
        return (y + radius) < 0;
    }

    // ====== 렌더링용 색상 반환 ======
    //  - 지금은 하드코딩 값을 반환(Theme와 동기화는 렌더러에서 처리)
    public Color getFillColor() {
        return switch (kind) {
            case RED   -> new Color(234, 84, 85);
            case BLUE  -> new Color(80, 156, 245);
            case GREEN -> new Color(75, 203, 148);
        };
    }

    // ====== 게터/세터 ======
    public String getWord() { return word; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getRadius() { return radius; }
    public Kind getKind() { return kind; }

    public void setWord(String word) { this.word = word; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setRadius(float radius) { this.radius = radius; }
    public void setKind(Kind kind) { this.kind = kind; }

    // 속도 조절이 필요할 때를 대비한 게터/세터
    public float getVy() { return vy; }
    public void setVy(float vy) { this.vy = vy; }
}
