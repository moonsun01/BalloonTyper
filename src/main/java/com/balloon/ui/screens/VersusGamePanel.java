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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
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
    private static final int INITIAL_TIME_SECONDS = 60;   // 듀얼 기본 시간(지금은 사실상 미사용)

    // 네트워크
    private VersusClient netClient;
    private String myRole = "P1";   // "P1" 또는 "P2"
    private boolean started = false;
    private boolean finished = false;

    // 한 플레이어가 시작할 때 풍선 개수
    private static final int TOTAL_BALLOONS_PER_PLAYER = 30;  // 3+4+5+6+5+4+3

    // 남은 풍선 개수
    private int p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
    private int p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

    // 랜덤 (아이템, 추가 풍선 등)
    private final Random rnd = new Random(20241118L);

    // 아이템 적용기
    private ItemEffectApplier itemApplier;
    // 풍선에 붙은 아이템
    private final Map<Balloon, Item> itemBalloons = new HashMap<>();

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

    // 싱글 모드처럼 중앙에 토스트 띄우기
    private void showItemToast(String msg, boolean positive) {
        itemToastText = msg;
        itemToastPositive = positive;
        itemToastExpireAt = System.currentTimeMillis() + 800; // 0.8초 정도 유지
        repaint();
    }

    // 싱글 모드 itemToastLabel 디자인을 그대로 흉내 내서 그림
    private void drawItemToast(Graphics2D g2, int w, int h) {
        if (itemToastText == null) return;

        long now = System.currentTimeMillis();
        if (now > itemToastExpireAt) {
            itemToastText = null;
            return;
        }

        Font oldFont = g2.getFont();
        Font toastFont = HUDRenderer.HUD_FONT.deriveFont(32f); // GamePanel이 32 정도 사용
        g2.setFont(toastFont);
        FontMetrics fm = g2.getFontMetrics();

        int textW = fm.stringWidth(itemToastText);
        int textH = fm.getAscent();

        int boxW = 420;  // 싱글 모드 itemToastLabel과 비슷한 크기
        int boxH = 70;
        int centerX = w / 2;
        int centerY = 260; // GamePanel에서 쓰던 위치랑 비슷하게

        int x = centerX - boxW / 2;
        int y = centerY - boxH / 2;

        // 배경: 검은 반투명 + 흰 테두리 (싱글 모드랑 동일)
        Composite oldComp = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRoundRect(x, y, boxW, boxH, 20, 20);
        g2.setComposite(oldComp);

        g2.setColor(new Color(255, 255, 255, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(x, y, boxW, boxH, 20, 20);

        // 글자 색: 싱글 모드 itemToastLabel처럼 노란 느낌
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


    // 결과 후 Retry/Home 오버레이 표시 여부
    private boolean showRetryOverlay = false;
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

        // 디버그용 키 (1,2,3,R,H)
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();

                if (code == KeyEvent.VK_1) {
                    startResultSequence(ResultState.P1_WIN);
                } else if (code == KeyEvent.VK_2) {
                    startResultSequence(ResultState.P2_WIN);
                } else if (code == KeyEvent.VK_3) {
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

    // 플레이어 이름 표시 (myRole에 따라 레이블만 바뀜, 보드는 항상 P1=왼쪽/P2=오른쪽)
    private void drawPlayerNames(Graphics2D g2, int w, int h) {
        g2.setColor(Color.BLACK);
        g2.setFont(NAME_FONT);

        int nameY = 40;

        String leftLabel;
        String rightLabel;

        if ("P1".equals(myRole)) {
            //내가 P1이면 왼쪽이 내 필드 -> ME
            leftLabel = "ME";
            rightLabel = "RIVAL";
        } else if ("P2".equals(myRole)) {
            //내가 P2이면 오른쪽이 내 필드 -> ME
            leftLabel = "RIVAL";
            rightLabel = "ME";
        } else {
            //혹시 ROLE을 못받은 이상한 상황일 때도, 내 기준은 그냥 ME
            leftLabel = "ME";
            rightLabel = "RIVAL";
        }

        // 왼쪽 이름
        g2.drawString(leftLabel, 20, nameY);

        // 오른쪽 이름
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(rightLabel);
        int rightMargin = 20;
        int rightX = w - rightMargin - textWidth;
        g2.drawString(rightLabel, rightX, nameY);
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

    // Balloon.Kind → 이미지
    private Image imageForKind(Balloon.Kind kind) {
        if (kind == null) {
            return balloonGreen;
        }
        switch (kind) {
            case RED:
                return balloonPink;
            case GREEN:
                return balloonGreen;
            case BLUE:
                return balloonPurple;
            default:
                return balloonGreen;
        }
    }

    // 풍선 색상을 랜덤으로 선택 (RED / GREEN / BLUE)
    private Balloon.Kind randomKind() {
        // GamePanel의 toKind()를 보면 Kind가 RED / GREEN / BLUE 3개만 쓰이니까
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

                // ★ 아이템에 따라 텍스트 색 결정
                Color textColor = Color.BLACK;
                Item item = itemBalloons.get(b);
                if (item != null) {
                    switch (item.getKind()) {
                        case BALLOON_PLUS_2 -> textColor = new Color(120, 160, 255);   // +2: 파란 느낌
                        case BALLOON_MINUS_2 -> textColor = new Color(255, 110, 110);  // -2: 빨간 느낌
                        default -> textColor = Color.BLACK;
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

        p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
        p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

        // 공통 words 리스트 한 번 로드
        java.util.List<String> allWords = loadWordsFromResource("/data/words.csv");

        // P1 / P2 단어 공급기 (한 번만 생성, 이후 RETRY에서도 재사용)
        if (p1Words == null) {
            p1Words = new StaticWordProvider(allWords, StaticWordProvider.Role.P1);
        }
        if (p2Words == null) {
            p2Words = new StaticWordProvider(allWords, StaticWordProvider.Role.P2);
        }

        // 듀얼 룰 초기화
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
                        System.out.println("[ITEM] " + message);
                    }
                    @Override
                    public void flashEffect(boolean positive) {
                        System.out.println(positive ? "[ITEM] GOOD" : "[ITEM] BAD");
                    }
                },
                // 필드 조작: 상대 풍선 추가/내 풍선 제거
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
                    SwingUtilities.invokeLater(() -> startIntroSequence());
                }
                else if (msg.startsWith("POP ")) {
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

                    // ★ 여기서 break 하지 않는다!
                    // 서버가 다음 라운드 START를 다시 보내면, 위의 START 분기에서 또 처리하게 두기
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
        if (b == null || itemApplier == null) return;

        Item item = itemBalloons.remove(b);
        if (item != null) {
            itemApplier.apply(item);
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

        // P1 풍선
        for (int i = 0; i < leftCount; i++) {
            Point p = leftPos.get(i);
            String word;
            if (p1Words != null) {
                word = p1Words.nextWord();
            } else {
                word = "P1-" + (i + 1);
            }

            Balloon b = new Balloon(
                    word,
                    p.x,
                    p.y,
                    randomKind()
            );
            p1Balloons.add(b);
            attachRandomItemToBalloon("P1", b);
        }

        // P2 풍선
        for (int i = 0; i < rightCount; i++) {
            Point p = rightPos.get(i);
            String word;
            if (p2Words != null) {
                word = p2Words.nextWord();
            } else {
                word = "P2-" + (i + 1);
            }

            Balloon b = new Balloon(
                    word,
                    p.x,
                    p.y,
                    randomKind()
            );
            p2Balloons.add(b);
            attachRandomItemToBalloon("P2", b);
        }

        p1Remaining = p1Balloons.size();
        p2Remaining = p2Balloons.size();
    }

    // 풍선에 랜덤 아이템 붙이기
    private void attachRandomItemToBalloon(String owner, Balloon b) {
        if (b == null) return;

        double chance = 0.2; // 20% 확률로만 아이템 풍선
        if (rnd.nextDouble() > chance) {
            b.setCategory(ItemCategory.NONE);
            return;
        }

        // ★ 듀얼 모드는 풍선 수 아이템만 사용
        ItemKind kind;
        int r = rnd.nextInt(2); // 0 또는 1만

        if (r == 0) {
            kind = ItemKind.BALLOON_PLUS_2;
        } else {
            kind = ItemKind.BALLOON_MINUS_2;
        }

        Item item = new Item(kind, 0, 0);

        // 전부 BALLOON 카테고리
        b.setCategory(ItemCategory.BALLOON);
        itemBalloons.put(b, item);
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
            addRandomBalloonsTo(opponent, n); //상대 풍선 +n
        }

        @Override
        public void removeBalloons(int n) {
            for (int i = 0; i < n; i++) {
                removeRandomBalloonFrom(myRole); //내 풍선 -n
            }
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
                    if (!line.isEmpty()) {
                        result.add(line);
                    }
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


    // HUD (Score만)
    private void drawHud(Graphics2D g2, int w, int h) {

    }

    // 내가 엔터 쳤을 때
    private void onEnterTyped(String typedWord) {
        if (!started || finished) return;

        if (typedWord == null) return;
        typedWord = typedWord.trim();
        if (typedWord.isEmpty()) return;

        boolean popped = tryPopMyBalloon(typedWord);

        if (!popped) {
            if (rules != null) {
                int playerIndex = "P1".equals(myRole) ? 1 : 2;
                rules.onMiss(playerIndex);
            }
            return;
        }

        removeMyBalloon(typedWord);

        if (netClient != null) {
            netClient.sendPop(typedWord);
        }

        if (myAllCleared() && !finished) {
            finished = true;
            if (netClient != null) {
                netClient.sendFinish();
            }
        }
    }

    // [ADD] 듀얼 인트로(미션 안내 + START!) 표시
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
        Font msgFont = NAME_FONT.deriveFont(NAME_FONT.getSize2D() + 8.0f); // 좀 크게
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

        // [ADD] 듀얼 시작 안내 문구
        drawStartMessage(g2, w, h);

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

        finished = true;
        showRetryOverlay = false;


        inputField.setText("");
        inputField.setEnabled(false);

        // 네트워크 연결 정리(선택)
        if (netClient != null) {
            try {
                netClient.close();
            } catch (Exception ignore) {}
        }

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

    // RETRY 클릭
    private void handleRetryClicked() {
        finished = false;
        resultState = ResultState.NONE;
        showRetryOverlay = false;

        p1Remaining = TOTAL_BALLOONS_PER_PLAYER;
        p2Remaining = TOTAL_BALLOONS_PER_PLAYER;

        // 풍선 새로 스폰
        spawnInitialBalloons();

        // 룰 초기화
        rules = new VersusGameRules(INITIAL_TIME_SECONDS);

        // 🔽 입력창 다시 활성화 + 포커스 주기
        inputField.setText("");
        inputField.setEnabled(true);
        inputField.setVisible(true); // 혹시 모르니 true 한 번 명시
        SwingUtilities.invokeLater(() -> inputField.requestFocusInWindow());

        repaint();

        if (netClient != null) {
            netClient.sendRetry();
        }
    }

}
