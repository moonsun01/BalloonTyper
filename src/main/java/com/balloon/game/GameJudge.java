package com.balloon.game;

import com.balloon.game.model.Balloon;
import com.balloon.items.Item;
import com.balloon.items.ItemSpawner;
import com.balloon.items.ItemEffectApplier;

import java.util.List;

public class GameJudge {

    // 아이템, 상태
    private GameState state;
    private ItemSpawner spawner;
    private ItemEffectApplier applier;
    private ItemEffectApplier.TimeApi timeApi;
    private ItemEffectApplier.UiApi uiApi;
    private ItemEffectApplier.FieldApi fieldApi;

    public GameJudge() {}

    public GameJudge(GameState state,
                     ItemSpawner spawner,
                     ItemEffectApplier applier,
                     ItemEffectApplier.TimeApi timeApi,
                     ItemEffectApplier.UiApi uiApi,
                     ItemEffectApplier.FieldApi fieldApi) {
        this.state = state;
        this.spawner = spawner;
        this.applier = applier;
        this.timeApi = timeApi;
        this.uiApi = uiApi;
        this.fieldApi = fieldApi;
    }

    public void setState(GameState state) { this.state = state; }
    public void setSpawner(ItemSpawner spawner) { this.spawner = spawner; }
    public void setApplier(ItemEffectApplier applier) { this.applier = applier; }
    public void setTimeApi(ItemEffectApplier.TimeApi timeApi) { this.timeApi = timeApi; }
    public void setUiApi(ItemEffectApplier.UiApi uiApi) { this.uiApi = uiApi; }
    public void setFieldApi(ItemEffectApplier.FieldApi fieldApi) { this.fieldApi = fieldApi; }

    public Balloon findBestMatch(List<Balloon> balloons, String input) {
        if (balloons == null || balloons.isEmpty()) return null;
        if (input == null) return null;

        String s = input.trim();
        if (s.isEmpty()) return null;              // 빈 입력은 매칭X

        for (Balloon b : balloons) {
            if (b == null) continue;
            if (!b.isActive()) continue;

            String w = b.getWord();
            if (w == null) continue;

            if (w.equals(s)) {
                return b;                          // 중복단어 없음 → 즉시 반환
            }
        }
        return null;
    }

    public boolean submit(List<Balloon> balloons, String input, GameRules rules) {
        if (input == null || input.trim().isEmpty()) {
            return false;                          // life 감소 없이 무시
        }

        Balloon matched = findBestMatch(balloons, input);

        if (matched != null) {                     // 정답
            matched.pop();                         // 풍선 터뜨림

            // 아이템 드랍 & 적용 (1P용)
            if (spawner != null && applier != null && state != null) {
                Item item = spawner.rollOnPop1P();
                if (item != null) {
                    applier.apply1P(item, state, timeApi, uiApi, fieldApi);
                }
            }

            rules.onPop(balloons);                 // 규칙 처리(남은 풍선 0 → 레벨 클리어 등)
            return true;
        }

        rules.onMiss();                            // 오답 처리(오직 오답에서만 life 감소)
        return false;
    }
}
