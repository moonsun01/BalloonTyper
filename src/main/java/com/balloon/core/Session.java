package com.balloon.core;

/**
 * 간단한 전역 세션(닉네임 보관)
 */
public final class Session {
    private static String nickname = "";

    private Session() {}

    public static String getNickname() {
        return nickname;
    }

    public static void setNickname(String name) {
        nickname = name == null ? "" : name.trim();
    }
}
