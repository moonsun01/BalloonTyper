package com.balloon.items;

public class Item {
    public final ItemKind kind;      // 아이템 종류
    public final int durationSec;    // BLIND/REVERSE 지속시간(초). 그 외는 0
    public final int lifeSec;        // 드랍이 화면에 남는 시간(초). 즉시형은 0

    // 기본(모두 0)
    public Item(ItemKind kind) {
        this(kind, 0, 0);
    }

    // TIME/BALLOON: (종류, 드랍수명)
    public Item(ItemKind kind, int lifeSec) {
        this(kind, 0, lifeSec);
    }

    // BLIND/REVERSE: (종류, 지속시간, 드랍수명)  ← 이 3-인자 생성자가 꼭 있어야 함!
    public Item(ItemKind kind, int durationSec, int lifeSec) {
        this.kind = kind;
        this.durationSec = Math.max(0, durationSec);
        this.lifeSec = Math.max(0, lifeSec);
    }

    public ItemKind getKind()   { return kind; }
    public int getDurationSec() { return durationSec; }
    public int getLifeSec()     { return lifeSec; }
}
