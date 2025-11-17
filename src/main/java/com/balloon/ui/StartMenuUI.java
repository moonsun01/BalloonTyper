package com.balloon.ui;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.Session;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * 메인(Start) 화면
 * - 배경이미지: /images/Main화면.png (1280x720 기준)
 * - 닉네임 입력 + START/GUIDE/RANKING 버튼 배치
 */
public class StartMenuUI extends JPanel {

    // [반응형] 기준 해상도
    private static final int BASE_W = 1280;
    private static final int BASE_H = 720;

    // [반응형] 우리가 조종할 컴포넌트들을 필드로
    private JButton startBtn, guideBtn, rankBtn;
    private JTextField nicknameField;

    // StartMenuUI 클래스 안(필드 영역)
    private static final boolean DEBUG_HOTSPOT = false; // ☆ 위치 맞춘 뒤 false로!

    private final Image backgroundImage;



    public StartMenuUI(ScreenRouter router) {
        // 배경 이미지 로드
        backgroundImage = new ImageIcon(getClass().getResource("/images/Main화면.png")).getImage();

        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));

        // 닉네임 입력창 (이미지 상 입력칸 위치에 맞춤)
        nicknameField = new JTextField();
        nicknameField.setHorizontalAlignment(JTextField.CENTER);
        nicknameField.setFont(new Font("Dialog", Font.PLAIN, 18));
        // x, y, w, h (이미지 기준 미세조정 가능)
        nicknameField.setBounds(465, 420, 330, 46);
        nicknameField.setBackground(new Color(255, 255, 255, 220));
        nicknameField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        add(nicknameField);

        // ★ 플레이스홀더 텍스트
        final String PLACEHOLDER = "닉네임을 입력하세요!";
        nicknameField.setText(PLACEHOLDER);
        nicknameField.setForeground(Color.GRAY);

// 포커스 잃었을 때만, 비어 있으면 다시 플레이스홀더 복구
        nicknameField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (nicknameField.getText().trim().isEmpty()) {
                    nicknameField.setText(PLACEHOLDER);
                    nicknameField.setForeground(Color.GRAY);
                }
            }
        });

// 키보드를 처음 누를 때 플레이스홀더 지우기
        nicknameField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (nicknameField.getText().equals(PLACEHOLDER)) {
                    nicknameField.setText("");
                    nicknameField.setForeground(Color.BLACK);
                }
            }
        });


// START (노란 칩 위)
        JButton startBtn = new JButton();                 // ← 텍스트 제거
        startBtn.setActionCommand("START");               // (선택)
        startBtn.setBounds(400, 500, 210, 64);            // 네가 쓰던 좌표면 OK
        styleTransparentHotspot(startBtn);
        add(startBtn);

// GUIDE (연두 칩 위)
        JButton guideBtn = new JButton();
        guideBtn.setActionCommand("GUIDE");
        guideBtn.setBounds(650, 500, 210, 64);
        styleTransparentHotspot(guideBtn);
        add(guideBtn);

// RANK (오른쪽 아래 구름)
        JButton rankBtn = new JButton();
        rankBtn.setActionCommand("RANKING");
        rankBtn.setBounds(870, 580, 170, 48);
        styleTransparentHotspot(rankBtn);
        add(rankBtn);


        // 라우팅 연결
        startBtn.addActionListener(e -> {
            String name = nicknameField.getText().trim();
            if (name.equals(PLACEHOLDER)) {  // 아직 플레이스홀더 상태면
                name = "";                   // 빈 문자열로 처리
            }
            Session.setNickname(nicknameField.getText());
            router.show(ScreenId.MODE_SELECT);
        });
        guideBtn.addActionListener(e -> router.show(ScreenId.GUIDE));
        rankBtn.addActionListener(e -> router.show(ScreenId.RANKING));
    }

    //배경 꽉 채우기
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 패널 크기에 맞춰 배경을 채움
        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    }


    private void styleTransparentHotspot(JButton b) {
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setForeground(new Color(0,0,0,0));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        if (DEBUG_HOTSPOT) {
            // ★ 디버그 모드: 반투명 배경 + 테두리로 위치 보이게
            b.setContentAreaFilled(true);
            b.setOpaque(true);
            b.setBackground(new Color(255, 0, 255, 60));  // 연보라 반투명
            b.setBorder(new LineBorder(new Color(120, 0, 120), 2));
            b.setForeground(Color.BLACK); // 글자 보이게(원하면 지워도 됨)
            b.setText(b.getActionCommand()); // 버튼 이름 보여주기
        }
    }


    /** 버튼을 완전 투명하게 만들고, 호버/포커스 UX를 가볍게 주는 공통 스타일
    private void styleTransparentButton(JButton b) {
        b.setContentAreaFilled(false);   // 배경 채우기 끔
        b.setBorderPainted(false);       // 테두리 끔
        b.setFocusPainted(false);        // 포커스 테두리 끔
        b.setOpaque(false);              // 불투명 끔
        b.setForeground(Color.BLACK);    // 텍스트만 보이게

        // 마우스 올리면 살짝 강조
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addChangeListener(e -> {
            boolean hover = b.getModel().isRollover();
            b.setForeground(hover ? new Color(20, 20, 20) : Color.BLACK);
        });

        // 키보드 접근성(스페이스/엔터) 기본 동작 유지됨
    }
*/
}
