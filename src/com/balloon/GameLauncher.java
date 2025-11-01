// 패키지: 실행 진입점을 관리하는 최상위 공간
package com.balloon;

import com.balloon.core.ScreenId;       // 화면 식별자 (START, GUIDE, 등)
import com.balloon.core.ScreenRouter;   // 화면 전환 담당
import com.balloon.ui.PlaceholderScreen;// 임시 테스트용 화면
import com.balloon.ui.StartMenuUI;      // 우리가 만든 시작화면 UI

import javax.swing.*;   // JFrame, JPanel 등
import java.awt.*;      // 레이아웃/크기 관련

/**
 * GameLauncher
 * - 프로그램의 시작점(main 메서드 보유)
 * - JFrame(메인 창)을 만들고, ScreenRouter를 이용해 화면을 전환한다.
 */
public class GameLauncher {

    public static void main(String[] args) {
        // Swing 프로그램은 이벤트 디스패치 스레드(EDT) 안에서 실행해야 안전함
        SwingUtilities.invokeLater(() -> {

            // -------------------- [1] 메인 창 생성 --------------------
            JFrame frame = new JFrame("Balloon Typer"); // 윈도우 제목
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); // 닫으면 종료
            frame.setMinimumSize(new Dimension(960, 600)); // 최소 크기 지정
            frame.setLocationRelativeTo(null); // 화면 정중앙 배치

            // -------------------- [2] 라우터 생성 --------------------
            ScreenRouter router = new ScreenRouter();

            // -------------------- [3] 화면 등록 --------------------
            // register(화면ID, 컴포넌트)
            router.register(ScreenId.START,   new StartMenuUI(router));          // 시작 화면
            router.register(ScreenId.GUIDE,   new PlaceholderScreen("GUIDE SCREEN"));   // 임시 가이드
            router.register(ScreenId.RANKING, new PlaceholderScreen("RANKING SCREEN"));// 임시 랭킹
            router.register(ScreenId.GAME,    new PlaceholderScreen("GAME SCREEN"));    // 임시 게임화면

            // -------------------- [4] 라우터 루트패널을 JFrame에 붙이기 --------------------
            frame.setContentPane(router.getRoot());

            // -------------------- [5] 창 보이기 --------------------
            frame.setVisible(true);

            // -------------------- [6] 첫 화면 표시 --------------------
            router.show(ScreenId.START); // "START" 화면부터 보이게
        });
    }
}
