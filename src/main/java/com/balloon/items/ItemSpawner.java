package com.balloon.items;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** 색 카테고리에서 확률로 아이템 1개를 뽑아주는 스포너 */
public final class ItemSpawner {

    // 색 카테고리별 등장 후보 & 가중치
    private static final Map<ItemKind.ColorKey, List<Entry>> TABLE =
            new EnumMap<>(ItemKind.ColorKey.class);

    static {
        // RED = TIME (1P)
        put(ItemKind.ColorKey.RED,
                w(ItemKind.TIME_PLUS5, 70),
                w(ItemKind.TIME_MINUS5, 30));

        // BLUE = Balloon (1P & 2P)
        put(ItemKind.ColorKey.BLUE,
                w(ItemKind.BAL_MY_P3,    40),
                w(ItemKind.BAL_MY_M3,    10),
                w(ItemKind.BAL_ENEMY_P3, 30),
                w(ItemKind.BAL_ENEMY_M3, 20));

        // GREEN = Trick (2P, 지속효과)
        put(ItemKind.ColorKey.GREEN,
                w(ItemKind.BLIND,   55),
                w(ItemKind.REVERSE, 45));
    }

    private static final int DEFAULT_TRICK_DURATION_SEC = 5; // BLIND/REVERSE 지속
    private static final int DEFAULT_DROP_LIFE_SEC     = 6;  // 드랍 잔존시간

    private ItemSpawner() {}

    public static Item roll(ItemKind.ColorKey colorKey) {
        List<Entry> entries = TABLE.get(colorKey);
        if (entries == null || entries.isEmpty()) return null;

        int total = entries.stream().mapToInt(e -> e.weight).sum();
        int r = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (Entry e : entries) {
            acc += e.weight;
            if (r < acc) {

                // 2P 트릭 아이템은 durationSec 포함
                if (e.kind == ItemKind.BLIND || e.kind == ItemKind.REVERSE) {
                    return new Item(e.kind, DEFAULT_TRICK_DURATION_SEC, DEFAULT_DROP_LIFE_SEC);
                }

                // TIME/BALLOON: lifeSec만
                return new Item(e.kind, DEFAULT_DROP_LIFE_SEC);
            }
        }
        return null; // unreachable
    }

    // ---------- 내부 유틸 ----------
    private static void put(ItemKind.ColorKey k, Entry... list) {
        TABLE.put(k, Arrays.asList(list));
    }

    private static Entry w(ItemKind kind, int weight) {
        return new Entry(kind, weight);
    }

    private static final class Entry {
        final ItemKind kind;
        final int weight;
        Entry(ItemKind kind, int weight) {
            this.kind = kind;
            this.weight = weight;
        }
    }
}
