package com.balloon.ui.screens;

import com.balloon.core.*;
import com.balloon.game.GameRules;
import com.balloon.game.GameState;
import com.balloon.game.LevelConfig;
import com.balloon.game.model.Balloon;
import com.balloon.game.model.BalloonSprite;
import com.balloon.items.ItemEffectApplier;
import com.balloon.items.ItemSpawner;
import com.balloon.ui.assets.BalloonSkins;
import com.balloon.ui.assets.BalloonSkins.Skin;
import com.balloon.ui.assets.ImageAssets;

// 단어 관련
import com.balloon.game.CsvWordLoader;
import com.balloon.game.WordProvider;
import com.balloon.game.NonRepeatingWordProvider;

// 새 랭킹용
import com.balloon.data.ScoreEntry;
import com.balloon.data.RankingRepository;
import com.balloon.data.CsvRankingRepository;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Random;

/**
 * UI는 1번 코드 스타일 유지 + 게임 로직은 GameState/Rules/Judge 구조 그대로
 */
public class GamePanel extends JPanel implements Showable {

    // ====== Game / State / Item ======
    private final LevelConfig levelConfig = new LevelConfig();
    private final GameState state = new GameState(levelConfig);
    private final ItemSpawner spawner = new ItemSpawner();

    // 모델 풍선 리스트 (GameJudge에 넘기는 리스트)
    private final List<Balloon> balloons = new ArrayList<>();

    // 단어 공급기 (CSV + 중복 방지)
    private final WordProvider wordProvider;

    // 스코어 브레이크다운 (UI용 임시)
    private int correctCount = 0;
    private int wrongCount = 0;

    // ✅ 총점: 각 스테이지 클리어 시 (남은 시간 × 10)을 누적
    private int totalScore = 0;

    // UI 콜백을 제공하는 Applier (시간/토스트/필드 조작)
    private final ItemEffectApplier applier = new ItemEffectApplier(
            // TimeApi
            new ItemEffectApplier.TimeApi() {
                @Override
                public void addSeconds(int delta) {
                    state.addSeconds(delta);
                    refreshHUD();
                }

                @Override
                public int getTimeLeft() {
                    return state.getTimeLeft();
                }
            },
            // UiApi
            new ItemEffectApplier.UiApi() {
                @Override
                public void showToast(String message) {
                    GamePanel.this.showToast(message, new java.awt.Color(40, 180, 100));
                }

                @Override
                public void flashEffect(boolean positive) {
                    GamePanel.this.flash(positive);
                }
            },
            // FieldApi
            new ItemEffectApplier.FieldApi() {
                @Override
                public void addBalloons(int n) {
                    GamePanel.this.addBalloons(n);
                }

                @Override
                public void removeBalloons(int n) {
                    GamePanel.this.removeBalloons(n);
                }
            }
    );

    // GameJudge(아이템 연동 버전)
    private final com.balloon.game.GameJudge judge = new com.balloon.game.GameJudge(spawner, applier);

    // GameRules 구현체 (싱글 모드 규칙)
    private final GameRules rules = new SingleGameRules();

    // ====== UI 필드 ======
    private final ScreenRouter router;

    // 상단 HUD 라벨
    private final JLabel timeLabel = new JLabel("Time: 0");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel playerLabel = new JLabel("Player: -");
    private final JLabel modeLabel = new JLabel("Mode: -");

    // 중앙 단어 가이드(현재는 숨김)
    private final JLabel wordLabel = new JLabel("", SwingConstants.CENTER);

    // 토스트 / 오버레이
    private final JLabel toastLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel overlayLabel = new JLabel(" ", SwingConstants.CENTER);

    // 입력 필드
    private final JTextField inputField = new JTextField();

    // 틱 타이머(1초) / 오버레이 off 타이머
    private final javax.swing.Timer tickTimer;
    private final javax.swing.Timer overlayTimer =
            new javax.swing.Timer(1200, e -> overlayLabel.setVisible(false));

    // 중앙 플레이 영역(풍선 캔버스)
    private final PlayField playField;

    // 렌더러
    private final com.balloon.ui.render.BalloonSpriteRenderer renderer =
            new com.balloon.ui.render.BalloonSpriteRenderer();

    // 배경 / 집 / 하트 이미지
    private BufferedImage bgImg;
    private BufferedImage houseImg;
    private BufferedImage heartImg;

    // 기타 상태
    private volatile boolean navigatedAway = false;
    private boolean stageClearedThisRound = false;
    private boolean resultShown = false;
    public static int lastCompletedStage = 1;

    // 전역 컨텍스트
    private final GameContext ctx = GameContext.getInstance();

    // HUD 활성 아이템 배지용 타이머(그냥 repaint만 돌리는 용도)
    private final javax.swing.Timer hudTimer =
            new javax.swing.Timer(200, e -> repaint());

    private boolean caretOn = true;

    // 기본 폰트 유틸
    private static Font fontM() {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(14f);
    }

    public GamePanel(ScreenRouter router) {
        this.router = router;

        // ====== 단어 로딩 (CSV + NonRepeating) ======
        List<String> wordList = CsvWordLoader.loadWords("/data/words.csv");
        this.wordProvider = new NonRepeatingWordProvider(wordList);

        // ========= 레이아웃/배경 =========
        setLayout(new BorderLayout());
        setOpaque(false);

        // ========= 상단 바 (좌: HUD, 우: 아이템 전설) =========
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);

        JPanel hud = new JPanel(new BorderLayout());
        hud.setOpaque(false);

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

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 8));
        legend.setOpaque(false);

        JLabel timeBadge = new JLabel("TIME ±5");
        timeBadge.setForeground(new Color(255, 120, 120));
        timeBadge.setFont(timeBadge.getFont().deriveFont(Font.BOLD, 12f));

        JLabel balloonBadge = new JLabel("BALLOON ±1");
        balloonBadge.setForeground(new Color(120, 160, 255));
        balloonBadge.setFont(balloonBadge.getFont().deriveFont(Font.BOLD, 12f));

        JLabel legendTitle = new JLabel("Items:");
        legendTitle.setForeground(new Color(235, 235, 235));
        legendTitle.setFont(legendTitle.getFont().deriveFont(Font.PLAIN, 12f));

        legend.add(legendTitle);
        legend.add(timeBadge);
        legend.add(balloonBadge);

        topBar.add(hud, BorderLayout.WEST);
        topBar.add(legend, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ========= 중앙 플레이 영역 =========
        playField = new PlayField();
        playField.setLayout(new BorderLayout());
        add(playField, BorderLayout.CENTER);

        // 중앙 단어 라벨(지금은 숨김)
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        wordLabel.setForeground(Color.WHITE);
        playField.add(wordLabel, BorderLayout.CENTER);
        wordLabel.setVisible(false);

        // ========= 토스트 라벨 =========
        toastLabel.setForeground(new Color(80, 120, 80));
        toastLabel.setFont(toastLabel.getFont().deriveFont(Font.PLAIN, 16f));
        toastLabel.setHorizontalAlignment(SwingConstants.CENTER);
        playField.add(toastLabel, BorderLayout.SOUTH);

        // ========= 하단 입력바 =========
        JPanel inputBar = new JPanel();
        inputBar.setOpaque(false);
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        inputBar.setLayout(new BoxLayout(inputBar, BoxLayout.X_AXIS));

        inputBar.add(Box.createHorizontalGlue());

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setOpaque(false);
        int rowW = 480;
        int rowH = 40;
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

        // ========= 오버레이 라벨 =========
        overlayLabel.setFont(overlayLabel.getFont().deriveFont(Font.BOLD, 42f));
        overlayLabel.setForeground(new Color(255, 255, 160));
        overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overlayLabel.setVerticalAlignment(SwingConstants.CENTER);
        overlayLabel.setVisible(false);
        playField.add(overlayLabel, BorderLayout.NORTH);
        overlayTimer.setRepeats(false);

        // ========= 입력/포커스 설정 =========
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        setupKeyBindings();
        SwingUtilities.invokeLater(this::grabFocusSafely);
        playField.setFocusable(false);
        wordLabel.setFocusable(false);
        toastLabel.setFocusable(false);

        // ========= 틱 타이머 (1초) =========
        tickTimer = new javax.swing.Timer(1000, e -> {
            if (resultShown) return;

            // 풍선이 다 사라졌으면 바로 클리어
            if (!stageClearedThisRound && allCleared()) {
                onStageCleared();
                return;
            }

            if (state.getTimeLeft() > 0) {
                state.decreaseTime();
                refreshHUD();

                if (state.getTimeLeft() == 0 && !allCleared()) {
                    onStageFailed();
                }
            }
        });

        // ========= 이미지 로드 / 배경 =========
        heartImg = ImageAssets.load("heart.png");
        houseImg = ImageAssets.load("home.png");
        applyStageBackground(state.getLevel());

        // ========= 초기 풍선 생성 / HUD 세팅 =========
        playField.spawnInitialBalloons();
        updateContextHud();
        refreshHUD();

        // 타이머 시작
        hudTimer.start();
        tickTimer.start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1280, 720);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImg != null) {
            g.drawImage(bgImg, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // --------------------------------------------------
    //  공통 유틸
    // --------------------------------------------------
    private void applyStageBackground(int stage) {
        String bgName = switch (stage) {
            case 1 -> "bg_level1.png";
            case 2 -> "bg_level2.png";
            default -> "bg_level3.png";
        };
        bgImg = ImageAssets.load(bgName);
        repaint();
    }

    private String resolvePlayerName() {
        String name = Session.getNickname();

        if (name == null || name.isBlank()) {
            try {
                String fromCtx = (ctx != null) ? ctx.getPlayerName() : null;
                if (fromCtx != null && !fromCtx.isBlank()) name = fromCtx;
            } catch (Exception ignore) {}
        }
        if (name == null || name.isBlank()) name = "-";
        return name;
    }

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

    private void refreshHUD() {
        // 원래: scoreLabel.setText("Score: " + state.getTotalScore());

        // ✅ 총점은 우리가 누적한 totalScore 사용
        scoreLabel.setText("Score: " + totalScore);
        timeLabel.setText("Time: " + Math.max(0, state.getTimeLeft()));
        repaint();
    }

    private void stopGameLoops() {
        if (tickTimer != null && tickTimer.isRunning()) tickTimer.stop();
        if (playField != null) playField.stop();
    }

    private void reloadStageBalloons() {
        if (playField != null) {
            balloons.clear();
            playField.clearSprites();
            playField.spawnInitialBalloons();
        }
    }

    // --------------------------------------------------
    //  단어/중복 관련 유틸
    // --------------------------------------------------
    private static String norm(String s) {
        if (s == null) return "";
        s = s.trim();
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
    }

    private boolean hasActiveBalloonWithWord(String word) {
        String needle = norm(word);
        for (Balloon b : balloons) {
            if (b.isActive() && norm(b.getWord()).equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    private String nextUniqueWord() {
        String w = "empty";
        int tries = 0;

        do {
            w = wordProvider.nextWord();
            if (w == null || w.isBlank()) return "empty";
            tries++;
            if (tries > 20) {
                break;
            }
        } while (hasActiveBalloonWithWord(w));

        return w;
    }

    private void grabFocusSafely() {
        inputField.requestFocusInWindow();
        if (!tickTimer.isRunning()) tickTimer.start();
    }

    // --------------------------------------------------
    //  Enter 처리
    // --------------------------------------------------
    private void onEnter() {
        String typed = inputField.getText().trim();

        if (typed.isEmpty()) {
            wrongCount++;
            rules.onMiss();
            showToast("✗ Miss", new Color(190, 60, 60));
            refreshHUD();
            playField.repaint();
            if (state.getLife() <= 0) {
                onStageFailed();
            }
            inputField.setText("");
            return;
        }

        boolean ok = judge.submit(balloons, typed, rules);

        if (ok) {
            correctCount++;
            // ✅ 단어 맞출 때는 점수 안 줌 (정답/오답 카운트만)
            removeFirstByWord(typed);
            showToast("✓ Pop!", new Color(25, 155, 75));

            if (allCleared()) {
                onStageCleared();
            }
        } else {
            wrongCount++;
            showToast("✗ Miss", new Color(190, 60, 60));
            if (state.getLife() <= 0) {
                onStageFailed();
                inputField.setText("");
                refreshHUD();
                playField.repaint();
                return;
            }
        }

        inputField.setText("");
        refreshHUD();
        playField.repaint();
    }

    private void showToast(String msg, Color color) {
        toastLabel.setForeground(color);
        toastLabel.setText(msg);
        javax.swing.Timer t = new javax.swing.Timer(600, e -> toastLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void flash(boolean positive) {
        Color c = positive ? new Color(0xCCFFCC) : new Color(0xFFCCCC);
        Color old = getBackground();
        setBackground(c);
        javax.swing.Timer t = new javax.swing.Timer(150, e -> setBackground(old));
        t.setRepeats(false);
        t.start();
    }

    private void removeFirstByWord(String word) {
        String needle = norm(word);

        playField.removeSpriteByWord(needle);

        Iterator<Balloon> it = balloons.iterator();
        while (it.hasNext()) {
            Balloon b = it.next();
            if (b.isActive() && norm(b.getWord()).equalsIgnoreCase(needle)) {
                b.pop();
                it.remove();
                break;
            }
        }
    }

    private boolean allCleared() {
        for (Balloon b : balloons) if (b.isActive()) return false;
        return true;
    }

    // --------------------------------------------------
    //  스테이지 / 결과
    // --------------------------------------------------
    private void onStageCleared() {
        if (stageClearedThisRound) return;
        stageClearedThisRound = true;

        stopGameLoops();

        int remain = Math.max(0, state.getTimeLeft());
        int bonus = remain * 10;

        // ✅ 우리가 정한 규칙: 남은 시간 × 10점을 누적
        totalScore += bonus;

        // 내부 GameState 점수 쓰고 싶으면 유지, 아니면 삭제해도 됨
        state.addRemainingTimeAsScore();

        refreshHUD();

        showOverlay("✔ SUCCESS!  +" + bonus + "점", new Color(110, 220, 110));
        showToast("남은 시간 " + remain + "초 → +" + bonus + "점!", new Color(255, 255, 150));

        lastCompletedStage = state.getLevel();

        new javax.swing.Timer(1000, e -> {
            state.nextLevel();
            if (state.isGameOver() || state.getLevel() > 3) {
                showResult();
                return;
            }

            stageClearedThisRound = false;
            applyStageBackground(state.getLevel());
            reloadStageBalloons();
            refreshHUD();
            showToast("Stage " + state.getLevel() + " Start!", new Color(100, 200, 100));

            resultShown = false;
            tickTimer.restart();
            playField.start();
        }) {{
            setRepeats(false);
            start();
        }};
    }

    private void onStageFailed() {
        stopGameLoops();

        showOverlay("✖ FAILED!  (Stage " + state.getLevel() + ")", new Color(230, 90, 90));

        javax.swing.Timer t = new javax.swing.Timer(600, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            showResult();
        });
        t.setRepeats(false);
        t.start();
    }

    private void showOverlay(String text, Color color) {
        overlayLabel.setText(text);
        overlayLabel.setForeground(color);
        overlayLabel.setVisible(true);
        overlayTimer.restart();
    }

    private void showResult() {

        if (resultShown) return;
        resultShown = true;

        stopGameLoops();

        int remainTime = Math.max(0, state.getTimeLeft());

        // ✅ 최종 점수 = 우리가 누적해둔 totalScore 그대로 사용
        int finalScore = totalScore;

        int totalTry = correctCount + wrongCount;
        double acc = (totalTry > 0) ? (correctCount * 1.0 / totalTry) : 0.0;

        // ResultContext에 결과 전달
        ResultData data = new ResultData(finalScore, remainTime, acc, correctCount, wrongCount);
        ResultContext.set(data);

        double accuracyPercent = acc * 100.0;
        // 랭킹 CSV 저장도 totalScore 사용
        // 랭킹은 SINGLE 모드에서만 저장
        if (ctx == null
                || ctx.getMode() == null
                || ctx.getMode() == GameContext.GameMode.SINGLE) {
            saveRanking(totalScore, accuracyPercent, remainTime);
        }



        if (router != null) {
            try {
                Component c = router.get(ScreenId.RESULT);
                if (c instanceof ResultScreen rs) {
                    rs.setResult(finalScore, acc, remainTime);
                    // 나중에 브레이크다운 보여주고 싶으면:
                    // rs.setBreakdown(wordScore, timeBonus, 0, itemBonus);
                }
                router.show(ScreenId.RESULT);
            } catch (Exception ex) {
                System.err.println("[GamePanel] RESULT navigation error: " + ex);
                router.show(ScreenId.RESULT);
            }
        }
    }

    @Override
    public void onShown() {
        navigatedAway = false;
        grabFocusSafely();
        updateContextHud();
        if (!tickTimer.isRunning()) tickTimer.start();
        playField.start();
    }

    public void onHidden() {
        navigatedAway = true;
        stopGameLoops();
        if (overlayTimer.isRunning()) overlayTimer.stop();
    }

    // --------------------------------------------------
    //  키 바인딩
    // --------------------------------------------------
    private void setupKeyBindings() {
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submitField");
        inputField.getActionMap().put("submitField", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                onEnter();
            }
        });

        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearField");
        inputField.getActionMap().put("clearField", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                inputField.setText("");
            }
        });
    }

    // --------------------------------------------------
    //  ItemEffectApplier FieldApi에서 호출하는 풍선 추가/삭제
    // --------------------------------------------------
    private void addBalloons(int n) {
        playField.addBalloons(n);
    }

    private void removeBalloons(int n) {
        playField.removeBalloons(n);
    }

    // --------------------------------------------------
    //  Skin → Balloon.Kind
    // --------------------------------------------------
    private static Balloon.Kind toKind(Skin skin) {
        return switch (skin) {
            case PURPLE, PINK -> Balloon.Kind.RED;
            case YELLOW, ORANGE -> Balloon.Kind.GREEN;
            case GREEN -> Balloon.Kind.BLUE;
        };
    }

    // --------------------------------------------------
    //  내부 클래스 : PlayField
    // --------------------------------------------------
    private final class PlayField extends JPanel {
        private static final int DESIGN_W = 1280;
        private static final int DESIGN_H = 720;

        private final com.balloon.ui.render.BalloonSpriteRenderer renderer =
                new com.balloon.ui.render.BalloonSpriteRenderer();
        private final ArrayList<BalloonSprite> sprites = new ArrayList<>();
        private final Random rnd = new Random();
        private final javax.swing.Timer frameTimer;

        private Rectangle houseRect = new Rectangle(0, 0, 0, 0);
        private Point houseAnchor = new Point(0, 0);

        PlayField() {
            setOpaque(false);

            SwingUtilities.invokeLater(() -> {
                layoutHouse();
                spawnInitialBalloons();
            });

            frameTimer = new javax.swing.Timer(16, e -> repaint());
            frameTimer.start();
        }

        private void layoutHouse() {
            int W = DESIGN_W;
            int H = DESIGN_H;

            if (houseImg == null) houseImg = ImageAssets.load("home.png");

            int hw = 80;
            int hh = 70;

            int hx = W / 2 - hw / 2;
            int hy = H - hh - 140;

            houseRect.setBounds(hx, hy, hw, hh);
            houseAnchor.setLocation(hx + hw / 2, hy + (int) (hh * 0.30));
        }

        private void spawnInitialBalloons() {
            sprites.clear();
            balloons.clear();

            int W = DESIGN_W;
            int centerX = W / 2;

            int s = 70;
            int gapX = 90;
            int gapY = 60;

            int[] pattern = {3, 4, 5, 6, 5, 4, 3};
            int rows = pattern.length;

            int margin = 12;
            int bottomY = (houseRect != null && houseRect.height > 0)
                    ? houseRect.y - s - margin
                    : 300;
            int topY = bottomY - (rows - 1) * gapY;

            Skin[] skins = new Skin[]{Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN};
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

                    String word = nextUniqueWord();

                    Balloon m = new Balloon(word, x, y, toKind(skin));
                    balloons.add(m);

                    BalloonSprite b = new BalloonSprite(
                            word,
                            img,
                            x, y,
                            houseAnchor.x,
                            houseAnchor.y
                    );
                    b.w = s;
                    b.h = s;

                    sprites.add(b);
                    idx++;
                }
            }
        }

        private void clearSprites() {
            sprites.clear();
        }

        private void drawLine(Graphics2D g2, BalloonSprite b) {
            if (b == null || b.state == BalloonSprite.State.DEAD) return;

            int ax = houseAnchor.x;
            int ay = houseAnchor.y;
            int bx = b.attachX();
            int by = b.attachY();

            int cx = (ax + bx) / 2;
            int cy = Math.min(ay, by) - 40;

            Stroke old = g2.getStroke();
            Color oldC = g2.getColor();

            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 220));
            g2.draw(new java.awt.geom.QuadCurve2D.Float(ax, ay, cx, cy, bx, by));

            g2.setStroke(old);
            g2.setColor(oldC);
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Dialog", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            int x = 16, y = 28;

            g2.drawString("life:", x, y);

            int lifeCount = Math.max(0, Math.min(3, state.getLife()));
            int hx = x + 50;
            for (int i = 0; i < lifeCount; i++) {
                if (heartImg != null) {
                    g2.drawImage(heartImg, hx + i * 28, y - 18, 24, 24, null);
                }
            }

            String timeStr = String.format("Time limit : %d m %02d s",
                    Math.max(0, state.getTimeLeft()) / 60,
                    Math.max(0, state.getTimeLeft()) % 60);
            g2.drawString(timeStr, x, y + 24);
        }

        private void addBalloons(int n) {
            int W = DESIGN_W;
            int s = 70;
            int y = Math.max(80, houseAnchor.y - 6 * s);

            Skin[] skins = new Skin[]{Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN};

            for (int i = 0; i < n; i++) {
                String word = nextUniqueWord();

                Skin skin = skins[rnd.nextInt(skins.length)];
                BufferedImage img = BalloonSkins.of(skin);
                int x = 40 + rnd.nextInt(Math.max(1, W - 80));

                Balloon m = new Balloon(word, x, y, toKind(skin));
                balloons.add(m);

                BalloonSprite b = new BalloonSprite(
                        word,
                        img,
                        x, y,
                        houseAnchor.x,
                        houseAnchor.y
                );
                b.w = s;
                b.h = s;
                sprites.add(b);
            }
            revalidate();
            repaint();
        }

        private void removeBalloons(int n) {
            int removed = 0;
            ListIterator<BalloonSprite> sit = sprites.listIterator(sprites.size());
            while (sit.hasPrevious() && removed < n) {
                BalloonSprite s = sit.previous();
                sit.remove();
                Iterator<Balloon> mit = balloons.iterator();
                while (mit.hasNext()) {
                    Balloon m = mit.next();
                    if (m.isActive() && norm(m.getWord()).equalsIgnoreCase(norm(s.text))) {
                        m.pop();
                        mit.remove();
                        break;
                    }
                }
                removed++;
            }
            repaint();
        }

        private void removeSpriteByWord(String normWord) {
            Iterator<BalloonSprite> it = sprites.iterator();
            while (it.hasNext()) {
                BalloonSprite s = it.next();
                if (norm(s.text).equalsIgnoreCase(normWord)) {
                    it.remove();
                    break;
                }
            }
        }

        void stop() {
            if (frameTimer != null && frameTimer.isRunning()) frameTimer.stop();
        }

        void start() {
            if (frameTimer != null && !frameTimer.isRunning()) frameTimer.start();
        }

        @Override
        public void invalidate() {
            super.invalidate();
            SwingUtilities.invokeLater(() -> {
                if (getWidth() > 0) {
                    layoutHouse();
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();

            for (var b : sprites) {
                b.anchorX = houseAnchor.x;
                b.anchorY = houseAnchor.y;
                drawLine(g2, b);
            }

            if (houseImg != null) {
                g2.drawImage(houseRect.width > 0 ? houseImg : houseImg,
                        houseRect.x, houseRect.y, houseRect.width, houseRect.height, null);
            }

            for (var b : sprites) {
                renderer.renderBalloonOnly(g2, b);
            }

            drawHUD(g2);

            g2.dispose();
        }
    }

    // --------------------------------------------------
    //  SingleGameRules : GameRules 구현
    // --------------------------------------------------
    private final class SingleGameRules implements GameRules {
        @Override public void onTick() {}
        @Override public void onPop(List<Balloon> bs) {}
        @Override public void onMiss() {
            state.loseLife();
        }
        @Override public boolean isGameOver() {
            return state.isGameOver();
        }
    }

    // --------------------------------------------------
    //  랭킹 CSV 저장
    // --------------------------------------------------
    private void saveRanking(int finalScore, double accuracyPercent, int timeLeftSeconds) {
        String playerName = resolvePlayerName();

        // 기본값
        GameMode mode = GameMode.SINGLE;

        // ✅ 여기 한 줄만 중요: 내부 enum → 새 GameMode 로 매핑
        try {
            if (ctx != null && ctx.getMode() != null) {
                mode = GameMode.valueOf(ctx.getMode().name());
            }
        } catch (Exception ignore) {
        }

        ScoreEntry entry = new ScoreEntry(playerName, finalScore, mode);

        try {
            RankingRepository repo = new CsvRankingRepository();
            repo.save(entry);
        } catch (Exception e) {
            System.err.println("[GamePanel] saveRanking failed: " + e);
        }
    }
}
