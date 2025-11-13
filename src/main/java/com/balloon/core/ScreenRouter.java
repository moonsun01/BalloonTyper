package com.balloon.core;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/** 카드 레이아웃 기반 화면 전환 라우터 */
public final class ScreenRouter {

    // 카드 컨테이너와 레이아웃
    private final Container contentPanel;
    private final CardLayout layout;

    // id -> 화면 매핑
    private final Map<String, JComponent> map = new LinkedHashMap<>();

    // 현재 화면
    private JComponent current;

    /** 프레임의 contentPane 같은 외부 컨테이너를 카드로 쓰는 생성자 */
    public ScreenRouter(Container contentPanel, CardLayout layout) {
        this.contentPanel = contentPanel;
        this.layout = (layout != null) ? layout : new CardLayout();
        this.contentPanel.setLayout(this.layout);
    }

    /** 화면 등록 */
    public void register(String id, JComponent view) {
        map.put(id, view);
        contentPanel.add(view, id);
    }

    /** 등록된 화면 가져오기 */
    public JComponent get(String id) {
        return map.get(id);
    }

    /** 화면 전환 */
    public void show(String id) {
        JComponent next = map.get(id);
        if (next == null) return;

        // 현재 화면 onHidden() 콜백
        if (current instanceof Showable sh) {
            try { sh.onHidden(); } catch (Exception ignore) {}
        }

        // 전환
        layout.show(contentPanel, id);
        contentPanel.validate();
        contentPanel.repaint();

        current = next;

        // 새 화면 onShown() 콜백
        if (current instanceof Showable sh2) {
            try { sh2.onShown(); } catch (Exception ignore) {}
        }
    }
}
