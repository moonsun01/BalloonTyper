package com.balloon.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class StaticWordProvider implements WordProvider {

    public enum Role { P1, P2 }

    private final List<String> pool;
    private final Role role;
    private int index = 0;        // 내가 몇 번째 단어까지 썼는지

    public StaticWordProvider(List<String> words, Role role) {
        this.role = (role != null) ? role : Role.P1;

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
        System.out.println("단어 로딩 완료(" + role + "): " + pool.size() + "개 (중복 제거 후)");
    }

    @Override
    public String nextWord() {
        if (pool.isEmpty()) return "empty";

        // 역할에 따라 서로 다른 인덱스 사용: P1은 0,2,4,... / P2는 1,3,5,...
        int offset = (role == Role.P1) ? 0 : 1;
        int realIndex = offset + index * 2;

        // 범위를 넘으면 순환
        realIndex = realIndex % pool.size();

        String word = pool.get(realIndex);
        index++;

        // 한 역할이 실제로 쓸 수 있는 단어 개수(대략 절반)
        int maxSteps = (pool.size() + 1) / 2;
        if (index >= maxSteps) index = 0;

        return word;
    }
}
