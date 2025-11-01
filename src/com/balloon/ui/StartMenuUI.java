// 패키지: 화면(UI) 관련 클래스들이 모이는 공간
package com.balloon.ui;

import com.balloon.core.ScreenId;     // 화면 식별자(문자열 상수)
import com.balloon.core.ScreenRouter; // CardLayout 기반 라우터

import javax.swing.*;  // 스윙 UI 컴포넌트
import java.awt.*;     // 레이아웃/색상/폰트 등

/**
 * StartMenuUI
 * - 게임 시작 화면(UI) 패널.
 * - 버튼 4개(Start/Guide/Ranking/Exit)와 단축키(Enter/G/R/Esc) 제공.
 * - 라우터를 받아서 다른 화면으로 전환한다.
 */
public class StartMenuUI extends JPanel {

    /**
     * 생성자
     * @param router 화면 전환을 위한 라우터(필수)
     */
    public StartMenuUI(ScreenRouter router) {
        // 화면 전체 레이아웃을 상/중/하로 나누기 쉬운 BorderLayout 사용
        setLayout(new BorderLayout());

        // 살짝 어두운 배경(다크 모드 느낌). 너무 까맣지 않게 18,18,18
        setBackground(new Color(18, 18, 18));

        // -------------------- [상단: 타이틀] --------------------
        // 중앙 정렬 라벨
        JLabel title = new JLabel("BALLOON TYPER", SwingConstants.CENTER);

        // 글자색: 밝은 회색(다크 배경 대비)
        title.setForeground(new Color(240, 240, 240));

        // 글꼴: 굵게, 크게 (시스템 Sans 대체)
        title.setFont(new Font("SansSerif", Font.BOLD, 36));

        // 위/아래 여백 주기 (위 40px, 아래 20px)
        title.setBorder(BorderFactory.createEmptyBorder(40, 0, 20, 0));

        // 레이아웃의 북쪽(NORTH)에 타이틀 배치
        add(title, BorderLayout.NORTH);

        // -------------------- [중앙: 버튼 스택] --------------------
        // 중앙 컨테이너는 투명으로(부모 배경색을 그대로 쓰기 위해)
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false); // 투명 처리

        // GridBag 제약조건(버튼 묶음을 가운데에, 가로로 늘어나게)
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;                         // 첫 번째 열
        gbc.fill = GridBagConstraints.NONE; // 가로로 늘어남
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 0, 10, 0); // 버튼 사이 간격
        gbc.weightx = 1.0;                     // 가로 공간 비중
        gbc.weighty = 1.0;

        // 버튼 4개 생성
        JButton startBtn   = makePrimaryButton("Start");   // 시작(파란 계열)
        JButton guideBtn   = makeSecondaryButton("Guide"); // 가이드(회색)
        JButton rankingBtn = makeSecondaryButton("Ranking");// 랭킹(회색)
        JButton exitBtn    = makeDangerButton("Exit");     // 종료(빨강)

        // 버튼 클릭 시 라우터로 화면 전환
        startBtn.addActionListener(e -> router.show(ScreenId.GAME));
        guideBtn.addActionListener(e -> router.show(ScreenId.GUIDE));
        rankingBtn.addActionListener(e -> router.show(ScreenId.RANKING));
        exitBtn.addActionListener(e -> System.exit(0)); // 프로그램 종료

        // 버튼을 세로로 정렬하는 컨테이너
        JPanel btnColumn = new JPanel(new GridLayout(0, 1, 0, 12)); // 세로로 1열
        btnColumn.setOpaque(false);

        btnColumn.add(startBtn);
        btnColumn.add(guideBtn);
        btnColumn.add(rankingBtn);
        btnColumn.add(exitBtn);

        // 가운데에 버튼 묶음 추가
        center.add(btnColumn, gbc);
        add(center, BorderLayout.CENTER);

        // -------------------- [하단: 푸터] --------------------
        JLabel footer = new JLabel("© 2025 Balloon Typer Team", SwingConstants.CENTER);
        footer.setForeground(new Color(150, 150, 150));               // 중간 회색
        footer.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0)); // 여백
        add(footer, BorderLayout.SOUTH);

        // -------------------- [단축키 등록] --------------------
        // Enter: Start
        registerKeyboardAction(
                e -> startBtn.doClick(),                            // 실행할 동작
                KeyStroke.getKeyStroke("ENTER"),                    // 트리거 키
                JComponent.WHEN_IN_FOCUSED_WINDOW                   // 포커스 무관
        );

        // G: Guide
        registerKeyboardAction(
                e -> guideBtn.doClick(),
                KeyStroke.getKeyStroke('G'),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // R: Ranking
        registerKeyboardAction(
                e -> rankingBtn.doClick(),
                KeyStroke.getKeyStroke('R'),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Esc: Exit
        registerKeyboardAction(
                e -> exitBtn.doClick(),
                KeyStroke.getKeyStroke("ESCAPE"),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    // -------------------- [버튼 스타일 헬퍼] --------------------

    /**
     * 공통 버튼 베이스: 글꼴/패딩/포커스 스타일을 통일
     */
    private JButton baseButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.BOLD, 18));                 // 굵은 18pt
        b.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));    // 내부 여백
        b.setFocusPainted(false);                                        // 포커스 테두리 제거
        return b;
    }

    /**
     * 주요 액션(파란색) 버튼
     */
    private JButton makePrimaryButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(83, 109, 254)); // 파란 톤
        b.setForeground(Color.WHITE);             // 흰 글자
        return b;
    }

    /**
     * 보조(회색) 버튼
     */
    private JButton makeSecondaryButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(48, 48, 48));           // 짙은 회색
        b.setForeground(new Color(230, 230, 230));        // 밝은 회색 글자
        return b;
    }

    /**
     * 위험(빨강) 버튼: 종료 등 파괴적 액션
     */
    private JButton makeDangerButton(String text) {
        JButton b = baseButton(text);
        b.setBackground(new Color(229, 57, 53)); // 레드
        b.setForeground(Color.WHITE);            // 흰 글자
        return b;
    }
}
