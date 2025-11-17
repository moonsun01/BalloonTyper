package com.balloon.ui.screens;

import com.balloon.game.WordProvider;
import com.balloon.game.model.Balloon;
import com.balloon.game.VersusGameRules;
import com.balloon.core.GameContext;
import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.Session;
import com.balloon.core.Showable;
import com.balloon.net.VersusClient;
import com.balloon.ui.hud.HUDRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VersusGamePanel extends JPanel implements Showable {

    private final ScreenRouter router;

    private final Image bgImage;
    private String p1Name = "player1";
    private String p2Name = "player2";

    // 풍선 PNG 5종
    private final Image balloonGreen;
    private final Image balloonOrange;
    private final Image balloonPink;
    private final Image balloonPurple;
    private final Image balloonYellow;

    // 집 PNG
    private final Image houseImg =
            new ImageIcon(getClass().getResource("/images/home.png")).getImage();

    // 하단 입력창
    private final JTextField inputField = new JTextField();

    // 닉네임 폰트
    private static final Font NAME_FONT =
            HUDRenderer.HUD_FONT.deriveFont(
                    HUDRenderer.HUD_FONT.getSize2D() + 12.0f
            );

    // 점수 (시간/라이프는 싱글 전용)
    private int p1Score = 0;
    private int p2Score = 0;

    // 룰(점수/올클리어/정확도/승패 판정)
    private VersusGameRules rules;

    // 한 판당 기본 제한 시간(예: 60초) - 나중에 조정 가능
    private static final int INITIAL_TIME_SECONDS = 60;


    // 네트워크
    private VersusClient netClient;
    private String myRole = "P1";
    private boolean started = false;
    private boolean finished = false;

    // 한 플레이어가 시작할 때 가지고 있는 풍선 개수
    private static final int TOTAL_BALLOONS_PER_PLAYER = 30;  // 3+4+5+6+5+4+3

    // 플레이어별 남은 풍선 개수
    private int p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
    private int p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

    private WordProvider p1Words;
    private WordProvider p2Words;

    private final java.util.List<Balloon> p1Balloons = new java.util.ArrayList<>();
    private final java.util.List<Balloon> p2Balloons = new java.util.ArrayList<>();


    // 결과 상태
    private enum ResultState {
        NONE, P1_WIN, P2_WIN, DRAW
    }

    private ResultState resultState = ResultState.NONE;

    // 결과가 나온 뒤 RETRY/HOME 오버레이를 띄울지 여부
    private boolean showRetryOverlay = false;

    // [ADD] 마우스로 클릭할 영역(RETTRY, HOME)
    private Rectangle retryRect = null;
    private Rectangle homeRect  = null;

    // 풍선 구조 3·4·5·6·5·4·3
    private static final int[] ROW_STRUCTURE = {3, 4, 5, 6, 5, 4, 3};

    public VersusGamePanel(ScreenRouter router) {
        this.router = router;
        this.bgImage = new ImageIcon(
                getClass().getResource("/images/DUAL_BG.png")
        ).getImage();

        // 풍선 이미지 로드
        balloonGreen  = new ImageIcon(getClass().getResource("/images/balloon_green.png")).getImage();
        balloonOrange = new ImageIcon(getClass().getResource("/images/balloon_orange.png")).getImage();
        balloonPink   = new ImageIcon(getClass().getResource("/images/balloon_pink.png")).getImage();
        balloonPurple = new ImageIcon(getClass().getResource("/images/balloon_purple.png")).getImage();
        balloonYellow = new ImageIcon(getClass().getResource("/images/balloon_yellow.png")).getImage();

        setBackground(new Color(167, 220, 255));
        setPreferredSize(new Dimension(1280, 720));
        setFocusable(true);

        // 레이아웃 + 하단 입력바
        setLayout(new BorderLayout());

        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.X_AXIS));

        bottom.add(Box.createHorizontalGlue());

        inputField.setFont(new Font("Dialog", Font.PLAIN, 18));
        inputField.setHorizontalAlignment(JTextField.CENTER);
        inputField.setMaximumSize(new Dimension(420, 40));
        inputField.setPreferredSize(new Dimension(420, 40));
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

        bottom.add(inputField);
        bottom.add(Box.createHorizontalGlue());

        add(bottom, BorderLayout.SOUTH);

        // 엔터 → 타이핑 처리
        inputField.addActionListener(e -> {
            String typed = inputField.getText();
            onEnterTyped(typed);
            inputField.setText("");
        });

        // ★ inputField 에 키 리스너 달기
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (code == KeyEvent.VK_1) {
                    // 테스트: P1이 이긴 상황
                    startResultSequence(ResultState.P1_WIN);

                } else if (code == KeyEvent.VK_2) {
                    // 테스트: P2가 이긴 상황
                    startResultSequence(ResultState.P2_WIN);

                } else if (code == KeyEvent.VK_3) {
                    // 테스트: 무승부
                    startResultSequence(ResultState.DRAW);

                } else if (code == KeyEvent.VK_R) {
                    resultState = ResultState.NONE;
                    showRetryOverlay = false;
                    finished = false;
                    repaint();

                } else if (code == KeyEvent.VK_H) {
                    resultState = ResultState.NONE;
                    showRetryOverlay = false;
                    finished = true;
                    router.show(ScreenId.START);
                }
            }
        });




        // [ADD] 결과 화면 클릭 처리 (RETRY / HOME)
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                // ★ 결과가 안 났거나, 아직 오버레이 안 뜬 상태면 무시
                if (resultState == ResultState.NONE || !showRetryOverlay) return;

                Point p = e.getPoint();

                if (retryRect != null && retryRect.contains(p)) {
                    handleRetryClicked();
                } else if (homeRect != null && homeRect.contains(p)) {
                    handleHomeClicked();
                }
            }
        });



    }


    // 플레이어 이름 표시
    private void drawPlayerNames(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.setFont(NAME_FONT);

        int nameY = 40;

        int leftX = 20;
        g2.drawString(p1Name, leftX, nameY);

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(p2Name);
        int rightMargin = 20;
        int rightX = w - rightMargin - textWidth;
        g2.drawString(p2Name, rightX, nameY);
    }

    // 집 그리기
    private void drawHouseArea(Graphics2D g2, int centerX, int panelHeight) {
        int groundMargin = 60;
        int baseY = panelHeight - groundMargin;

        double houseScale = 0.3; // 집 크기 비율

        int origW = houseImg.getWidth(this);
        int origH = houseImg.getHeight(this);

        int houseW = (int) (origW * houseScale);
        int houseH = (int) (origH * houseScale);

        int houseX = centerX - houseW / 2;
        int houseY = baseY - houseH;

        g2.drawImage(houseImg, houseX, houseY, houseW, houseH, null);
    }

    // 풍선 PNG 랜덤 선택
    private Image pickRandomBalloonImage() {
        int r = (int) (Math.random() * 5);
        switch (r) {
            case 0:
                return balloonGreen;
            case 1:
                return balloonOrange;
            case 2:
                return balloonPink;
            case 3:
                return balloonPurple;
            default:
                return balloonYellow;
        }
    }


    // 풍선 좌표 계산
    private List<Point> buildBalloonPositions(double anchorX, double anchorY) {
        List<Point> pos = new ArrayList<>();

        int rowCount = ROW_STRUCTURE.length;
        int baseSpacingY = 65; //세로 간격
        int baseSpacingX = 80; //가로 간격

        int offsetDown = 30;      // 전체를 아래로 30px 내리기
        int offsetLeft  = -30;    // 전체 왼쪽으로. 0보다 작으면 왼쪽, 크면 오른쪽

        for (int r = 0; r < rowCount; r++) {
            int count = ROW_STRUCTURE[r];

            double totalWidth = (count - 1) * baseSpacingX;
            double startX = anchorX - totalWidth / 2.0;

            // ↓ 전체를 offsetDown 만큼 내려줌
            double y = anchorY - r * baseSpacingY + offsetDown;

            for (int i = 0; i < count; i++) {
                double x = startX + i * baseSpacingX + offsetLeft;
                pos.add(new Point((int) x, (int) y));
            }
        }
        return pos;
    }

    // 풍선 클러스터 + 줄
    private void drawBalloonCluster(Graphics2D g2,
                                    List<Point> positions,
                                    int centerX,
                                    int panelHeight,
                                    boolean leftSide) {

        int groundMargin = 90;
        int baseY = panelHeight - groundMargin;
        int anchorY = baseY - 60; //집 지붕 위쪽 근처

        int balloonSize = 65;

        g2.setStroke(new BasicStroke(1.5f));

        // 줄
        g2.setColor(new Color(235, 235, 235));
        for (Point p : positions) {
            int bx = p.x;
            int by = p.y;

            g2.drawLine(centerX, anchorY,
                    bx + balloonSize / 2,
                    by + balloonSize);
        }
        // 2) 그 다음 풍선 PNG를 전부 그리기 → "앞" 레이어
        for (Point p : positions) {
            int bx = p.x;
            int by = p.y;

            Image img = pickRandomBalloonImage();
            g2.drawImage(img, bx, by, balloonSize, balloonSize, null);
        }
    }

    // 화면에 들어올 때
    @Override
    public void onShown() {
        // ★ 결과 화면에서 돌아왔을 때를 대비해서 입력창 활성화/보이기
        inputField.setEnabled(true);
        inputField.setVisible(true);

        p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
        p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

        // ★ 듀얼 룰도 새 판으로 초기화
        rules = new VersusGameRules(INITIAL_TIME_SECONDS);

        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());

        String nameFromSession = Session.getNickname();
        if (nameFromSession == null || nameFromSession.isBlank()) {
            nameFromSession = "player";
        }

        p1Name = nameFromSession;
        p2Name = "opponent";
        repaint();

        String host = JOptionPane.showInputDialog(this, "서버 IP를 입력하세요", "127.0.0.1");
        if (host == null || host.isBlank()) {
            host = "127.0.0.1";
        }

        try {
            netClient = new VersusClient(host, 5555, nameFromSession);

            Thread t = new Thread(this::networkLoop);
            t.setDaemon(true);
            t.start();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "서버 접속 실패: " + ex.getMessage());
        }
    }

    private void networkLoop() {
        try {
            String line;
            while ((line = netClient.readLine()) != null) {
                final String msg = line.trim();
                System.out.println("[CLIENT] << " + msg);

                if (msg.startsWith("ROLE ")) {
                    String role = msg.substring(5).trim();
                    myRole = role;
                } else if (msg.equals("START")) {
                    SwingUtilities.invokeLater(() -> {
                        started = true;
                        repaint();
                    });
                } else if (msg.startsWith("POP ")) {
                    String[] parts = msg.split(" ", 3);
                    if (parts.length == 3) {
                        String who = parts[1];
                        String word = parts[2];
                        SwingUtilities.invokeLater(() -> onRemotePop(who, word));
                    }
                } else if (msg.startsWith("RESULT")) {
                    String[] parts = msg.split(" ");
                    String keyword = (parts.length >= 2) ? parts[1].trim() : "";

                    ResultState state;
                    if ("DRAW".equalsIgnoreCase(keyword)) {
                        state = ResultState.DRAW;
                    } else {
                        boolean isWin = "WIN".equalsIgnoreCase(keyword);
                        if ("P1".equals(myRole)) {
                            state = isWin ? ResultState.P1_WIN : ResultState.P2_WIN;
                        } else {
                            state = isWin ? ResultState.P2_WIN : ResultState.P1_WIN;
                        }
                    }
                    final ResultState finalState = state;
                    SwingUtilities.invokeLater(() -> startResultSequence(finalState));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 서버에서 POP 수신
    private void onRemotePop(String who, String word) {
        int scoreDelta = 10;

        if ("P1".equals(who)) {
            if (p1Remaining > 0) {
                p1Remaining--;
            }
            p1Score += scoreDelta;
        } else if ("P2".equals(who)) {
            if (p2Remaining > 0) {
                p2Remaining--;
            }
            p2Score += scoreDelta;
        }

        // ★ VersusGameRules에도 반영
        if (rules != null) {
            int playerIndex = "P1".equals(who) ? 1 : 2;
            boolean allCleared;
            if (playerIndex == 1) {
                allCleared = (p1Remaining <= 0);
            } else {
                allCleared = (p2Remaining <= 0);
            }
            rules.onPop(playerIndex, scoreDelta, allCleared);
        }

        repaint();
    }



//    // 서버에서 RESULT 수신
//    private void showResultOverlay(boolean isWin) {
//        if ("P1".equals(myRole)) {
//            resultState = isWin ? ResultState.P1_WIN : ResultState.P2_WIN;
//        } else {
//            resultState = isWin ? ResultState.P2_WIN : ResultState.P1_WIN;
//        }
//
//        // 1단계: WIN / LOSE만 먼저 그림
//        showRetryOverlay = false;
//        repaint();
//
//        // 2단계: 1초 뒤에 RETRY/HOME 오버레이 등장
//        javax.swing.Timer t = new javax.swing.Timer(1000, e -> {
//            showRetryOverlay = true;
//            repaint();
//            ((javax.swing.Timer) e.getSource()).stop();
//        });
//        t.setRepeats(false);
//        t.start();
//    }





    // 내 풍선 하나 제거(임시: 점수만)
    private void removeMyBalloon(String typedWord) {
        int scoreDelta = 10;

        if ("P1".equals(myRole)) {
            if (p1Remaining > 0) {
                p1Remaining--;
            }
            p1Score += scoreDelta;
        } else if ("P2".equals(myRole)) {
            if (p2Remaining > 0) {
                p2Remaining--;
            }
            p2Score += scoreDelta;
        }

        // ★ VersusGameRules에도 반영
        if (rules != null) {
            int playerIndex = "P1".equals(myRole) ? 1 : 2;
            boolean allCleared = myAllCleared();
            rules.onPop(playerIndex, scoreDelta, allCleared);
        }

        repaint();
    }

    // 🔹 내 역할(P1/P2)에 따라 내 풍선 리스트 반환
    private java.util.List<Balloon> getMyBalloonList() {
        if ("P1".equals(myRole)) {
            return p1Balloons;
        } else if ("P2".equals(myRole)) {
            return p2Balloons;
        }
        // 혹시 역할 못 받았을 때 안전빵
        return p1Balloons;
    }

    /**
     * 내가 입력한 단어로 내 풍선 리스트에서 일치하는 풍선을 찾아서 터뜨린다.
     *
     * @return true  실제로 풍선 하나가 터졌다면
     *         false 매칭되는 풍선이 없으면
     */
    private boolean tryPopMyBalloon(String typedWord) {
        if (typedWord == null || typedWord.isBlank()) return false;

        String trimmed = typedWord.trim();
        java.util.List<Balloon> myList = getMyBalloonList();

        // 리스트가 아직 비어 있으면(스폰 전 등) → 아직 POP 못 함
        if (myList.isEmpty()) {
            return false;
        }

        for (Balloon b : myList) {
            if (!b.isActive()) continue;
            if (trimmed.equals(b.getWord())) {   // 단어 일치
                b.pop();
                // 남은 개수 카운트는 기존 로직 유지(다음 단계에서 정교하게 맞출 수 있음)
                return true;
            }
        }
        return false;
    }




    // 아직은 항상 false (나중에 진짜 풍선 리스트 만들 때 수정)
    private boolean myAllCleared() {
        if ("P1".equals(myRole)) {
            return p1Remaining <= 0;
        } else if ("P2".equals(myRole)) {
            return p2Remaining <= 0;
        }
        return false;
    }


    // HUD(Score만)
    private void drawHud(Graphics2D g2, int w, int h) {
        g2.setFont(HUDRenderer.HUD_FONT);
        g2.setColor(Color.BLACK);

        FontMetrics fm = g2.getFontMetrics();
        int baseY = 70;

        String p1ScoreText = "Score : " + p1Score;
        int leftX = 18;
        g2.drawString(p1ScoreText, leftX, baseY);

        String p2ScoreText = "Score : " + p2Score;
        int rightMargin = 18;
        int p2X = w - rightMargin - fm.stringWidth(p2ScoreText);
        g2.drawString(p2ScoreText, p2X, baseY);
    }

    // 내가 엔터쳤을 때
    private void onEnterTyped(String typedWord) {
        if (!started || finished) return;

        typedWord = typedWord.trim();
        if (typedWord.isEmpty()) return;

        removeMyBalloon(typedWord);

        if (netClient != null) {
            netClient.sendPop(typedWord);
        }

        if (myAllCleared() && !finished) {
            finished = true;
            if (netClient != null) netClient.sendFinish();
        }
    }

    // 결과 오버레이
    private void drawResultOverlay(Graphics2D g2, int w, int h) {
        if (resultState == ResultState.NONE) return;

        Font oldFont = g2.getFont();
        Font bigFont = NAME_FONT.deriveFont(NAME_FONT.getSize2D() + 8.0f);
        g2.setFont(bigFont);
        FontMetrics fm = g2.getFontMetrics();

        int centerLeftX  = w / 4;
        int centerRightX = w * 3 / 4;
        int centerY      = h / 2;

        String leftText  = "";
        String rightText = "";
        Color leftColor  = Color.BLACK;
        Color rightColor = Color.BLACK;

        switch (resultState) {
            case P1_WIN:
                leftText  = "WIN !";
                rightText = "LOSE";
                leftColor = Color.BLACK;
                rightColor = new Color(255, 80, 80);
                break;

            case P2_WIN:
                leftText  = "LOSE";
                rightText = "WIN !";
                leftColor = new Color(255, 80, 80);
                rightColor = Color.BLACK;
                break;

            case DRAW:
                leftText  = "DRAW";
                rightText = "DRAW";
                leftColor = rightColor = Color.BLACK;
                break;
            default:
                break;
        }

        int leftW  = fm.stringWidth(leftText);
        int rightW = fm.stringWidth(rightText);

        // ── 1단계: WIN / LOSE 텍스트만 (밝은 배경 위) ──
        if (!showRetryOverlay) {
            g2.setColor(leftColor);
            g2.drawString(leftText, centerLeftX - leftW / 2, centerY);

            g2.setColor(rightColor);
            g2.drawString(rightText, centerRightX - rightW / 2, centerY);

            g2.setFont(oldFont);
            return;
        }

        // ── 2단계: 화면 어둡게 덮기 ──
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(oldComp);

        // 어두운 배경 위에 다시 WIN / LOSE
        g2.setFont(bigFont);
        fm = g2.getFontMetrics();

        g2.setColor(leftColor);
        g2.drawString(leftText, centerLeftX - leftW / 2, centerY);

        g2.setColor(rightColor);
        g2.drawString(rightText, centerRightX - rightW / 2, centerY);

        // ── RETRY? / HOME 텍스트 버튼 ──
        String retryText = "RETRY";
        String homeText  = "HOME";

        Font buttonFont = HUDRenderer.HUD_FONT
                .deriveFont(HUDRenderer.HUD_FONT.getSize2D() + 6.0f);
        g2.setFont(buttonFont);
        FontMetrics fmBtn = g2.getFontMetrics();

        int buttonW = 200;
        int buttonH = 60;
        int gap = 40;                 // 두 버튼 사이 간격

        int centerX = w / 2;
        int btnTop = centerY + 70;    // WIN/LOSE 아래쪽 위치

        int retryX = centerX - buttonW - gap / 2;  // 왼쪽 버튼
        int homeX  = centerX + gap / 2;            // 오른쪽 버튼

        Color btnBg = new Color(0, 0, 0, 150);     // 약간 어두운 배경색
        g2.setStroke(new BasicStroke(3f));

        // --- RETRY 버튼 ---
        g2.setColor(btnBg);
        g2.fillRoundRect(retryX, btnTop, buttonW, buttonH, 18, 18);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(retryX, btnTop, buttonW, buttonH, 18, 18);

        int retryTextW = fmBtn.stringWidth(retryText);
        int retryTextX = retryX + (buttonW - retryTextW) / 2;
        int retryTextY = btnTop + (buttonH + fmBtn.getAscent()) / 2 - 4;
        g2.drawString(retryText, retryTextX, retryTextY);

        // --- HOME 버튼 ---
        g2.setColor(btnBg);
        g2.fillRoundRect(homeX, btnTop, buttonW, buttonH, 18, 18);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(homeX, btnTop, buttonW, buttonH, 18, 18);

        int homeTextW = fmBtn.stringWidth(homeText);
        int homeTextX = homeX + (buttonW - homeTextW) / 2;
        int homeTextY = btnTop + (buttonH + fmBtn.getAscent()) / 2 - 4;
        g2.drawString(homeText, homeTextX, homeTextY);

        // 마우스 클릭 판정용 영역을 버튼 크기에 맞게 갱신
        retryRect = new Rectangle(retryX, btnTop, buttonW, buttonH);
        homeRect  = new Rectangle(homeX,  btnTop, buttonW, buttonH);

        g2.setFont(oldFont);
    }



    @Override
    public void onHidden() {
        // 아직 특별히 할 일 없음
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        int w = getWidth();
        int h = getHeight();

        g2.drawImage(bgImage, 0, 0, w, h, this);

        drawPlayerNames(g2, w, h);
        drawHud(g2, w, h);

        int centerLeft = w / 4;
        int centerRight = w * 3 / 4;

        drawHouseArea(g2, centerLeft, h);
        drawHouseArea(g2, centerRight, h);

        // 풍선 앵커 높이(대략 집 위쪽)
        double balloonAnchorY = h - 260;

        List<Point> leftPos = buildBalloonPositions(centerLeft, balloonAnchorY);
        List<Point> rightPos = buildBalloonPositions(centerRight, balloonAnchorY);

        drawBalloonCluster(g2, leftPos, centerLeft, h, true);
        drawBalloonCluster(g2, rightPos, centerRight, h, false);

        drawResultOverlay(g2, w, h);
    }

    // ★ 공통 결과 연출: WIN/LOSE 표시 → 2초 후 RETRY/HOME 오버레이
    // ★ 공통 결과 연출: WIN/LOSE 표시 → 2초 후 RETRY/HOME 오버레이
    private void startResultSequence(ResultState state) {

        // 1) GameContext에 Versus 결과 스냅샷 저장
        GameContext ctx = GameContext.getInstance();

        // ResultState → VersusWinner 매핑 (서버 기준 결과를 우선 반영)
        GameContext.VersusWinner winner;
        switch (state) {
            case P1_WIN:
                winner = GameContext.VersusWinner.P1;
                break;
            case P2_WIN:
                winner = GameContext.VersusWinner.P2;
                break;
            case DRAW:
                winner = GameContext.VersusWinner.DRAW;
                break;
            default:
                winner = GameContext.VersusWinner.NONE;
                break;
        }

        int p1ScoreSnapshot = p1Score;
        int p2ScoreSnapshot = p2Score;
        double p1AccSnapshot = 1.0;
        double p2AccSnapshot = 1.0;
        boolean p1ClearedSnapshot = false;
        boolean p2ClearedSnapshot = false;

        // ★ VersusGameRules가 있으면 거기 값으로 덮어쓰기
        if (rules != null) {
            VersusGameRules.PlayerState ps1 = rules.getP1();
            VersusGameRules.PlayerState ps2 = rules.getP2();

            p1ScoreSnapshot = ps1.getScore();
            p2ScoreSnapshot = ps2.getScore();
            p1AccSnapshot = ps1.getAccuracy();   // 0.0 ~ 1.0
            p2AccSnapshot = ps2.getAccuracy();
            p1ClearedSnapshot = ps1.isCleared();
            p2ClearedSnapshot = ps2.isCleared();
        }

        GameContext.VersusSnapshot snapshot =
                new GameContext.VersusSnapshot(
                        p1ScoreSnapshot,
                        p2ScoreSnapshot,
                        p1AccSnapshot,
                        p2AccSnapshot,
                        p1ClearedSnapshot,
                        p2ClearedSnapshot,
                        winner
                );

        ctx.setVersusSnapshot(snapshot);

        // 2) 기존 연출 로직
        resultState = state;
        finished = true;
        showRetryOverlay = false;

        inputField.setEnabled(false);
        inputField.setVisible(false);

        repaint();

        javax.swing.Timer t = new javax.swing.Timer(2000, e -> {
            showRetryOverlay = true;
            repaint();
            ((javax.swing.Timer) e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }


    // HOME 클릭 시 동작
    private void handleHomeClicked() {
        try {
            if (netClient != null) {
                netClient.close();
            }
        } catch (Exception ignore) {}

        resultState = ResultState.NONE;
        finished = true;
        showRetryOverlay = false;

        // 홈으로 나갈 때는 입력창은 다음에 듀얼 모드 들어올 때 onShown()에서 다시 켜짐
        router.show(ScreenId.START);
    }

    // RETRY 클릭 시 동작
    private void handleRetryClicked() {
        finished = false;
        resultState = ResultState.NONE;
        showRetryOverlay = false;
        p1Score = 0;
        p2Score = 0;

        p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
        p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

        // ★ 룰도 새 판으로 초기화
        rules = new VersusGameRules(INITIAL_TIME_SECONDS);


        // 입력창 다시 보이게 + 포커스
        inputField.setEnabled(true);
        inputField.setVisible(true);
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());

        repaint();

        if (netClient != null) {
            netClient.sendRetry();   // 서버에 다시 시작 알림 (이미 만들어둔 메서드)
        }
    }


}
