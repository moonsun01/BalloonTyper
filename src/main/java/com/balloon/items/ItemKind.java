package com.balloon.items;

public enum ItemKind {
    TIME_PLUS_5("+5초"),
    TIME_MINUS_5("-5초"),
    BALLOON_PLUS_2("풍선 +2"),
    BALLOON_MINUS_2("풍선 -2"),
    REVERSE_5S("거꾸로 타이핑 5초");


    private final String label;
    ItemKind(String label) { this.label = label; }
    public String label() { return label; }

}
