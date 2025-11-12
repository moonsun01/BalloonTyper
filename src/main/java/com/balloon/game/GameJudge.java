package com.balloon.game;

import com.balloon.game.model.Balloon;
import com.balloon.items.ItemEffectApplier;
import com.balloon.items.ItemSpawner;

import java.text.Normalizer;
import java.util.List;

public class GameJudge {
    private final ItemSpawner spawner;               // null 허용
    private final ItemEffectApplier applier;         // null 허용

    public GameJudge(ItemSpawner spawner, ItemEffectApplier applier) {
        this.spawner = spawner;
        this.applier = applier;
    }

    private static String norm(String s) {
        if (s == null) return "";
        s = s.trim();
        return Normalizer.normalize(s, Normalizer.Form.NFC);
    }

    /** 입력 단어와 일치하는 풍선 찾기 (NFC + trim + equalsIgnoreCase) */
    public Balloon findBestMatch(List<Balloon> balloons, String input) {
        if (balloons == null || balloons.isEmpty()) return null;
        String needle = norm(input);
        if (needle.isEmpty()) return null;

        for (Balloon b : balloons) {
            if (!b.isActive()) continue;
            if (norm(b.getWord()).equalsIgnoreCase(needle)) {
                return b;
            }
        }
        return null;
    }

    /** 입력 판정: 맞으면 pop + (있으면)아이템 처리, 틀리면 onMiss */
    public boolean submit(List<Balloon> balloons, String input, GameRules rules) {
        Balloon matched = findBestMatch(balloons, input);

        if (matched != null) {
            matched.pop();

            // 아이템 드랍/적용 (연동된 경우에만)
            if (spawner != null && applier != null) {
                spawner.maybeSpawnOnBalloonPop(matched).ifPresent(applier::apply);
            }

            rules.onPop(balloons);
            return true;
        }
        rules.onMiss();
        return false;
    }
}
