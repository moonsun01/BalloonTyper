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
import com.balloon.ui.render.BalloonSpriteRenderer;

import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GamePanel (refactored to match provided models):
 *  - UI는 렌더/입력만 담당
 *  - 시간/생명/점수/레벨은 GameState가 단일 소스
 *  - 정답 판정은 GameJudge가 수행, 아이템은 ItemSpawner+ItemEffectApplier로 즉시 반영
 *  - GameRules(싱글 규칙)로 레벨 클리어/게임오버 플로우 처리
 *  - Balloon 모델 시그니처( text,x,y,Kind,active )에 맞춤
 */
public class GamePanel extends JPanel implements Showable {

    // ====== 모델/로직 ======
    private final LevelConfig levelConfig = new LevelConfig();
    private final GameState state = new GameState(levelConfig);
    private final ItemSpawner spawner = new ItemSpawner();

    // UI 콜백을 제공하는 Applier (시간/토스트/필드 조작)
    private final ItemEffectApplier applier = new ItemEffectApplier(
            // TimeApi
            new ItemEffectApplier.TimeApi() {
                @Override public void addSeconds(int delta) { state.addSeconds(delta); refreshHUD(); }
                @Override public int getTimeLeft() { return state.getTimeLeft(); }
            },
            // UiApi
            new ItemEffectApplier.UiApi() {
                @Override public void showToast(String message) {
                    GamePanel.this.showToast(message, new java.awt.Color(40, 180, 100));
                }
                @Override public void flashEffect(boolean positive) {
                    GamePanel.this.flash(positive);
                }
            },

            // FieldApi
            new ItemEffectApplier.FieldApi() {
                @Override public void addBalloons(int n) { addBalloons(n); revalidate(); repaint(); }
                @Override public void removeBalloons(int n) { removeBalloons(n); repaint(); }
            }
    );

    // GameJudge(아이템 연동 버전)
    private final com.balloon.game.GameJudge judge = new com.balloon.game.GameJudge(spawner, applier);

    // 싱글 규칙 구현체
    private final GameRules rules = new com.balloon.game.SingleGameRules(state);

    // 화면전환 라우터
    private final ScreenRouter router;

    // ====== UI 요소 ======
    private final JLabel timeLabel = new JLabel();
    private final JLabel scoreLabel = new JLabel();
    private final JLabel playerLabel = new JLabel();
    private final JLabel modeLabel   = new JLabel();
    private final JLabel toastLabel  = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel overlayLabel= new JLabel(" ", SwingConstants.CENTER);

    private final JTextField inputField = new JTextField();
    private final Timer tickTimer;
    private final Timer overlayTimer = new Timer(1200, e -> overlayLabel.setVisible(false));

    // ====== 스프라이트/렌더 ======
    private final PlayField playField = new PlayField();
    private final BalloonSpriteRenderer renderer = new BalloonSpriteRenderer();

    // 모델 풍선 리스트(판정용)
    private final java.util.List<Balloon> balloons = new ArrayList<>();

    // UI 스프라이트 — 화면 렌더용. 각 스프라이트는 대응되는 Balloon 모델을 가진다
    private final java.util.List<Sprite> sprites = new ArrayList<>();

    // 배경/집/하트
    private BufferedImage bgImg, houseImg, heartImg;

    // 기타
    private volatile boolean navigatedAway = false;
    private boolean stageClearedThisRound = false;

    public GamePanel(ScreenRouter router) {
        this.router = router;

        // ====== 루트 레이아웃 ======
        setLayout(new BorderLayout());
        setOpaque(false);

        // ====== HUD (상단) ======
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        hud.setOpaque(false);
        for (JLabel l : new JLabel[]{ timeLabel, scoreLabel, playerLabel, modeLabel }) {
            l.setForeground(Color.WHITE);
        }
        hud.add(timeLabel);
        hud.add(scoreLabel);
        hud.add(new JLabel(" | "));
        hud.add(playerLabel);
        hud.add(modeLabel);
        add(hud, BorderLayout.NORTH);

        // ====== 중앙: OverlayLayout (playField + overlayLabel만 겹치기) ======
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new OverlayLayout(center));

        // playField가 중앙을 가득 채우도록
        playField.setOpaque(false);
        playField.setAlignmentX(0.5f);
        playField.setAlignmentY(0.5f);
        playField.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        // 큰 오버레이(성공/실패)
        overlayLabel.setOpaque(false);
        overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overlayLabel.setVerticalAlignment(SwingConstants.CENTER);
        overlayLabel.setAlignmentX(0.5f);
        overlayLabel.setAlignmentY(0.5f);
        overlayLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        overlayLabel.setFont(overlayLabel.getFont().deriveFont(Font.BOLD, 42f));
        overlayLabel.setForeground(new Color(255, 255, 160));
        overlayLabel.setVisible(false);
        overlayTimer.setRepeats(false);

        // 추가 순서: 바닥(playField) → 최상단(overlayLabel)
        center.add(playField);
        center.add(overlayLabel);
        add(center, BorderLayout.CENTER);

        // ====== 하단: 토스트 + 입력바 묶어서 SOUTH에 배치 ======
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);

        // (1) 토스트: 입력창 바로 위에 얹기 (Pop/Miss/점수 노출)
        toastLabel.setOpaque(false);
        toastLabel.setHorizontalAlignment(SwingConstants.CENTER);
        toastLabel.setVerticalAlignment(SwingConstants.CENTER);
        toastLabel.setForeground(new Color(200, 230, 200));
        toastLabel.setFont(toastLabel.getFont().deriveFont(16f));
        south.add(toastLabel, BorderLayout.NORTH);

        // (2) 입력바
        JPanel inputBar = new JPanel(new BorderLayout());
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
        inputBar.setOpaque(false);

        inputField.setFont(inputField.getFont().deriveFont(Font.BOLD, 18f));
        inputField.setColumns(18);
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(Color.BLACK);
        inputField.setCaretColor(Color.BLACK);
        inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
        inputField.setPreferredSize(new Dimension(560, 34));
        inputBar.add(inputField, BorderLayout.CENTER);

        JLabel hint = new JLabel("타이핑 · Enter=확인 · Backspace=삭제 · Esc=지우기");
        hint.setForeground(new Color(200, 210, 230));
        inputBar.add(hint, BorderLayout.EAST);

        south.add(inputBar, BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);

        // ====== 입력/포커스/키 바인딩 ======
        setupKeyBindings();
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        SwingUtilities.invokeLater(this::grabFocusSafely);

        // ====== 틱 타이머 (1초) ======
        tickTimer = new Timer(1000, e -> {
            if (state.getTimeLeft() > 0) {
                state.decreaseTime();
                refreshHUD();
                if (state.getTimeLeft() == 0 && !allCleared()) onStageFailed();
            }
        });

        // ====== 자산 로드/배경 ======
        heartImg = ImageAssets.load("heart.png");
        houseImg = ImageAssets.load("home.png");
        applyStageBackground(state.getLevel());

        // ====== 초기 스폰 & HUD ======
        spawnInitialBalloons(20);
        updateContextHud();
        refreshHUD();
    }



    // ====== 이벤트 ======
    private void setupKeyBindings() {
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submitField");
        inputField.getActionMap().put("submitField", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onEnter(); }
        });
        inputField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearField");
        inputField.getActionMap().put("clearField", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { inputField.setText(""); }
        });
    }

    private void onEnter() {
        String typed = inputField.getText().trim();
        if (typed.isEmpty()) {
            rules.onMiss();
            refreshHUD();
            return;
        }

        boolean ok = judge.submit(balloons, typed, rules);
        if (ok) {
            removeFirstSpriteByWord(typed);
            showToast("✓ Pop!", new Color(25, 155, 75));

            if (allCleared()) onStageCleared();
        } else {
            showToast("✗ Miss", new Color(190, 60, 60));
            if (state.getLife() <= 0) {
                onStageFailed();
                return;
            }
        }
        inputField.setText("");
        refreshHUD();
    }


    // ====== HUD/UI ======
    private void refreshHUD() {
        timeLabel.setText("Time: " + Math.max(0, state.getTimeLeft()));
        scoreLabel.setText("Score: " + state.getTotalScore());
        repaint();
    }

    private void showToast(String msg, Color color) {
        toastLabel.setForeground(color);
        toastLabel.setText(msg);
        Timer t = new Timer(700, e -> toastLabel.setText(" "));
        t.setRepeats(false);
        t.start();
    }

    private void flash(boolean positive) {
        Color c = positive ? new Color(0xCCFFCC) : new Color(0xFFCCCC);
        Color old = getBackground();
        setBackground(c);
        Timer t = new Timer(150, e -> setBackground(old));
        t.setRepeats(false);
        t.start();
    }

    private void updateContextHud() {
        String name = Session.getNickname();
        if (name == null || name.isBlank()) {
            try {
                String fromCtx = GameContext.getInstance().getPlayerName();
                if (fromCtx != null && !fromCtx.isBlank()) name = fromCtx;
            } catch (Exception ignore) {}
        }
        if (name == null || name.isBlank()) name = "-";
        playerLabel.setText("Player: " + name);

        String mode = "-";
        try {
            String m = String.valueOf(GameContext.getInstance().getMode());
            if (m != null && !m.equalsIgnoreCase("null") && !m.isBlank()) mode = m;
        } catch (Exception ignore) {}
        modeLabel.setText("Mode: " + mode);
    }

    private void grabFocusSafely() {
        inputField.requestFocusInWindow();
        if (!tickTimer.isRunning()) tickTimer.start();
    }

    @Override public void onShown() { navigatedAway = false; grabFocusSafely(); updateContextHud(); }
    public void onHidden() {
        navigatedAway = true;
        if (tickTimer.isRunning()) tickTimer.stop();
        if (overlayTimer.isRunning()) overlayTimer.stop();
    }

    // ====== 스테이지/레벨 ======
    private void onStageCleared() {
        if (stageClearedThisRound) return;
        stageClearedThisRound = true;

        tickTimer.stop();

        int remain = Math.max(0, state.getTimeLeft());
        int bonus  = remain * 10;

        // 점수 누적
        state.addRemainingTimeAsScore();

        // 중앙 오버레이(큰 글자) + SOUTH 토스트(입력창 위)
        showOverlay("✔ SUCCESS!  +" + bonus + "점", new Color(110, 220, 110));
        showToast("남은 시간 " + remain + "초 → +" + bonus + "점!", new Color(255, 255, 150));

        // HUD 즉시 갱신
        refreshHUD();

        // 1초 뒤 다음 레벨로 이동
        new javax.swing.Timer(1000, e -> {
            state.nextLevel();
            if (state.isGameOver()) { showResult(); return; }

            stageClearedThisRound = false;
            applyStageBackground(state.getLevel());
            sprites.clear();
            balloons.clear();
            spawnInitialBalloons(20);
            refreshHUD();
            showToast("Stage " + state.getLevel() + " Start!", new Color(100, 200, 100));
            tickTimer.restart();
        }) {{ setRepeats(false); start(); }};
    }



    private void onStageFailed() {
        if (tickTimer.isRunning()) tickTimer.stop();
        showOverlay("✖ FAILED!  (Stage " + state.getLevel() + ")", new Color(230, 90, 90));
        new Timer(500, e -> showResult()).start();
    }

    private void showResult() {
        if (navigatedAway) return;
        double accuracy = 0.0; int correctCount = 0; int wrongCount = 0;
        ResultData data = new ResultData(state.getTotalScore(), Math.max(0, state.getTimeLeft()), accuracy, correctCount, wrongCount);
        ResultContext.set(data);
        if (router != null) {
            try {
                Component c = router.get(ScreenId.RESULT);
                if (c instanceof ResultScreen rs) rs.setResult(state.getTotalScore(), accuracy, Math.max(0, state.getTimeLeft()));
                router.show(ScreenId.RESULT);
            } catch (Exception ex) {
                System.err.println("[GamePanel] navigate RESULT error: " + ex);
                router.show(ScreenId.RESULT);
            }
        }
    }

    private void showOverlay(String text, Color color) {
        overlayLabel.setText(text);
        overlayLabel.setForeground(color);
        overlayLabel.setVisible(true);
        overlayTimer.restart();
    }

    // ====== 필드/풍선 ======
    private static Balloon.Kind toKind(Skin skin) {
        return switch (skin) {
            case PURPLE, PINK -> Balloon.Kind.RED;   // 임의 매핑
            case YELLOW, ORANGE -> Balloon.Kind.GREEN;
            case GREEN -> Balloon.Kind.BLUE;
        };
    }

    private void spawnInitialBalloons(int count) {
        String[] bank = { "도서관","고양이","운동장","한가람","바다빛","이야기","도전정신",
                "자다","인터넷","병원","전문가","초롱빛","노력하다","택시","집","나라",
                "달빛","별빛","산책","행복","용기","친구","추억","봄날","밤하늘" };
        Skin[] skins = new Skin[]{ Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN };

        int W = Math.max(getWidth(), 900), H = Math.max(getHeight(), 600);
        int centerX = W / 2;
        int[] pattern = {3,4,5,6,5,4,3};
        int s = Math.max(68, Math.min(92, (int)(W * 0.07)));
        int gapX = (int)(s * 1.20), gapY = (int)(s * 0.95);
        int topY = Math.max(110, playField.houseAnchor.y - (gapY * (pattern.length + 1)));

        sprites.clear(); balloons.clear();
        int idx = 0;
        for (int r = 0; r < pattern.length && balloons.size() < count; r++) {
            int n = pattern[r];
            int y = topY + r * gapY;
            int totalWidth = (n - 1) * gapX;
            int startX = centerX - totalWidth / 2;
            for (int c = 0; c < n && balloons.size() < count; c++) {
                String word = bank[idx % bank.length];
                Skin skin = skins[(idx + c) % skins.length];
                BufferedImage img = BalloonSkins.of(skin);
                int x = startX + c * gapX;

                Balloon m = new Balloon(word, x, y, toKind(skin));
                balloons.add(m);
                sprites.add(new Sprite(m, img, x, y, s, s));
                idx++;
            }
        }
        repaint();
    }

    private void addBalloons(int n) {
        int W = Math.max(getWidth(), 900);
        int s = Math.max(68, Math.min(92, (int)(W * 0.07)));
        int y = Math.max(80, playField.houseAnchor.y - 6 * s);
        Skin[] skins = new Skin[]{ Skin.PURPLE, Skin.YELLOW, Skin.PINK, Skin.ORANGE, Skin.GREEN };
        Random rnd = new Random();
        String[] bank = {"보라","노랑","분홍","주황","초록","파랑","하양","검정"};
        for (int i = 0; i < n; i++) {
            String word = bank[rnd.nextInt(bank.length)] + (rnd.nextInt(90)+10);
            Skin skin = skins[rnd.nextInt(skins.length)];
            BufferedImage img = BalloonSkins.of(skin);
            int x = 40 + rnd.nextInt(Math.max(1, getWidth() - 80));
            Balloon m = new Balloon(word, x, y, toKind(skin));
            balloons.add(m);
            sprites.add(new Sprite(m, img, x, y, s, s));
        }
        revalidate();
        repaint();
    }

    private void removeBalloons(int n) {
        int removed = 0;
        for (int i = sprites.size() - 1; i >= 0 && removed < n; i--) {
            Sprite s = sprites.get(i);
            if (s.model.isActive()) {
                s.model.pop();
                sprites.remove(i);
                balloons.remove(s.model);
                removed++;
            }
        }
    }

    // 입력 단어와 같은 풍선을 찾아 제거 (NFC/trim 기준으로 통일)
    private static String norm(String s) {
        if (s == null) return "";
        s = s.trim();
        return java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC);
    }

    private void removeFirstSpriteByWord(String word) {
        String needle = norm(word);
        for (int i = 0; i < sprites.size(); i++) {
            if (norm(sprites.get(i).model.getWord()).equalsIgnoreCase(needle)) {
                sprites.remove(i);
                break;
            }
        }
        for (Balloon b : balloons) {
            if (b.isActive() && norm(b.getWord()).equalsIgnoreCase(needle)) {
                b.pop();
                balloons.remove(b);
                break;
            }
        }
    }


    private boolean allCleared() { for (Balloon b : balloons) if (b.isActive()) return false; return true; }

    // ====== 렌더 ======
    private void applyStageBackground(int stage) {
        String bgName = switch (stage) { case 1 -> "bg_level1.png"; case 2 -> "bg_level2.png"; default -> "bg_level3.png"; };
        bgImg = ImageAssets.load(bgName);
        repaint();
    }

    @Override protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        if (bgImg != null) g2.drawImage(bgImg, 0, 0, getWidth(), getHeight(), null);
        g2.dispose();
    }

    // ====== 내부 컴포넌트 ======
    private final class PlayField extends JPanel {
        private final Timer frameTimer = new Timer(16, e -> repaint());
        private final Point houseAnchor = new Point(0,0);
        private Rectangle houseRect = new Rectangle(0,0,0,0);

        PlayField() {
            setOpaque(false);
            SwingUtilities.invokeLater(() -> { layoutHouse(); repaint(); });
            frameTimer.start();
        }

        private void layoutHouse() {
            int W = Math.max(getWidth(), 900);
            int H = Math.max(getHeight(), 600);
            if (houseImg == null) houseImg = ImageAssets.load("home.png");
            int hw = Math.max(90, (int)(W * 0.09));
            int hh = (int)(hw * (houseImg.getHeight() / (double) houseImg.getWidth()));
            int hx = W/2 - hw/2;
            int hy = H - hh - 72;
            houseRect.setBounds(hx, hy, hw, hh);
            houseAnchor.setLocation(hx + hw/2, hy + (int)(hh * 0.30));
        }

        @Override public void invalidate() {
            super.invalidate();
            SwingUtilities.invokeLater(() -> { if (getWidth() > 0) { layoutHouse(); repaint(); } });
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();

            if (houseImg != null) g2.drawImage(houseImg, houseRect.x, houseRect.y, houseRect.width, houseRect.height, null);

            // 줄
            for (Sprite s : sprites) {
                s.anchorX = houseAnchor.x; s.anchorY = houseAnchor.y;
                renderer.renderLineOnly(g2, s.toSprite());
            }
            // 풍선
            for (Sprite s : sprites) renderer.renderBalloonOnly(g2, s.toSprite());

            drawHUD(g2);
            g2.dispose();
        }

        private void drawHUD(Graphics2D g2) {
            g2.setFont(new Font("Dialog", Font.BOLD, 18));
            g2.setColor(Color.WHITE);
            int x = 16, y = 28;
            g2.drawString("life:", x, y);
            int lifeCount = Math.max(0, Math.min(3, state.getLife()));
            int hx = x + 50;
            for (int i = 0; i < lifeCount; i++) if (heartImg != null) g2.drawImage(heartImg, hx + i * 28, y - 18, 24, 24, null);
            String timeStr = String.format("Time limit : %d m %02d s", Math.max(0, state.getTimeLeft())/60, Math.max(0, state.getTimeLeft())%60);
            g2.drawString(timeStr, x, y + 24);
        }
    }

    // 스프라이트 ↔ 모델 어댑터
    private static final class Sprite {
        final Balloon model;
        final BufferedImage img;
        int x, y, w, h; int anchorX, anchorY;
        Sprite(Balloon model, BufferedImage img, int x, int y, int w, int h) {
            this.model = model; this.img = img; this.x = x; this.y = y; this.w = w; this.h = h;
        }
        BalloonSprite toSprite() {
            BalloonSprite bs = new BalloonSprite(model.getWord(), img, x, y, anchorX, anchorY);
            bs.w = w; bs.h = h; return bs;
        }
    }

   // ====== 싱글 규칙 ======
    private final class SingleGameRules implements GameRules {
        @Override public void onTick() { /* GamePanel의 tickTimer가 state.decreaseTime() 호출 */ }
        @Override public void onPop(java.util.List<Balloon> bs) { /* 레벨 클리어는 GamePanel에서 allCleared()로 판단 */ }
        @Override public void onMiss() { state.loseLife(); }
        @Override public boolean isGameOver() { return state.isGameOver(); }
    }

}
