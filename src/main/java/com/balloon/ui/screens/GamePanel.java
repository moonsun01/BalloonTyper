// [TEMP-A] 점수 누적 임시 로직 (Team C ScoreCalculator 들어오면 대체)
package com.balloon.ui.screens;

import com.balloon.core.*;
import com.balloon.game.model.BalloonSprite;
import com.balloon.ui.assets.BalloonSkins;
import com.balloon.ui.assets.BalloonSkins.Skin;
import com.balloon.ui.assets.ImageAssets;
import com.balloon.ui.render.BalloonSpriteRenderer;
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;

// [HUD] 활성 아이템 배지용
import com.balloon.core.GameContext;
import com.balloon.ui.hud.HUDRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// [랭킹 CSV 저장용]
import com.balloon.ranking.RankingCsvRepository;
import com.balloon.ranking.RankingRecord;

/**
 * GamePanel :
 * - 배경: 레벨별 PNG
 * - 집: PNG (하단 중앙). 집 지붕의 한 점이 줄(실) 앵커
 * - 풍선: PNG(5색) + 줄(실)은 코드로 그림
 * - 좌상단 HUD: life 하트(현재 라이프 수만큼), 타이머
 * - 기존 점수/시간/토스트/스테이지 플로우 유지
 */
public class GamePanel extends JPanel implements Showable {

    // ★ 결과 화면 한 번만 띄우기 위한 플래그
    private boolean resultShown = false;

    @Override
    public Dimension getPreferredSize() {
        // 배경 원본 이미지 크기에 맞추고 싶으면 여기 사이즈를 동일하게 지정
        return new Dimension(1280, 720);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 배경 png (부모 패널 전체)
        if (bgImg != null) {
            g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // --- 모델 상태 ---
    private int timeLeft = 60;        // 남은 시간(초)
    private int score = 0;            // 점수
    private int wordIndex = 0;        // 현재 타겟 단어 인덱스(라벨용)
    private int stage = 1;            // 현재 스테이지 (1 -> 2 -> 3)
    public static int lastCompletedStage = 1;
    private final ScreenRouter router;

    // --- Day11: 브레이크다운 집계용 ---
    private int correctCount = 0;
    private int wrongCount   = 0;
    private int wordScore    = 0;  // = correctCount * 10 (현재 설계)
    private int timeBonus    = 0;  // 스테이지 성공 시 남은 시간 가산
    private int itemBonus    = 0;  // BALLOON 아이템 ± 보너스 합

    private int lives = 3; // life(하트)

    private final JLabel overlayLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Timer overlayTimer = new Timer(1200, e -> overlayLabel.setVisible(false));
    private boolean stageClearedThisRound = false;
    private volatile boolean navigatedAway = false;
    private final JTextField inputField = new JTextField(); // 한글입력 가능

    // 임시 단어 리스트(후에 words.csv로 교체 예정)
    private final List<String> words = List.of(
            "apple","orange","banana","grape","melon",
            "keyboard","monitor","java","swing","balloon",
            "house","safety","ground","landing","rope",
            "cloud","river","green","delta","timer",
            "purple","queen","sun","tree","unity",
            "water","xenon","youth","zebra","type"
    );

    // --- 라벨 HUD(상단 바는 기존 유지) ---
    private final JLabel timeLabel = new JLabel("Time: 60");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel wordLabel  = new JLabel("", SwingConstants.CENTER);
    private final JLabel toastLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel playerLabel = new JLabel("Player: -");
    private final JLabel modeLabel   = new JLabel("Mode: -");

    // 중앙 플레이 영역(풍선 캔버스)
    private final PlayField playField;

    private boolean caretOn = true;
    private final Timer tickTimer;

    // 전역 컨텍스트
    private final GameContext ctx = GameContext.getInstance();

    /** ★ 풍선을 맞췄을 때 적용할 효과 정보 */
    private static final class PopResult {
        final ItemCategory category;  // TIME, BALLOON, NORMAL 등
        final int itemValue;          // TIME: ±5, BALLOON: ±1 등
        PopResult(ItemCategory category, int itemValue) {
            this.category = category;
            this.itemValue = itemValue;
        }
    }

    // ▼ 이미지 자산 (배경/집/하트)
    private BufferedImage bgImg;
    private BufferedImage houseImg;
    private BufferedImage heartImg;

    // [HUD] 활성 아이템 배지 갱신 타이머(0.2초마다 repaint)
    private final javax.swing.Timer hudTimer = new javax.swing.Timer(200, e -> repaint());

    public GamePanel(ScreenRouter router) {
        this.router = router;

        // 상단 바 컨테이너(좌: 기존 HUD, 우: 아이템 전설)
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        // 레이아웃/배경
        setLayout(new BorderLayout());

        // 상단 HUD: 좌측 정보
        JPanel hud = new JPanel(new BorderLayout());
        hud.setOpaque(false);

        // 좌측 묶음(기존 라벨들)
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        left.setOpaque(false);

        timeLabel.setForeground(Color.WHITE);
        scoreLabel.setForeground(Color.WHITE);
        playerLabel.setForeground(Color.WHITE);
        modeLabel.setForeground(Color.WHITE);

        left.add(timeLabel);
        left.add(scoreLabel);
        left.add(new JLabel(" | "));
        left.add(playerLabel);
        left.add(modeLabel);

        hud.add(left, BorderLayout.WEST);

        // (우) 아이템 전설 패널
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        legend.setOpaque(false);

        JLabel timeBadge = new JLabel("TIME ±5");
        timeBadge.setForeground(new Color(255, 120, 120)); // 붉은 톤
        timeBadge.setFont(timeBadge.getFont().deriveFont(Font.BOLD, 12f));

        JLabel balloonBadge = new JLabel("BALLOON ±1");
        balloonBadge.setForeground(new Color(120, 160, 255)); // 푸른 톤
        balloonBadge.setFont(balloonBadge.getFont().deriveFont(Font.BOLD, 12f));

        JLabel legendTitle = new JLabel("Items:");
        legendTitle.setForeground(new Color(235, 235, 235));
        legendTitle.setFont(legendTitle.getFont().deriveFont(Font.PLAIN, 12f));

        legend.add(legendTitle);
        legend.add(timeBadge);
        legend.add(balloonBadge);

        // topBar에 배치
        topBar.add(hud, BorderLayout.WEST);
        topBar.add(legend, BorderLayout.EAST);

        // 상단에 장착
        add(topBar, BorderLayout.NORTH);

        // [중앙 플레이]
        playField = new PlayField();
        playField.setLayout(new BorderLayout());
        add(playField, BorderLayout.CENTER);

        // [중앙 단어 라벨(가이드)]
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        wordLabel.setForeground(Color.WHITE);
        playField.add(wordLabel, BorderLayout.CENTER);
        wordLabel.setVisible(false); // Day5: 중앙 가이드 비표시

        // [토스트 라벨]
        toastLabel.setForeground(new Color(80, 120, 80));
        toastLabel.setFont(toastLabel.getFont().deriveFont(Font.PLAIN, 16f));
        playField.add(toastLabel, BorderLayout.SOUTH);

        // 하단 입력바 (가운데 정렬)
        JPanel inputBar = new JPanel();
        inputBar.setOpaque(false);
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        inputBar.setLayout(new BoxLayout(inputBar, BoxLayout.X_AXIS));

        inputBar.add(Box.createHorizontalGlue());

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setOpaque(false);
        int rowW = 480; // 전체 폭 줄이기
        int rowH = 40;  // 높이 줄이기
        inputRow.setPreferredSize(new Dimension(rowW, rowH));

        inputField.setFont(inputField.getFont().deriveFont(Font.PLAIN, 18f));
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(Color.BLACK);
        inputField.setCaretColor(Color.BLACK);
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        inputField.setPreferredSize(new Dimension(rowW - 60, rowH - 4));
        inputRow.add(inputField, BorderLayout.CENTER);

        JLabel hint = new JLabel("타이핑 · Enter=확인 · Backspace=삭제 · Esc=지우기");
        hint.setForeground(new Color(200, 210, 230));
        hint.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        inputRow.add(hint, BorderLayout.EAST);

        inputBar.add(inputRow);
        inputBar.add(Box.createHorizontalGlue());

        add(inputBar, BorderLayout.SOUTH);

        // [오버레이 라벨]
        overlayLabel.setFont(overlayLabel.getFont().deriveFont(Font.BOLD, 42f));
        overlayLabel.setForeground(new Color(255, 255, 160));
        overlayLabel.setVisible(false);
        playField.add(overlayLabel, BorderLayout.NORTH);
        overlayTimer.setRepeats(false);

        // [입력 안정화]
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setupKeyBindings();
        SwingUtilities.invokeLater(this::grabFocusSafely);
        playField.setFocusable(false);
        wordLabel.setFocusable(false);
        toastLabel.setFocusable(false);

        // [교체] 1초 틱: 시간 감소 + 0이하 즉시 실패
        tickTimer = new Timer(1000, e -> {
            if (resultShown) return; // 이미 결과로 넘어갔으면 더 이상 처리하지 않음

            if (timeLeft > 0) {
                timeLeft--;
                timeLabel.setText("Time: " + timeLeft);
            }

            if (timeLeft <= 0) {
                timeLeft = 0;
                timeLabel.setText("Time: 0");
                showResult();
                return;
            }

            if (lives <= 0) {
                showResult();
            }
        });

        // 이미지 로드
        heartImg = ImageAssets.load("heart.png");
        houseImg = ImageAssets.load("home.png");
        applyStageBackground(stage);

        setStage(1);
        applyStageBackground(1);

        // HUD 동기화/첫 단어
        updateContextHud();
        showCurrentWord();

        // [HUD] 활성 아이템 배지 주기 갱신 시작
        hudTimer.start();
        tickTimer.start();
    }

    // --- 공통 유틸 메서드들 -------------------------------------------------

    // 스테이지별 배경 로드
    private void applyStageBackground(int stage) {
        String bgName = switch (stage) {
            case 1 -> "bg_level1.png";
            case 2 -> "bg_level2.png";
            default -> "bg_level3.png";
        };
        bgImg = ImageAssets.load(bgName);
        repaint();
    }

    // ★ 공통: Session / GameContext에서 플레이어 이름을 하나의 메서드로 해결
    private String resolvePlayerName() {
        String name = Session.getNickname(); // 1순위

        if (name == null || name.isBlank()) {
            try {
                String fromCtx = (ctx != null) ? ctx.getPlayerName() : null;
                if (fromCtx != null && !fromCtx.isBlank()) {
                    name = fromCtx;
                }
            } catch (Exception ignore) {}
        }

        if (name == null || name.isBlank()) {
            name = "-";
        }

        return name;
    }

    // HUD: Session/Context에서 플레이어/모드 읽어 표시
    private void updateContextHud() {
        String name = resolvePlayerName();
        playerLabel.setText("Player: " + name);

        String mode = "-";
        try {
            String m = (ctx != null) ? String.valueOf(ctx.getMode()) : null;
            if (m != null && !m.equalsIgnoreCase("null") && !m.isBlank()) mode = m;
        } catch (Exception ignore) {}
        modeLabel.setText("Mode: " + mode);
    }

    // ★ 게임 루프(타이머, 프레임 타이머) 중지 통합
    private void stopGameLoops() {
        if (tickTimer != null && tickTimer.isRunning()) {
            tickTimer.stop();
        }
        if (playField != null) {
            playField.stop();
        }
    }

    // ★ 스테이지용 풍선 재생성 로직 공통화
    private void reloadStageBalloons() {
        if (playField != null) {
            playField.balloons.clear();
            playField.spawnInitialBalloons();
        }
    }

    // ★ 스테이지 시작 시 공통 초기화(포커스, 타이머 등)
    private void restartStageCommon() {
        caretOn = true;
        if (!tickTimer.isRunning()) {
            tickTimer.start();
        }
        grabFocusSafely();
    }

    // ------------------------------------------------------------------------

    // Enter 처리: 단어 일치 검사 → 점수/아이템/브레이크다운 갱신 → HUD/토스트 표시
    private void onEnter() {
        String typed = inputField.getText().trim();

        // 1) 빈 입력은 Miss (하트-1 처리 유지)
        if (typed.isEmpty()) {
            showToast("✗ Miss", new Color(190, 60, 60));
            lives = Math.max(0, lives - 1);
            playField.repaint();
            if (lives == 0) {
                onStageFailed();
                return;
            }
            inputField.setText("");
            return;
        }

        // 2) 맞춘 효과 정보 받기 (없으면 null)
        PopResult popped = playField.popFirstByText(typed);

        if (popped != null) {
            // --- 성공 처리 ---
            correctCount++;
            wordScore += 10;        // 단어점수 누적(+10)
            score += 10;            // 총점 반영
            scoreLabel.setText("Score: " + score);

            // ★ 아이템 효과 적용
            if (popped.category == ItemCategory.TIME) {
                // TIME: 남은 시간 ±5 적용
                timeLeft = Math.max(0, timeLeft + popped.itemValue);
                timeLabel.setText("Time: " + timeLeft);

                // HUD 배지(우상단) 2초 표시
                String label = (popped.itemValue >= 0 ? "+" : "") + popped.itemValue + "s";
                GameContext.getInstance().activateItem(ItemCategory.TIME, label, 2000L);

            } else if (popped.category == ItemCategory.BALLOON) {
                // BALLOON: 점수 보너스(±1 → ±5점)
                int delta = popped.itemValue * 5;
                itemBonus += delta;
                score += delta;
                scoreLabel.setText("Score: " + score);

                // HUD 배지(우상단) 2초 표시 (원래 아이템 값 표시)
                String label = (popped.itemValue >= 0 ? "+" : "") + popped.itemValue;
                GameContext.getInstance().activateItem(ItemCategory.BALLOON, label, 2000L);
            }

            showToast("✓ Pop!", new Color(25, 155, 75));

            // 모든 풍선 제거 시 스테이지 클리어 처리
            if (playField.isAllCleared()) {
                onStageCleared();
                inputField.setText("");
                return;
            }

        } else {
            // --- 실패 처리: 단어 불일치 ---
            wrongCount++;
            showToast("✗ Miss", new Color(190, 60, 60));

            lives = Math.max(0, lives - 1); // 오타 1회 = 목숨 1 감소
            playField.repaint();             // 하트 즉시 갱신
            if (lives == 0) {
                onStageFailed();
                return;
            }
        }

        // 3) 입력창 정리
        inputField.setText("");
    }

    private void showToast(String msg, Color color) {
        toastLabel.setForeground(color);
        toastLabel.setText(msg);
        Timer t = new Timer(600, e -> toastLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void nextWord() {
        wordIndex = (wordIndex + 1) % words.size();
        showCurrentWord();
    }

    private String getCurrentWord() {
        return words.get(wordIndex);
    }

    private void showCurrentWord() {
        wordLabel.setText(getCurrentWord());
    }

    private void gameOver() {
        showToast("Time Over!", new Color(90, 90, 90));
        if (tickTimer.isRunning()) tickTimer.stop();
        lastCompletedStage = stage;
        showResult();
    }

    // grabFocusSafely() 안에서
    private void grabFocusSafely() {
        inputField.requestFocusInWindow();
        if (!tickTimer.isRunning()) tickTimer.start();
    }

    // ★ 화면이 보일 때 호출됨(라우터가 onShown()을 불러주어야 함)
    @Override
    public void onShown() {
        navigatedAway = false;

        // 결과에서 돌아왔거나(플래그 true), 시간/라이프가 소진된 상태라면 완전 초기화
        if (resultShown || timeLeft <= 0 || lives <= 0) {
            fullResetToStage1();
        }

        grabFocusSafely();
        updateContextHud();
        if (!tickTimer.isRunning()) tickTimer.start();
    }

    public void onHidden() {
        navigatedAway = true;
        stopGameLoops();
        if (overlayTimer.isRunning()) overlayTimer.stop();
    }

    // ==========================================================
    // 내부 클래스 PlayField : PNG 풍선 렌더/업데이트
    // ==========================================================
    private final class PlayField extends JPanel {
        private static final int DESIGN_W = 1280;
        private static final int DESIGN_H = 720;

        private final BalloonSpriteRenderer renderer = new BalloonSpriteRenderer();
        private final ArrayList<BalloonSprite> balloons = new ArrayList<>();
        private final Random rnd = new Random();
        private final Timer frameTimer; // ~60fps

        // 단어별 아이템 효과 매핑(한 스테이지에 4개 배정)
        private final java.util.Map<String, PopResult> itemByWord = new java.util.HashMap<>();

        // 집 위치/크기/앵커
        private Rectangle houseRect = new Rectangle(0,0,0,0);
        private Point houseAnchor = new Point(0,0);

        PlayField() {
            setOpaque(false); // 배경은 png만 쓰므로

            SwingUtilities.invokeLater(() -> {
                layoutHouse();
                spawnInitialBalloons();
            });

            // 16ms ≈ 60fps
            frameTimer = new Timer(16, e -> {
                updateModel();
                repaint();
            });
            frameTimer.start();
        }

        // 집 너비 = 화면폭의 10%, 바닥과 72px 간격, 앵커는 지붕 30% 높이 지점
        private void layoutHouse() {
            int W = DESIGN_W;
            int H = DESIGN_H;

            if (houseImg == null) houseImg = ImageAssets.load("home.png");

            int hw = 80; // 집 너비
            int hh = 70;

            int hx = W / 2 - hw / 2;         // 가로 중앙에 두기
            int hy = H - hh - 140;           // 바닥과 간격

            houseRect.setBounds(hx, hy, hw, hh);
            // 지붕 1/3 높이 지점에 앵커
            houseAnchor.setLocation(hx + hw / 2, hy + (int)(hh * 0.30));
        }

        /** 처음 풍선 대량 생성 */
        private void spawnInitialBalloons() {
            balloons.clear();

            int W = DESIGN_W;
            int H = DESIGN_H;
            int centerX = W / 2;

            int s = 70; // 풍선 한 변

            int gapX = 90;
            int gapY = 60;

            int[] pattern = {3, 4, 5, 6, 5, 4, 3};
            int rows = pattern.length;

            int margin = 12; // 집 윗부분과 풍선 사이 여유
            int bottomY;
            if (houseRect != null && houseRect.height > 0) {
                bottomY = houseRect.y - s - margin;
            } else {
                bottomY = 300;
            }

            int topY = bottomY - (rows - 1) * gapY;

            String[] bank = {
                    "도서관","고양이","운동장","한가람","바다빛","이야기","도전정신",
                    "자다","인터넷","병원","전문가","초롱빛","노력하다","택시","집","나라",
                    "달빛","별빛","산책","행복","용기","친구","추억","봄날","밤하늘"
            };

            Skin[] skins = new Skin[]{ Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN };
            int idx = 0;

            for (int r = 0; r < rows; r++) {
                int count = pattern[r];
                int y = topY + r * gapY;

                int totalWidth = (count - 1) * gapX;
                int startX = centerX - totalWidth / 2;

                for (int c = 0; c < count; c++) {
                    Skin skin = skins[(idx + c) % skins.length];
                    BufferedImage img = BalloonSkins.of(skin);
                    int x = startX + c * gapX;

                    BalloonSprite b = new BalloonSprite(
                            bank[idx % bank.length],
                            img,
                            x, y,
                            houseAnchor.x,
                            houseAnchor.y
                    );
                    b.w = s;
                    b.h = s;

                    balloons.add(b);
                    idx++;
                }
            }

            // SINGLE 모드 아이템 4개 배정(빨강 2, 파랑 2)
            assignRandomItemsForSingleMode();
        }

        /** 단어를 가진 풍선의 글자색을 지정 */
        private void tintWordTextColor(String word, java.awt.Color color) {
            for (BalloonSprite b : balloons) {
                if (b.text.equals(word)) {
                    b.textColor = color;
                    break;
                }
            }
        }

        // ★ TIME 아이템 글자색 결정 로직 분리
        private java.awt.Color timeColorFor(int val) {
            return (val >= 0)
                    ? new java.awt.Color(255, 110, 110)  // +5
                    : new java.awt.Color(200, 70, 70);   // -5
        }

        // ★ BALLOON 아이템 글자색 결정 로직 분리
        private java.awt.Color balloonColorFor(int val) {
            return (val >= 0)
                    ? new java.awt.Color(120, 170, 255)  // +1
                    : new java.awt.Color(80, 120, 200);  // -1
        }

        /** SINGLE MODE: TIME(빨강) 2개, BALLOON(파랑) 2개를 중복 없이 랜덤 단어에 배정 */
        private void assignRandomItemsForSingleMode() {
            itemByWord.clear();

            java.util.List<String> wordsOnScreen = new java.util.ArrayList<>();
            for (BalloonSprite b : balloons) wordsOnScreen.add(b.text);

            if (wordsOnScreen.size() < 4) return; // 안전장치

            java.util.List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < wordsOnScreen.size(); i++) indices.add(i);
            java.util.Collections.shuffle(indices, rnd);

            int[] pick = { indices.get(0), indices.get(1), indices.get(2), indices.get(3) };

            // TIME 2개: 각 +5 또는 -5
            for (int k = 0; k < 2; k++) {
                String w = wordsOnScreen.get(pick[k]);
                int val = (rnd.nextBoolean() ? +5 : -5);
                itemByWord.put(w, new PopResult(ItemCategory.TIME, val));
                tintWordTextColor(w, timeColorFor(val));
            }

            // BALLOON 2개: 각 +1 또는 -1
            for (int k = 2; k < 4; k++) {
                String w = wordsOnScreen.get(pick[k]);
                int val = (rnd.nextBoolean() ? +1 : -1);
                itemByWord.put(w, new PopResult(ItemCategory.BALLOON, val));
                tintWordTextColor(w, balloonColorFor(val));
            }
        }

        private void updateModel() {
            // Day5-Static: 풍선 고정 (움직임 없음)
        }

        /** 입력 텍스트와 같은 첫 PNG 풍선을 제거하고, 효과정보(PopResult)를 반환(없으면 null) */
        PopResult popFirstByText(String text) {
            if (text == null || text.isEmpty()) return null;
            for (int i = 0; i < balloons.size(); i++) {
                BalloonSprite b = balloons.get(i);
                if (b.text.equalsIgnoreCase(text)) {
                    PopResult effect = itemByWord.getOrDefault(
                            b.text,
                            new PopResult(null, 0)   // 기본값
                    );
                    balloons.remove(i);
                    return effect;
                }
            }
            return null;
        }

        boolean isAllCleared() { return balloons.isEmpty(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();

            // 1) 모든 줄(실)
            for (var b : balloons) {
                b.anchorX = houseAnchor.x;
                b.anchorY = houseAnchor.y;
                drawLine(g2, b);
            }

            // 2) 집
            if (houseImg != null) {
                g2.drawImage(houseImg, houseRect.x, houseRect.y, houseRect.width, houseRect.height, null);
            }

            // 3) 모든 풍선 이미지+글자
            for (var b : balloons) {
                renderer.renderBalloonOnly(g2, b);
            }

            // 4) 좌상단 HUD: life 하트 + 타이머
            drawHUD(g2);

            // 5) 우상단: 현재 활성 아이템 배지
            HUDRenderer.drawCurrentItemBadge(g2, getWidth(), getHeight(), GameContext.getInstance());

            g2.dispose();
        }

        /** 풍선 '실'을 곡선으로 그리기 */
        private void drawLine(Graphics2D g2, BalloonSprite b) {
            if (b == null || b.state == BalloonSprite.State.DEAD) return;

            int ax = b.anchorX, ay = b.anchorY;
            int bx = b.attachX(), by = b.attachY();

            int cx = (ax + bx) / 2;
            int cy = Math.min(ay, by) - 40; // 살짝 위로 휘게

            Stroke old = g2.getStroke();
            Color oldC = g2.getColor();

            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 220));
            g2.draw(new java.awt.geom.QuadCurve2D.Float(ax, ay, cx, cy, bx, by));

            g2.setStroke(old);
            g2.setColor(oldC);
        }

        /** 좌상단 하트/타이머 */
        private void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Dialog", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            int x = 16, y = 28;

            g2.drawString("life:", x, y);

            int lifeCount = Math.max(0, Math.min(3, GamePanel.this.lives));

            int hx = x + 50;
            for (int i = 0; i < lifeCount; i++) {
                if (heartImg != null) {
                    g2.drawImage(heartImg, hx + i * 28, y - 18, 24, 24, null);
                }
            }

            String timeStr = String.format("Time limit : %d m %02d s",
                    Math.max(0, timeLeft) / 60, Math.max(0, timeLeft) % 60);
            g2.drawString(timeStr, x, y + 24);
        }

        void stop() {
            if (frameTimer != null) frameTimer.stop();
        }

        @Override
        public void invalidate() {
            super.invalidate();
            SwingUtilities.invokeLater(() -> {
                if (getWidth() > 0) {
                    layoutHouse();
                    for (var b : balloons) {
                        b.anchorX = houseAnchor.x;
                        b.anchorY = houseAnchor.y;
                    }
                    repaint();
                }
            });
        }
    }

    /** 스테이지에 따라 시작 시간 설정: 1=90s, 2=80s, 3=70s */
    public void setStage(int stage) {
        int t;
        switch (stage) {
            case 1 -> t = 90;
            case 2 -> t = 80;
            case 3 -> t = 70;
            default -> t = 90;
        }
        this.stage = stage;
        this.timeLeft = t;
        this.timeLabel.setText("Time: " + t);
        applyStageBackground(stage); // 배경 교체
    }

    private void showResult() {
        // ★ 중복 호출 방지
        if (resultShown) return;
        resultShown = true;

        // ★ 타이머/루프 정지
        stopGameLoops();

        int totalTry = correctCount + wrongCount;
        double acc = (totalTry > 0) ? (correctCount * 1.0 / totalTry) : 0.0;

        ResultData data = new ResultData(score, Math.max(0, timeLeft), acc, correctCount, wrongCount);
        ResultContext.set(data);

        // [DAY14] 랭킹 CSV에 현재 판 결과 저장
        double accuracyPercent = acc * 100.0;
        saveRanking(score, accuracyPercent, Math.max(0, timeLeft));

        if (router != null) {
            try {
                Component c = router.get(ScreenId.RESULT);
                if (c instanceof ResultScreen rs) {
                    rs.setResult(score, acc, Math.max(0, timeLeft));
                    rs.setBreakdown(wordScore, timeBonus, 0, itemBonus);
                }
                System.out.println("[DEBUG] showResult() → router.show(RESULT)");
                router.show(ScreenId.RESULT);
            } catch (Exception ex) {
                System.err.println("[GamePanel] inject failed: " + ex);
                router.show(ScreenId.RESULT);
            }
        } else {
            System.err.println("[GamePanel] router is null");
        }
    }

    private void resetGame() {
        stage++;
        if (stage > 3) stage = 1;
        setStage(stage);
        score = 0;
        scoreLabel.setText("Score: 0");

        reloadStageBalloons();
        restartStageCommon();
        showToast("Stage " + stage + " Start!", new Color(100, 200, 100));
    }

    private void onStageCleared() {
        if (stageClearedThisRound) return;
        stageClearedThisRound = true;

        if (timeLeft > 0) {
            timeBonus = Math.max(0, timeLeft);  // ★ 브레이크다운용 저장
            score += timeLeft;
            scoreLabel.setText("Score: " + score);
        }

        showOverlay("✔ SUCCESS!  (+ " + Math.max(0, timeLeft) + ")", new Color(110, 220, 110));
        lastCompletedStage = stage;

        if (stage >= 3) {
            Timer t = new Timer(200, e -> showResult());
            t.setRepeats(false);
            t.start();
        } else {
            Timer t1 = new Timer(1250, e -> {
                showOverlay("Stage " + (stage + 1) + " Ready...", new Color(255, 230, 140));
                Timer t2 = new Timer(1250, ev -> startNextStage());
                t2.setRepeats(false);
                t2.start();
            });
            t1.setRepeats(false);
            t1.start();
        }
    }

    private void onStageFailed() {
        stopGameLoops();

        showOverlay("✖ FAILED!  (Stage " + stage + ")", new Color(230, 90, 90));

        Timer t = new Timer(600, e -> {
            ((Timer)e.getSource()).stop();
            System.out.println("[DEBUG] onStageFailed → showResult()");
            showResult();
        });
        t.setRepeats(false);
        t.start();
    }

    private void startNextStage() {
        if (navigatedAway) return;
        stageClearedThisRound = false;

        if (stage >= 3) {
            showRanking();
            return;
        }

        stage++;
        setStage(stage);

        reloadStageBalloons();
        restartStageCommon();

        showToast("Stage " + stage + " Start!", new Color(100, 200, 100));
    }

    private void showOverlay(String text, Color color) {
        overlayLabel.setText(text);
        overlayLabel.setForeground(color);
        overlayLabel.setVisible(true);
        overlayTimer.restart();
    }

    private void showRanking() { showResult(); }

    // setupKeyBindings()는 엔터/ESC 정도만 남기고,
    // 글자 입력은 IME에 맡김
    private void setupKeyBindings() {
        // ENTER → 제출
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submitField");
        inputField.getActionMap().put("submitField", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onEnter(); }
        });
        // ESC → 전체 삭제
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearField");
        inputField.getActionMap().put("clearField", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { inputField.setText(""); }
        });
    }

    // ★ RESULT 후 재진입 시 완전 초기화(다시하기/메인→게임 공통)
    private void fullResetToStage1() {
        // 스코어/집계 리셋
        score = 0; scoreLabel.setText("Score: 0");
        correctCount = 0; wrongCount = 0;
        wordScore = 0; timeBonus = 0; itemBonus = 0;

        // 라이프/스테이지/시간 리셋
        lives = 3;
        setStage(1);          // 내부에서 timeLeft/배경/라벨 갱신

        // 풍선 재생성
        reloadStageBalloons();

        // 플래그/포커스/타이머
        resultShown = false;
        inputField.setText("");
        if (!tickTimer.isRunning()) tickTimer.start();
    }

    // [DAY14] 한 판 결과를 ranking.csv에 저장하는 메서드
    private void saveRanking(int finalScore, double accuracyPercent, int timeLeftSeconds) {
        String playerName = resolvePlayerName(); // 공통 메서드 사용

        // 현재 시각 문자열 만들기
        String playedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // RankingRecord 생성 (name, score, accuracy, timeLeft, playedAt)
        RankingRecord record = new RankingRecord(
                playerName,
                finalScore,
                accuracyPercent,
                timeLeftSeconds,
                playedAt
        );

        // CSV에 한 줄 추가 저장
        RankingCsvRepository repo = new RankingCsvRepository();
        repo.append(record);
    }
}
