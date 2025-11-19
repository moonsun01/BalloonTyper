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
import com.balloon.ui.hud.HUDRenderer;
import com.balloon.ui.skin.SecretItemSkin;

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
    private GameState state;
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

    // ★ 추가: GamePanel이 처음 보여졌는지 여부
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

//    // ====== 추가: 레벨 시작 안내 오버레이 ======
//    private final javax.swing.Timer levelIntroTimer =
//            new javax.swing.Timer(2000, e -> hideLevelIntro()); // 2초간 표시

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
            //new javax.swing.Timer(1200, e -> overlayLabel.setVisible(false));
            new javax.swing.Timer(1200, e -> {

                // ⚠ 인트로(레벨 안내) 중이면 SUCCESS/FAIL 타이머가 건드리지 않도록
                if (levelIntroShowing || resultShown) {
                    return;
                }

                // ★ SUCCESS/FAIL 표시 끝나면 wordLabel 초기화
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
    //private boolean resultShown = false;
    public static int lastCompletedStage = 1;

    // 전역 컨텍스트
    private final GameContext ctx = GameContext.getInstance();

    // HUD 활성 아이템 배지용 타이머(그냥 repaint만 돌리는 용도)
    private final javax.swing.Timer hudTimer =
            new javax.swing.Timer(200, e -> repaint());

    private boolean caretOn = true;

    public GamePanel(ScreenRouter router) {
        this.router = router;

        // 🔥 여기서 처음 GameState 생성
        this.state = new GameState(levelConfig);

        // ★★★ 전체 패널(게임 화면)의 레이아웃/배경 설정 ★★★
        setLayout(new BorderLayout());   // 위(HUD) / 가운데(PlayField) / 아래(입력창) 배치
        //setOpaque(true);                 // GamePanel이 직접 배경 이미지를 그림
        setOpaque(false);
        //setBackground(Color.BLACK);      // 혹시 bgImg가 null일 때 기본 배경색

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

        //topBar.add(hud, BorderLayout.WEST);
        topBar.add(legend, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

//        // ========= 중앙 플레이 영역 =========
//        playField = new PlayField();
//        playField.setLayout(new BorderLayout());
//        //add(playField, BorderLayout.CENTER);


        // ========= 중앙 플레이 영역 =========
        playField = new PlayField();
        playField.setLayout(new BorderLayout());

// ========= layeredPane 생성 =========
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.setLayout(null);
        playField.setBounds(0, 0, 1280, 720);
        layeredPane.add(playField, JLayeredPane.DEFAULT_LAYER); // playField는 기본 레이어에 추가

// ========= itemToastLabel 추가 =========
        int boxW = 420;   // 박스 가로 길이
        int boxH = 70;    // 박스 세로 길이
        int boxX = (1280 - boxW) / 2;  // 화면 가운데에 오도록 x 좌표 계산
        int boxY = 260;                // 세로 위치(원래 250이었으니까 비슷하게)

        itemToastLabel.setBounds(boxX, boxY, boxW, boxH);
        layeredPane.add(itemToastLabel, Integer.valueOf(JLayeredPane.PALETTE_LAYER));


        // ★★★★★ 중앙 wordLabel도 layeredPane의 위 레이어에 추가 ★★★★★
        wordLabel.setBounds(0, 160, 1280, 200);   // 화면 중앙쯤, 필요하면 Y값 수정 가능
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        layeredPane.add(wordLabel, Integer.valueOf(JLayeredPane.MODAL_LAYER));

        add(layeredPane, BorderLayout.CENTER);


        // 중앙 단어 라벨(지금은 숨김)
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        wordLabel.setForeground(Color.WHITE);
        //playField.add(wordLabel, BorderLayout.CENTER);
        wordLabel.setVisible(false);

        // ========= 토스트 라벨 =========
        toastLabel.setForeground(new Color(80, 120, 80));
        toastLabel.setFont(toastLabel.getFont().deriveFont(Font.PLAIN, 16f));
        toastLabel.setHorizontalAlignment(SwingConstants.CENTER);
        playField.add(toastLabel, BorderLayout.SOUTH);




        // ========= 하단 입력바 =========
        // 하단 입력바 (가운데 정렬)
        JPanel inputBar = new JPanel();
        inputBar.setOpaque(false);
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 0, 12, 0));
        inputBar.setLayout(new BoxLayout(inputBar, BoxLayout.X_AXIS));

        inputBar.add(Box.createHorizontalGlue());

        // ▶ 여기부터
        int rowW = 600;   // 전체 바 가로 길이 (원하면 500, 550 등으로 조정)
        int rowH = 40;    // 전체 바 높이

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.setOpaque(false);

// 크기를 확실히 고정해 줌
        Dimension rowSize = new Dimension(rowW, rowH);
        inputRow.setPreferredSize(rowSize);
        inputRow.setMaximumSize(rowSize);   // ★ 제일 중요: BoxLayout이 더 못 키우게 막기
        inputRow.setMinimumSize(rowSize);

        inputField.setFont(inputField.getFont().deriveFont(Font.PLAIN, 16f)); // 살짝 줄여도 됨
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(Color.BLACK);
        inputField.setCaretColor(Color.BLACK);
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        inputRow.add(inputField, BorderLayout.CENTER);

        inputBar.add(inputRow);
        inputBar.add(Box.createHorizontalGlue());

        add(inputBar, BorderLayout.SOUTH);

        // ========= 오버레이 라벨 (SUCCESS / FAIL + SCORE) =========
        // ※ 기존 초록 SUCCESS 대신, 중앙에 크게 뜨는 결과창 역할만 담당
        overlayLabel.setFont(overlayLabel.getFont().deriveFont(Font.BOLD, 42f));
        overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overlayLabel.setVerticalAlignment(SwingConstants.CENTER);
        overlayLabel.setVisible(false);

        // HTML을 써서 두 줄(제목 + 점수)을 중앙 정렬로 표현할 거라
        // 여기서는 기본 색만 일단 흰색으로
        //overlayLabel.setForeground(Color.WHITE);

        // ★★★ 가장 중요: NORTH → CENTER로 변경 ★★★
        // 이제 중앙(CENTER)을 overlayLabel이 차지하게 만들기
        //playField.add(overlayLabel, BorderLayout.CENTER);

        // 기존 overlayTimer는 더 이상 결과창에 쓰지 않을 거라서 일단 그대로 놔둬도 되고,
        // "다른 용도로 쓰고 있다면" 유지, 아니라면 아래 한 줄을 주석 처리해도 됨
        overlayTimer.setRepeats(false);

        // ========= 아이템 토스트 라벨(아이템 효과 표시용) =========
        itemToastLabel.setFont(new Font("Dialog", Font.BOLD, 32));
        itemToastLabel.setForeground(new Color(255, 240, 180));

// 🔹 배경 박스 보이게 만들기
        itemToastLabel.setOpaque(true);  // 배경색이 실제로 그려지도록
        itemToastLabel.setBackground(new Color(0, 0, 0, 180)); // 살짝 투명한 검정 박스

// 🔹 안쪽 여백 + 테두리(선택)
        itemToastLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 200), 2),  // 흰 테두리
                BorderFactory.createEmptyBorder(10, 20, 10, 20)                    // 안쪽 여백
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
            hideLevelIntro();      // 아래에서 만들 함수

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
        refreshHUD();

        // ★ 시작 시간 1초 보정 (지금 89라면 90으로 올려주기)
        if (state.getLevel() == 1 && state.getTimeLeft() == 89) {
            state.addSeconds(1);   // GameState 안에 이미 있는 메서드(아이템 효과에서도 쓰고 있음)
            refreshHUD();          // HUD 라벨도 다시 갱신
        }

        // 타이머 시작
        hudTimer.start();

        //tickTimer.start();

        showLevelIntroForCurrentStage();
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

        // norm()과 비슷하게 정리
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

        //앞 뒤 공백 제거
        s = s.trim();
        //return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
        // 2) 유니코드 정규화 (한글 조합 통일)
        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);

        // 3) 한글/영문/숫자가 아닌 글자는 전부 제거
        //    → "□풍선", " 풍선\t" 이런 것들을 "풍선"으로 맞춰줌
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
        //if (!tickTimer.isRunning()) tickTimer.start();
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

            //단어 맞출 때마다 총점 10점 증가
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
        //state.addRemainingTimeAsScore();

        //남은 시간 보너스를 총점에 반영
        totalScore += bonus;
        refreshHUD();

        showOverlay("✔ SUCCESS!  +" + bonus + "점", Color.WHITE);
        showToast("남은 시간 " + remain + "초 → +" + bonus + "점!", new Color(255, 255, 150));

        lastCompletedStage = state.getLevel();

        new javax.swing.Timer(1000, e -> {
            state.nextLevel();
            if (state.isGameOver() || state.getLevel() > 3) {
                showFinalResult(true);
                return;
            }

            stageClearedThisRound = false;
            applyStageBackground(state.getLevel());
            reloadStageBalloons();
            refreshHUD();

            // ★ Stage 2, 3 시작 안내 오버레이
            showLevelIntroForCurrentStage();

            showToast("Stage " + state.getLevel() + " Start!", new Color(100, 200, 100));

            resultShown = false;
            //tickTimer.restart();

// ⚠ 게임 루프는 인트로가 끝난 뒤(levelIntroTimer)에서 다시 시작
            if (tickTimer.isRunning()) {
                tickTimer.stop();
            }
            playField.stop();
        }) {{
            setRepeats(false);
            start();
        }};
    }

    /** 싱글모드를 완전히 처음부터 다시 시작할 때 호출 */
    private void resetGameForNewRun() {
        // 1) 타이머/루프/인트로 상태 정리
        stopGameLoops();                    // tickTimer, playField 정지
        if (overlayTimer.isRunning()) overlayTimer.stop();
        if (levelIntroTimer.isRunning()) levelIntroTimer.stop();

        levelIntroShowing = false;
        stageClearedThisRound = false;
        resultShown = false;
        showingResult = false;
        navigatedAway = false;

        // 2) 점수/카운트 리셋
        correctCount = 0;
        wrongCount = 0;
        wordScore = 0;
        timeBonus = 0;
        itemBonus = 0;
        totalScore = 0;

        // 3) GameState 새로 생성 (레벨 1, life 3, 초기시간)
        state = new GameState(levelConfig);

        // 4) 배경/풍선 다시 세팅
        applyStageBackground(state.getLevel());
        reloadStageBalloons();   // balloons + sprites 다시 채우기

        // 오버레이/라벨 정리
        wordLabel.setVisible(false);
        wordLabel.setIcon(null);
        wordLabel.setText("");
        wordLabel.setOpaque(false);
        wordLabel.setBackground(null);

        toastLabel.setText(" ");

        // HUD 갱신
        refreshHUD();
        updateContextHud();

        // 5) 다시 첫 진입처럼 레벨 인트로부터 시작
        firstShown = false;              // 이미 여기서 직접 인트로 띄울 거라 true일 필요 없음
        showLevelIntroForCurrentStage(); // gray.png + Level 안내
        grabFocusSafely();
    }


    private void onStageFailed() {
        stopGameLoops();

        showOverlay("✖ FAILED!  (Stage " + state.getLevel() + ")", new Color(230, 90, 90));

        javax.swing.Timer t = new javax.swing.Timer(600, e -> {
            ((javax.swing.Timer) e.getSource()).stop();
            // 실패 엔딩
            showFinalResult(false);
        });
        t.setRepeats(false);
        t.start();
    }

//    private void showOverlay(String text, Color color) {
//        overlayLabel.setText(text);
//        overlayLabel.setForeground(color);
//        overlayLabel.setVisible(true);
//        overlayTimer.restart();
//    }

    private void showOverlay(String text, Color color) {
        // 결과 오버레이는 levelIntroShowing과는 별개로 사용
        // (이미 게임이 끝난 상태라 intro는 안 떠 있음)

        // ★ SUCCESS/FAIL을 중앙에 크게 표시
        String html =
                "<html><div style='text-align:center;'>" +
                        "<span style='font-size:32px; font-weight:bold;'>" + text + "</span>" +
                        "</div></html>";

        wordLabel.setText(html);
        wordLabel.setForeground(color);
        wordLabel.setHorizontalAlignment(SwingConstants.CENTER);
        wordLabel.setVerticalAlignment(SwingConstants.CENTER);

        // ★★★ 여기서 회색 박스를 "조금 좁게" 스케일해서 사용 ★★★
        if (grayOverlayImg != null) {
            int panelW = getWidth();
            // 👉 전체 폭의 45% 정도만 쓰도록 (너무 넓으면 0.4, 더 넓게는 0.5로 조절 가능)
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
            wordLabel.setOpaque(false);   // 아이콘 위에 글자만
            wordLabel.setBackground(null);
        } else {
            // gray.png를 굳이 안 쓰고 싶으면 반투명 배경만
            wordLabel.setIcon(null);
            wordLabel.setOpaque(true);
            wordLabel.setBackground(new Color(0, 0, 0, 160));
        }

        wordLabel.setVisible(true);

        // 1.2초 뒤 overlayTimer가 호출되어 wordLabel을 다시 숨김
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
            playField.clearSprites();   // 이미 GamePanel 안에서 쓰고 있는 메서드라 호출 가능
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

        // 약간 깔끔하게 보이도록 토스트/오버레이 정리
        toastLabel.setText(" ");
        //overlayLabel.setVisible(false);

        // 3초 뒤 RANKING 화면으로 이동
        new javax.swing.Timer(3000, e -> {
            ((javax.swing.Timer) e.getSource()).stop();

            if (router != null) {
                try {
                    router.show(ScreenId.RANKING);  // ★ RANKING 화면으로 이동
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
        updateContextHud();   // HUD는 항상 최신으로

        // 1) 이전에 한 번 게임이 끝났던 상태라면 → 완전 리셋해서 새 게임 시작
        //  - resultShown == true (SUCCESS/FAIL 화면까지 갔던 상태)
        //  - 또는 GameState 기준으로 이미 게임오버 상태
        if (resultShown || state.isGameOver()) {
            resetGameForNewRun();
            return;
        }

        // 2) 레벨 인트로(gray 박스)가 떠 있는 중이면: 타이머 건드리지 말고 포커스만
        if (levelIntroShowing) {
            grabFocusSafely();
            return;
        }

        // 3) 완전 최초 진입(처음 싱글 들어올 때만): Level 1 인트로 띄우기
        if (firstShown && state.getLevel() == 1 && !resultShown) {
            firstShown = false;
            showLevelIntroForCurrentStage();
            grabFocusSafely();
            return;
        }

        // 4) 그 외에는 그냥 게임 재개
        if (!tickTimer.isRunning()) {
            tickTimer.start();
        }
        playField.start();   // 혹시 멈춰있다면 재시작
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

                    // 🔽 현재 필드에서 안 쓰는 단어만 사용
                    String word = nextUniqueWord();

                    // ★ 화면에 찍힐 단어도 깨끗하게 정리
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
        // PlayField 내부 메서드
        private void assignRandomItemCategoriesForSingleMode() {

            // 풍선/스프라이트가 너무 적으면 패스
            if (balloons.size() < 4 || sprites.size() < 4) {
                return;
            }

            // 1) 인덱스 리스트 만들고 섞기
            java.util.List<Integer> indices = new java.util.ArrayList<>();
            for (int i = 0; i < balloons.size(); i++) {
                indices.add(i);
            }
            java.util.Collections.shuffle(indices);

            // 2) 전부 NONE으로 초기화
            for (Balloon b : balloons) {
                b.setCategory(SecretItemSkin.ItemCategory.NONE);
            }

            int timeCount = 2;      // TIME 풍선 2개
            int balloonCount = 2;   // BALLOON 풍선 2개
            int idxPos = 0;

            // 3) TIME 카테고리 2개
            for (int i = 0; i < timeCount && idxPos < indices.size(); i++, idxPos++) {
                int bi = indices.get(idxPos);
                balloons.get(bi).setCategory(SecretItemSkin.ItemCategory.TIME);
            }

            // 4) BALLOON 카테고리 2개
            for (int i = 0; i < balloonCount && idxPos < indices.size(); i++, idxPos++) {
                int bi = indices.get(idxPos);
                balloons.get(bi).setCategory(SecretItemSkin.ItemCategory.BALLOON);
            }

            // 5) BalloonSprite에 카테고리 + 글자색 반영
            int limit = Math.min(balloons.size(), sprites.size());
            for (int i = 0; i < limit; i++) {
                Balloon m = balloons.get(i);
                BalloonSprite s = sprites.get(i);

                // enum 그대로 복사
                s.category = m.getCategory();

                // 카테고리에 따른 글자색 지정
                if (s.category == SecretItemSkin.ItemCategory.TIME) {
                    s.textColor = new Color(255, 110, 110);   // 빨간 계열 (시간 아이템)
                } else if (s.category == SecretItemSkin.ItemCategory.BALLOON) {
                    s.textColor = new Color(120, 160, 255);   // 파란 계열 (풍선 아이템)
                } else {
                    s.textColor = null; // 기본색 쓰도록
                }
            }
        }


        private void clearSprites() {
            sprites.clear();
        }

        /**
         * 카테고리별 글자색 결정
         */
        private Color colorForCategory(SecretItemSkin.ItemCategory category) {
            if (category == SecretItemSkin.ItemCategory.TIME) {
                // 시간 아이템: 빨간 계열
                return new Color(255, 110, 110);
            }
            if (category == SecretItemSkin.ItemCategory.BALLOON) {
                // 풍선 개수 아이템: 파란 계열
                return new Color(120, 160, 255);
            }
            // 그 외(NONE, TRICK 등): 기본색(렌더러 기본 값 사용)
            return null;
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

            // 기준 좌표 + 줄 간격
            int x = 18;
            int baseY = 70;     // life 줄
            int gap = 32;       // 줄 간격 (player / life / time / score 사이 거리)

            // 0) Player : 맨 위 줄
            //  - GamePanel에 이미 있는 resolvePlayerName() 재사용
            String playerName = GamePanel.this.resolvePlayerName();
            g2.drawString("Player : " + playerName, x, baseY - gap);

            // 1) life 줄
            int lifeY = baseY;
            g2.drawString("life:", x, lifeY);

            int lifeCount = Math.max(0, Math.min(3, state.getLife()));
            int hx = x + 60; // life: 뒤에서부터 하트 시작 위치
            for (int i = 0; i < lifeCount; i++) {
                if (heartImg != null) {
                    g2.drawImage(heartImg,
                            hx + i * 32,   // 하트 간격 32px 정도
                            lifeY - 18,    // 글자 기준 위로 약간 올리기
                            24, 24,
                            null);
                }
            }

            // 2) time 줄
            String timeStr = String.format("Time limit : %d m %02d s",
                    Math.max(0, state.getTimeLeft()) / 60,
                    Math.max(0, state.getTimeLeft()) % 60);
            g2.drawString(timeStr, x, baseY + gap);

            // 3) score 줄
            //  - 점수는 GamePanel의 totalScore 필드에 누적되고 있음
            int score = GamePanel.this.totalScore;
            g2.drawString("Score : " + score, x, baseY + 2 * gap);
        }


        private void addBalloons(int n) {
            int W = DESIGN_W;
            int s = 70;
            int y = Math.max(80, houseAnchor.y - 6 * s);

            Skin[] skins = new Skin[]{Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN};

            for (int i = 0; i < n; i++) {
                // 🔽 아이템으로 추가되는 풍선도 중복 없는 단어 사용
                String word = nextUniqueWord();

                Skin skin = skins[rnd.nextInt(skins.length)];
                BufferedImage img = BalloonSkins.of(skin);
                int x = 40 + rnd.nextInt(Math.max(1, W - 80));

                Balloon m = new Balloon(word, x, y, toKind(skin));
                // ★ 새로 추가되는 풍선은 항상 "일반 풍선" (아이템 없음)
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

                // ★ 스프라이트도 일반 풍선으로 (검정 글씨)
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

//            // ★ 배경 이미지를 PlayField 전체 크기에 맞춰서 먼저 그리기
//            if (GamePanel.this.bgImg != null) {
//                g2.drawImage(GamePanel.this.bgImg, 0, 0, getWidth(), getHeight(), null);
//            }

            // 줄(실) 그리기
            for (var b : sprites) {
                b.anchorX = houseAnchor.x;
                b.anchorY = houseAnchor.y;
                drawLine(g2, b);
            }

            // 집 그리기
            if (houseImg != null) {
                g2.drawImage(
                        houseRect.width > 0 ? houseImg : houseImg,
                        houseRect.x, houseRect.y,
                        houseRect.width, houseRect.height,
                        null
                );
            }

            // 풍선 그리기
            for (var b : sprites) {
                renderer.renderBalloonOnly(g2, b);
            }

            // HUD(목숨, 타임리밋) 그리기
            drawHUD(g2);

            g2.dispose();
        }
    }   // ★ 여기까지가 PlayField 클래스 끝!



    // --------------------------------------------------
    //  SingleGameRules : GameRules 구현
    // --------------------------------------------------
    private final class SingleGameRules implements GameRules {
        @Override
        public void onTick() {
        }

        @Override
        public void onPop(List<Balloon> bs) {
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

        // 1) 기준 폭 계산: 화면 폭의 약 70% 정도 (원하면 0.6, 0.8 등으로 조절 가능)
        int panelW = getWidth();
        int targetW = (panelW > 0) ? (int) (panelW * 0.7) : 800; // 화면 크기 없으면 기본 800

        // 2) 원본 비율 유지하면서 높이 계산
        int origW = grayOverlayImg.getWidth();
        int origH = grayOverlayImg.getHeight();
        int targetH = (int) ((double) origH * targetW / origW);

        // 3) 부드럽게 스케일링
        Image scaled = grayOverlayImg.getScaledInstance(
                targetW,
                targetH,
                Image.SCALE_SMOOTH
        );

        // 4) wordLabel에 적용
        wordLabel.setIcon(new ImageIcon(scaled));
        wordLabel.setHorizontalTextPosition(SwingConstants.CENTER);
        wordLabel.setVerticalTextPosition(SwingConstants.CENTER);
    }

    // ★ 현재 레벨의 제한시간 안내를 gray.png 위에 띄우기
    private void showLevelIntroForCurrentStage() {
        levelIntroShowing = true;
        tickTimer.stop();       // ★ 여기가 핵심!
        playField.stop();       // 풍선 움직임도 중지

        // 현재 레벨의 남은 시간으로 "1 m 30 s" 형식 만들기
        int sec = Math.max(0, state.getTimeLeft());
        int m = sec / 60;
        int s = sec % 60;
        String timeStr = String.format("%d m %02d s", m, s);

        int level = state.getLevel(); // ★ 현재 레벨

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

        // ★ 회색 박스를 적당한 크기로 축소해서 중앙에 표시
        if (grayOverlayImg != null) {
            applyGrayOverlayIcon();   // ← 새로 만든 함수 사용
        } else {
            // 혹시 gray.png 로딩 실패했을 때 대비
            wordLabel.setIcon(null);
            wordLabel.setOpaque(true);
            wordLabel.setBackground(new Color(0, 0, 0, 140));
        }


        // 입력 박스 등은 그대로 두고 가운데에만 띄움
        wordLabel.setVisible(true);

        // 타이머 시작 (2초 뒤 levelIntroTimer가 실행되어 게임 시작)
        levelIntroTimer.restart();
    }

    private void hideLevelIntro() {

        // ★ 인트로가 이제 끝났다는 표시
        levelIntroShowing = false;

        // ★ 인트로 때 썼던 라벨 초기화
        wordLabel.setVisible(false);
        wordLabel.setIcon(null);
        wordLabel.setText("");
        wordLabel.setOpaque(false);
        wordLabel.setBackground(null);

        // ★★★ 인트로 종료 후 게임 시작 ★★★
        playField.start();                   // 풍선 낙하 시작
        if (!tickTimer.isRunning()) {
            tickTimer.start();               // 시간 카운트 시작
        }

        // ★ 포커스 다시 Player에게
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