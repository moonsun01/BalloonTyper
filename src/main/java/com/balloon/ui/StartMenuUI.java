package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;

import javax.swing.*;
import java.awt.*;

public class StartMenuUI extends JPanel {

    private final ScreenRouter router;

    public StartMenuUI(ScreenRouter router) {
        this.router = router;                    // ← 대입 누락 보완
        setLayout(new BorderLayout());
        setBackground(new Color(18, 18, 18));

        // ---- 상단 타이틀 ----
        JLabel title = new JLabel("BALLOON TYPER", SwingConstants.CENTER);
        title.setForeground(new Color(240, 240, 240));
        title.setFont(new Font("SansSerif", Font.BOLD, 36));
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        // ---- 중앙 버튼들 ----
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.weightx = 1.0; gbc.weighty = 1.0;

        JButton startBtn   = makePrimaryButton("Start");
        JButton guideBtn   = makeSecondaryButton("Guide");
        JButton rankingBtn = makeSecondaryButton("Ranking");
        JButton exitBtn    = makeDangerButton("Exit");

        // 화면 전환
        startBtn.addActionListener(e -> router.show(ScreenId.GAME));
        guideBtn.addActionListener(e -> router.show(ScreenId.GUIDE));
        rankingBtn.addActionListener(e -> router.show(ScreenId.RANKING));   // ← RANKING 말고 RANK
        exitBtn.addActionListener(e -> System.exit(0));

        JPanel col = new JPanel(new GridLayout(0, 1, 0, 12));
        col.setOpaque(false);
        col.add(startBtn); col.add(guideBtn); col.add(rankingBtn); col.add(exitBtn);

        center.add(col, gbc);
        add(center, BorderLayout.CENTER);

        // ---- 하단 푸터 ----
        JLabel footer = new JLabel("© 2025 Balloon Typer Team", SwingConstants.CENTER);
        footer.setForeground(new Color(150, 150, 150));
        footer.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        add(footer, BorderLayout.SOUTH);

        // ---- 단축키 ----
        registerKeyboardAction(e -> startBtn.doClick(),
                KeyStroke.getKeyStroke("ENTER"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> guideBtn.doClick(),
                KeyStroke.getKeyStroke('G'), JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> rankingBtn.doClick(),
                KeyStroke.getKeyStroke('R'), JComponent.WHEN_IN_FOCUSED_WINDOW);
        registerKeyboardAction(e -> exitBtn.doClick(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    // ---- 버튼 스타일 헬퍼들 ----
    private JButton baseButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 18));
        b.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        b.setFocusPainted(false);
        return b;
    }
    private JButton makePrimaryButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(83, 109, 254));
        b.setForeground(Color.WHITE);
        return b;
    }
    private JButton makeSecondaryButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(48, 48, 48));
        b.setForeground(new Color(230, 230, 230));
        return b;
    }
    private JButton makeDangerButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(229, 57, 53));
        b.setForeground(Color.WHITE);
        return b;
    }
}
