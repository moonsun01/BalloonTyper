package com.balloon.items;

import com.balloon.game.model.Balloon;
import com.balloon.ui.skin.SecretItemSkin;

import java.util.Optional;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;


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

        // ★ 1) Balloon에 붙어 있는 카테고리 확인
        SecretItemSkin.ItemCategory cat = popped.getCategory();

        // ★ 1-1) 카테고리가 NONE이면(=검정 글씨) 아이템 없음
        if (cat == null || cat == SecretItemSkin.ItemCategory.NONE) {
            return Optional.empty();
        }

        // ★ 2) TIME / BALLOON 카테고리는 100% 드랍 (확률 체크 안 함)
        ItemKind kind = rollKindByCategory(cat);

        int x = Math.round(popped.getX());
        int y = Math.round(popped.getY());
        return Optional.of(new Item(kind, x, y));
    }

    private ItemKind rollKindByCategory(SecretItemSkin.ItemCategory category) {
        if (category == SecretItemSkin.ItemCategory.TIME) {
            // 빨간 글씨 → 시간 아이템(+5 또는 -3초)만
            return rollTimeKind();
        } else if (category == SecretItemSkin.ItemCategory.BALLOON) {
            // 파란 글씨 → 풍선 아이템(+2 또는 -2개)만
            return rollBalloonKind();
        }

        // 혹시 TRICK 같은 다른 카테고리를 쓸 일이 생기면 기존 로직 사용
        return rollKind();
    }

    /** 시간 계열 아이템만 뽑기: TIME_PLUS_5 또는 TIME_MINUS_3 */
    private ItemKind rollTimeKind() {
        int sum = wTimePlus5 + wTimeMinus3;
        if (sum <= 0) return ItemKind.TIME_PLUS_5;

        int r = rnd.nextInt(sum);
        if (r < wTimePlus5) return ItemKind.TIME_PLUS_5;
        return ItemKind.TIME_MINUS_5;
    }

    /** 풍선 계열 아이템만 뽑기: BALLOON_PLUS_2 또는 BALLOON_MINUS_2 */
    private ItemKind rollBalloonKind() {
        int sum = wBalloonPlus2 + wBalloonMinus2;
        if (sum <= 0) return ItemKind.BALLOON_PLUS_2;

        int r = rnd.nextInt(sum);
        if (r < wBalloonPlus2) return ItemKind.BALLOON_PLUS_2;
        return ItemKind.BALLOON_MINUS_2;
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
        acc += wTimeMinus3;     if (r < acc) return ItemKind.TIME_MINUS_5;
        acc += wBalloonPlus2;   if (r < acc) return ItemKind.BALLOON_PLUS_2;
        // 남은 케이스
        return ItemKind.BALLOON_MINUS_2;
    }

    // =====================[ 듀얼 모드 전용 카테고리 배치 ]=====================

    /**
     * 듀얼 모드에서:
     *  - 플레이어 1 풍선들: 파란(BALLOON) 2개 + 초록(TRICK) 2개
     *  - 플레이어 2 풍선들: 파란(BALLOON) 2개 + 초록(TRICK) 2개
     * 로 category를 미리 배치한다.
     */
    public void assignFixedCategoriesForVersus(List<Balloon> p1Balloons,
                                               List<Balloon> p2Balloons) {
        assignFixedCategoriesForOnePlayer(p1Balloons);
        assignFixedCategoriesForOnePlayer(p2Balloons);
    }

    /**
     * 한 플레이어 풍선 리스트에 대해
     *  - 파란(BALLOON) 카테고리 2개
     *  - 초록(TRICK) 카테고리 2개
     * 만 설정하고 나머지는 NONE 으로 만든다.
     */
    private void assignFixedCategoriesForOnePlayer(List<Balloon> balloons) {
        if (balloons == null || balloons.isEmpty()) return;

        // 전체 풍선을 섞어서 랜덤한 풍선에 아이템이 붙도록 함
        List<Balloon> shuffled = new ArrayList<>(balloons);
        Collections.shuffle(shuffled, rnd);

        // 일단 전부 NONE 으로 초기화(아이템 없는 풍선)
        for (Balloon b : shuffled) {
            b.setCategory(SecretItemSkin.ItemCategory.NONE);
        }

        int idx = 0;

        // 1) 파란 풍선 아이템(BALLOON) 2개
        for (int i = 0; i < 2 && idx < shuffled.size(); i++) {
            Balloon b = shuffled.get(idx++);
            b.setCategory(SecretItemSkin.ItemCategory.BALLOON);
        }

        // 2) 초록 트릭 아이템(TRICK) 2개
        for (int i = 0; i < 2 && idx < shuffled.size(); i++) {
            Balloon b = shuffled.get(idx++);
            b.setCategory(SecretItemSkin.ItemCategory.TRICK);
        }
    }

}
