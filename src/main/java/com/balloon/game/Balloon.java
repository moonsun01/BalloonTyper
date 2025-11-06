package com.balloon.game;
/*
풍선 한개를 표현하는 데이터, 행동
 */

import java.util.Objects;

public class Balloon {
    private static int NEXT_ID = 1;             //모든 풍선에 고유 id부여

    private final int id;                       //풍선 고유 id
    private final String word;                  //풍선에 적힌단어

    private int x;
    private int y;
    private boolean active;                     //아직 안터진풍선(true)

    private final boolean hasItem;              //아이템 아직 없음

    public Balloon(String word, int x, int y) {
        this.id = NEXT_ID++;

        if (word == null) { this.word = ""; }   // 단어 null인지 확인 후 저장
        else { this.word = word; }

        this.x = x;
        this.y = y;
        this.active = true;
        this.hasItem = false;                   //아이템없
    }

    public int getId() {return id;}
    public String getWord() {return word;}
    public int getX() {return x;}
    public int getY() {return y;}
    public boolean isActive() {return active;}
    public boolean hasItem() {return hasItem;}

    public boolean matchesExact(String input) {         //입력단어-풍선단어 일치확인
        if (input == null) return false;
        return word.equals(input);
    }

    public boolean tryPop(String input) {
        if (!active) return false;
        if (matchesExact(input)) {
            active = false;
            return true;
        }
        return false;
    }

    public void pop() {this.active = false;}

}











