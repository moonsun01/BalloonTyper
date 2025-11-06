package com.balloon.core;

/**
 * 화면 수명주기 훅.
 * 기존 구현을 깨지 않기 위해 기본 구현을 제공한다.
 */
public interface Showable {
    /** 화면이 막 보였을 때 호출 */
    default void onShown() {}

    /** 다른 화면으로 전환되어 숨겨질 때 호출 */
    default void onHidden() {}
}
