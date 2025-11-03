package com.balloon.ui.screens;

import com.balloon.core.Showable;
import com.balloon.ui.InputBuffer;

// ★ NEW: 풍선 모델/렌더러/테마 임포트
import com.balloon.game.model.Balloon;
import com.balloon.game.model.Balloon.Kind;
import com.balloon.game.render.BalloonRenderer;
import com.balloon.ui.theme.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * GamePanel :
 * 기존 HUD/입력/키입력 구조는 유지하고,
 * 중앙 플레이 영역만 내부 클래스 PlayField로 바꿔서 풍선 렌더링을 담당시킨다.
 */
public class GamePanel extends JPanel implements Showable {

    // --- 모델 상태 ---
    private final InputBuffer inputBuffer = new InputBuffer();
    private int timeLeft = 60;        // 남은 시간(초)
    private int score = 0;            // 점수
    private int wordIndex = 0;        // 현재 단어 인덱스

    // 임시 단어 리스트(나중에 words.csv 로 바꿀 예정)
    private final List<String> words = List.of(
            "apple","orange","banana","grape","melon",
            "keyboard","monitor","java","swing","balloon"
    );

    // --- UI 컴포넌트 ---
    private final JLabel timeLabel = new JLabel("Time: 60");
    private final JLabel scoreLabel = new JLabel("Score: 0");
    private final JLabel wordLabel  = new JLabel("", SwingConstants.CENTER);   // 현재 타겟 단어
    private final JLabel inputLabel = new JLabel("▶ ", SwingConstants.LEFT);   // 입력 표시
    private final JLabel toastLabel = new JLabel(" ", SwingConstants.CENTER);  // 성공/실패 토스트

    // ★ NEW: 중앙 플레이 영역(풍선 캔버스) 참조
    private PlayField playField;

    private boolean caretOn = true;
    private final Timer caretTimer;   // 입력 캐럿 깜빡임
    private final Timer tickTimer;    // 1초 틱 타이머

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK); // ★ CHANGED: 전체 배경을 Theme 다크 톤으로

        // 상단 HUD
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        // ★ 옵션(디자인 톤 통일): HUD도 다크 톤으로 깔끔하게
        hud.setBackground(Theme.BG_PANEL);                // ★ NEW
        timeLabel.setForeground(Theme.FG_TEXT);           // ★ NEW
        scoreLabel.setForeground(Theme.FG_TEXT);          // ★ NEW
        hud.add(timeLabel);
        hud.add(scoreLabel);
        add(hud, BorderLayout.NORTH);

        // ================================
        // ★ CHANGED: 중앙 플레이 영역 교체
        // 기존 익명 JPanel playArea {...} 블록 전체 삭제하고 PlayField 사용
        // ================================
        playField = new PlayField();                      // ★ NEW: 풍선 렌더 캔버스
        playField.setLayout(new BorderLayout());          // 가운데 단어/아래 토스트 배치
        add(playField, BorderLayout.CENTER);              // 중앙에 추가

        // 중앙에 단어 라벨(크게) 배치
        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        wordLabel.setForeground(Color.WHITE);             // ★ NEW: 다크 배경 대비
        playField.add(wordLabel, BorderLayout.CENTER);

        // 토스트(성공/실패) 라벨
        toastLabel.setForeground(new Color(80, 120, 80));
        toastLabel.setFont(toastLabel.getFont().deriveFont(Font.PLAIN, 16f));
        playField.add(toastLabel, BorderLayout.SOUTH);

        // 하단 입력바
        JPanel inputBar = new JPanel(new BorderLayout());
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        inputBar.setBackground(Theme.BG_PANEL);           // ★ CHANGED: 다크 톤 일관화

        inputLabel.setFont(inputLabel.getFont().deriveFont(Font.BOLD, 16f));
        inputLabel.setForeground(Theme.FG_TEXT);          // ★ NEW
        inputBar.add(inputLabel, BorderLayout.CENTER);

        JLabel hint = new JLabel("타이핑 · Enter=확인 · Backspace=삭제 · Esc=지우기");
        hint.setForeground(new Color(160, 170, 190));     // ★ CHANGED: 다크 톤에서 가독성
        inputBar.add(hint, BorderLayout.EAST);

        add(inputBar, BorderLayout.SOUTH);

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
                if (timeLeft == 0) {
                    gameOver();
                }
            }
        });

        // 첫 단어 표시
        showCurrentWord();
    }

    private void onEnter() {
        String typed = inputBuffer.getText().trim();
        String target = getCurrentWord();

        if (typed.equals(target)) {
            score += 10; // 임시 규칙: 정답 +10
            scoreLabel.setText("Score: " + score);
            showToast("✓ Good!", new Color(25, 155, 75));
            nextWord();
        } else {
            showToast("✗ Miss (" + target + ")", new Color(190, 60, 60));
        }
        inputBuffer.clear();
        refreshInputLabel();
    }

    private void showToast(String msg, Color color) {
        toastLabel.setForeground(color);
        toastLabel.setText(msg);
        // 600ms 후에 흐리게 지우기
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
        tickTimer.stop();
        // TODO: 라우터로 RESULT 화면 전환 (Day5 예정)
    }

    private void grabFocusSafely() {
        requestFocusInWindow();
        // 게임 시작 시 타이머 스타트(중복 스타트 방지)
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
    public void onShown() { // 표시 직후 포커스/타이머 보장
        grabFocusSafely();
    }

    // ==========================================================
    // ★ NEW: 내부 클래스 PlayField
    //  - 중앙 영역을 담당하는 캔버스: 배경(라운드 박스) + 풍선 렌더링 + 애니메이션
    //  - 기존 익명 JPanel을 대체한다.
    // ==========================================================
    private final class PlayField extends JPanel {
        private final BalloonRenderer renderer = new BalloonRenderer();
        private final java.util.List<Balloon> balloons = new java.util.ArrayList<>();
        private final Timer frameTimer; // ~60fps

        PlayField() {
            setOpaque(true);
            setBackground(Theme.BG_DARK);

            // 데모용 풍선 스폰(이후 WordProvider/Spawner로 교체 예정)
            spawnDemoBalloons();

            // 16ms ≈ 60fps 주기로 모델 업데이트 + 화면 리페인트
            frameTimer = new Timer(16, e -> {
                updateModel();
                repaint();
            });
            frameTimer.start();
        }

        private void spawnDemoBalloons() {
            balloons.add(new Balloon("apple", 120, 360, 28, Kind.RED));
            balloons.add(new Balloon("river", 260, 420, 32, Kind.BLUE));
            balloons.add(new Balloon("green", 400, 390, 30, Kind.GREEN));
            balloons.add(new Balloon("delta", 540, 450, 26, Kind.BLUE));
            balloons.add(new Balloon("timer", 680, 410, 28, Kind.RED));
        }

        private void updateModel() {
            for (Balloon b : balloons) b.update();                // 위로 이동
            balloons.removeIf(b -> b.isOffscreenTop(getHeight())); // 화면 밖 제거
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2 = (Graphics2D) g.create();

            // 1) 배경: 라운드 박스 + 그라디언트(살짝 깊이감)
            Paint old = g2.getPaint();
            g2.setPaint(new GradientPaint(0, 0, Theme.BG_DARK, 0, getHeight(), Theme.BG_PANEL));
            g2.fillRoundRect(20, 20, getWidth() - 40, getHeight() - 40, 16, 16);
            g2.setPaint(old);

            // 2) 풍선 렌더링
            renderer.render(g2, balloons);

            g2.dispose();
        }
    }
}
