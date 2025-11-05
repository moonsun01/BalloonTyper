package com.balloon.ui.screens;

import com.balloon.core.*;
import com.balloon.ui.InputBuffer;

import com.balloon.game.model.Balloon;
import com.balloon.game.model.Balloon.Kind;
import com.balloon.game.render.BalloonRenderer;
import com.balloon.ui.theme.Theme;

import com.balloon.core.ResultContext;
import com.balloon.core.ResultData;
import com.balloon.core.ScreenId;


import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * GamePanel :
 * 중앙 플레이 영역은 내부 클래스 PlayField가 풍선 렌더링/업데이트 담당.
 */
public class GamePanel extends JPanel implements Showable {

    // --- 모델 상태 ---
    private final InputBuffer inputBuffer = new InputBuffer();
    private int timeLeft = 60;        // 남은 시간(초)
    private int score = 0;            // 점수
    private int wordIndex = 0;        // 현재 타겟 단어 인덱스(라벨용)
    private int stage = 1; //현재 스테이지 (1 -> 2 -> 3)
    public static int lastCompletedStage = 1;
    private final ScreenRouter router;

    private final JLabel overlayLabel = new JLabel(" ", SwingConstants.CENTER); // Success/Ready 오버레이 텍스트
    private final Timer overlayTimer = new Timer(1200, e -> overlayLabel.setVisible(false)); // 오버레이 표시 타이머(1.2s)
    private boolean stageClearedThisRound = false; // 한 스테이지 내 중복 성공 호출 방지

    // 임시 단어 리스트(후에 words.csv로 교체 예정)
    private final List<String> words = List.of(
            "apple","orange","banana","grape","melon",
            "keyboard","monitor","java","swing","balloon",
            "house","safety","ground","landing","rope",
            "cloud","river","green","delta","timer",
            "purple","queen","sun","tree","unity",
            "water","xenon","youth","zebra","type"
    );

    // --- UI 컴포넌트 ---
    private final JLabel timeLabel = new JLabel("Time: 60");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel wordLabel  = new JLabel("", SwingConstants.CENTER);   // 가이드용 타겟 단어
    private final JLabel inputLabel = new JLabel("▶ ", SwingConstants.LEFT);   // 입력 표시
    private final JLabel toastLabel = new JLabel(" ", SwingConstants.CENTER);  // 성공/실패 토스트

    // 중앙 플레이 영역(풍선 캔버스)
    private final PlayField playField;

    private boolean caretOn = true;
    private final Timer caretTimer;   // 입력 캐럿 깜빡임
    private final Timer tickTimer;    // 1초 틱 타이머

    public GamePanel(ScreenRouter router) {
        this.router = router;                          // ★ 주입된 라우터 저장

        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);

        // 상단 HUD
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        hud.setBackground(Theme.BG_PANEL);
        timeLabel.setForeground(Theme.FG_TEXT);
        scoreLabel.setForeground(Theme.FG_TEXT);
        hud.add(timeLabel);
        hud.add(scoreLabel);
        add(hud, BorderLayout.NORTH);

        // 중앙 플레이 영역
        playField = new PlayField();
        playField.setLayout(new BorderLayout());
        add(playField, BorderLayout.CENTER);

        // 중앙 단어 라벨(가이드)
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        wordLabel.setForeground(Color.WHITE);
        playField.add(wordLabel, BorderLayout.CENTER);
        wordLabel.setVisible(false); // Day5: 중앙 가이드 비표시

        // 토스트 라벨
        toastLabel.setForeground(new Color(80, 120, 80));
        toastLabel.setFont(toastLabel.getFont().deriveFont(Font.PLAIN, 16f));
        playField.add(toastLabel, BorderLayout.SOUTH);

        // 하단 입력바
        JPanel inputBar = new JPanel(new BorderLayout());
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        inputBar.setBackground(Theme.BG_PANEL);

        inputLabel.setFont(inputLabel.getFont().deriveFont(Font.BOLD, 16f));
        inputLabel.setForeground(Theme.FG_TEXT);
        inputBar.add(inputLabel, BorderLayout.CENTER);

        JLabel hint = new JLabel("타이핑 · Enter=확인 · Backspace=삭제 · Esc=지우기");
        hint.setForeground(new Color(160, 170, 190));
        inputBar.add(hint, BorderLayout.EAST);
        add(inputBar, BorderLayout.SOUTH);

        // ★ 오버레이 라벨 (Success/Ready 공용)
        overlayLabel.setFont(overlayLabel.getFont().deriveFont(Font.BOLD, 42f));
        overlayLabel.setForeground(new Color(255, 255, 160));
        overlayLabel.setVisible(false);
        playField.add(overlayLabel, BorderLayout.NORTH);
        overlayTimer.setRepeats(false);

        // 키 입력
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                final int code = e.getKeyCode();
                if (code == KeyEvent.VK_BACK_SPACE) {
                    inputBuffer.backspace();
                    refreshInputLabel();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    inputBuffer.clear();
                    refreshInputLabel();
                } else if (code == KeyEvent.VK_ENTER) {
                    onEnter();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isISOControl(c)) {
                    inputBuffer.appendChar(c);
                    refreshInputLabel();
                }
            }
        });

        setFocusable(true);
        SwingUtilities.invokeLater(this::grabFocusSafely);

        // 캐럿 타이머
        caretTimer = new Timer(500, e -> { caretOn = !caretOn; refreshInputLabel(); });
        caretTimer.start();

        // 1초 틱 타이머 (시간 감소)
        tickTimer = new Timer(1000, e -> {
            if (timeLeft > 0) {
                timeLeft--;
                timeLabel.setText("Time: " + timeLeft);
                if (timeLeft == 0 && !playField.isAllCleared()) {
                    onStageFailed(); // ★ 실패: 즉시 Ranking
                }
            }
        });

        // 첫 단어 표시
        showCurrentWord();
    }


    // Enter 처리: 입력 단어로 풍선 팝 시도 → 점수/토스트
    private void onEnter() {
        String typed = inputBuffer.getText().trim();
        if (typed.isEmpty()) {
            showToast("✗ Miss", new Color(190, 60, 60));
            return;
        }

        boolean popped = playField.popFirstByText(typed); // 풍선 제거 시도
        if (popped) {
            score += 10; // 항상 +10점
            scoreLabel.setText("Score: " + score);
            showToast("✓ Pop!", new Color(25, 155, 75));

            //모든 풍선 제거 시 스테이지 성공 처리
            if(playField.isAllCleared()){
                onStageCleared(); // ★ 성공 흐름으로
                return;
            }
        } else {
            showToast("✗ Miss", new Color(190, 60, 60));
        }

        inputBuffer.clear();
        refreshInputLabel();
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
        setStage(stage); //여기가 단계별 SWITCH
    }

    // [D6-3] 시간 종료 시 결과 화면으로 전환
    private void gameOver() {
        showToast("Time Over!", new Color(90, 90, 90)); // 토스트 출력
        if (tickTimer.isRunning()) tickTimer.stop();    // 1초 타이머 정지
        // 캐럿/프레임 타이머는 onHidden()에서 정리됨

        //방금 끝낸 스테이지를 기록 -> ResultScreen의 Retry분기에 사용
        lastCompletedStage = stage;

        showResult(); // ★ 점수/시간만으로 결과 전달(정답/오답 카운터 없이)
    }


    private void grabFocusSafely() {
        requestFocusInWindow();
        if (!tickTimer.isRunning()) tickTimer.start();
    }

    private void refreshInputLabel() {
        String text = inputBuffer.getText();
        String caret = caretOn ? "▌" : " ";
        inputLabel.setText("▶ " + text + " " + caret);
        repaint();
    }

    /** 라우터가 카드 전환 직후 호출해 포커스/타이머를 보장 */
    @Override
    public void onShown() {
        grabFocusSafely();
    }

    // Showable에 onHidden()이 없을 수 있으므로 @Override 제거
    public void onHidden() {
        if (tickTimer.isRunning()) tickTimer.stop();
        if (caretTimer.isRunning()) caretTimer.stop();
        if (playField != null) playField.stop();

        //resetGame(); // [D6-4] 화면이 숨겨질 때 상태 초기화
    }


    // ==========================================================
    // 내부 클래스 PlayField : 풍선 30개 일괄 생성 + 렌더/업데이트
    // ==========================================================
    private final class PlayField extends JPanel {
        private final BalloonRenderer renderer = new BalloonRenderer();
        private final ArrayList<Balloon> balloons = new ArrayList<>();
        private final Random rnd = new Random();
        private final Timer frameTimer; // ~60fps

        PlayField() {
            setOpaque(true);
            setBackground(Theme.BG_DARK);

            // 크기 확정 뒤 1회성으로 30개 스폰
            SwingUtilities.invokeLater(this::spawnInitialBalloons);

            // 16ms ≈ 60fps 주기로 모델 업데이트 + 화면 리페인트
            frameTimer = new Timer(16, e -> {
                updateModel();
                repaint();
            });
            frameTimer.start();
        }

        /** 게임 컨셉: 처음에 30개 풍선을 한꺼번에 생성 */
        private void spawnInitialBalloons() {
            balloons.clear();
            final int total = 30;
            int w = Math.max(getWidth(), 800);   // 레이아웃 전 0일 수 있어 디폴트 보정
            int h = Math.max(getHeight(), 500);

            int margin = 40;
            for (int i = 0; i < total; i++) {
                String text = words.get(rnd.nextInt(words.size()));

                int x = margin + rnd.nextInt(Math.max(1, w - margin * 2));
                int y = h - margin - rnd.nextInt(120); // 바닥에서 약간 위 랜덤

                int speed = 26 + rnd.nextInt(8); // 26~33 정도
                Kind kind = Kind.RED;            // NORMAL이 없으므로 기본 RED로 세팅

                balloons.add(new Balloon(text, x, y, speed, kind));
            }
        }

        private void updateModel() {
            // Day5-Static: 풍선은 이동하지 않음. 업데이트/제거 없음.
            // (나중에 Day6에서 팝 이펙트만 잠깐 쓸 때 여기서 처리)
        }


        /** 입력 텍스트(대소문자 무시)와 같은 첫 풍선을 제거 */
        boolean popFirstByText(String text) {
            if (text == null || text.isEmpty()) return false;
            for (int i = 0; i < balloons.size(); i++) {
                Balloon b = balloons.get(i);
                String bt = readBalloonText(b);
                if (bt.equalsIgnoreCase(text)) {
                    balloons.remove(i);
                    // TODO: Day6 - 팝 이펙트/사운드
                    return true;
                }
            }
            return false;
        }

        boolean isAllCleared() {
            return balloons.isEmpty();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();

            // 1) 배경: 라운드 박스 + 그라디언트
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(0, 0, Theme.BG_DARK, 0, getHeight(), Theme.BG_PANEL));
            g2.fillRoundRect(20, 20, getWidth() - 40, getHeight() - 40, 16, 16);
            g2.setPaint(old);

            // 2) 풍선 렌더링 (리스트로 전달)
            renderer.render(g2, balloons);

            g2.dispose();
        }

        void stop() {
            if (frameTimer != null) frameTimer.stop();
        }

        /** Balloon의 텍스트를 API에 상관없이 읽어오는 유틸(리플렉션 사용) */
        private String readBalloonText(Balloon b) {
            try { var m = b.getClass().getMethod("getText"); Object r = m.invoke(b); if (r != null) return r.toString(); } catch (Exception ignored) {}
            try { var m = b.getClass().getMethod("getWord"); Object r = m.invoke(b); if (r != null) return r.toString(); } catch (Exception ignored) {}
            try { var m = b.getClass().getMethod("getLabel"); Object r = m.invoke(b); if (r != null) return r.toString(); } catch (Exception ignored) {}
            try { var f = b.getClass().getDeclaredField("text"); f.setAccessible(true); Object r = f.get(b); if (r != null) return r.toString(); } catch (Exception ignored) {}
            try { var f = b.getClass().getDeclaredField("word"); f.setAccessible(true); Object r = f.get(b); if (r != null) return r.toString(); } catch (Exception ignored) {}
            try { var f = b.getClass().getDeclaredField("label"); f.setAccessible(true); Object r = f.get(b); if (r != null) return r.toString(); } catch (Exception ignored) {}
            return "";
        }
    }
    /** 스테이지에 따라 시작 시간 설정: 1=90s, 2=80s, 3=70s */
    public void setStage(int stage) {
        int t;
        switch (stage) {
            case 1 -> t = 90;
            case 2 -> t = 80;
            default -> t = 70;
        }
        this.timeLeft = t;
        this.timeLabel.setText("Time: " + t);
    }

    // [D6-3] 결과 컨텍스트 세팅 → Result 화면으로 전환(정답/오답 카운터 없이)
    private void showResult() {
        // [1] 정확도/카운터는 사용하지 않으므로 0으로 채움
        double accuracy   = 0.0; // 정답률 미집계 → 0.0
        int correctCount  = 0;   // 정답 수 미집계 → 0
        int wrongCount    = 0;   // 오답 수 미집계 → 0

        // [2] 결과 패킷 구성 (현재 필드 score/timeLeft 사용)
        ResultData data = new ResultData(
                score,                         // 최종 점수
                Math.max(0, timeLeft),         // 남은 시간(음수 방지)
                accuracy,                      // 정확도(0.0)
                correctCount,                  // 맞힌 개수(0)
                wrongCount                     // 틀린 개수(0)
        );

        // [3] 컨텍스트 저장
        ResultContext.set(data);

        // [4] 화면 전환
        if (router != null) {
            router.show(ScreenId.RESULT);      // 결과 화면으로 이동
        } else {
            System.err.println("[GamePanel] router is null → cannot navigate to RESULT");
        }
    }

    // [D6-4+] GamePanel 초기화(시간/점수/입력/풍선 재생성 + 자동 다음 스테이지)
    private void resetGame() {
        // 1. 다음 스테이지로 증가 (3 초과면 다시 1로)
        stage++;
        if (stage > 3) stage = 1;

        // 2. 스테이지에 따른 시간 재설정
        setStage(stage); // ★ 기존 함수 활용 (1=90, 2=80, 3=70초)

        // 3. 점수 리셋
        score = 0;
        scoreLabel.setText("Score: 0");

        // 4. 입력창 초기화
        inputBuffer.clear();
        refreshInputLabel();

        // 5. 풍선 리셋 (기존 풍선 제거 후 새로 스폰)
        if (playField != null) {
            playField.balloons.clear();
            playField.spawnInitialBalloons();
        }

        // 6. 포커스 복구 & 타이머 재시작
        caretOn = true;
        if (!tickTimer.isRunning()) tickTimer.start();
        if (!caretTimer.isRunning()) caretTimer.start();
        grabFocusSafely();

        // 7. 토스트 표시 (시각적 확인용)
        showToast("Stage " + stage + " Start!", new Color(100, 200, 100));
    }

    // ─────────────── 스테이지 성공/실패/전환 플로우 ───────────────

    // 스테이지 성공 시 호출 (모든 풍선 제거)
    private void onStageCleared() {
        if (stageClearedThisRound) return;   // 중복 방지
        stageClearedThisRound = true;

        // 1) 남은 시간 보너스(1초=1점) 즉시 합산
        if (timeLeft > 0) {
            score += timeLeft;
            scoreLabel.setText("Score: " + score);
        }

        // 2) 성공 오버레이 표시
        showOverlay("✔ SUCCESS!  (+ " + Math.max(0, timeLeft) + ")", new Color(110, 220, 110));

        // 3) 마지막 스테이지 성공이면 Ranking으로, 아니면 Ready → 다음 스테이지 시작
        lastCompletedStage = stage;

        if (stage >= 3) {
            Timer t = new Timer(200, e -> showRanking());
            t.setRepeats(false);
            t.start();
        } else {
            // 다음 스테이지 준비 → 시작 (각각 한번만)
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

    // 스테이지 실패(시간 0인데 풍선 남음) → 바로 랭킹
    private void onStageFailed() {
        // 실패 오버레이(짧게)
        showOverlay("✖ FAILED!  (Stage " + stage + ")", new Color(230, 90, 90));
        // 바로 랭킹으로
        new Timer(500, e -> showRanking()).start();
    }

    // 다음 스테이지로 넘어가 실제 시작
    private void startNextStage() {
        stageClearedThisRound = false;

        if (stage >= 3) { // 방어: 혹시라도 3 이상이면 증가 금지
            showRanking();
            return;
        }

        stage++; //1->2, 2->3
        setStage(stage); // 1=90, 2=80, 3=70

        // 풍선 재생성
        if (playField != null) {
            playField.balloons.clear();
            playField.spawnInitialBalloons();
        }

        // 타이머/포커스 재시작
        caretOn = true;
        if (!tickTimer.isRunning()) tickTimer.start();
        if (!caretTimer.isRunning()) caretTimer.start();
        grabFocusSafely();

        showToast("Stage " + stage + " Start!", new Color(100, 200, 100));
    }

    // 중앙 상단 오버레이 공용 표시
    private void showOverlay(String text, Color color) {
        overlayLabel.setText(text);
        overlayLabel.setForeground(color);
        overlayLabel.setVisible(true);
        overlayTimer.restart(); // 1.2s 후 자동 숨김
    }

    // 누적 점수로 Ranking 화면으로 이동
    private void showRanking() {
        // ResultData로 기본 필드 채워서 넘김(정확도/정답/오답은 0 유지)
        double accuracy = 0.0;
        int correct = 0;
        int wrong = 0;

        ResultData data = new ResultData(
                score,                      // 누적 점수(보너스 포함)
                Math.max(0, timeLeft),      // 현재 남은 시간(정보용)
                accuracy,
                correct,
                wrong
        );

        ResultContext.set(data);
        if (router != null) {
            router.show(ScreenId.RANKING);
        } else {
            System.err.println("[GamePanel] router is null → cannot navigate to RANKING");
        }
    }


}
