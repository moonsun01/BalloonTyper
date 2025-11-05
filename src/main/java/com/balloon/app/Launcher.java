// 패키지: 실행 진입점을 관리하는 최상위 공간
package com.balloon.app;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.ui.PlaceholderScreen;
import com.balloon.ui.StartMenuUI;
import com.balloon.ui.screens.GamePanel;
import com.balloon.ui.theme.Theme;
import com.balloon.ui.screens.ResultScreen;  // 결과 화면
import com.balloon.ui.RankingScreenUI;      // 랭킹 화면(UI)

import javax.swing.*;
import java.awt.*;

/**
 * Launcher
 * - 프로그램의 시작점(main 메서드 보유)
 * - JFrame(메인 창)을 만들고, ScreenRouter를 이용해 화면을 전환한다.
 */
public class Launcher {

    public static void main(String[] args) {

        // ① OS 룩앤필 적용(버튼/체크박스가 OS 스타일로 보이게)
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignore) {}

        // ② 전역 Theme 주입(프레임/컴포넌트 생성 '이전'에 반드시 실행!)
        Theme.applyGlobalUI();

        // Swing 프로그램은 이벤트 디스패치 스레드(EDT) 안에서 실행해야 안전함
        SwingUtilities.invokeLater(() -> {

            // -------------------- [1] 메인 창 생성 --------------------
            JFrame frame = new JFrame("Balloon Typer"); // 윈도우 제목
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 닫으면 종료
            frame.setMinimumSize(new Dimension(900, 600)); // 최소 크기 지정
            frame.setLocationRelativeTo(null); // 화면 정중앙 배치

            // -------------------- [2] 라우터 생성 --------------------
            ScreenRouter router = ScreenRouter.get();

            // -------------------- [3] 화면 등록 --------------------
            // register(화면ID, 컴포넌트)
            router.register(ScreenId.START,   new StartMenuUI(router));          // 시작 화면
            router.register(ScreenId.GAME,    new GamePanel(router));            // 게임 화면
            router.register(ScreenId.GUIDE,   new PlaceholderScreen("GUIDE"));   // 임시 가이드
            router.register(ScreenId.RANKING, new RankingScreenUI(router));      // ★ 랭킹 화면(실제 UI)
            router.register(ScreenId.RESULT,  new ResultScreen(router));         // 결과 화면

            // -------------------- [4] 라우터 루트패널을 JFrame에 붙이기 --------------------
            frame.setContentPane(router.getRoot());
            router.show(ScreenId.START); // 첫 화면

            // -------------------- [5] 창 보이기 --------------------
            frame.setVisible(true);

        });
    }
}
