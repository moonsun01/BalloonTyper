package com.balloon.ui.screens;

import com.balloon.core.GameContext;
import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.core.Session;
import com.balloon.core.Showable;
import com.balloon.game.StaticWordProvider;
import com.balloon.game.VersusGameRules;
import com.balloon.game.WordProvider;
import com.balloon.game.model.Balloon;
import com.balloon.items.Item;
import com.balloon.items.ItemEffectApplier;
import com.balloon.items.ItemKind;
import com.balloon.net.VersusClient;
import com.balloon.ui.hud.HUDRenderer;
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
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

    // Reverse 중앙 오버레이 메시지
    private String centerReverseMessage = null;
    private long centerReverseMessageEnd = 0;

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

    // [ADD] 듀얼 인트로 단계 관리
    private enum IntroPhase {
        NONE,       // 평소 상태 (인트로 없음)
        MISSION,    // "상대보다 풍선을 먼저 터뜨리세요!"
        START       // "START!"
    }

    private IntroPhase introPhase = IntroPhase.NONE;

    // 듀얼 룰(점수/정확도/올클리어/승패)
    private VersusGameRules rules;
    private static final int INITIAL_TIME_SECONDS = 60;

    // 네트워크
    private VersusClient netClient;
    private String myRole = "P1";   // "P1" 또는 "P2"
    private boolean started = false;
    private boolean finished = false;

    // 나(me)가 맡은 역할과 반대편(상대방) 역할을 구하는 헬퍼
    private String getRivalRole() {
        if ("P1".equals(myRole)) {
            return "P2";
        } else if ("P2".equals(myRole)) {
            return "P1";
        } else {
            // 혹시 모를 예외 상황: 기본값은 그냥 내 역할 그대로
            return myRole;
        }
    }

    // 한 플레이어가 시작할 때 풍선 개수
    private static final int TOTAL_BALLOONS_PER_PLAYER = 30;  // 3+4+5+6+5+4+3

    // 남은 풍선 개수
    private int p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
    private int p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

    // 랜덤 (풍선 색, 아이템)
    private final Random rnd = new Random();
    private final Map<Balloon, Image> balloonImages = new HashMap<>();

    // 아이템 적용기
    private ItemEffectApplier itemApplier;
    // 풍선에 붙은 아이템 (텍스트 색칠용)
    private final Map<Balloon, Item> itemBalloons = new HashMap<>();

    // reverse 상태 (0 = P1, 1 = P2)
    private final boolean[] reverseActive = new boolean[2];
    private final long[] reverseEndMillis = new long[2];

    // reverse 남은 초 (0 = P1, 1 = P2)
    private final int[] reverseRemainSeconds = new int[2];

    // reverse / 중앙 문구 자동 갱신용 타이머
    private javax.swing.Timer reverseTimer;

    // 단어 공급기
    private WordProvider p1Words;
    private WordProvider p2Words;

    // 풍선 리스트
    private final java.util.List<Balloon> p1Balloons = new ArrayList<>();
    private final java.util.List<Balloon> p2Balloons = new ArrayList<>();

    // 결과 상태
    private enum ResultState {
        NONE, P1_WIN, P2_WIN, DRAW
    }

    private ResultState resultState = ResultState.NONE;

    // === 아이템 토스트(싱글 모드랑 같은 박스 디자인) ===
    private String itemToastText = null;   // 표시할 문구
    private long   itemToastExpireAt = 0L; // 끝나는 시간(ms)
    private boolean itemToastPositive = true; // 좋은 효과인지(색 구분용)

    // 결과 후 Retry/Home 오버레이 표시 여부
    private boolean showRetryOverlay = false;
    private Rectangle retryRect = null;
    private Rectangle homeRect  = null;

    // 풍선 구조 3·4·5·6·5·4·3
    private static final int[] ROW_STRUCTURE = {3, 4, 5, 6, 5, 4, 3};

    // BLIND 상태(반쪽 가리기용)
    private boolean blindP1 = false;
    private boolean blindP2 = false;
    private long    blindEndP1 = 0L;
    private long    blindEndP2 = 0L;

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

        // 결과 화면 클릭 처리 (RETRY / HOME)
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
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

    // reverse 활성 여부
    // playerIndex: 0 = P1, 1 = P2
    private boolean isReverseActive(int playerIndex) {
        if (!reverseActive[playerIndex]) return false;

        long now = System.currentTimeMillis();
        if (now >= reverseEndMillis[playerIndex]) {
            // 시간이 지나면 자동으로 꺼준다
            reverseActive[playerIndex] = false;
            reverseRemainSeconds[playerIndex] = 0;
            return false;
        }
        return true;
    }

    private int getMyIndex() {
        return "P1".equals(myRole) ? 0 : 1;
    }

    private boolean isReverseActiveForMe() {
        return isReverseActive(getMyIndex());
    }

    public void activateReverseFor(int playerIndex, long durationMillis) {
        long now = System.currentTimeMillis();

        // reverse 상태 켜기
        reverseActive[playerIndex] = true;
        reverseEndMillis[playerIndex] = now + durationMillis;

        // 처음 시작 초 (예: 5000 -> 5초)
        reverseRemainSeconds[playerIndex] =
                (int) Math.ceil(durationMillis / 1000.0);

        // 타이머 돌리기
        ensureReverseTimerRunning();
    }

    // reverse 남은 시간, 중앙 문구를 주기적으로 갱신하는 타이머
    private void ensureReverseTimerRunning() {
        if (reverseTimer == null) {
            reverseTimer = new javax.swing.Timer(200, e -> {
                long now = System.currentTimeMillis();
                boolean anyActive = false;

                // 각 플레이어 reverse 남은 시간 계산
                for (int i = 0; i < 2; i++) {
                    if (reverseActive[i]) {
                        long remainMs = reverseEndMillis[i] - now;
                        if (remainMs <= 0) {
                            reverseActive[i] = false;
                            reverseRemainSeconds[i] = 0;
                        } else {
                            anyActive = true;
                            reverseRemainSeconds[i] =
                                    (int) Math.ceil(remainMs / 1000.0);
                        }
                    } else {
                        reverseRemainSeconds[i] = 0;
                    }
                }

                // 중앙 검정 박스 문구 시간도 여기서 같이 처리
                if (centerReverseMessage != null &&
                        now >= centerReverseMessageEnd) {
                    centerReverseMessage = null;
                }

                // 더 이상 보여줄 것이 없으면 타이머 멈춤
                if (!anyActive && centerReverseMessage == null) {
                    reverseTimer.stop();
                }

                repaint();
            });
            reverseTimer.setRepeats(true);
        }

        if (!reverseTimer.isRunning()) {
            reverseTimer.start();
        }
    }

    // 플레이어 이름 표시 (myRole에 따라 레이블만 바뀜, 보드는 항상 P1=왼쪽/P2=오른쪽)
    private void drawPlayerNames(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.setFont(NAME_FONT);

        int nameY = 40;

        String leftLabel;
        String rightLabel;

        // 화면 기준으로 P1 = 왼쪽, P2 = 오른쪽
        int remainP1 = reverseRemainSeconds[0];
        int remainP2 = reverseRemainSeconds[1];

        int remainLeft;
        int remainRight;

        if ("P1".equals(myRole)) {
            // 내가 P1이면 왼쪽이 ME, 오른쪽이 RIVAL
            leftLabel = "ME";
            rightLabel = "RIVAL";
            remainLeft = remainP1;
            remainRight = remainP2;
        } else if ("P2".equals(myRole)) {
            // 내가 P2이면 왼쪽이 RIVAL(P1), 오른쪽이 ME(P2)
            leftLabel = "RIVAL";
            rightLabel = "ME";
            remainLeft = remainP1;
            remainRight = remainP2;
        } else {
            leftLabel = "ME";
            rightLabel = "RIVAL";
            remainLeft = remainP1;
            remainRight = remainP2;
        }

        // 왼쪽 이름
        FontMetrics nameFm = g2.getFontMetrics();
        int leftX = 20;
        g2.drawString(leftLabel, leftX, nameY);
        int leftLabelW = nameFm.stringWidth(leftLabel);

        // 오른쪽 이름
        int rightMargin = 20;
        int rightLabelW = nameFm.stringWidth(rightLabel);
        int rightX = w - rightMargin - rightLabelW;
        g2.drawString(rightLabel, rightX, nameY);

        // 숫자 폰트
        Font numFont = HUDRenderer.HUD_FONT.deriveFont(
                HUDRenderer.HUD_FONT.getSize2D() + 6.0f
        );
        g2.setFont(numFont);
        FontMetrics numFm = g2.getFontMetrics();
        int numY = nameY + 24;

        g2.setColor(Color.BLACK);

        // 왼쪽 숫자
        if (remainLeft > 0) {
            String s = String.valueOf(remainLeft);
            int sw = numFm.stringWidth(s);
            int numX = leftX + (leftLabelW - sw) / 2;
            g2.drawString(s, numX, numY);
        }

        // 오른쪽 숫자
        if (remainRight > 0) {
            String s = String.valueOf(remainRight);
            int sw = numFm.stringWidth(s);
            int numX = rightX + (rightLabelW - sw) / 2;
            g2.drawString(s, numX, numY);
        }
    }

    // 집 그리기
    private void drawHouseArea(Graphics2D g2, int centerX, int panelHeight) {
        int groundMargin = 60;
        int baseY = panelHeight - groundMargin;

        double houseScale = 0.3;

        int origW = houseImg.getWidth(this);
        int origH = houseImg.getHeight(this);

        int houseW = (int) (origW * houseScale);
        int houseH = (int) (origH * houseScale);

        int houseX = centerX - houseW / 2;
        int houseY = baseY - houseH;

        g2.drawImage(houseImg, houseX, houseY, houseW, houseH, null);
    }

    // Balloon.Kind → 이미지
    private Image imageForKind(Balloon.Kind kind) {
        if (kind == null) {
            return balloonGreen;
        }
        switch (kind) {
            case RED:   return balloonPink;
            case GREEN: return balloonGreen;
            case BLUE:  return balloonPurple;
            default:    return balloonGreen;
        }
    }

    // 풍선 색상을 랜덤으로 선택 (RED / GREEN / BLUE)
    private Balloon.Kind randomKind() {
        Balloon.Kind[] kinds = {
                Balloon.Kind.RED,
                Balloon.Kind.GREEN,
                Balloon.Kind.BLUE
        };
        return kinds[rnd.nextInt(kinds.length)];
    }

    // 풍선 좌표 계산 (7줄 3·4·5·6·5·4·3)
    private java.util.List<Point> buildBalloonPositions(double anchorX, double anchorY) {
        java.util.List<Point> pos = new ArrayList<>();

        int rowCount = ROW_STRUCTURE.length;
        int baseSpacingY = 65;
        int baseSpacingX = 80;

        int offsetDown = 30;
        int offsetLeft  = -30;

        for (int r = 0; r < rowCount; r++) {
            int count = ROW_STRUCTURE[r];

            double totalWidth = (count - 1) * baseSpacingX;
            double startX = anchorX - totalWidth / 2.0;

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
                                    java.util.List<Balloon> balloons,
                                    int centerX,
                                    int panelHeight) {

        if (balloons == null || balloons.isEmpty()) return;

        int groundMargin = 90;
        int baseY = panelHeight - groundMargin;
        int anchorY = baseY - 60;

        int balloonSize = 65;

        // 줄 먼저
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(235, 235, 235));

        for (Balloon b : balloons) {
            if (b == null || !b.isActive()) continue;

            int bx = Math.round(b.getX());
            int by = Math.round(b.getY());

            g2.drawLine(centerX, anchorY,
                    bx + balloonSize / 2,
                    by + balloonSize);
        }

        // 풍선 + 텍스트
        for (Balloon b : balloons) {
            if (b == null || !b.isActive()) continue;

            int bx = Math.round(b.getX());
            int by = Math.round(b.getY());

            Image img = imageForKind(b.getKind());
            g2.drawImage(img, bx, by, balloonSize, balloonSize, null);

            String text = b.getText();
            if (text != null && !text.isEmpty()) {
                g2.setFont(HUDRenderer.HUD_FONT);
                FontMetrics fm = g2.getFontMetrics();

                int tw = fm.stringWidth(text);
                int tx = bx + (balloonSize - tw) / 2;
                int ty = by + (balloonSize / 2) + fm.getAscent() / 2 - 4;

                // 아이템에 따라 텍스트 색 결정
                Color textColor = Color.BLACK;
                Item item = itemBalloons.get(b);
                if (item != null && item.getKind() != null) {
                    switch (item.getKind()) {

                        // 파란 글씨: 풍선 아이템(±2)
                        case BALLOON_PLUS_2:
                        case BALLOON_MINUS_2:
                        case SELF_BALLOON_PLUS_2:
                        case SELF_BALLOON_MINUS_2:
                            textColor = new Color(120, 160, 255);
                            break;

                        // 초록 글씨: 트릭 아이템 (BLIND / REVERSE)
                        case BLIND:
                        case REVERSE_5S:
                            textColor = new Color(0, 180, 0);
                            break;

                        // 그 외
                        default:
                            textColor = Color.BLACK;
                            break;
                    }
                }


                g2.setColor(textColor);
                g2.drawString(text, tx, ty);
            }
        }
    }

    // 화면에 들어올 때
    @Override
    public void onShown() {
        inputField.setEnabled(true);
        inputField.setVisible(true);

        started = false;
        finished = false;
        resultState = ResultState.NONE;
        showRetryOverlay = false;

        p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
        p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

        // 단어 공급기 초기화 (P1/P2 모두 동일 순서로 시작)
        resetWordProviders();

        // 룰 초기화
        rules = new VersusGameRules(INITIAL_TIME_SECONDS);

        // 풍선 스폰
        spawnInitialBalloons();

        // 아이템 적용기
        itemApplier = new ItemEffectApplier(
                // 듀얼 모드는 시간 조작 안 씀
                new ItemEffectApplier.TimeApi() {
                    @Override
                    public void addSeconds(int delta) { }
                    @Override
                    public int getTimeLeft() { return 0; }
                },
                // UI 효과: 일단 콘솔만
                new ItemEffectApplier.UiApi() {
                    @Override
                    public void showToast(String message) {
                        System.out.println("[ITEM-UI] " + message);
                    }
                    @Override
                    public void flashEffect(boolean positive) {
                        System.out.println(positive ? "[ITEM] GOOD" : "[ITEM] BAD");
                    }
                },
                // 필드 조작: 상대 풍선 추가/내 풍선 제거, reverse
                new VersusFieldApi()
        );

        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());

        String nameFromSession = Session.getNickname();
        if (nameFromSession == null || nameFromSession.isBlank()) {
            nameFromSession = "player";
        }

        p1Name = nameFromSession;
        p2Name = "RIVAL";
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
                }
                else if (msg.equals("START")) {
                    SwingUtilities.invokeLater(this::startIntroSequence);
                }
                else if (msg.startsWith("POP ")) {
                    String[] parts = msg.split(" ", 3);
                    if (parts.length == 3) {
                        String who = parts[1];
                        String word = parts[2];
                        SwingUtilities.invokeLater(() -> onRemotePop(who, word));
                    }
                }
                else if (msg.startsWith("BLIND ")) {
                    // 서버에서 "BLIND P1" 또는 "BLIND P2" 형식으로 옴
                    String attackerRole = msg.substring(6).trim(); // 아이템 사용한 사람(P1 / P2)

                    SwingUtilities.invokeLater(() -> startBlindFor(attackerRole));
                }

                else if (msg.startsWith("RESULT")) {
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
                    // 같은 소켓으로 다음 라운드 계속
                }
                else if (msg.startsWith("TOAST ")) {
                    String[] parts = msg.split(" ", 3);
                    if (parts.length >= 3) {
                        boolean positive = "1".equals(parts[1]);
                        String toastMsg = parts[2];
                        SwingUtilities.invokeLater(() ->
                                showItemToast(toastMsg, positive, false)
                        );
                    }
                }
                else if (msg.startsWith("REVERSE ")) {
                    // 포맷: REVERSE <시전자역할> <지속ms>
                    // 예: REVERSE P1 5000  (P1이 사용 → P2가 거꾸로 입력)
                    String[] parts = msg.split(" ");
                    if (parts.length >= 3) {
                        final String casterRole = parts[1].trim();   // "P1" 또는 "P2"
                        final long duration = Long.parseLong(parts[2]);

                        // 시전자 인덱스 (0 = P1, 1 = P2)
                        final int casterIndex = "P1".equals(casterRole) ? 0 : 1;
                        // 실제로 reverse 걸리는 타겟 인덱스는 상대방
                        final int targetIndex = (casterIndex == 0) ? 1 : 0;

                        SwingUtilities.invokeLater(() -> {

                            // 0) 기존 reverse 상태 초기화
                            for (int i = 0; i < 2; i++) {
                                reverseActive[i] = false;
                                reverseRemainSeconds[i] = 0;
                            }

                            // 1) 타겟 플레이어에게만 reverse 켜기
                            activateReverseFor(targetIndex, duration);

                            // 2) "내 입장" 기준으로, 내가 타겟인지 계산
                            boolean iAmTarget;
                            if ("P1".equals(myRole)) {
                                iAmTarget = (targetIndex == 0);
                            } else if ("P2".equals(myRole)) {
                                iAmTarget = (targetIndex == 1);
                            } else {
                                iAmTarget = false;
                            }

                            // 3) 안내 문구 띄우기
                            showReverseMessage(iAmTarget);
                            repaint();
                        });
                    }
                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // START 수신 후: 미션 -> START! -> 입력 가능
    private void startIntroSequence() {
        started = true;
        finished = false;

        // 1단계: 미션 안내
        introPhase = IntroPhase.MISSION;
        inputField.setEnabled(false);
        repaint();

        // 2초 후 START!로 전환
        javax.swing.Timer missionTimer = new javax.swing.Timer(2000, e -> {
            introPhase = IntroPhase.START;
            repaint();
            ((javax.swing.Timer) e.getSource()).stop();

            // START! 1초 후 입력 가능
            javax.swing.Timer startTimer = new javax.swing.Timer(1000, e2 -> {
                introPhase = IntroPhase.NONE;
                inputField.setEnabled(true);
                inputField.requestFocusInWindow();
                repaint();
                ((javax.swing.Timer) e2.getSource()).stop();
            });
            startTimer.setRepeats(false);
            startTimer.start();
        });

        missionTimer.setRepeats(false);
        missionTimer.start();
    }

    // 서버에서 POP 수신
    private void onRemotePop(String who, String word) {

        boolean popped = tryPopBalloonFor(who, word);
        if (!popped) {
            return;
        }

        if ("P1".equals(who)) {
            if (p1Remaining > 0) p1Remaining--;
        } else if ("P2".equals(who)) {
            if (p2Remaining > 0) p2Remaining--;
        }

        if (rules != null) {
            int playerIndex = "P1".equals(who) ? 1 : 2;
            boolean allCleared = (playerIndex == 1) ? (p1Remaining <= 0) : (p2Remaining <= 0);
            rules.onPop(playerIndex, 0, allCleared);
        }

        repaint();
    }

    // 내 필드에서 풍선 하나 터뜨렸을 때(점수, 룰 반영)
    private void removeMyBalloon(String typedWord) {
        if ("P1".equals(myRole)) {
            if (p1Remaining > 0) {
                p1Remaining--;
            }
        } else if ("P2".equals(myRole)) {
            if (p2Remaining > 0) {
                p2Remaining--;
            }
        }

        if (rules != null) {
            int playerIndex = "P1".equals(myRole) ? 1 : 2;
            boolean allCleared = myAllCleared();
            rules.onPop(playerIndex, 0, allCleared);
        }

        repaint();
    }

    private java.util.List<Balloon> getMyBalloonList() {
        if ("P1".equals(myRole)) return p1Balloons;
        if ("P2".equals(myRole)) return p2Balloons;
        return p1Balloons;
    }

    private String getOpponentRole() {
        if ("P1".equals(myRole)) return "P2";
        if ("P2".equals(myRole)) return "P1";
        return "P2";
    }

    // REVERSE 안내 문구를 "내 입장" 기준으로 만들어 주는 메서드
// iAmTarget: true  → 내가 거꾸로 쳐야 함
//           false → 상대가 거꾸로 침
    private void showReverseMessage(boolean iAmTarget) {
        String msg;
        if (iAmTarget) {
            msg = "10초간 단어를 거꾸로 입력해야 합니다!";
        } else {
            msg = "상대가 10초간 거꾸로 입력합니다!";
        }

        showCenterReverseMessage(msg);
    }


    private java.util.List<Balloon> getBalloonListFor(String who) {
        if ("P1".equals(who)) return p1Balloons;
        if ("P2".equals(who)) return p2Balloons;
        return p1Balloons;
    }

    // 내가 친 단어가 내 풍선 중 하나와 일치하면 POP
    private boolean tryPopMyBalloon(String typedWord) {
        if (typedWord == null || typedWord.isBlank()) return false;

        String trimmed = typedWord.trim();
        java.util.List<Balloon> myList = getMyBalloonList();
        if (myList.isEmpty()) return false;

        for (Balloon b : myList) {
            if (!b.isActive()) continue;
            if (trimmed.equals(b.getWord())) {
                b.pop();
                applyItemIfExists(b);
                return true;
            }
        }
        return false;
    }

    // POP된 풍선에 아이템 붙어있으면 터뜨리기
    private void applyItemIfExists(Balloon b) {
        if (b == null) return;

        // 풍선 객체에 붙어 있던 아이템 꺼내기 + 맵에서도 제거
        Item item = b.detachAttachedItem();
        if (item == null) {
            System.out.println("[ITEM] no item on balloon word=" + b.getWord());
            itemBalloons.remove(b);
            return;
        }
        itemBalloons.remove(b);

        ItemKind kind = item.getKind();
        if (kind == null) return;

        String owner;
        if (p1Balloons.contains(b)) {
            owner = "P1";
        } else if (p2Balloons.contains(b)) {
            owner = "P2";
        } else {
            owner = myRole;
        }
        String opponent = "P1".equals(owner) ? "P2" : "P1";

        System.out.println("[ITEM] apply kind=" + kind +
                ", owner=" + owner + ", opponent=" + opponent +
                ", word=" + b.getWord());

        switch (kind) {
            // 상대 필드에 작용
            case BALLOON_PLUS_2 -> {
                addRandomBalloonsTo(opponent, 2);
                showBalloonChangeToast(opponent, +2);
            }
            case BALLOON_MINUS_2 -> {
                removeRandomBalloonFrom(opponent);
                removeRandomBalloonFrom(opponent);
                showBalloonChangeToast(opponent, -2);
            }

            // 내 필드에 작용
            case SELF_BALLOON_PLUS_2 -> {
                addRandomBalloonsTo(owner, 2);
                showBalloonChangeToast(owner, +2);
            }
            case SELF_BALLOON_MINUS_2 -> {
                removeRandomBalloonFrom(owner);
                removeRandomBalloonFrom(owner);
                showBalloonChangeToast(owner, -2);
            }

            case BLIND -> {
                // 듀얼 BLIND
                handleBlindItem();
            }

            case REVERSE_5S -> {
                if (itemApplier != null) {
                    itemApplier.apply(item);
                }
            }

            // 혹시 모르는 나머지
            default -> {
                if (itemApplier != null) {
                    itemApplier.apply(item);
                }
            }
        }
    }

    // who("P1"/"P2") 쪽에서 단어 매칭 풍선 POP
    private boolean tryPopBalloonFor(String who, String word) {
        if (word == null) return false;
        String trimmed = word.trim();
        if (trimmed.isEmpty()) return false;

        java.util.List<Balloon> list;
        if ("P1".equals(who)) {
            list = p1Balloons;
        } else if ("P2".equals(who)) {
            list = p2Balloons;
        } else {
            return false;
        }

        if (list.isEmpty()) return false;

        for (Balloon b : list) {
            if (!b.isActive()) continue;
            if (trimmed.equals(b.getWord())) {
                b.pop();
                applyItemIfExists(b);
                return true;
            }
        }
        return false;
    }

    // 내 필드 올클리어?
    private boolean myAllCleared() {
        if ("P1".equals(myRole)) {
            return p1Remaining <= 0;
        } else if ("P2".equals(myRole)) {
            return p2Remaining <= 0;
        }
        return false;
    }

    // 듀얼 시작 시 풍선 스폰 (P1=왼쪽, P2=오른쪽)
    private void spawnInitialBalloons() {
        p1Balloons.clear();
        p2Balloons.clear();
        itemBalloons.clear();

        int w = getWidth();
        int h = getHeight();
        if (w <= 0) w = 1280;
        if (h <= 0) h = 720;

        int centerLeft = w / 4;
        int centerRight = w * 3 / 4;
        double balloonAnchorY = h - 260;

        java.util.List<Point> leftPos = buildBalloonPositions(centerLeft, balloonAnchorY);
        java.util.List<Point> rightPos = buildBalloonPositions(centerRight, balloonAnchorY);

        int leftCount = Math.min(TOTAL_BALLOONS_PER_PLAYER, leftPos.size());
        int rightCount = Math.min(TOTAL_BALLOONS_PER_PLAYER, rightPos.size());

        // ★ 두 클라이언트가 완전히 같은 배치를 갖도록 고정 seed 사용
        Random layoutRnd = new Random(7777L);

        Balloon.Kind[] kinds = {
                Balloon.Kind.RED,
                Balloon.Kind.GREEN,
                Balloon.Kind.BLUE
        };

        // ----- P1 풍선 -----
        for (int i = 0; i < leftCount; i++) {
            Point p = leftPos.get(i);
            String word;
            if (p1Words != null) {
                word = p1Words.nextWord();
            } else {
                word = "P1-" + (i + 1);
            }

            Balloon.Kind kind = kinds[layoutRnd.nextInt(kinds.length)];

            Balloon b = new Balloon(
                    word,
                    p.x,
                    p.y,
                    randomKind()
            );
            p1Balloons.add(b);
            assignRandomImageToBalloon(b);
            attachRandomItemToBalloon("P1", b);
        }

        // ----- P2 풍선 -----
        for (int i = 0; i < rightCount; i++) {
            Point p = rightPos.get(i);
            String word;
            if (p2Words != null) {
                word = p2Words.nextWord();
            } else {
                word = "P2-" + (i + 1);
            }

            Balloon.Kind kind = kinds[layoutRnd.nextInt(kinds.length)];

            Balloon b = new Balloon(
                    word,
                    p.x,
                    p.y,
                    kind
            );
            p2Balloons.add(b);
            assignRandomImageToBalloon(b);
            attachRandomItemToBalloon("P2", b);
            // ★ 여기서도 아이템 안 붙임
        }

        // ★ 여기서 아이템 2+2 고정 배치 (같은 layoutRnd 사용)
        assignFixedItemsForVersus(layoutRnd);

        p1Remaining = p1Balloons.size();
        p2Remaining = p2Balloons.size();
    }

    // 듀얼 모드: 플레이어당
// - 파란 풍선 아이템 2개 (내/상대 필드 ±2 네 가지 중 랜덤)
// - 초록 트릭 아이템 2개 (REVERSE_5S)
    private void assignFixedItemsForVersus(Random rndLayout) {
        assignFixedItemsForPlayer("P1", p1Balloons, rndLayout);
        assignFixedItemsForPlayer("P2", p2Balloons, rndLayout);
    }

    private void assignFixedItemsForPlayer(
            String owner,
            java.util.List<Balloon> balloons,
            Random rndLayout
    ) {
        if (balloons == null || balloons.isEmpty()) return;

        // 1) 기존 아이템/카테고리 초기화
        for (Balloon b : balloons) {
            b.setCategory(ItemCategory.NONE);
            b.setAttachedItem(null);
            itemBalloons.remove(b);
        }

        // 2) 어떤 풍선이 아이템 풍선이 될지 랜덤으로 고르기
        java.util.List<Balloon> shuffled = new ArrayList<>(balloons);
        Collections.shuffle(shuffled, rndLayout);

        int idx = 0;

        // ----- 파란 풍선 아이템 2개 -----
        for (int i = 0; i < 2 && idx < shuffled.size(); i++) {
            Balloon b = shuffled.get(idx++);

            // 네 가지 중 하나를 랜덤 선택
            ItemKind[] blueKinds = {
                    ItemKind.BALLOON_PLUS_2,        // 상대 풍선 +2
                    ItemKind.BALLOON_MINUS_2,       // 상대 풍선 -2
                    ItemKind.SELF_BALLOON_PLUS_2,   // 내 풍선 +2
                    ItemKind.SELF_BALLOON_MINUS_2   // 내 풍선 -2
            };
            ItemKind kind = blueKinds[rndLayout.nextInt(blueKinds.length)];

            Item item = new Item(kind, 0, 0);
            b.setCategory(ItemCategory.BALLOON); // 파란 글자
            b.setAttachedItem(item);
            itemBalloons.put(b, item);

            System.out.println("[ITEM-FIXED] BLUE " + kind + " to " + owner +
                    " word=" + b.getWord());
        }

        // ----- 초록 트릭 아이템 2개 -----
// → reverse 1개 + blind 1개로 고정
        for (int i = 0; i < 2 && idx < shuffled.size(); i++) {
            Balloon b = shuffled.get(idx++);

            // i == 0 : REVERSE_5S,  i == 1 : BLIND
            ItemKind kind = (i == 0)
                    ? ItemKind.REVERSE_5S
                    : ItemKind.BLIND;

            Item item = new Item(kind, 0, 0);
            b.setCategory(ItemCategory.TRICK); // 초록 글자
            b.setAttachedItem(item);
            itemBalloons.put(b, item);

            System.out.println("[ITEM-FIXED] GREEN " + kind + " to " + owner +
                    " word=" + b.getWord());
        }

    }

    // 듀얼 모드: 플레이어당
    // - 파란 풍선 아이템( BALLOON_PLUS_2 / BALLOON_MINUS_2 ) 2개
    // - 초록 트릭 아이템( REVERSE_5S ) 2개
    // 만 배치
    private void assignFixedItemsForVersus() {
        assignFixedItemsForPlayer("P1", p1Balloons);
        assignFixedItemsForPlayer("P2", p2Balloons);
    }

    private void assignFixedItemsForPlayer(String owner, java.util.List<Balloon> balloons) {
        if (balloons == null || balloons.isEmpty()) return;

        // 1) 기존 아이템/카테고리 싹 초기화
        for (Balloon b : balloons) {
            b.setCategory(ItemCategory.NONE);
            b.setAttachedItem(null);
            itemBalloons.remove(b);
        }

        // 2) 랜덤으로 섞어서 어떤 풍선이 아이템풍선이 될지 결정
        java.util.List<Balloon> shuffled = new ArrayList<>(balloons);
        Collections.shuffle(shuffled, rnd);

        int idx = 0;

        // 3) 파란 풍선 아이템 2개 (BALLOON_PLUS_2 / BALLOON_MINUS_2)
        for (int i = 0; i < 2 && idx < shuffled.size(); i++) {
            Balloon b = shuffled.get(idx++);

            // 두 개 중 하나는 +2, 하나는 -2 정도로
            ItemKind kind = (i == 0) ? ItemKind.BALLOON_PLUS_2 : ItemKind.BALLOON_MINUS_2;

            Item item = new Item(kind, 0, 0);
            b.setCategory(ItemCategory.BALLOON);
            b.setAttachedItem(item);
            itemBalloons.put(b, item);

            System.out.println("[ITEM-FIXED] attach " + kind + " to " + owner +
                    " word=" + b.getWord());
        }

        // 4) 초록 트릭 아이템 2개 (REVERSE_5S 고정)
        for (int i = 0; i < 2 && idx < shuffled.size(); i++) {
            Balloon b = shuffled.get(idx++);

            ItemKind kind = ItemKind.REVERSE_5S;
            Item item = new Item(kind, 0, 0);
            b.setCategory(ItemCategory.TRICK);
            b.setAttachedItem(item);
            itemBalloons.put(b, item);

            System.out.println("[ITEM-FIXED] attach " + kind + " to " + owner +
                    " word=" + b.getWord());
        }
    }

    // 풍선에 랜덤 아이템 붙이기
    private void attachRandomItemToBalloon(String owner, Balloon b) {
        if (b == null) return;

        double chance = 0.2; // 20% 확률로만 아이템 풍선
        if (rnd.nextDouble() > chance) {
            b.setCategory(ItemCategory.NONE);
            b.setAttachedItem(null);
            itemBalloons.remove(b);
            return;
        }

        ItemKind kind;
        ItemCategory category;

        // 0: +2, 1: -2, 2: REVERSE_5S 3:blind
        int r = rnd.nextInt(4);
        if (r == 0) {
            kind = ItemKind.BALLOON_PLUS_2;
            category = ItemCategory.BALLOON;
        } else if (r == 1) {
            kind = ItemKind.BALLOON_MINUS_2;
            category = ItemCategory.BALLOON;
        } else if (r ==2 ){
            kind = ItemKind.REVERSE_5S;      // 또는 BLIND로 바꾸고 싶으면 여기 수정
            category = ItemCategory.TRICK;
        }else {
            kind = ItemKind.BLIND;         // 화면 블라인드 아이템
            category = ItemCategory.TRICK;
        }

        Item item = new Item(kind, 0, 0);

        b.setCategory(category);
        b.setAttachedItem(item);
        itemBalloons.put(b, item);

        System.out.println("[ITEM] attach " + kind + " to " + owner +
                " word=" + b.getWord());
    }

    // who 쪽에 풍선 n개 랜덤 추가
    private void addRandomBalloonsTo(String who, int count) {
        if (count <= 0) return;

        int w = getWidth();
        int h = getHeight();
        if (w <= 0) w = 1280;
        if (h <= 0) h = 720;

        int centerX = "P1".equals(who) ? w / 4 : w * 3 / 4;
        double anchorY = h - 260;

        java.util.List<Point> basePos = buildBalloonPositions(centerX, anchorY);
        java.util.List<Balloon> list = getBalloonListFor(who);

        for (int i = 0; i < count; i++) {
            if (basePos.isEmpty()) break;

            Point p = basePos.get(rnd.nextInt(basePos.size()));

            String word;
            if ("P1".equals(who)) {
                if (p1Words != null) {
                    word = p1Words.nextWord();
                } else {
                    word = "P1-extra-" + (list.size() + 1);
                }
            } else {
                if (p2Words != null) {
                    word = p2Words.nextWord();
                } else {
                    word = "P2-extra-" + (list.size() + 1);
                }
            }

            Balloon b = new Balloon(
                    word,
                    p.x,
                    p.y,
                    randomKind()
            );
            list.add(b);
            assignRandomImageToBalloon(b);
            attachRandomItemToBalloon(who, b);

            if ("P1".equals(who)) {
                p1Remaining++;
            } else {
                p2Remaining++;
            }
        }

        repaint();
    }

    // who 쪽에서 살아있는 풍선 하나 랜덤 제거
    private boolean removeRandomBalloonFrom(String who) {
        java.util.List<Balloon> list = getBalloonListFor(who);
        if (list.isEmpty()) return false;

        java.util.List<Balloon> candidates = new ArrayList<>();
        for (Balloon b : list) {
            if (b != null && b.isActive()) {
                candidates.add(b);
            }
        }
        if (candidates.isEmpty()) return false;

        Balloon target = candidates.get(rnd.nextInt(candidates.size()));
        target.pop();

        if ("P1".equals(who)) {
            if (p1Remaining > 0) p1Remaining--;
        } else {
            if (p2Remaining > 0) p2Remaining--;
        }

        repaint();
        return true;
    }

    // 아이템에서 쓸 필드 API
    private class VersusFieldApi implements ItemEffectApplier.FieldApi {
        @Override
        public void addBalloons(int n) {
            String opponent = getOpponentRole();
            addRandomBalloonsTo(opponent, n); // 상대 풍선 +n
        }

        @Override
        public void removeBalloons(int n) {
            for (int i = 0; i < n; i++) {
                removeRandomBalloonFrom(myRole); // 내 풍선 -n
            }
        }

        @Override
        public void activateReverseOnOpponent(long durationMillis) {
            if (netClient == null) return;

            // reverse를 걸 "타겟 역할" = 나의 상대
            String targetRole = getOpponentRole();

            // 서버로 REVERSE 메시지 전송
            netClient.sendReverse(targetRole, durationMillis);
        }
    }

    /**
     * 리소스(예: /data/words.csv)에서 단어를 읽는다.
     * primaryPath가 없으면 fallbackPath 시도.
     * 둘 다 실패하면 defaultPrefix-번호 형식으로 더미 단어 만든다.
     */
    private java.util.List<String> loadWordsFromResource(
            String primaryPath,
            String fallbackPath,
            String defaultPrefix
    ) {
        java.util.List<String> result = new ArrayList<>();
        String usedPath = primaryPath;

        InputStream in = getClass().getResourceAsStream(primaryPath);
        if (in == null && fallbackPath != null) {
            usedPath = fallbackPath;
            in = getClass().getResourceAsStream(fallbackPath);
        }

        if (in != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    // 완전 빈 줄은 건너뛰기
                    if (line.isEmpty()) continue;

                    // UTF-8 BOM(﻿) 있으면 제거
                    if (line.charAt(0) == '\uFEFF') {
                        line = line.substring(1);
                        // 다시 한 번 비어 있나 체크
                        if (line.isEmpty()) continue;
                    }

                    result.add(line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (result.isEmpty()) {
            if (defaultPrefix == null) defaultPrefix = "WORD";
            for (int i = 1; i <= TOTAL_BALLOONS_PER_PLAYER; i++) {
                result.add(defaultPrefix + "-" + i);
            }
        }

        System.out.println("Loaded " + result.size()
                + " words (" + defaultPrefix + ") from " + usedPath);

        return result;
    }

    // 단일 path용 간단 오버로드
    private java.util.List<String> loadWordsFromResource(String path) {
        return loadWordsFromResource(path, null, "WORD");
    }

    // 단어 공급기 리셋 (양쪽 클라가 동일한 CSV 순서로 시작하도록)
    private void resetWordProviders() {
        java.util.List<String> allWords = loadWordsFromResource("/data/words.csv");
        p1Words = new StaticWordProvider(allWords, StaticWordProvider.Role.P1);
        p2Words = new StaticWordProvider(allWords, StaticWordProvider.Role.P2);
    }

    // 듀얼 HUD (현재는 비워둠)
    private void drawHud(Graphics2D g2, int w, int h) {
        // 듀얼 모드에서는 시간/점수 HUD 별도 표시 X
    }

    // 내가 엔터 쳤을 때
    private void onEnterTyped(String typedWord) {
        if (!started || finished) return;

        if (typedWord == null) return;
        typedWord = typedWord.trim();
        if (typedWord.isEmpty()) return;

        // reverse 상태라면, 사용자가 거꾸로 입력한 걸 다시 뒤집어서 원래 단어로
        String effectiveWord = typedWord;
        if (isReverseActiveForMe()) {
            effectiveWord = new StringBuilder(typedWord).reverse().toString();
        }

        boolean popped = tryPopMyBalloon(effectiveWord);

        if (!popped) {
            if (rules != null) {
                int playerIndex = "P1".equals(myRole) ? 1 : 2;
                rules.onMiss(playerIndex);
            }
            return;
        }

        // 남은 개수/점수 처리도 같은 단어 기준으로
        removeMyBalloon(effectiveWord);

        if (netClient != null) {
            netClient.sendPop(effectiveWord);
        }

        if (myAllCleared() && !finished) {
            finished = true;
            if (netClient != null) {
                netClient.sendFinish();
            }
        }
    }

    // 듀얼 인트로(미션 안내 + START!) 표시
    private void drawStartMessage(Graphics2D g2, int w, int h) {
        if (introPhase == IntroPhase.NONE) return;

        // 반투명 어두운 배경
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(oldComp);

        // 텍스트 설정
        String text;
        if (introPhase == IntroPhase.MISSION) {
            text = "상대보다 풍선을 먼저 터뜨리세요!";
        } else { // IntroPhase.START
            text = "START!";
        }

        Font oldFont = g2.getFont();
        Font msgFont = NAME_FONT.deriveFont(NAME_FONT.getSize2D() + 8.0f);
        g2.setFont(msgFont);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();

        int centerX = w / 2;
        int centerY = h / 2;

        int x = centerX - textW / 2;
        int y = centerY + textH / 2;

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);

        g2.setFont(oldFont);
    }

    private void showCenterReverseMessage(String msg) {
        centerReverseMessage = msg;
        centerReverseMessageEnd = System.currentTimeMillis() + 2000; // 2초
        ensureReverseTimerRunning();   // 문구 시간도 타이머로 갱신
    }

    // 중앙 검정 박스에 reverse 안내 문구 표시
    private void drawCenterReverseOverlay(Graphics2D g2, int w, int h) {
        if (centerReverseMessage == null) return;

        long now = System.currentTimeMillis();
        if (now >= centerReverseMessageEnd) {
            centerReverseMessage = null;
            return;
        }

        Font oldFont = g2.getFont();
        Font msgFont = HUDRenderer.HUD_FONT.deriveFont(28f);
        g2.setFont(msgFont);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(centerReverseMessage);
        int textH = fm.getAscent();

        int boxPaddingX = 40;
        int boxPaddingY = 20;
        int boxW = textW + boxPaddingX * 2;
        int boxH = textH + boxPaddingY * 2;

        int centerX = w / 2;
        int centerY = h / 2;
        int x = centerX - boxW / 2;
        int y = centerY - boxH / 2;

        // 반투명 검정 박스
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(x, y, boxW, boxH, 25, 25);
        g2.setComposite(oldComp);

        // 흰 테두리
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, boxW, boxH, 25, 25);

        // 글자
        int textX = centerX - textW / 2;
        int textY = centerY + textH / 2 - 4;
        g2.drawString(centerReverseMessage, textX, textY);

        g2.setFont(oldFont);
    }

    // 네트워크 전송 안 하고 화면에만 토스트 띄우는 간단 버전
    private void showItemToast(String msg, boolean positive) {
        showItemToast(msg, positive, false);
    }

    // 아이템 토스트 표시 (broadcast: 서버로도 알릴지 여부)
    private void showItemToast(String msg, boolean positive, boolean broadcast) {
        itemToastText = msg;
        itemToastPositive = positive;
        itemToastExpireAt = System.currentTimeMillis() + 2000; // 2초 유지
        repaint();

        // 내가 직접 아이템을 썼을 때만 서버로 알림 → 서버가 양쪽에 브로드캐스트
        if (broadcast && netClient != null) {
            String flag = positive ? "1" : "0"; // 1 = 좋은 효과, 0 = 나쁜 효과
            netClient.sendToast(flag, msg);
        }
    }

    /**
     * targetRole("P1"/"P2") 쪽 풍선 개수가 deltaCount만큼 변했을 때,
     * 내 화면 기준으로 "내 풍선 ±n", "상대 풍선 ±n" 문구를 만들어서 토스트로 띄우는 헬퍼.
     */
    private void showBalloonChangeToast(String targetRole, int deltaCount) {
        if (targetRole == null) return;

        boolean iAmTarget = targetRole.equals(myRole);
        boolean gained = (deltaCount > 0);

        String whoText = iAmTarget ? "내" : "상대";
        String sign = gained ? "+" : "-";
        int abs = Math.abs(deltaCount);

        String msg = whoText + " 풍선 " + sign + abs + "!";

        // 내 입장에서 좋은 효과인지 / 나쁜 효과인지
        boolean positive;
        if (iAmTarget) {
            // 내 풍선: 늘면 좋고 줄면 나쁨
            positive = gained;
        } else {
            // 상대 풍선: 늘면 나쁘고 줄면 좋음
            positive = !gained;
        }

        showItemToast(msg, positive);
    }

    private void drawItemToast(Graphics2D g2, int w, int h) {
        if (itemToastText == null) return;

        long now = System.currentTimeMillis();
        if (now > itemToastExpireAt) {
            itemToastText = null;
            return;
        }

        Font oldFont = g2.getFont();
        Font toastFont = HUDRenderer.HUD_FONT.deriveFont(32f);
        g2.setFont(toastFont);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(itemToastText);
        int textH = fm.getAscent();

        int boxW = 420;
        int boxH = 70;
        int centerX = w / 2;
        int centerY = 260;

        int x = centerX - boxW / 2;
        int y = centerY - boxH / 2;

        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(x, y, boxW, boxH, 20, 20);
        g2.setComposite(oldComp);

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, boxW, boxH, 20, 20);

        if (itemToastPositive) {
            g2.setColor(new Color(255, 240, 180));
        } else {
            g2.setColor(new Color(255, 150, 150));
        }

        int tx = centerX - textW / 2;
        int ty = centerY + textH / 2 - 4;
        g2.drawString(itemToastText, tx, ty);

        g2.setFont(oldFont);
    }

    // ================= BLIND ==================

    // BLIND 아이템을 먹었을 때 서버에 알림
    private void handleBlindItem() {
        // 네트워크 클라이언트나 내 ROLE 이 없으면 그냥 무시
        if (netClient == null || myRole == null) {
            return;
        }
        // 서버에 "BLIND P1" 또는 "BLIND P2" 전송
        netClient.sendBlind();
    }

    // BLIND 아이템: 공격자/피격자에 따라 내 화면에서 ME / RIVAL 중 어디를 가릴지 결정
    private void startBlindFor(String attackerRole) {
        if (attackerRole == null || myRole == null) return;

        long now   = System.currentTimeMillis();
        long until = now + 3000L; // 3초 유지

        boolean iAmAttacker = attackerRole.equals(myRole);

        // 먼저 둘 다 끄고 시작 (중첩 방지)
        blindP1 = false;
        blindP2 = false;

        if (iAmAttacker) {
            // 내가 BLIND를 쓴 경우 -> 내 화면에서는 RIVAL 영역만 가리기
            if ("P1".equals(myRole)) {
                // P1 화면: 왼쪽 = ME(P1), 오른쪽 = RIVAL(P2)
                blindP2 = true;      // RIVAL(P2) 영역만 블라인드
                blindEndP2 = until;
            } else if ("P2".equals(myRole)) {
                // P2 화면: 왼쪽 = RIVAL(P1), 오른쪽 = ME(P2)
                blindP1 = true;      // RIVAL(P1) 영역만 블라인드
                blindEndP1 = until;
            }
        } else {
            // 상대가 BLIND를 쓴 경우 -> 내 화면에서는 ME 영역만 가리기
            if ("P1".equals(myRole)) {
                // 난 P1 -> 왼쪽이 ME
                blindP1 = true;
                blindEndP1 = until;
            } else if ("P2".equals(myRole)) {
                // 난 P2 -> 오른쪽이 ME
                blindP2 = true;
                blindEndP2 = until;
            }
        }

        repaint();

        // 0.1초마다 확인해서 자동 해제
        javax.swing.Timer t = new javax.swing.Timer(100, e -> {
            long now2 = System.currentTimeMillis();
            boolean changed = false;

            if (blindP1 && now2 >= blindEndP1) {
                blindP1 = false;
                changed = true;
            }
            if (blindP2 && now2 >= blindEndP2) {
                blindP2 = false;
                changed = true;
            }

            if (changed) {
                repaint();
            }

            if (!blindP1 && !blindP2) {
                ((javax.swing.Timer) e.getSource()).stop();
            }
        });
        t.setRepeats(true);
        t.start();
    }

    // 반쪽을 3x6 그리드로 꽉 채우는 BLIND 오버레이
    private void drawBlindHalf(Graphics2D g2, int x, int y, int width, int height) {

        // 3 × 6 = 18칸
        int rows = 6;
        int cols = 3;

        int cellW = width / cols;
        int cellH = height / rows;

        // 반투명 블랙 배경
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(x, y, width, height);
        g2.setComposite(oldComp);

        // 텍스트 설정
        String text = "BLIND";
        Font oldFont = g2.getFont();
        Font f = NAME_FONT.deriveFont(NAME_FONT.getSize2D() + 20.0f);
        g2.setFont(f);
        g2.setColor(new Color(255, 180, 180));

        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();

        // 각 칸에 BLIND 텍스트 채우기
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                int cx = x + c * cellW;
                int cy = y + r * cellH;

                int tx = cx + (cellW - textW) / 2;
                int ty = cy + (cellH + textH) / 2 - 4;

                g2.drawString(text, tx, ty);
            }
        }

        g2.setFont(oldFont);
    }


    // P1 / P2 반쪽만 가리는 블라인드 오버레이
    private void drawBlindOverlay(Graphics2D g2, int w, int h) {
        long now = System.currentTimeMillis();
        int half = w / 2;

        if (blindP1) {
            if (now > blindEndP1) {
                blindP1 = false;
            } else {
                drawBlindHalf(g2, 0, 0, half, h);
            }
        }

        if (blindP2) {
            if (now > blindEndP2) {
                blindP2 = false;
            } else {
                drawBlindHalf(g2, half, 0, half, h);
            }
        }
    }

    // ================= RESULT =================

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

        if (!showRetryOverlay) {
            g2.setColor(leftColor);
            g2.drawString(leftText, centerLeftX - leftW / 2, centerY);

            g2.setColor(rightColor);
            g2.drawString(rightText, centerRightX - rightW / 2, centerY);

            g2.setFont(oldFont);
            return;
        }

        // 어둡게 덮기
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(0, 0, w, h);
        g2.setComposite(oldComp);

        g2.setFont(bigFont);
        fm = g2.getFontMetrics();

        g2.setColor(leftColor);
        g2.drawString(leftText, centerLeftX - leftW / 2, centerY);

        g2.setColor(rightColor);
        g2.drawString(rightText, centerRightX - rightW / 2, centerY);

        String retryText = "RETRY";
        String homeText  = "HOME";

        Font buttonFont = HUDRenderer.HUD_FONT
                .deriveFont(HUDRenderer.HUD_FONT.getSize2D() + 6.0f);
        g2.setFont(buttonFont);
        FontMetrics fmBtn = g2.getFontMetrics();

        int buttonW = 200;
        int buttonH = 60;
        int gap = 40;

        int centerX = w / 2;
        int btnTop = centerY + 70;

        int retryX = centerX - buttonW - gap / 2;
        int homeX  = centerX + gap / 2;

        Color btnBg = new Color(0, 0, 0, 150);
        g2.setStroke(new BasicStroke(3f));

        // RETRY
        g2.setColor(btnBg);
        g2.fillRoundRect(retryX, btnTop, buttonW, buttonH, 18, 18);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(retryX, btnTop, buttonW, buttonH, 18, 18);

        int retryTextW = fmBtn.stringWidth(retryText);
        int retryTextX = retryX + (buttonW - retryTextW) / 2;
        int retryTextY = btnTop + (buttonH + fmBtn.getAscent()) / 2 - 4;
        g2.drawString(retryText, retryTextX, retryTextY);

        // HOME
        g2.setColor(btnBg);
        g2.fillRoundRect(homeX, btnTop, buttonW, buttonH, 18, 18);
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(homeX, btnTop, buttonW, buttonH, 18, 18);

        int homeTextW = fmBtn.stringWidth(homeText);
        int homeTextX = homeX + (buttonW - homeTextW) / 2;
        int homeTextY = btnTop + (buttonH + fmBtn.getAscent()) / 2 - 4;
        g2.drawString(homeText, homeTextX, homeTextY);

        retryRect = new Rectangle(retryX, btnTop, buttonW, buttonH);
        homeRect  = new Rectangle(homeX,  btnTop, buttonW, buttonH);

        g2.setFont(oldFont);
    }

    @Override
    public void onHidden() {
        // 아직 특별한 건 없음
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

        // 항상 P1 = 왼쪽, P2 = 오른쪽
        drawBalloonCluster(g2, p1Balloons, centerLeft, h);
        drawBalloonCluster(g2, p2Balloons, centerRight, h);

        // 듀얼 시작 안내 문구
        drawStartMessage(g2, w, h);

        // 아이템 토스트
        drawItemToast(g2, w, h);
        // BLIND 반쪽 오버레이
        drawBlindOverlay(g2, w, h);
        // reverse 중앙 오버레이
        drawCenterReverseOverlay(g2, w, h);

        // 싱글 모드처럼 중앙 토스트 박스
        drawItemToast(g2, w, h);

        drawResultOverlay(g2, w, h);
    }

    // 결과 연출 + GameContext에 결과 스냅샷 기록
    private void startResultSequence(ResultState state) {
        GameContext ctx = GameContext.getInstance();

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

        int p1ScoreSnapshot = 0;
        int p2ScoreSnapshot = 0;
        double p1AccSnapshot = 1.0;
        double p2AccSnapshot = 1.0;
        boolean p1ClearedSnapshot = false;
        boolean p2ClearedSnapshot = false;

        if (rules != null) {
            VersusGameRules.PlayerState ps1 = rules.getP1();
            VersusGameRules.PlayerState ps2 = rules.getP2();

            p1ScoreSnapshot = ps1.getScore();
            p2ScoreSnapshot = ps2.getScore();
            p1AccSnapshot   = ps1.getAccuracy();
            p2AccSnapshot   = ps2.getAccuracy();
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

        // 결과 상태 적용
        resultState = state;
        finished = true;
        showRetryOverlay = false;

        inputField.setText("");
        inputField.setEnabled(false);

        // 소켓은 닫지 않는다 → RETRY 후 같은 연결로 계속 진행
        repaint();

        javax.swing.Timer t = new javax.swing.Timer(2000, e -> {
            showRetryOverlay = true;
            repaint();
            ((javax.swing.Timer) e.getSource()).stop();
        });
        t.setRepeats(false);
        t.start();
    }

    // HOME 클릭
    private void handleHomeClicked() {
        try {
            if (netClient != null) {
                netClient.close();
            }
        } catch (Exception ignore) {}

        resultState = ResultState.NONE;
        finished = true;
        showRetryOverlay = false;

        router.show(ScreenId.START);
    }

    // 풍선마다 랜덤 색 PNG 하나 골라서 매핑해 두는 헬퍼
    private void assignRandomImageToBalloon(Balloon b) {
        if (b == null) return;

        // 사용할 풍선 이미지 목록
        Image[] imgs = {
                balloonGreen,
                balloonOrange,
                balloonPink,
                balloonPurple,
                balloonYellow
        };

        // 랜덤으로 하나 선택
        Image img = imgs[rnd.nextInt(imgs.length)];

        // 이 풍선에 어떤 이미지를 쓸지 저장
        balloonImages.put(b, img);
    }

    // RETRY 클릭
    private void handleRetryClicked() {
        finished = false;
        resultState = ResultState.NONE;
        showRetryOverlay = false;

        p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
        p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

        // 단어 공급기 리셋 (양쪽 클라가 같은 CSV 순서에서 다시 시작)
        resetWordProviders();

        // 풍선 새로 스폰 (여기서 아이템까지 다시 붙음)
        spawnInitialBalloons();

        // 룰 초기화
        rules = new VersusGameRules(INITIAL_TIME_SECONDS);

        // 입력창 다시 활성화 + 포커스
        inputField.setText("");
        inputField.setEnabled(true);
        inputField.setVisible(true);
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());

        repaint();

        if (netClient != null) {
            netClient.sendRetry();
        }
    }

}
