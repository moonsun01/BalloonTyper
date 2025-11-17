package com.balloon.game;

import java.util.*;

public class NonRepeatingWordProvider implements WordProvider {

    private final List<String> pool;
    private final Random rnd = new Random();
    private int index = 0;

    public NonRepeatingWordProvider(List<String> words) {
        // 중복 제거 + 공백/널 제거 + 순서 유지
        Set<String> unique = new LinkedHashSet<>();
        if (words != null) {
            for (String w : words) {
                if (w == null) continue;
                w = w.trim();
                if (w.isEmpty()) continue;
                unique.add(w);
            }
        }
        this.pool = new ArrayList<>(unique);

        if (pool.size() < 40) {
            System.err.println("[NonRepeatingWordProvider] 단어 수: " + pool.size());
        }

        reshuffle();
    }

    private void reshuffle() {
        if (!pool.isEmpty()) {
            Collections.shuffle(pool, rnd);
        }
        index = 0;
    }

    @Override
    public String nextWord() {
        if (pool.isEmpty()) return "empty";

        // 한 바퀴 다 쓰면 다시 섞어서 새 라운드
        if (index >= pool.size()) {
            reshuffle();
        }

        String word = pool.get(index);
        index++;
        return word;
    }
}
