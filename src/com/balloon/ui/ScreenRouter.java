package com.balloon.ui; // 이 파일이 com.balloon.ui 패키지(폴더) 안에 있다는 선언

import javax.swing.*;  // Swing 컴포넌트(JPanel, JLabel, InputMap 등) 사용
import java.awt.*;     // AWT의 레이아웃/색상 클래스(CardLayout, Color, GridBagLayout 등) 사용

/**
 * ScreenRouter
 * - "화면 전환"을 담당하는 패널(컨테이너)이다.
 * - CardLayout을 사용해 여러 화면을 '카드'처럼 겹쳐두고, 원하는 화면만 보여준다.
 * - 오늘(Day1)은 아직 실제 화면을 만들지 않았으므로
 *   색만 다른 임시 패널 5개(START/GUIDE/PLAY/RESULT/RANK)를 등록해
 *   전환이 잘 되는지만 확인한다.
 */
public class ScreenRouter extends JPanel {

    // 화면 별 '키'를 상수로 관리 → 오타 방지 & 재사용 편리
    public static final String START  = "START";   // 시작 화면
    public static final String GUIDE  = "GUIDE";   // 가이드 화면
    public static final String PLAY   = "PLAY";    // 게임 화면(오늘은 더미)
    public static final String RESULT = "RESULT";  // 결과 화면
    public static final String RANK   = "RANK";    // 랭킹 화면

    // CardLayout: 여러 컴포넌트를 카드처럼 쌓아놓고 하나만 보여주는 레이아웃
    private final CardLayout cards = new CardLayout();

    /** 생성자: 이 패널이 new 될 때 한 번 실행된다. */
    public ScreenRouter() {
        // 1) 이 패널의 레이아웃을 CardLayout으로 설정
        setLayout(cards);

        // 2) 임시 더미 화면(색만 다름) 추가
        //    add(컴포넌트, 키) 형태로 등록 → 나중에 cards.show(this, "키")로 전환
        add(dummy("START",  0x1f2937), START);   // 어두운 회색
        add(dummy("GUIDE",  0x0f766e), GUIDE);   // 청록
        add(dummy("PLAY",   0x334155), PLAY);    // 회청
        add(dummy("RESULT", 0x3730a3), RESULT);  // 보라
        add(dummy("RANK",   0x9333ea), RANK);    // 보라(밝음)

        // 3) 처음에 보여줄 화면을 START로 지정
        cards.show(this, START);

        // 4) (오늘만) 방향키로 화면을 바꾸는 테스트 바인딩
        //    내일부터는 버튼 클릭으로 바꿀 예정
        setupTempKeyBindings();
    }

    /**
     * 더미(임시) 화면을 만드는 유틸 함수.
     * - 배경색만 다른 JPanel을 만들고,
     * - 가운데에 큰 라벨을 배치한다.
     */
    private JPanel dummy(String label, int rgb) {
        // 중앙 배치를 해 주는 GridBagLayout 사용
        JPanel p = new JPanel(new GridBagLayout());

        // 배경색 지정 (0xRRGGBB 정수값으로 Color 생성)
        p.setBackground(new Color(rgb));

        // 가운데 표시할 라벨 생성
        JLabel l = new JLabel(label);                 // 라벨 텍스트
        l.setForeground(Color.WHITE);                 // 글자색: 흰색
        l.setFont(l.getFont().deriveFont(Font.BOLD, 28f)); // 굵게 28pt

        // 레이아웃 중앙에 라벨 추가
        p.add(l);

        return p; // 완성된 패널 반환
    }

    /**
     * 실제 버튼/메뉴에서 화면을 바꿀 때 외부에서 호출할 공개 함수.
     * ex) go(ScreenRouter.PLAY);
     */
    public void go(String key) {
        cards.show(this, key); // 해당 키의 카드로 전환
        revalidate();          // 레이아웃 갱신(구조 변화 반영)
        repaint();             // 화면 다시 그리기(시각 갱신)
    }

    /**
     * Day1 전환 확인용 임시 키 바인딩:
     * - RIGHT(→) : 다음 카드
     * - LEFT(←)  : 이전 카드
     *  → 전환이 잘 되는지 눈으로 확인하기 위한 임시 장치
     */
    private void setupTempKeyBindings() {
        // 포커스 여부와 관계없이 이 패널이 키 입력을 받을 수 있게
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // 키보드 입력을 액션 문자열에 연결
        im.put(KeyStroke.getKeyStroke("RIGHT"), "next");
        im.put(KeyStroke.getKeyStroke("LEFT"),  "prev");

        // 실제 동작: CardLayout의 next/previous 호출
        am.put("next", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cards.next(ScreenRouter.this); // 다음 화면
            }
        });
        am.put("prev", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cards.previous(ScreenRouter.this); // 이전 화면
            }
        });
    }
}
