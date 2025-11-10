// [TEMP-A] 점수 누적 임시 로직 (Team C ScoreCalculator 들어오면 대체)
package com.balloon.ui.screens;

import com.balloon.core.*;
import com.balloon.game.model.BalloonSprite;
import com.balloon.ui.assets.BalloonSkins;
import com.balloon.ui.assets.BalloonSkins.Skin;
import com.balloon.ui.assets.ImageAssets;
import com.balloon.ui.render.BalloonSpriteRenderer;
// [ADD] 아이템 카테고리 enum
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GamePanel :
 * - 배경: 레벨별 PNG
 * - 집: PNG (하단 중앙). 집 지붕의 한 점이 줄(실) 앵커
 * - 풍선: PNG(5색) + 줄(실)은 코드로 그림
 * - 좌상단 HUD: life 하트(현재 라이프 수만큼), 타이머
 * - 기존 점수/시간/토스트/스테이지 플로우 유지
 */
public class GamePanel extends JPanel implements Showable {

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g.create();

        //배경 png
        if (bgImg != null) {
            g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), null); // 부모 패널 전체에 배경 적용
        }
    }

    // --- 모델 상태 ---
    private int timeLeft = 60;        // 남은 시간(초)
    private int score = 0;            // 점수
    private int wordIndex = 0;        // 현재 타겟 단어 인덱스(라벨용)
    private int stage = 1;            // 현재 스테이지 (1 -> 2 -> 3)
    public static int lastCompletedStage = 1;
    private final ScreenRouter router;

    private int lives = 3; //life(하트) - single mode : 오타 1회당 1감소, 0이면 게임 오버

    private final JLabel overlayLabel = new JLabel(" ", SwingConstants.CENTER);
    private final Timer overlayTimer = new Timer(1200, e -> overlayLabel.setVisible(false));
    private boolean stageClearedThisRound = false;
    private volatile boolean navigatedAway = false;
    private final JTextField inputField = new JTextField(); //한글입력 가능

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

    // ▼ [NEW] 이미지 자산 (배경/집/하트)
    private BufferedImage bgImg;
    private BufferedImage houseImg;
    private BufferedImage heartImg;

    public GamePanel(ScreenRouter router) {
        this.router = router;

        // [레이아웃/배경]
        setLayout(new BorderLayout());

        setOpaque(false);

        // HUD 바는 투명 (또는 아주 살짝 어두운 고정색을 쓰고 싶으면 setBackground(new Color(0,0,0,120)))
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        hud.setOpaque(false);

// 글자색은 그냥 흰색으로
        timeLabel.setForeground(Color.WHITE);
        scoreLabel.setForeground(Color.WHITE);
        playerLabel.setForeground(Color.WHITE);
        modeLabel.setForeground(Color.WHITE);

        hud.add(timeLabel);
        hud.add(scoreLabel);

        hud.add(new JLabel(" | "));
        hud.add(playerLabel);
        hud.add(modeLabel);
        add(hud, BorderLayout.NORTH);

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

        // [하단 입력바]
        JPanel inputBar = new JPanel(new BorderLayout());
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
        inputBar.setOpaque(false);

        // ★ 입력칸 길이 축소
        inputField.setFont(inputField.getFont().deriveFont(Font.BOLD, 18f));
        inputField.setColumns(18);   //더 짧게 하고싶으면 이거 낮추기        // 24 → 18
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(Color.BLACK);
        inputField.setCaretColor(Color.BLACK);
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));

// 가변 너비가 너무 길어지지 않게 선호 크기 고정(최대 560px)
        inputField.setPreferredSize(new Dimension(560, 34));          // ★ 추가
        inputBar.add(inputField, BorderLayout.CENTER);


// 힌트 라벨은 우측에 그대로 둬도 되고 제거해도 됩니다.
        JLabel hint = new JLabel("타이핑 · Enter=확인 · Backspace=삭제 · Esc=지우기");
        hint.setForeground(new Color(200, 210, 230));
        inputBar.add(hint, BorderLayout.EAST);

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


        // [1초 틱]
        tickTimer = new Timer(1000, e -> {
            if (timeLeft > 0) {
                timeLeft--;
                timeLabel.setText("Time: " + timeLeft);
                if (timeLeft == 0 && !playField.isAllCleared()) onStageFailed();
            }
        });

        // ▼ [NEW] 이미지 로드
        heartImg = ImageAssets.load("heart.png");
        houseImg = ImageAssets.load("home.png");
        applyStageBackground(stage);

        setStage(1);
        applyStageBackground(1);

        // HUD 동기화/첫 단어
        updateContextHud();
        showCurrentWord();
    }

    // ▼ [NEW] 스테이지별 배경 로드
    private void applyStageBackground(int stage) {
        String bgName = switch (stage) {
            case 1 -> "bg_level1.png";
            case 2 -> "bg_level2.png";
            default -> "bg_level3.png";
        };
        bgImg = ImageAssets.load(bgName);
        repaint();
    }

    // HUD: Session/Context에서 플레이어/모드 읽어 표시
    private void updateContextHud() {
        String name = Session.getNickname();
        if (name == null || name.isBlank()) {
            try {
                String fromCtx = (ctx != null) ? ctx.getPlayerName() : null;
                if (fromCtx != null && !fromCtx.isBlank()) name = fromCtx;
            } catch (Exception ignore) {}
        }
        if (name == null || name.isBlank()) name = "-";
        playerLabel.setText("Player: " + name);

        String mode = "-";
        try {
            String m = (ctx != null) ? String.valueOf(ctx.getMode()) : null;
            if (m != null && !m.equalsIgnoreCase("null") && !m.isBlank()) mode = m;
        } catch (Exception ignore) {}
        modeLabel.setText("Mode: " + mode);
    }


    // Enter 처리: 입력 단어로 풍선 팝 시도 → 점수/토스트
    private void onEnter() {
        String typed = inputField.getText().trim();

        // 1) 빈 입력은 바로 Miss
        if (typed.isEmpty()) {
            showToast("✗ Miss", new Color(190, 60, 60));
            return;
        }

        // 2) 맞춘 효과 정보 받기 (없으면 null)
        PopResult popped = playField.popFirstByText(typed);

        if (popped != null) {
            // --- 성공 처리 ---
            score += 10; // 임시 가산점
            scoreLabel.setText("Score: " + score);

            // ★ 아이템 효과 적용
            if (popped.category == ItemCategory.TIME) {
                timeLeft = Math.max(0, timeLeft + popped.itemValue);
                timeLabel.setText("Time: " + timeLeft);
            } else if (popped.category == ItemCategory.BALLOON) {
                // 예: ±1을 점수 보너스(×5)로 반영
                score += (popped.itemValue * 5);
                scoreLabel.setText("Score: " + score);
            }

            showToast("✓ Pop!", new Color(25, 155, 75));

            if (playField.isAllCleared()) {
                onStageCleared();
                return;
            }
        } else {
            // --- 실패 처리: 단어 불일치 ---
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
    private String getCurrentWord() { return words.get(wordIndex); }
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
        inputField.requestFocusInWindow();    // ← 텍스트필드에 포커스!
        if (!tickTimer.isRunning()) tickTimer.start();
    }


    @Override public void onShown() {
        navigatedAway = false;
        grabFocusSafely();
        updateContextHud();
    }

    public void onHidden() {
        navigatedAway = true;
        if (tickTimer.isRunning()) tickTimer.stop();
        if (playField != null) playField.stop();
        if (overlayTimer.isRunning()) overlayTimer.stop();
    }

    // ==========================================================
    // 내부 클래스 PlayField : PNG 풍선 렌더/업데이트
    // ==========================================================
    private final class PlayField extends JPanel {
        private final BalloonSpriteRenderer renderer = new BalloonSpriteRenderer();
        private final ArrayList<BalloonSprite> balloons = new ArrayList<>();
        private final Random rnd = new Random();
        private final Timer frameTimer; // ~60fps

        // [ADD] 단어별 아이템 효과 매핑(한 스테이지에 4개 배정)
        private final java.util.Map<String, PopResult> itemByWord = new java.util.HashMap<>();

        // 집 위치/크기/앵커
        private Rectangle houseRect = new Rectangle(0,0,0,0);
        private Point houseAnchor = new Point(0,0);

        PlayField() {
            setOpaque(false); //배경은 png만 쓰므로

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

        //집 너비 = 화면폭의 10%, 바닥과 72px 간격, 앵커는 지붕 30% 높이 지점
        private void layoutHouse() {
            int W = Math.max(getWidth(), 900);
            int H = Math.max(getHeight(), 600);

            if (houseImg == null) houseImg = ImageAssets.load("home.png");

            // ★ 집 전체 스케일 다운 (화면 폭의 9% 정도, 최소 90px, 최대 130px)
            int hw = Math.max(90, (int)(W * 0.09));
            int hh = (int)(hw * (houseImg.getHeight() / (double) houseImg.getWidth()));

            int hx = W/2 - hw/2;
            int hy = H - hh - 72; // 바닥과 간격

            houseRect.setBounds(hx, hy, hw, hh);
            // ★ 지붕 1/3 높이 지점에 앵커
            houseAnchor.setLocation(hx + hw/2, hy + (int)(hh * 0.30));
        }

        /** 처음 풍선 대량 생성 */
        private void spawnInitialBalloons() {
            balloons.clear();

            Skin[] skins = new Skin[]{ Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN };

            int W = Math.max(getWidth(), 900);
            int H = Math.max(getHeight(), 600);
            int centerX = W / 2;

            // 3-4-5-6-5-4-3 패턴
            int[] pattern = {3, 4, 5, 6, 5, 4, 3};
            int s = Math.max(68, Math.min(92, (int)(W * 0.07)));   // 한 변 길이
            int gapX = (int)(s * 1.20);                             // 가로 간격
            int gapY = (int)(s * 0.95);                             // 세로 간격
            //다발의 맨 윗줄 Y (집 앵커에서 충분히 위로)
            int topY = Math.max(110, houseAnchor.y - (gapY * (pattern.length + 1)));


            String[] bank = {
                    "도서관","고양이","운동장","한가람","바다빛","이야기","도전정신",
                    "자다","인터넷","병원","전문가","초롱빛","노력하다","택시","집","나라",
                    "달빛","별빛","산책","행복","용기","친구","추억","봄날","밤하늘"
            };
            int idx = 0;

            for (int r = 0; r < pattern.length; r++) {
                int count = pattern[r];
                int y = topY + r * gapY;

                int totalWidth = (count - 1) * gapX;
                int startX = centerX - totalWidth / 2;

                for (int c = 0; c < count; c++) {
                    Skin skin = skins[(idx + c) % skins.length];
                    BufferedImage img = BalloonSkins.of(skin);
                    int x = startX + c * gapX;

                    BalloonSprite b = new BalloonSprite(
                            bank[idx % bank.length], img, x, y,
                            houseAnchor.x, houseAnchor.y
                    );
                    // ★ 정방형(1:1) 보장
                    b.w = s;
                    b.h = s;

                    balloons.add(b);
                    idx++;
                }
            }
            // ★ SINGLE 모드 규칙대로 아이템 4개를 배정(빨강 2, 파랑 2 / 값은 각자 ± 랜덤)
            assignRandomItemsForSingleMode();
        }

        /** ★ SINGLE MODE: TIME(빨강) 2개, BALLOON(파랑) 2개를 중복 없이 랜덤 단어에 배정 */
        private void assignRandomItemsForSingleMode() {
            itemByWord.clear();

            // 현재 화면에 올라간 모든 단어 수집
            java.util.List<String> wordsOnScreen = new java.util.ArrayList<>();
            for (BalloonSprite b : balloons) wordsOnScreen.add(b.text);

            if (wordsOnScreen.size() < 4) return; // 안전장치

            // 인덱스 셔플
            java.util.List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < wordsOnScreen.size(); i++) indices.add(i);
            java.util.Collections.shuffle(indices, rnd);

            // 앞에서 4개 선택: 0,1 → TIME / 2,3 → BALLOON
            int[] pick = { indices.get(0), indices.get(1), indices.get(2), indices.get(3) };

            // TIME 2개: 각 +5 또는 -5
            for (int k = 0; k < 2; k++) {
                String w = wordsOnScreen.get(pick[k]);
                int val = (rnd.nextBoolean() ? +5 : -5);
                itemByWord.put(w, new PopResult(ItemCategory.TIME, val));
            }
            // BALLOON 2개: 각 +1 또는 -1
            for (int k = 2; k < 4; k++) {
                String w = wordsOnScreen.get(pick[k]);
                int val = (rnd.nextBoolean() ? +1 : -1);
                itemByWord.put(w, new PopResult(ItemCategory.BALLOON, val));
            }
        }



        private void updateModel() { /* Day5-Static: 풍선 고정 */ }

        /** 입력 텍스트와 같은 첫 PNG 풍선을 제거하고, 효과정보(PopResult)를 반환(없으면 null) */
        PopResult popFirstByText(String text) {
            if (text == null || text.isEmpty()) return null;
            for (int i = 0; i < balloons.size(); i++) {
                BalloonSprite b = balloons.get(i);
                if (b.text.equalsIgnoreCase(text)) {
                    // 제거 전에 이 단어에 배정된 효과를 가져온다(없으면 카테고리 없음 + 0 효과)
                    PopResult effect = itemByWord.getOrDefault(
                            b.text,
                            new PopResult(null, 0)   // ★ 기본값: category=null, itemValue=0
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

            // 0) 배경 PNG
            //if (bgImg != null) g2.drawImage(bgImg, 0, 0, getWidth(), getHeight(), null);

            // 1) 집
            if (houseImg != null) {
                g2.drawImage(houseImg, houseRect.x, houseRect.y, houseRect.width, houseRect.height, null);
            }

            // 2) 모든 줄(실)을 먼저 한 번에 그리기
            for (var b : balloons) {
                b.anchorX = houseAnchor.x; // 앵커 최신화
                b.anchorY = houseAnchor.y;
                renderer.renderLineOnly(g2, b);
            }

// 3) 그 다음에 모든 풍선 이미지+글자를 한 번에
            for (var b : balloons) {
                renderer.renderBalloonOnly(g2, b);
            }


            // 3) 좌상단 HUD: life 하트 + 타이머
            drawHUD(g2);

            g2.dispose();
        }

        /** 좌상단 하트/타이머 */
        private void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Dialog", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            int x = 16, y = 28;

            g2.drawString("life:", x, y);

            // ★ 필드 값 사용 (0~3로 보정)
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


        void stop() { if (frameTimer != null) frameTimer.stop(); }

        @Override public void invalidate() {
            super.invalidate();
            SwingUtilities.invokeLater(() -> {
                if (getWidth() > 0) {
                    layoutHouse();
                    // 풍선들의 앵커 최신화
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
        applyStageBackground(stage); // ▼ [NEW] 배경 교체
    }

    // 결과 컨텍스트 → Result 화면
    private void showResult() {
        if (navigatedAway) return;
        double accuracy   = 0.0;
        int correctCount  = 0;
        int wrongCount    = 0;

        ResultData data = new ResultData(score, Math.max(0, timeLeft), accuracy, correctCount, wrongCount);
        ResultContext.set(data);

        if (router != null) {
            try {
                Component c = router.get(ScreenId.RESULT);
                if (c instanceof ResultScreen rs) {
                    rs.setResult(score, accuracy, Math.max(0, timeLeft));
                }
                router.show(ScreenId.RESULT);
            } catch (Exception ex) {
                System.err.println("[GamePanel] cannot inject result into ResultScreen: " + ex);
                router.show(ScreenId.RESULT);
            }
        } else {
            System.err.println("[GamePanel] router is null → cannot navigate to RESULT");
        }
    }

    private void resetGame() {
        stage++; if (stage > 3) stage = 1;
        setStage(stage);
        score = 0; scoreLabel.setText("Score: 0");

        if (playField != null) {
            playField.balloons.clear();
            playField.spawnInitialBalloons();
        }

        caretOn = true;
        if (!tickTimer.isRunning()) tickTimer.start();
        grabFocusSafely();
        showToast("Stage " + stage + " Start!", new Color(100, 200, 100));
    }

    private void onStageCleared() {
        if (stageClearedThisRound) return;
        stageClearedThisRound = true;

        if (timeLeft > 0) {
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
        showOverlay("✖ FAILED!  (Stage " + stage + ")", new Color(230, 90, 90));
        new Timer(500, e -> showResult()).start();
    }

    private void startNextStage() {
        if (navigatedAway) return;
        stageClearedThisRound = false;

        if (stage >= 3) { showRanking(); return; }

        stage++; setStage(stage);

        if (playField != null) {
            playField.balloons.clear();
            playField.spawnInitialBalloons();
        }

        caretOn = true;
        if (!tickTimer.isRunning()) tickTimer.start();
        grabFocusSafely();

        showToast("Stage " + stage + " Start!", new Color(100, 200, 100));
    }

    private void showOverlay(String text, Color color) {
        overlayLabel.setText(text);
        overlayLabel.setForeground(color);
        overlayLabel.setVisible(true);
        overlayTimer.restart();
    }

    private void showRanking() { showResult(); }

    // setupKeyBindings()는 엔터/ESC/백스페이스 정도만 남기고,
// 글자 입력 바인딩은 전부 제거하세요(IME가 처리).
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
}
