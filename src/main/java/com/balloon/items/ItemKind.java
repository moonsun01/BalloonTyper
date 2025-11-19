package com.balloon.items;

public enum ItemKind {
    TIME_PLUS_5("+5초"),
    TIME_MINUS_5("-5초"),
    BALLOON_PLUS_2("풍선 +2"),
    BALLOON_MINUS_2("풍선 -2"),
    SELF_BALLOON_PLUS_2("내 풍선 +2"),
    SELF_BALLOON_MINUS_2("내 풍선 -2");

    private final String label;
    ItemKind(String label) { this.label = label; }
    public String label() { return label; }

}
