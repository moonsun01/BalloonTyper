package com.balloon.core;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * CardLayout 기반 라우터
 */
public class ScreenRouter {
    private final Container root;
    private final CardLayout cards;
    private final Map<String, JComponent> views = new HashMap<>();

    public ScreenRouter(Container root, CardLayout cards) {
        this.root = root;
        this.cards = cards;
        root.setLayout(cards);
    }

    public void register(String id, JComponent view) {
        views.put(id, view);
        root.add(view, id);
    }

    public void show(String id) {
        cards.show(root, id);
    }

    public JComponent getView(String id) {
        return views.get(id);
    }

    // ⭐ 기존 코드 호환용: GamePanel 등에서 router.get("...")를 쓰는 경우 지원
    public JComponent get(String id) {
        return views.get(id);
    }
}
