package com.balloon.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class StaticWordProvider implements WordProvider {
    private final List<String> pool;
    private final Random rnd = new Random();            //랜덤뽑기

    public StaticWordProvider(List<String> words) {     //중복제거,null방지
        Set<String> unique = new HashSet<>(words);

        this.pool = new ArrayList<>();
        for (String w : unique) {
            if (w == null) continue;
            if (w.isEmpty()) continue;
            pool.add(w);
            }
    }


    @Override
    public String nextWord() {                  //단어 비었을경우
        if (pool.isEmpty())  return "empty";
        return pool.get(rnd.nextInt(pool.size()));
    }
}
