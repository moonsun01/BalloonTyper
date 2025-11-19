// src/main/java/com/balloon/ui/ModeSelectScreenUI.java
package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.GameContext;
import com.balloon.core.GameContext.GameMode;

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

        // =========================
        // [ADD] GUIDE랑 같은 위치의 투명 BACK 버튼
        //  - 좌상단 (28, 24) / 크기 (160 x 80)
        //  - 클릭하면 START 화면으로 돌아가기
        // =========================
        JButton back = new JButton("BACK");
        styleTransparent(back);               // 글자/배경 모두 보이지 않게
        back.setBounds(28, 24, 160, 80);      // GUIDE의 back과 동일 좌표
        add(back);

        back.addActionListener(e -> router.show(ScreenId.START));

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
        single.addActionListener(e -> {
            GameContext ctx = GameContext.getInstance();
            ctx.setMode(GameMode.SINGLE);          // ⭐ 싱글 모드 설정
            router.show(ScreenId.GAME);
        });

        dual.addActionListener(e -> {
            GameContext ctx = GameContext.getInstance();
            ctx.setMode(GameMode.VERSUS);          // ⭐ 듀얼 모드 설정
            router.show(ScreenId.VERSUS_GAME);
        });

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
        b.setForeground(new Color(0, 0, 0, 0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        // 중앙정렬 고정
        b.setHorizontalAlignment(SwingConstants.CENTER);
        b.setVerticalAlignment(SwingConstants.CENTER);
    }
}
