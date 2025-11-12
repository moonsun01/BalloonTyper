package com.balloon.items;

import com.balloon.game.model.Balloon;

import java.util.Optional;
import java.util.Random;

/** 풍선 '터짐' 이벤트에서 아이템 드랍 여부/종류를 즉석 결정 */
public class ItemSpawner {
    private final Random rnd = new Random();

    /** 드랍 확률(0.0~1.0). 예: 0.25 = 25% */
    private double dropChance = 0.25;

    /** 가중치: 긍정 3, 부정 2 느낌. 합은 비율만 맞으면 됨 */
    private int wTimePlus5 = 3;
    private int wTimeMinus3 = 2;
    private int wBalloonPlus2 = 3;
    private int wBalloonMinus2 = 2;

    public ItemSpawner() {}

    public void setDropChance(double p) {
        this.dropChance = Math.max(0, Math.min(1, p));
    }

    public Optional<Item> maybeSpawnOnBalloonPop(Balloon popped) {
        if (popped == null) return Optional.empty();
        if (!rollDrop()) return Optional.empty();

        ItemKind kind = rollKind();
        int x = Math.round(popped.getX());
        int y = Math.round(popped.getY());
        return Optional.of(new Item(kind, x, y));
    }

    private boolean rollDrop() {
        return rnd.nextDouble() < dropChance;
    }

    private ItemKind rollKind() {
        int sum = wTimePlus5 + wTimeMinus3 + wBalloonPlus2 + wBalloonMinus2;
        if (sum <= 0) return ItemKind.TIME_PLUS_5;
        int r = rnd.nextInt(sum);
        int acc = 0;

        acc += wTimePlus5;      if (r < acc) return ItemKind.TIME_PLUS_5;
        acc += wTimeMinus3;     if (r < acc) return ItemKind.TIME_MINUS_3;
        acc += wBalloonPlus2;   if (r < acc) return ItemKind.BALLOON_PLUS_2;
        // 남은 케이스
        return ItemKind.BALLOON_MINUS_2;
    }
}
