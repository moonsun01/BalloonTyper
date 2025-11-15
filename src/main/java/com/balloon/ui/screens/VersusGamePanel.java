package com.balloon.ui.screens;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.Showable;
import com.balloon.core.Session;

import com.balloon.ui.hud.HUDRenderer;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;



import javax.swing.*;
import java.awt.*;

/**
 * VersusGamePanel
 * - 2인용(듀얼) 게임 화면 뼈대
 * - 지금은 그냥 빈 하늘색 배경만 있고,
 *   나중에 풍선/집/HUD/WIN·LOSE를 채워 넣을 예정
 */
public class VersusGamePanel extends JPanel implements Showable {

    private final ScreenRouter router;

    private final Image bgImage;
    private String p1Name = "player1";  // 임시
    private String p2Name = "player2";  // 임시

    // [ADD] 닉네임 전용 폰트 (HUD_FONT보다 조금 크게)
    private static final Font NAME_FONT =
            HUDRenderer.HUD_FONT.deriveFont(
                    HUDRenderer.HUD_FONT.getSize2D() + 12.0f   // +4 정도 키우기
            );

    // [ADD] 듀얼 결과 상태
    private enum ResultState {
        NONE,       // 아직 진행 중
        P1_WIN,     // 왼쪽 승
        P2_WIN,     // 오른쪽 승
        DRAW        // 무승부 (필요하면 사용)
    }

    private ResultState resultState = ResultState.NONE;

    public VersusGamePanel(ScreenRouter router) {
        this.router = router;
        this.bgImage = new ImageIcon(
                getClass().getResource("/images/DUAL_BG.png")
        ).getImage();

        setBackground(new Color(167, 220, 255));
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);

        setBackground(new Color(167, 220, 255));
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);

        // [TEMP] 테스트용 키 리스너 (나중에 게임 로직 붙이면 제거/수정 가능)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                // 숫자 1 → 왼쪽 승, 2 → 오른쪽 승, 3 → 무승부
                if (code == KeyEvent.VK_1) {
                    resultState = ResultState.P1_WIN;
                    repaint();
                } else if (code == KeyEvent.VK_2) {
                    resultState = ResultState.P2_WIN;
                    repaint();
                } else if (code == KeyEvent.VK_3) {
                    resultState = ResultState.DRAW;
                    repaint();
                }
                // R : 결과 리셋(게임 진행 중 상태로)
                else if (code == KeyEvent.VK_R) {
                    resultState = ResultState.NONE;
                    repaint();
                }
                // H : 홈(START) 화면으로 이동
                else if (code == KeyEvent.VK_H) {
                    resultState = ResultState.NONE;
                    router.show(ScreenId.START);
                }
            }
        });

    }

    // [ADD] 여기!  ← 클래스 안, 생성자 아래, paintComponent 위
    private void drawPlayerNames(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.setFont(NAME_FONT); //닉네임은 더 큰 폰트 사용

        // 공통 위쪽 정렬 Y 값
        int nameY = 40;

        // 왼쪽 이름: 왼쪽 여백 20
        int leftX = 20;
        g2.drawString(p1Name, leftX, nameY);

        // 오른쪽 이름: 오른쪽 여백 20을 기준으로 오른쪽 정렬
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(p2Name);
        int rightMargin = 20;
        int rightX = w - rightMargin - textWidth;

        g2.drawString(p2Name, rightX, nameY);
    }

    // 집 + 받침대 그리기 (centerX 기준)
    private void drawHouseArea(Graphics2D g2, int centerX, int panelHeight) {
        // 받침대
        int baseWidth = 200;
        int baseHeight = 20;
        int baseY = panelHeight - 80;  // 화면 아래에서 조금 띄우기

        int baseX = centerX - baseWidth / 2;

        g2.setColor(Color.WHITE);
        g2.fillRect(baseX, baseY, baseWidth, baseHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(baseX, baseY, baseWidth, baseHeight);

        // 집
        int houseWidth = 40;
        int houseHeight = 32;
        int houseX = centerX - houseWidth / 2;
        int houseY = baseY - houseHeight;

        // 집 몸통
        g2.setColor(new Color(255, 220, 120));
        g2.fillRect(houseX, houseY + 8, houseWidth, houseHeight - 8);
        g2.setColor(Color.BLACK);
        g2.drawRect(houseX, houseY + 8, houseWidth, houseHeight - 8);

        // 지붕
        Polygon roof = new Polygon();
        roof.addPoint(houseX - 4, houseY + 8);
        roof.addPoint(houseX + houseWidth / 2, houseY);
        roof.addPoint(houseX + houseWidth + 4, houseY + 8);

        g2.setColor(new Color(255, 80, 80));
        g2.fillPolygon(roof);
        g2.setColor(Color.BLACK);
        g2.drawPolygon(roof);

        // 나중에 풍선 줄(anchor) 위치 잡을 때 houseY / centerX 사용 가능
    }

    // 풍선 자리(원 + 줄) 간단히 여러 개 그리기
    private void drawBalloonCluster(Graphics2D g2, int centerX, int panelHeight, boolean leftSide) {
        int baseY = panelHeight - 80;  // 받침대 Y와 맞추기
        int anchorY = baseY - 32;      // 집 지붕 위쪽쯤

        g2.setStroke(new BasicStroke(1.5f));

        // 10개 정도 간단히 배치 (실제 로직은 B팀이 BalloonSprite로 교체)
        for (int i = 0; i < 10; i++) {
            int offsetX = (i % 5) * 60 - 120;   // 좌우 벌어지게
            int offsetY = (i / 5) * -60 - 40;   // 위로 올라가게

            int bx = centerX + offsetX;
            int by = anchorY + offsetY;

            // 줄
            g2.setColor(new Color(240, 240, 240));
            g2.drawLine(centerX, anchorY, bx + 16, by + 16);

            // 풍선(간단한 원)
            g2.setColor(new Color(255, 255, 255, 220));
            g2.fillOval(bx, by, 32, 32);
            g2.setColor(new Color(130, 130, 180));
            g2.drawOval(bx, by, 32, 32);
        }
    }


    // 화면에 들어올 때
    @Override
    public void onShown() {
        // 포커스 먼저 잡아주고
        SwingUtilities.invokeLater(this::requestFocusInWindow);

        // ★ Start 화면에서 저장해 둔 닉네임 가져오기
        String nameFromSession = Session.getNickname();

        if (nameFromSession == null || nameFromSession.isBlank()) {
            p1Name = "player1";   // 아무것도 안 적었을 때 기본값
        } else {
            p1Name = nameFromSession;
        }

        // ★ 상대 이름은 일단 임시로 고정 (원하면 "player2" 등으로 바꿔도 됨)
        p2Name = "COMPUTER";

        // 이름 바뀌었으니 다시 그리기
        repaint();
    }

    // 2P HUD (점수만) 그리기
    private void drawHud(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.setFont(HUDRenderer.HUD_FONT);


        int nameY   = 40;      // 이름 Y와 비슷한 높이
        int lineGap = 22;

        // ----- P1 SCORE (왼쪽 위) -----
        int leftX   = 20;
        int scoreY  = nameY + lineGap;

        String p1ScoreText = "SCORE : 000";   // 지금은 더미 값

        g2.drawString(p1ScoreText, leftX, scoreY);

        // ----- P2 SCORE (오른쪽 위, 오른쪽 정렬) -----
        String p2ScoreText = "SCORE : 000";

        FontMetrics fm = g2.getFontMetrics();
        int rightMargin = 20;
        int p2ScoreX = w - rightMargin - fm.stringWidth(p2ScoreText);

        g2.drawString(p2ScoreText, p2ScoreX, scoreY);
    }


    // [ADD] 결과 오버레이 (WIN / LOSE + 회색 배경)
    private void drawResultOverlay(Graphics2D g2, int w, int h) {
        if (resultState == ResultState.NONE) return; // 아직 게임 중이면 안 그림

        // 반투명 회색 배경
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(oldComp);

        // 텍스트 색/폰트
        g2.setColor(Color.WHITE);
        Font oldFont = g2.getFont();
        Font bigFont = NAME_FONT.deriveFont(NAME_FONT.getSize2D() + 8.0f);
        g2.setFont(bigFont);

        // 왼쪽/오른쪽 중심
        int centerLeftX  = w / 4;
        int centerRightX = w * 3 / 4;
        int centerY      = h / 2;

        FontMetrics fm = g2.getFontMetrics();

        String leftText  = "";
        String rightText = "";

        switch (resultState) {
            case P1_WIN -> {
                leftText  = "WIN !";
                rightText = "LOSE";
            }
            case P2_WIN -> {
                leftText  = "LOSE";
                rightText = "WIN !";
            }
            case DRAW -> {
                leftText  = "DRAW";
                rightText = "DRAW";
            }
        }

        int leftW  = fm.stringWidth(leftText);
        int rightW = fm.stringWidth(rightText);

        g2.drawString(leftText,  centerLeftX  - leftW / 2,  centerY);
        g2.drawString(rightText, centerRightX - rightW / 2, centerY);

        // 아래쪽에 RETRY / HOME 안내 텍스트
        String retryHome = "RETRY : R   /   HOME : H";
        Font smallFont = HUDRenderer.HUD_FONT.deriveFont(HUDRenderer.HUD_FONT.getSize2D() + 2.0f);
        g2.setFont(smallFont);
        fm = g2.getFontMetrics();

        int msgW = fm.stringWidth(retryHome);
        int msgX = (w - msgW) / 2;
        int msgY = centerY + 80;

        g2.drawString(retryHome, msgX, msgY);

        g2.setFont(oldFont);
    }


    // 화면에서 나갈 때
    @Override
    public void onHidden() {
        // 아직 특별히 할 건 없음
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();

        // 1) 배경 이미지 전체 깔기
        g2.drawImage(bgImage, 0, 0, w, h, this);

        // 2) 플레이어 이름 그리기
        drawPlayerNames(g2, w, h);
        drawHud(g2, w, h);

        // ===== 3) 왼쪽/오른쪽 집 + 받침대 그리기 =====
        int centerLeft  = w / 4;
        int centerRight = w * 3 / 4;

        drawHouseArea(g2, centerLeft, h);   // P1 집
        drawHouseArea(g2, centerRight, h);  // P2 집

        // ===== 4) 왼쪽/오른쪽 풍선 클러스터(placeholder) =====
        drawBalloonCluster(g2, centerLeft, h, true);   // P1 풍선 자리
        drawBalloonCluster(g2, centerRight, h, false); // P2 풍선 자리

        // ===== 5) 결과 오버레이 (게임 종료 시) =====
        drawResultOverlay(g2, w, h);

    }

}
