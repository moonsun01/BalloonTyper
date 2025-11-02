package com.balloon.ui.screens;

import com.balloon.core.Showable;
import com.balloon.ui.InputBuffer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Day3 (2/3): HUD 타이머 + 단어 매칭
 * - 남은 시간 카운트다운
 * - 현재 타겟 단어 표시
 * - Enter로 정답 판정(일치하면 점수++)
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
    private final JLabel wordLabel = new JLabel("", SwingConstants.CENTER); // 현재 타겟 단어
    private final JLabel inputLabel = new JLabel("▶ ", SwingConstants.LEFT); // 입력 표시
    private final JLabel toastLabel = new JLabel(" ", SwingConstants.CENTER); // 성공/실패 토스트

    private boolean caretOn = true;
    private final Timer caretTimer;   // 입력 캐럿 깜빡임
    private final Timer tickTimer;    // 1초 틱 타이머

    public GamePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // 상단 HUD
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 8));
        hud.add(timeLabel);
        hud.add(scoreLabel);
        add(hud, BorderLayout.NORTH);

        // 중앙 플레이 영역
        JPanel playArea = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(245, 247, 252));
                g2.fillRoundRect(20, 20, getWidth() - 40, getHeight() - 40, 16, 16);
            }
        };
        playArea.setBackground(new Color(250, 252, 255));

        wordLabel.setFont(wordLabel.getFont().deriveFont(Font.BOLD, 36f));
        playArea.add(wordLabel, BorderLayout.CENTER);

        toastLabel.setForeground(new Color(80, 120, 80));
        toastLabel.setFont(toastLabel.getFont().deriveFont(Font.PLAIN, 16f));
        playArea.add(toastLabel, BorderLayout.SOUTH);

        add(playArea, BorderLayout.CENTER);

        // 하단 입력바
        JPanel inputBar = new JPanel(new BorderLayout());
        inputBar.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        inputBar.setBackground(Color.WHITE);

        inputLabel.setFont(inputLabel.getFont().deriveFont(Font.BOLD, 16f));
        inputBar.add(inputLabel, BorderLayout.CENTER);

        JLabel hint = new JLabel("타이핑 · Enter=확인 · Backspace=삭제 · Esc=지우기");
        hint.setForeground(new Color(130, 140, 160));
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
    @Override                   // 표시 직후 포커스/타이머 보장
    public void onShown() {
        grabFocusSafely();
    }
}
