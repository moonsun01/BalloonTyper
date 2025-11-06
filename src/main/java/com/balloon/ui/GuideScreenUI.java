package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * GUIDE 화면: 배경 이미지를 꽉 채워 그리고,
 * 좌상단 "Back" 글자 위치에 투명 버튼을 얹어 START로 되돌아간다.
 */
public class GuideScreenUI extends JPanel {

    private final Image bg;
    private final ScreenRouter router;

    public GuideScreenUI(ScreenRouter router) {
        this.router = router;
        this.bg = new ImageIcon(getClass().getResource("/images/GUIDE.png")).getImage();

        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));

        // ← Back 투명 버튼 (그림의 "Back" 텍스트 위에 겹침)
        JButton back = new JButton("BACK"); // 접근성용 라벨(화면엔 안 보이게)
        styleTransparent(back);
        // ★ 좌표는 1280x720 기준 대략값. 필요하면 x,y,w,h 숫자만 미세 조정.
        back.setBounds(28, 24, 160, 80);
        // 텍스트가 비치지 않도록 여백/색 최소화
        back.setBorder(new EmptyBorder(0,0,0,0));
        add(back);

        back.addActionListener(e -> router.show(ScreenId.START));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 패널 크기에 맞춰 GUIDE.png를 채움
        g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
    }

    private void styleTransparent(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(new Color(0,0,0,0)); // 텍스트도 보이지 않게
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
