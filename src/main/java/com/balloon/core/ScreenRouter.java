package com.balloon.core;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * CardLayout 기반 화면 라우터 (싱글턴)
 * - register(id, comp): 화면 등록
 * - show(id): onHidden → 카드 전환 → onShown
 */
public final class ScreenRouter {

    // ====== 싱글턴 ======
    private static final ScreenRouter INSTANCE = new ScreenRouter();
    public static ScreenRouter get() { return INSTANCE; }
    private ScreenRouter() { /* 외부 생성 금지 */ }

    // ====== 내부 상태 ======
    private final JPanel root = new JPanel(new CardLayout());  // 카드 컨테이너
    private final CardLayout card = (CardLayout) root.getLayout();
    private final Map<String, Component> views = new HashMap<>();
    private String currentId = null;

    /** 외부 프레임/런처에서 setContentPane(root)로 붙여서 사용 */
    public JPanel getRoot() { return root; }

    /** 화면 등록: root에 add하고 맵에도 보관 */
    public void register(String id, Component comp) {
        if (!views.containsKey(id)) {
            views.put(id, comp);
            root.add(comp, id);
        }
    }

    /** 화면 전환: onHidden → 카드 전환 → onShown */
    public void show(String id) {
        if (!views.containsKey(id)) return;

        // 1) 이전 화면 onHidden()
        if (currentId != null) {
            Component prev = views.get(currentId);
            if (prev instanceof Showable) {
                ((Showable) prev).onHidden();
            }
        }

        // 2) 카드 전환
        card.show(root, id);
        currentId = id;

        // 3) 새 화면 onShown()
        Component now = views.get(id);
        if (now instanceof Showable) {
            ((Showable) now).onShown();
        }
    }
}
