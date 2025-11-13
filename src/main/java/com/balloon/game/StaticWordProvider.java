package com.balloon.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class StaticWordProvider implements WordProvider {
    private final List<String> pool;
    private final Random rnd = new Random();            // 랜덤 뽑기

    public StaticWordProvider(List<String> words) {     // 중복 제거, null/공백 방지
        // 순서 유지 + 중복 제거
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
        System.out.println("단어 로딩 완료: " + pool.size() + "개 (중복 제거 후)");
    }

    @Override
    public String nextWord() {                  // 단어 비었을 경우
        if (pool.isEmpty())  return "empty";
        return pool.get(rnd.nextInt(pool.size()));
    }
}
