package com.balloon.items;

import java.util.concurrent.atomic.AtomicLong;

/** 필드에 나타나는 경량 아이템 오브젝트 (이번 버전은 즉시 적용형) */
public class Item {
    private static final AtomicLong SEQ = new AtomicLong(1);

    private final long id;
    private final ItemKind kind;
    private final int x;
    private final int y;
    private boolean active = true;

    public Item(ItemKind kind, int x, int y) {
        this.id = SEQ.getAndIncrement();
        this.kind = kind;
        this.x = x;
        this.y = y;
    }

    public long getId() { return id; }
    public ItemKind getKind() { return kind; }
    public int getX() { return x; }
    public int getY() { return y; }
    public boolean isActive() { return active; }
    public void deactivate() { this.active = false; }
}
