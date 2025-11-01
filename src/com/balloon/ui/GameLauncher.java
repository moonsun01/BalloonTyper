package com.balloon.ui; // ScreenRouter와 같은 패키지여야 import 없이 사용 가능

import javax.swing.*; // SwingUtilities, JFrame, WindowConstants 등 사용

/**
 * GameLauncher
 * - 프로그램의 시작점(main 메서드)을 가진 클래스.
 * - JFrame(창)을 만들고, 안에 ScreenRouter(화면 전환 패널)를 넣어 보여준다.
 * - 이 클래스를 만들고 실행해야, ScreenRouter가 "사용 중" 상태가 되어
 *   방금 보던 'never used' 경고가 사라진다.
 */
public class GameLauncher {

    /** 자바 애플리케이션의 진입점 */
    public static void main(String[] args) {
        // Swing UI 초기화는 Event Dispatch Thread(EDT)에서 하는 게 안전하다.
        SwingUtilities.invokeLater(() -> {
            // 1) 최상위 창 생성 + 제목 세팅
            JFrame frame = new JFrame("Balloon Typer");

            // 2) X 버튼 클릭 시 프로세스 종료
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // 3) 내용 영역에 '화면 전환 관리자'인 ScreenRouter를 부착
            frame.setContentPane(new ScreenRouter());

            // 4) 창 크기 설정 (필요하면 나중에 조절)
            frame.setSize(800, 500);

            // 5) 화면 가운데로 위치
            frame.setLocationRelativeTo(null);

            // 6) 눈에 보이게 표시
            frame.setVisible(true);
        });
    }
}
