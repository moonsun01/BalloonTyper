package com.balloon.ui.components; // 공통 위젯 생성 헬퍼

import com.balloon.ui.theme.DesignSpec; // 디자인 토큰
import com.balloon.ui.theme.Theme;      // 전역 색/폰트
import javax.swing.*;                   // Swing
import javax.swing.border.EmptyBorder;  // 여백 보더
import java.awt.*;                      // Graphics

public final class UIKit { // 인스턴스화 방지
    private UIKit() {}

    // 포인트 색 라운드 버튼
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);                                           // 기본 버튼
        b.setFocusPainted(false);                                                // 포커스 테두리 제거
        b.setBorder(new EmptyBorder(10, 18, 10, 18));                            // 내부 패딩
        b.setFont(Theme.body());                                                 // 폰트
        b.setForeground(DesignSpec.ACCENT_TEXT_ON);                              // 글자색
        b.setBackground(DesignSpec.ACCENT);                                      // 배경색
        b.setContentAreaFilled(false);                                           // 커스텀 페인트
        b.setOpaque(false);                                                      // 투명
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI(){                      // 간단 UI 오버라이드
            @Override public void paint(Graphics g, JComponent c) {              // 커스텀 페인트
                Graphics2D g2 = (Graphics2D) g.create();                         // 그래픽스 복사
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,            // 안티앨리어싱
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(b.isEnabled() ? b.getBackground() : new Color(0x9CA3AF)); // 비활성 색
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(),             // 라운드 배경
                        DesignSpec.RADIUS_BUTTON, DesignSpec.RADIUS_BUTTON);
                g2.dispose();                                                    // 해제
                super.paint(g, c);                                               // 텍스트 페인트
            }
        });
        return b;                                                                // 반환
    }

    // 카드 패널(라운드 + 내부 패딩)
    public static JPanel cardPanel(LayoutManager layout) {
        return new JPanel(layout) {                                              // 익명 클래스
            { setOpaque(false); setBorder(new EmptyBorder(                       // 내부 패딩
                    DesignSpec.GAP_LG, DesignSpec.GAP_LG, DesignSpec.GAP_LG, DesignSpec.GAP_LG)); }
            @Override protected void paintComponent(Graphics g) {                // 배경 페인트
                Graphics2D g2 = (Graphics2D) g.create();                         // 복사
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,            // 안티앨리어싱
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.cardColor());                                  // 카드 배경색
                g2.fillRoundRect(0, 0, getWidth(), getHeight(),                  // 라운드 사각형
                        DesignSpec.RADIUS_CARD, DesignSpec.RADIUS_CARD);
                g2.dispose();                                                    // 해제
                super.paintComponent(g);                                         // 기본 처리
            }
        };
    }

    // 입력 필드(여백 + 폰트)
    public static JTextField inputField(int columns) {
        JTextField tf = new JTextField(columns);                                 // 생성
        tf.setFont(Theme.body());                                                // 폰트
        tf.setBorder(new EmptyBorder(10, 12, 10, 12));                           // 내부 여백
        return tf;                                                               // 반환
    }
}
