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
import com.balloon.ui.skin.SecretItemSkin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import com.balloon.core.GameContext;
import com.balloon.ranking.RankingCsvRepository;
import com.balloon.ranking.RankingRecord;

// 🔽 단어 관련
import com.balloon.game.CsvWordLoader;
import com.balloon.game.WordProvider;
import com.balloon.game.NonRepeatingWordProvider;

/**
 * UI는 1번 코드 스타일 유지 + 게임 로직은 GameState/Rules/Judge 구조 그대로
 */
public class GamePanel extends JPanel implements Showable {

    private JLayeredPane layeredPane;

    // ====== Game / State / Item ======
    private final LevelConfig levelConfig = new LevelConfig();
    private final GameState state = new GameState(levelConfig);
    private final ItemSpawner spawner = new ItemSpawner();

    // 안내 오버레이용 상태
    private boolean levelIntroShowing = false;
    private javax.swing.Timer levelIntroTimer;

    // gray.png 배경 이미지
    private BufferedImage grayOverlayImg;

    // 모델 풍선 리스트 (GameJudge에 넘기는 리스트)
    private final List<Balloon> balloons = new ArrayList<>();

    // 단어 공급기 (CSV + 중복 방지)
    private final WordProvider wordProvider;

    // 스코어 브레이크다운 (UI용 임시)
    private int correctCount = 0;
    private int wrongCount = 0;
    private int wordScore = 0;  // 정답 1개당 10점
    private int timeBonus = 0;  // 남은 시간 기반 보너스
    private int itemBonus = 0;  // 아이템으로 인한 변화

    private int totalScore = 0; //총점 (단어 + 시간 + 아이템)

    // ★ GamePanel이 처음 보여졌는지 여부
    private boolean firstShown = true;

    private final JLabel itemToastLabel = new JLabel("", SwingConstants.CENTER);

    // ===== [RESULT OVERLAY] 게임 종료 후 SUCCESS/FAIL + SCORE 표시용 =====
    private JPanel resultOverlayPanel;   // 반투명 배경 패널
    private JLabel resultTitleLabel;     // "SUCCESS" / "FAIL"
    private JLabel resultScoreLabel;     // "SCORE : 12345"
    private javax.swing.Timer resultTimer;  // 3초 뒤에 Ranking 화면으로 전환
    private boolean showingResult = false;  // 오버레이 표시 여부

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
                    GamePanel.this.showItemToast(message);
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

    private void showItemToast(String msg) {
        itemToastLabel.setText(msg);
        itemToastLabel.setVisible(true);
        itemToastLabel.repaint();   // 박스 포함해서 다시 그리기

        javax.swing.Timer t = new javax.swing.Timer(800, e -> itemToastLabel.setVisible(false));
        t.setRepeats(false);
        t.start();
    }

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

    // SUCCESS/FAIL 결과가 떠 있는 동안에는 overlayTimer가 wordLabel을 건드리지 않기 위한 플래그
    private boolean resultShown = false;

    private final javax.swing.Timer overlayTimer =
            new javax.swing.Timer(1200, e -> {
                // ⚠ 인트로(레벨 안내) 중이면 SUCCESS/FAIL 타이머가 건드리지 않도록
                if (levelIntroShowing || resultShown) {
                    return;
                }
                // SUCCESS/FAIL 표시 끝나면 wordLabel 초기화
                wordLabel.setVisible(false);
                wordLabel.setText("");
                wordLabel.setIcon(null);
                wordLabel.setOpaque(false);
                wordLabel.setBackground(null);
            });

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
    public static int lastCompletedStage = 1;

    // 전역 컨텍스트
    private final GameContext ctx = GameContext.getInstance();

    // HUD 활성 아이템 배지용 타이머(그냥 repaint만 돌리는 용도)
    private final javax.swing.Timer hudTimer =
            new javax.swing.Timer(200, e -> repaint());

    private boolean caretOn = true;

    public GamePanel(ScreenRouter router) {
        this.router = router;

        // ★★★ 전체 패널(게임 화면)의 레이아웃/배경 설정 ★★★
        setLayout(new BorderLayout());   // 위(HUD) / 가운데(PlayField) / 아래(입력창) 배치
        setOpaque(false);

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

        topBar.add(legend, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // ========= 중앙 플레이 영역 =========
        playField = new PlayField();
        playField.setLayout(new BorderLayout());

        // ========= layeredPane 생성 =========
        layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        playField.setBounds(0, 0, 1280, 720);
        layeredPane.add(playField, JLayeredPane.DEFAULT_LAYER); // playField는 기본 레이어에 추가

        // ========= itemToastLabel 추가 =========
        int boxW = 420;   // 박스 가로 길이
        int boxH = 70;    // 박스 세로 길이
        int boxX = (1280 - boxW) / 2;  // 화면 가운데에 오도록 x 좌표 계산
        int boxY = 260;                // 세로 위치

        itemToastLabel.setBounds(boxX, boxY, boxW, boxH);
        layeredPane.add(itemToastLabel, Integer.valueOf(JLayeredPane.PALETTE_LAYER));

        // ★ 중앙 wordLabel도 layeredPane의 위 레이어에 추가
        wordLabel.setBounds(0, 160, 1280, 200);   // 화면 중앙쯤
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        layeredPane.add(wordLabel, Integer.valueOf(JLayeredPane.MODAL_LAYER));

        add(layeredPane, BorderLayout.CENTER);

        // 중앙 단어 라벨(지금은 숨김)
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        wordLabel.setForeground(Color.WHITE);
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

        int rowW = 600;   // 전체 바 가로 길이
        int rowH = 40;    // 전체 바 높이

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setOpaque(false);

        Dimension rowSize = new Dimension(rowW, rowH);
        inputRow.setPreferredSize(rowSize);
        inputRow.setMaximumSize(rowSize);
        inputRow.setMinimumSize(rowSize);

        inputField.setFont(inputField.getFont().deriveFont(Font.PLAIN, 16f));
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(Color.BLACK);
        inputField.setCaretColor(Color.BLACK);
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        inputRow.add(inputField, BorderLayout.CENTER);

        inputBar.add(inputRow);
        inputBar.add(Box.createHorizontalGlue());

        add(inputBar, BorderLayout.SOUTH);

        // ========= 오버레이 라벨 (SUCCESS / FAIL + SCORE) =========
        overlayLabel.setFont(overlayLabel.getFont().deriveFont(Font.BOLD, 42f));
        overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overlayLabel.setVerticalAlignment(SwingConstants.CENTER);
        overlayLabel.setVisible(false);

        overlayTimer.setRepeats(false);

        // ========= 아이템 토스트 라벨(아이템 효과 표시용) =========
        itemToastLabel.setFont(new Font("Dialog", Font.BOLD, 32));
        itemToastLabel.setForeground(new Color(255, 240, 180));

        itemToastLabel.setOpaque(true);
        itemToastLabel.setBackground(new Color(0, 0, 0, 180));

        itemToastLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        itemToastLabel.setVisible(false);
        itemToastLabel.setHorizontalAlignment(SwingConstants.CENTER);

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

            // 🔥 인트로(레벨 안내) 떠 있는 동안에는 시간 줄이지 않기
            if (levelIntroShowing) return;

            // ✅ 풍선이 이미 다 사라져 있으면, 시간 남았어도 바로 클리어 처리
            if (!stageClearedThisRound && allCleared()) {
                onStageCleared();
                return;
            }

            if (state.getTimeLeft() > 0) {
                state.decreaseTime();
                refreshHUD();

                // 시간 다 됐는데 풍선 남아 있으면 실패
                if (state.getTimeLeft() == 0 && !allCleared()) {
                    onStageFailed();
                }
            }
        });

        // ★ 레벨 시작 안내 타이머 (2초)
        levelIntroTimer = new javax.swing.Timer(2000, ev -> {
            // 안내 끝나면 오버레이 숨기고 게임 시작
            levelIntroShowing = false;
            hideLevelIntro();

            // 이제부터 실제 게임 진행
            if (!tickTimer.isRunning()) tickTimer.start();
            playField.start();
            grabFocusSafely();
        });
        levelIntroTimer.setRepeats(false);

        // ========= 이미지 로드 / 배경 =========
        heartImg = ImageAssets.load("heart.png");
        houseImg = ImageAssets.load("home.png");
        bgImg = null;
        grayOverlayImg = ImageAssets.load("gray.png");
        applyStageBackground(state.getLevel());

        // ========= 초기 풍선 생성 / HUD 세팅 =========
        playField.spawnInitialBalloons();
        updateContextHud();

        // ★★★ 레벨별 시작 시간을 강제로 1:90 / 2:80 / 3:70 으로 맞추기 ★★★
        resetTimeForCurrentLevel();
        refreshHUD();

        // 타이머 시작 (HUD 업데이트용)
        hudTimer.start();

        // 레벨 인트로 표시 (인트로 안에서도 한 번 더 보정)
        showLevelIntroForCurrentStage();
    }

    // --------------------------------------------------
    //  레벨 시작 시간 보정:
    //  현재 GameState의 timeLeft를
    //  1레벨: 90초, 2레벨: 80초, 3레벨: 70초 로 강제 설정
    // --------------------------------------------------
    private void resetTimeForCurrentLevel() {
        int level = state.getLevel();
        int targetSec;

        switch (level) {
            case 1 -> targetSec = 90;
            case 2 -> targetSec = 80;
            case 3 -> targetSec = 70;
            default -> targetSec = 70;   // 혹시 모를 예외
        }

        int delta = targetSec - state.getTimeLeft();
        state.addSeconds(delta);  // 현재 timeLeft를 targetSec으로 덮어쓰기
    }

    // --------------------------------------------------
    //  paintComponent : 배경 PNG
    // --------------------------------------------------
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
            } catch (Exception ignore) {
            }
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
        } catch (Exception ignore) {
        }
        modeLabel.setText("Mode: " + mode);
    }

    private void refreshHUD() {
        timeLabel.setText("Time: " + Math.max(0, state.getTimeLeft()));
        scoreLabel.setText("Score: " + totalScore);
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

    // ★ CSV에서 읽어온 단어를 화면용으로 정리
    private String cleanWord(String w) {
        if (w == null) return "";
        w = w.trim();
        w = java.text.Normalizer.normalize(w, java.text.Normalizer.Form.NFC);
        w = w.replaceAll("[^\\p{L}\\p{Nd}]", "");
        return w;
    }

    // --------------------------------------------------
    //  단어/중복 관련 유틸
    // --------------------------------------------------
    private static String norm(String s) {
        if (s == null) return "";
        s = s.trim();
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
        s = s.replaceAll("[^\\p{L}\\p{Nd}]", "");
        return s;
    }

    // 현재 활성 풍선들 중에 같은 단어가 있는지 확인
    private boolean hasActiveBalloonWithWord(String word) {
        String needle = norm(word);
        for (Balloon b : balloons) {
            if (b.isActive() && norm(b.getWord()).equalsIgnoreCase(needle)) {
                return true;
            }
        }
        return false;
    }

    // wordProvider에서 "현재 필드에 없는" 단어 골라오기
    private String nextUniqueWord() {
        String w = "empty";
        int tries = 0;

        do {
            w = wordProvider.nextWord();
            if (w == null || w.isBlank()) return "empty";
            tries++;
            if (tries > 20) { // 단어 부족할 때 무한루프 방지
                break;
            }
        } while (hasActiveBalloonWithWord(w));

        return w;
    }

    private void grabFocusSafely() {
        inputField.requestFocusInWindow();
    }

    // --------------------------------------------------
    //  Enter 처리 : GameJudge + GameState
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
            wordScore += 10;
            totalScore += 10;

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

        timeBonus += bonus;
        totalScore += bonus;
        refreshHUD();

        showOverlay("✔ SUCCESS!  +" + bonus + "점", Color.WHITE);
        showToast("남은 시간 " + remain + "초 → +" + bonus + "점!", new Color(255, 255, 150));

        lastCompletedStage = state.getLevel();

        new javax.swing.Timer(1000, e -> {
            // 다음 레벨로 이동
            state.nextLevel();

            // ★ 다음 레벨 시작할 때도 항상 90/80/70초로 초기화
            resetTimeForCurrentLevel();

            // 레벨 3까지 클리어했다면 게임 종료
            if (state.isGameOver() || state.getLevel() > 3) {
                showFinalResult(true);
                return;
            }

            stageClearedThisRound = false;
            applyStageBackground(state.getLevel());
            reloadStageBalloons();
            refreshHUD();

            // Stage 2, 3 시작 안내 오버레이
            showLevelIntroForCurrentStage();

            showToast("Stage " + state.getLevel() + " Start!", new Color(100, 200, 100));

            resultShown = false;

            // 인트로가 끝난 뒤(levelIntroTimer)에서 다시 시작
            if (tickTimer.isRunning()) {
                tickTimer.stop();
            }
            playField.stop();
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
            showFinalResult(false);
        });
        t.setRepeats(false);
        t.start();
    }

    private void showOverlay(String text, Color color) {
        String html =
                "<html><div style='text-align:center;'>" +
                        "<span style='font-size:32px; font-weight:bold;'>" + text + "</span>" +
                        "</div></html>";

        wordLabel.setText(html);
        wordLabel.setForeground(color);
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wordLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (grayOverlayImg != null) {
            int panelW = getWidth();
            int targetW = (panelW > 0) ? (int) (panelW * 0.45) : 550;

            int origW = grayOverlayImg.getWidth();
            int origH = grayOverlayImg.getHeight();
            int targetH = (int) ((double) origH * targetW / origW);

            Image scaled = grayOverlayImg.getScaledInstance(
                    targetW,
                    targetH,
                    Image.SCALE_SMOOTH
            );

            wordLabel.setIcon(new ImageIcon(scaled));
            wordLabel.setHorizontalTextPosition(SwingConstants.CENTER);
            wordLabel.setVerticalTextPosition(SwingConstants.CENTER);
            wordLabel.setOpaque(false);
            wordLabel.setBackground(null);
        } else {
            wordLabel.setIcon(null);
            wordLabel.setOpaque(true);
            wordLabel.setBackground(new Color(0, 0, 0, 160));
        }

        wordLabel.setVisible(true);
        overlayTimer.restart();
    }

    /**
     * 게임이 끝났을 때(SUCCESS or FAIL) 중앙에 결과를 보여주고
     * 3초 뒤 자동으로 RANKING 화면으로 이동한다.
     *
     * @param success true = SUCCESS, false = FAIL
     */
    private void showFinalResult(boolean success) {
        if (resultShown) return;
        resultShown = true;

        // 게임 루프 정지
        stopGameLoops();

        // 점수/정확도 계산
        int remainTime = Math.max(0, state.getTimeLeft());
        int finalTotalScore = totalScore;
        int totalTry = correctCount + wrongCount;
        double acc = (totalTry > 0) ? (correctCount * 1.0 / totalTry) : 0.0;
        double accuracyPercent = acc * 100.0;

        // CSV에 저장
        saveRanking(finalTotalScore, accuracyPercent, remainTime);

        // 풍선/스프라이트 비우기(엔딩 화면만 깔끔하게 보이도록)
        if (playField != null) {
            playField.clearSprites();
        }

        // SUCCESS / FAIL + SCORE 중앙에 크게 표시
        String mainText = success ? "SUCCESS!" : "FAIL";
        Color mainColor = success ? new Color(0, 0, 0) : new Color(220, 40, 40);

        String html =
                "<html><div style='text-align:center;'>" +
                        "<span style='font-size:56px; font-weight:bold;'>" + mainText + "</span><br/><br/>" +
                        "<span style='font-size:32px;'>SCORE : " + finalTotalScore + "</span>" +
                        "</div></html>";

        wordLabel.setText(html);
        wordLabel.setForeground(mainColor);
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wordLabel.setVerticalAlignment(SwingConstants.CENTER);
        wordLabel.setIcon(null);
        wordLabel.setOpaque(false);
        wordLabel.setBackground(null);
        wordLabel.setVisible(true);

        toastLabel.setText(" ");

        // 3초 뒤 RANKING 화면으로 이동
        new javax.swing.Timer(3000, e -> {
            ((javax.swing.Timer) e.getSource()).stop();

            if (router != null) {
                try {
                    router.show(ScreenId.RANKING);
                } catch (Exception ex) {
                    System.err.println("[GamePanel] ranking navigation error: " + ex);
                }
            }
        }) {{
            setRepeats(false);
            start();
        }};
    }

    @Override
    public void onShown() {
        navigatedAway = false;
        updateContextHud();

        // 이미 인트로 중이면 그냥 포커스만
        if (levelIntroShowing) {
            grabFocusSafely();
            return;
        }

        // 처음 들어올 때 레벨1이면 인트로 표시
        if (firstShown && state.getLevel() == 1 && !resultShown) {
            firstShown = false;
            showLevelIntroForCurrentStage();
            grabFocusSafely();
            return;
        }

        // 그 외에는 그냥 게임 재개
        if (!tickTimer.isRunning()) {
            tickTimer.start();
        }

        grabFocusSafely();
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

                    // 현재 필드에서 안 쓰는 단어만 사용
                    String word = nextUniqueWord();
                    word = cleanWord(word);

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
            assignRandomItemCategoriesForSingleMode();
        }

        /**
         * SINGLE 모드에서만:
         * - TIME 카테고리 2개
         * - BALLOON 카테고리 2개
         * 를 랜덤 단어에 붙이고, Balloon / BalloonSprite 양쪽 모두에 반영.
         */
        private void assignRandomItemCategoriesForSingleMode() {

            if (balloons.size() < 4 || sprites.size() < 4) {
                return;
            }

            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < balloons.size(); i++) {
                indices.add(i);
            }
            Collections.shuffle(indices);

            for (Balloon b : balloons) {
                b.setCategory(SecretItemSkin.ItemCategory.NONE);
            }

            int timeCount = 2;
            int balloonCount = 2;
            int idxPos = 0;

            for (int i = 0; i < timeCount && idxPos < indices.size(); i++, idxPos++) {
                int bi = indices.get(idxPos);
                balloons.get(bi).setCategory(SecretItemSkin.ItemCategory.TIME);
            }

            for (int i = 0; i < balloonCount && idxPos < indices.size(); i++, idxPos++) {
                int bi = indices.get(idxPos);
                balloons.get(bi).setCategory(SecretItemSkin.ItemCategory.BALLOON);
            }

            int limit = Math.min(balloons.size(), sprites.size());
            for (int i = 0; i < limit; i++) {
                Balloon m = balloons.get(i);
                BalloonSprite s = sprites.get(i);

                s.category = m.getCategory();

                if (s.category == SecretItemSkin.ItemCategory.TIME) {
                    s.textColor = new Color(255, 110, 110);
                } else if (s.category == SecretItemSkin.ItemCategory.BALLOON) {
                    s.textColor = new Color(120, 160, 255);
                } else {
                    s.textColor = null;
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

            int x = 18;
            int baseY = 70;
            int gap = 32;

            String playerName = GamePanel.this.resolvePlayerName();
            g2.drawString("Player : " + playerName, x, baseY - gap);

            int lifeY = baseY;
            g2.drawString("life:", x, lifeY);

            int lifeCount = Math.max(0, Math.min(3, state.getLife()));
            int hx = x + 60;
            for (int i = 0; i < lifeCount; i++) {
                if (heartImg != null) {
                    g2.drawImage(heartImg,
                            hx + i * 32,
                            lifeY - 18,
                            24, 24,
                            null);
                }
            }

            String timeStr = String.format("Time limit : %d m %02d s",
                    Math.max(0, state.getTimeLeft()) / 60,
                    Math.max(0, state.getTimeLeft()) % 60);
            g2.drawString(timeStr, x, baseY + gap);

            int score = GamePanel.this.totalScore;
            g2.drawString("Score : " + score, x, baseY + 2 * gap);
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
                m.setCategory(SecretItemSkin.ItemCategory.NONE);
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

                b.category = SecretItemSkin.ItemCategory.NONE;
                b.textColor = null;
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
                g2.drawImage(
                        houseImg,
                        houseRect.x, houseRect.y,
                        houseRect.width, houseRect.height,
                        null
                );
            }

            for (var b : sprites) {
                renderer.renderBalloonOnly(g2, b);
            }

            drawHUD(g2);

            g2.dispose();
        }
    }   // PlayField 끝

    // --------------------------------------------------
    //  SingleGameRules : GameRules 구현
    // --------------------------------------------------
    private final class SingleGameRules implements GameRules {
        @Override
        public void onTick() {
            // 시간 감소는 tickTimer에서 처리
        }

        @Override
        public void onPop(List<Balloon> bs) {
            // 스테이지 클리어 처리는 GamePanel.onStageCleared()에서 처리
        }

        @Override
        public void onMiss() {
            state.loseLife();
        }

        @Override
        public boolean isGameOver() {
            return state.isGameOver();
        }
    }

    // ★ gray.png를 적당히 축소해서 wordLabel 아이콘으로 설정하는 공통 함수
    private void applyGrayOverlayIcon() {
        if (grayOverlayImg == null) return;

        int panelW = getWidth();
        int targetW = (panelW > 0) ? (int) (panelW * 0.7) : 800;

        int origW = grayOverlayImg.getWidth();
        int origH = grayOverlayImg.getHeight();
        int targetH = (int) ((double) origH * targetW / origW);

        Image scaled = grayOverlayImg.getScaledInstance(
                targetW,
                targetH,
                Image.SCALE_SMOOTH
        );

        wordLabel.setIcon(new ImageIcon(scaled));
        wordLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        wordLabel.setVerticalTextPosition(SwingConstants.CENTER);
    }

    // ★ 현재 레벨의 제한시간 안내를 gray.png 위에 띄우기
    private void showLevelIntroForCurrentStage() {
        levelIntroShowing = true;
        tickTimer.stop();
        playField.stop();

        // ⭐ 인트로를 띄우기 직전에 항상 90/80/70초로 강제 세팅
        resetTimeForCurrentLevel();

        int sec = Math.max(0, state.getTimeLeft());
        int m = sec / 60;
        int s = sec % 60;
        String timeStr = String.format("%d m %02d s", m, s);

        int level = state.getLevel();

        String html =
                "<html><div style='text-align:center;'>" +
                        "<span style='font-size:28px; font-weight:bold;'>Level " + level + "</span><br/><br/>" +
                        "제한시간 안에 단어를 모두 입력하세요!<br/>" +
                        "<span style='font-size:24px;'>time : " + timeStr + "</span>" +
                        "</div></html>";

        wordLabel.setText(html);
        wordLabel.setForeground(Color.WHITE);
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wordLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (grayOverlayImg != null) {
            applyGrayOverlayIcon();
        } else {
            wordLabel.setIcon(null);
            wordLabel.setOpaque(true);
            wordLabel.setBackground(new Color(0, 0, 0, 140));
        }

        wordLabel.setVisible(true);

        levelIntroTimer.restart();
    }

    private void hideLevelIntro() {
        levelIntroShowing = false;

        wordLabel.setVisible(false);
        wordLabel.setIcon(null);
        wordLabel.setText("");
        wordLabel.setOpaque(false);
        wordLabel.setBackground(null);

        playField.start();
        if (!tickTimer.isRunning()) {
            tickTimer.start();
        }

        grabFocusSafely();
    }

    // --------------------------------------------------
    //  랭킹 CSV 저장
    // --------------------------------------------------
    private void saveRanking(int finalScore, double accuracyPercent, int timeLeftSeconds) {
        String playerName = resolvePlayerName();

        String playedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        RankingRecord record = new RankingRecord(
                playerName,
                finalScore,
                accuracyPercent,
                timeLeftSeconds,
                playedAt
        );

        try {
            RankingCsvRepository repo = new RankingCsvRepository();
            repo.append(record);
        } catch (Exception e) {
            System.err.println("[GamePanel] saveRanking failed: " + e);
        }
    }
}
