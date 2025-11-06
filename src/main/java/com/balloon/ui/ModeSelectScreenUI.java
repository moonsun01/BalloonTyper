// src/main/java/com/balloon/ui/ModeSelectScreenUI.java
package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;

import javax.swing.*;
import java.awt.*;

public class ModeSelectScreenUI extends JPanel {
    private final Image bg;
    private final ScreenRouter router;

    public ModeSelectScreenUI(ScreenRouter router) {
        this.router = router;
        this.bg = new ImageIcon(getClass().getResource("/images/START.png")).getImage();

        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));

        // SINGLE MODE 버튼 (왼쪽 핑크 캡슐 위)
        JButton single = new JButton("SINGLE MODE");
        single.setFont(new Font("Dialog", Font.BOLD, 22));
        single.setBounds(233, 448, 360, 82);    // ← 필요시 5~10px 단위로 미세조정
        styleTransparent(single);
        add(single);

        // DUAL MODE 버튼 (오른쪽 핑크 캡슐 위)
        JButton dual = new JButton("DUAL MODE");
        dual.setFont(new Font("Dialog", Font.BOLD, 22));
        dual.setBounds(670, 448, 360, 82);      // ← 필요시 미세조정
        styleTransparent(dual);
        add(dual);

        // 동작
        single.addActionListener(e -> router.show(ScreenId.GAME));                   // 싱글=현재 게임
        dual.addActionListener(e -> router.show(ScreenId.RANKING));                  // 임시: 듀얼은 준비중 화면 대체 원하면 바꿔줘
        // ※ 듀얼을 나중에 구현하면 ScreenId.DUAL 같은 새 화면으로 연결하면 됨.
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this); // 배경 꽉 채우기
    }

    private void styleTransparent(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(Color.BLACK);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 중앙정렬 고정
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setVerticalAlignment(SwingConstants.CENTER);
    }
}
