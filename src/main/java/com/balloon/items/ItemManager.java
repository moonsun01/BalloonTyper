package com.balloon.items;

import java.util.ArrayList;
import java.util.List;

import com.balloon.game.render.GameRuntime;

/**
 * 화면에 떠 있는 드랍 아이템을 관리하는 매니저
 */
public class ItemManager {

    /** 드랍 상태로 떠 있는 아이템 리스트 */
    private final List<LiveDrop> liveDrops = new ArrayList<>();

    /** 아이템 하나 드랍시키기 (색 카테고리 기준 확률 뽑기) */
    public void rollAndDrop(ItemKind.ColorKey colorKey) {
        Item item = ItemSpawner.roll(colorKey);
        if (item == null) return;

        // 드랍 수명(sec → ms 변환)
        liveDrops.add(new LiveDrop(item, item.lifeSec * 1000L));
    }

    /** 플레이어가 아이템을 먹었을 때 실제 효과 적용 */
    public void pickupAndApply(LiveDrop drop, GameRuntime rt) {
        ItemEffectApplier.apply(drop.item, rt);
        liveDrops.remove(drop);
    }

    /** 프레임 업데이트: 남은 수명(잔존 시간) 감소 */
    public void update(long deltaMs) {
        for (int i = liveDrops.size() - 1; i >= 0; i--) {
            LiveDrop d = liveDrops.get(i);
            d.lifeMs -= deltaMs;
            if (d.lifeMs <= 0) liveDrops.remove(i);
        }
    }

    /** 현재 화면 위 드랍 아이템들 반환 (UI에서 그림 그릴 때 사용) */
    public List<LiveDrop> getLiveDrops() {
        return liveDrops;
    }

    /** ------------ 내부 클래스: 하나의 드랍 아이템 ------------ */
    public static final class LiveDrop {
        public final Item item;   // 아이템 종류 데이터
        public long lifeMs;       // 밀리초 단위 잔존 시간

        public LiveDrop(Item item, long lifeMs) {
            this.item = item;
            this.lifeMs = lifeMs;
        }
    }
}
