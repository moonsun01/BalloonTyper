// 패키지: 실행 진입점을 관리하는 최상위 공간
package com.balloon.app;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.ui.PlaceholderScreen;
import com.balloon.ui.StartMenuUI;
import com.balloon.ui.screens.GamePanel;


import javax.swing.*;
import java.awt.*;

/**
 * GameLauncher
 * - 프로그램의 시작점(main 메서드 보유)
 * - JFrame(메인 창)을 만들고, ScreenRouter를 이용해 화면을 전환한다.
 */
public class Launcher {

    public static void main(String[] args) {
        // Swing 프로그램은 이벤트 디스패치 스레드(EDT) 안에서 실행해야 안전함
        SwingUtilities.invokeLater(() -> {

            // -------------------- [1] 메인 창 생성 --------------------
            JFrame frame = new JFrame("Balloon Typer"); // 윈도우 제목
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 닫으면 종료
            frame.setMinimumSize(new Dimension(900, 600)); // 최소 크기 지정
            frame.setLocationRelativeTo(null); // 화면 정중앙 배치

            // -------------------- [2] 라우터 생성 --------------------
            ScreenRouter router = new ScreenRouter();

            // -------------------- [3] 화면 등록 --------------------
            // register(화면ID, 컴포넌트)
            router.register(ScreenId.START,   new StartMenuUI(router));          // 시작 화면
            router.register(ScreenId.GAME,  new GamePanel());     // ← GamePanel 등록
            router.register(ScreenId.GUIDE,   new PlaceholderScreen("GUIDE"));   // 임시 가이드
            router.register(ScreenId.RANKING, new PlaceholderScreen("RANKING"));// 임시 랭킹

            // -------------------- [4] 라우터 루트패널을 JFrame에 붙이기 --------------------
            frame.setContentPane(router.getRoot());
            router.show(ScreenId.START); //첫화면

            // -------------------- [5] 창 보이기 --------------------
            frame.setVisible(true);

        });
    }
}
