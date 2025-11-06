package com.balloon.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * Theme가 전역으로 잘 적용되는지 “눈으로” 확인하는 미니 프리뷰 실행용 클래스.
 * - 본 게임 코드와 분리되어 안전하게 색/폰트 적용 여부만 빠르게 확인한다.
 * - 중요: Theme.applyGlobalUI()는 프레임 생성 전에 반드시 호출해야 한다.
 */
public class ThemePreview {

    public static void main(String[] args) {
        // OS 룩앤필 적용(윈도우/맥/리눅스 기본 위젯 느낌 유지)
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignore) {}

        // ===== 전역 Theme 주입(프레임/컴포넌트 생성 전에!) =====
        Theme.applyGlobalUI();

        // 프레임 기본 셋업
        JFrame f = new JFrame("Theme Preview");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(520, 300);
        f.setLocationRelativeTo(null); // 화면 중앙

        // 루트 패널: 다크 배경 확인용
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_DARK); // 직접 지정해서 차이를 확실히 보자

        // 상단 타이틀 라벨(강조 폰트/강조색 확인)
        JLabel title = new JLabel("전역 Theme 적용 테스트", SwingConstants.CENTER);
        title.setFont(Theme.FONT_HUD);          // 두껍고 큰 폰트
        title.setForeground(Theme.HUD_ACCENT);  // 포인트 컬러(노란색)

        // 중앙: 다크 톤에서 입력/버튼 색 확인
        JPanel center = new JPanel();
        center.setOpaque(false); // 부모 다크 배경 비치도록
        JTextField tf = new JTextField(18);
        JButton btn = new JButton("확인");
        center.add(tf);
        center.add(btn);

        root.add(title, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);

        f.setContentPane(root);
        f.setVisible(true);
    }
}
