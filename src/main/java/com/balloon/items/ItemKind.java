package com.balloon.items;

public enum ItemKind {
    TIME_PLUS_5("+5초"),
    TIME_MINUS_5("-5초"),
    BALLOON_PLUS_2("풍선 +2"),
    BALLOON_MINUS_2("풍선 -2"),
    // 듀얼 전용 트릭 아이템
    BLIND("블라인드"),
    SELF_BALLOON_PLUS_2("내 풍선 +2"),
    SELF_BALLOON_MINUS_2("내 풍선 -2"),
    REVERSE_5S("거꾸로 타이핑 5초");   // 마지막 enum 값은 세미콜론

    private final String label;

    ItemKind(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
