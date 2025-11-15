package com.balloon.app;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;

// ↓ 트리 구조에 맞춰 패키지 경로 정확히 임포트
import com.balloon.ui.StartMenuUI;        // ui/theme/StartMenuUI.java
import com.balloon.ui.screens.GamePanel;        // ui/screens/GamePanel.java
import com.balloon.ui.RankingScreenUI;    // ui/theme/RankingScreenUI.java
import com.balloon.ui.GuideScreenUI;
import com.balloon.ui.ModeSelectScreenUI;
import com.balloon.ui.screens.ResultScreen;

import javax.swing.*;
import java.awt.*;

public class Launcher {
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}

        JFrame frame = new JFrame("Balloon Typer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1280, 720);
        frame.setLocationRelativeTo(null);

// ★★★ 추가 1: 프레임/내용 영역 배경을 검정으로 통일 ★★★
        frame.getContentPane().setBackground(Color.BLACK);
        frame.setBackground(Color.BLACK);

        // ★ 1) 루트 패널을 직접 만들고, BorderLayout으로 전체 꽉 채우기
        JPanel rootPanel = new JPanel(new CardLayout());
        rootPanel.setBorder(null);                 // 여백 절대 없음
        rootPanel.setOpaque(true);

        // ★★★ 추가 2: 루트 패널도 검정 배경 ★★★
        rootPanel.setBackground(Color.BLACK);

        frame.setContentPane(rootPanel);

        // ★ 3) ScreenRouter는 rootPanel을 쓰도록
        ScreenRouter router = new ScreenRouter(rootPanel, (CardLayout) rootPanel.getLayout());



        //ScreenRouter router = new ScreenRouter(frame.getContentPane(), new CardLayout());

        // 화면 등록
        router.register(ScreenId.START,   new StartMenuUI(router));     // OK
        router.register(ScreenId.MODE_SELECT, new ModeSelectScreenUI(router)); // ★ 추가
        router.register(ScreenId.GAME,    new GamePanel(router));       // ★ 여기 수정: () → (router)
        router.register(ScreenId.GUIDE,   new GuideScreenUI(router));  // 임시 화면
        router.register(ScreenId.RANKING, new RankingScreenUI(router)); // ★ 여기 수정: () → (router)
        router.register(ScreenId.RESULT,  new ResultScreen(router));  // ★ 이 줄 필수

        // 첫 화면
        router.show(ScreenId.START);
        frame.setVisible(true);
    }

    // GUIDE 임시 대체용
    private static JComponent placeholder(String text) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(text));
        return p;
    }
}
