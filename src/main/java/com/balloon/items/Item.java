package com.balloon.items;

public class Item {
    private final ItemKind kind;
    private final int amount;
    private final int durationMs;

    public Item(ItemKind kind, int amount, int durationMs) {
        this.kind = kind;
        this.amount = amount;
        this.durationMs = durationMs;
    }

    public ItemKind getKind() {return kind;}
    public int getAmount() {return amount;}
    public int getDurationMs() {return durationMs;}

    //1p
    public static Item timePlus5() {return new Item(ItemKind.TIME_PLUS_5, 5, 0);}
    public static Item timeMinus3() {return new Item(ItemKind.TIME_MINUS_3, 3, 0);}
    public static Item balloonPlus2() {return new Item(ItemKind.BALLOON_PLUS_2, 2, 0);}
    public static Item balloonMinus2() {return new Item(ItemKind.BALLOON_MINUS_2, 2, 0);}
    public static Item dud() {return new Item(ItemKind.DUD, 0, 800);}

    //2p
    public static Item selfPlus3() {return new Item(ItemKind.SELF_FIELD_PLUS_3, 3, 0);}
    public static Item selfMinus2() {return new Item(ItemKind.SELF_FIELD_MINUS_2, 2, 0);}
    public static Item oppPlus3() {return new Item(ItemKind.OPP_FIELD_PLUS_3, 3, 0);}
    public static Item oppMinus2() {return new Item(ItemKind.OPP_FIELD_MINUS_2, 2, 0);}
    public static Item trickBlind(int ms) {return new Item(ItemKind.TRICK_BLIND, 0, ms);}
    public static Item pvpDud() {return new Item(ItemKind.PVP_DUD, 0, 800);}

}
