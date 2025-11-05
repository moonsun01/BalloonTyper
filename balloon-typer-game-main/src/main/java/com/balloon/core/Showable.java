package com.balloon.core;

/** 화면이 CardLayout으로 보여진 직후 호출되는 훅 */
public interface Showable {
    void onShown();   // ← 대소문자 정확히 이 이름
}
