package com.balloon.core;

public final class PlayerNameStore {

    private PlayerNameStore() {}

    private static String playerName = "player1";
    private static String versusOpponentName = "player2";

    public static void setPlayerName(String name) {
        playerName = (name == null || name.isBlank()) ? "player1" : name;
    }

    public static void setVersusOpponentName(String name) {
        versusOpponentName = (name == null || name.isBlank()) ? "player2" : name;
    }

    public static String getPlayerName() {
        return playerName;
    }

    public static String getVersusOpponentName() {
        return versusOpponentName;
    }
}
