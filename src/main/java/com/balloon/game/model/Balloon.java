package com.balloon.game.model;

import java.awt.Color;
import java.util.Objects;

/**
 * 도메인 엔티티: Balloon (로직 + 렌더링 공용)
 * - 단어/좌표/반지름/종류/활성(active) 상태
 * - pop(), matchesExact(), tryPop() 등 로직 메서드 포함
 * - getFillColor(), update() 등 렌더링 보조 메서드 포함
 */
public class Balloon {
    private static int NEXT_ID = 1;

    private final int id;
    private String word;

    // 위치/표시
    private float x;
    private float y;
    private float radius = 24f;
    private float vy = -0.5f; // 위로 살짝 상승(렌더링 연출용)
    public enum Kind { RED, BLUE, GREEN }
    private Kind kind = Kind.RED;

    // 로직 상태
    private boolean active = true;

    public Balloon(String word, float x, float y, Kind kind) {
        this.id = NEXT_ID++;
        this.word = word;
        this.x = x;
        this.y = y;
        if (kind != null) this.kind = kind;
    }

    // ===== 로직 메서드 =====
    public boolean isActive() { return active; }

    /** 강제 팝 */
    public void pop() { this.active = false; }

    /** 입력과 정확 일치 (대소문자 무시) */
    public boolean matchesExact(String input) {
        if (!active) return false;
        if (input == null) return false;
        String s = input.trim();
        if (s.isEmpty() || word == null) return false;
        return word.equalsIgnoreCase(s);
    }

    /** 정확 일치 시 팝 처리 */
    public boolean tryPop(String input) {
        if (!isActive()) return false;
        if (matchesExact(input)) {
            pop();
            return true;
        }
        return false;
    }

    // ===== 렌더링 보조 =====
    public void update() {
        // 필요시 상승 연출 (UI 연동 상황에 따라 끄거나 수정)
        y += vy;
    }

    public boolean isOffscreen(int width, int height) {
        // 화면 상단을 벗어났는지 체크(필요 시 조건 조정)
        return (y + radius) < 0 || (y - radius) > height || (x + radius) < 0 || (x - radius) > width;
    }

    public Color getFillColor() {
        return switch (kind) {
            case RED   -> new Color(234, 84, 85);
            case BLUE  -> new Color(80, 156, 245);
            case GREEN -> new Color(75, 203, 148);
        };
    }

    // ===== 게터/세터 =====
    public int getId() { return id; }
    public String getWord() { return word; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getRadius() { return radius; }
    public Kind getKind() { return kind; }
    public float getVy() { return vy; }

    public void setWord(String word) { this.word = word; }
    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }
    public void setRadius(float radius) { this.radius = radius; }
    public void setKind(Kind kind) { this.kind = kind; }
    public void setVy(float vy) { this.vy = vy; }

    @Override
    public String toString() {
        return "Balloon{" +
                "id=" + id +
                ", word='" + word + '\'' +
                ", x=" + x +
                ", y=" + y +
                ", active=" + active +
                ", kind=" + kind +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Balloon)) return false;
        Balloon balloon = (Balloon) o;
        return id == balloon.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
