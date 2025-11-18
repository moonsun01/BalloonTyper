package com.balloon.ui.screens;

import com.balloon.core.GameMode;
import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;
import com.balloon.data.CsvRankingRepository;
import com.balloon.data.ScoreEntry;
import com.balloon.ui.assets.ImageAssets;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ResultScreen extends JPanel {

    private final ScreenRouter router;

    // ====== 결과 값 ======
    private int totalScore;       // 누적 총점
    private double accuracyPct;   // 정확도(%)
    private int timeLeftSec;      // 남은 시간(초)

    // ====== UI 라벨 ======
    private JLabel titleLabel;
    private JLabel accuracyLabel;
    private JLabel scoreLabel;
    private JLabel timeLabel;

    // ====== 배경 / 풍선 ======
    private BufferedImage bgImage;
    private final List<BalloonIcon> balloons = new ArrayList<>();
    private final Random rnd = new Random();

    // ====== 폰트 유틸 ======
    private static Font fontXL() {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(Font.BOLD, 40f);
    }

    private static Font fontBig() {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(Font.BOLD, 36f);
    }

    private static Font fontM() {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(Font.BOLD, 22f);
    }

    private static Font fontButton() {
        Font base = UIManager.getFont("Button.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(Font.BOLD, 18f);
    }

    // =====================================================
    //  생성자
    // =====================================================
    public ResultScreen(ScreenRouter router) {
        this.router = router;

        setLayout(new BorderLayout());
        setOpaque(false); // 배경 직접 그림

        // 현재까지 클리어한 스테이지에 맞게 배경 이미지 선택
        updateBackgroundByStage();

        // ---------- 상단 타이틀 ----------
        titleLabel = new JLabel("RESULT", SwingConstants.CENTER);
        titleLabel.setFont(fontXL());
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(24, 0, 0, 0));
        add(titleLabel, BorderLayout.NORTH);

        // ---------- 중앙 정보 영역 ----------
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        // 텍스트 블록을 조금 아래로
        center.add(Box.createVerticalStrut(120));

        accuracyLabel = new JLabel("정확도: 0.0%", SwingConstants.CENTER);
        accuracyLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        accuracyLabel.setFont(fontBig());
        accuracyLabel.setForeground(Color.WHITE);

        scoreLabel = new JLabel("총점: 0", SwingConstants.CENTER);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreLabel.setFont(fontBig());
        scoreLabel.setForeground(Color.WHITE);

        timeLabel = new JLabel("남은 시간: 0초", SwingConstants.CENTER);
        timeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeLabel.setFont(fontM());
        timeLabel.setForeground(Color.WHITE);

        center.add(accuracyLabel);
        center.add(Box.createVerticalStrut(18));
        center.add(scoreLabel);
        center.add(Box.createVerticalStrut(12));
        center.add(timeLabel);
        center.add(Box.createVerticalGlue());

        add(center, BorderLayout.CENTER);

        // ---------- 하단 버튼 영역 ----------
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 6));
        south.setOpaque(false);
        south.setBorder(new EmptyBorder(0, 0, 20, 0));  // 버튼 살짝 위로

        JButton toRanking = createCuteButton("랭킹 보기", new Color(255, 236, 236));
        JButton retry     = createCuteButton("다시 하기", new Color(236, 255, 236));
        JButton toMain    = createCuteButton("메인으로", new Color(232, 234, 255));

        toRanking.addActionListener(e -> router.show(ScreenId.RANKING));
        retry.addActionListener(e -> router.show(ScreenId.GAME));
        toMain.addActionListener(e -> router.show(ScreenId.START));

        south.add(toRanking);
        south.add(retry);
        south.add(toMain);

        add(south, BorderLayout.SOUTH);

        // 처음 한 번 라벨 업데이트
        refreshUI();
    }

    // =====================================================
    //  귀여운 버튼 + hover 애니메이션
    // =====================================================

    private JButton createCuteButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(fontButton());
        btn.setForeground(new Color(40, 40, 60));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setBorder(new EmptyBorder(10, 26, 10, 26));

        btn.putClientProperty("hoverProgress", 0.0f);

        btn.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                float p = (float) c.getClientProperty("hoverProgress");
                if (Float.isNaN(p)) p = 0f;

                // 1.0 ~ 1.06 정도로 살짝 커지게
                float scale = 1.0f + 0.06f * p;
                int scaledW = (int) (w * scale);
                int scaledH = (int) (h * scale);
                int x = (w - scaledW) / 2;
                int y = (h - scaledH) / 2;

                int arc = 26;

                // 그림자
                g2.setColor(new Color(180, 180, 200, 150));
                g2.fillRoundRect(x, y + 3, scaledW, scaledH, arc + 6, arc + 6);

                // 본체
                g2.setColor(bgColor);
                g2.fillRoundRect(x, y, scaledW, scaledH, arc, arc);

                // 테두리
                g2.setStroke(new BasicStroke(2f));
                g2.setColor(new Color(190, 195, 220));
                g2.drawRoundRect(x, y, scaledW, scaledH, arc, arc);

                // 텍스트
                super.paint(g2, c);
                g2.dispose();
            }
        });

        // hover 애니메이션
        final int fps = 60;
        final int durationMs = 120;
        final int steps = durationMs * fps / 1000;
        final Timer timer = new Timer(1000 / fps, null);
        final float[] target = {0f};

        timer.addActionListener(e -> {
            float current = (float) btn.getClientProperty("hoverProgress");
            float step = 1f / steps;
            if (target[0] > current) {
                current = Math.min(target[0], current + step);
            } else if (target[0] < current) {
                current = Math.max(target[0], current - step);
            }
            btn.putClientProperty("hoverProgress", current);
            btn.repaint();
            if (current == target[0]) {
                timer.stop();
            }
        });

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                target[0] = 1f;
                if (!timer.isRunning()) timer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                target[0] = 0f;
                if (!timer.isRunning()) timer.start();
            }
        });

        return btn;
    }

    // =====================================================
    //  GamePanel 에서 결과 전달
    // =====================================================

    /**
     * GamePanel에서 직접 전달받는 최종 결과
     */
    public void setResult(int totalScore, double accuracyRatio, int timeLeft) {
        this.totalScore = totalScore;
        this.accuracyPct = accuracyRatio * 100.0; // %로 변환
        this.timeLeftSec = Math.max(0, timeLeft);

        // 스테이지에 맞는 배경 다시 선택
        updateBackgroundByStage();

        refreshUI();
    }

    /**
     * 랭킹 저장용 공개 메서드
     * GamePanel 쪽에서:
     *
     *   rs.setResult(...);
     *   rs.saveRanking(playerName, currentMode);
     *
     * 이런 식으로 호출하면 돼.
     */
    public void saveRanking(String playerName, GameMode mode) {
        CsvRankingRepository repo = new CsvRankingRepository();
        ScoreEntry entry = new ScoreEntry(playerName, totalScore, mode);
        // 아래 append 메서드는 CsvRankingRepository에 추가(2번에서 코드 줌)
        repo.append(entry);
    }

    // 기존 구조 유지 (사용 안 함)
    public void setBreakdown(int wordScore, int timeBonus, int accuracyScore, int itemBonus) {
        // 점수 구성요소를 사용하지 않는 버전이므로 빈 메서드 유지
    }

    private void refreshUI() {
        if (accuracyLabel != null) {
            accuracyLabel.setText(String.format("정확도: %.1f%%", accuracyPct));
        }
        if (scoreLabel != null) {
            scoreLabel.setText("총점: " + totalScore);
        }
        if (timeLabel != null) {
            timeLabel.setText("남은 시간: " + timeLeftSec + "초");
        }
        repaint();
    }

    // =====================================================
    //  배경 선택 + 풍선 랜덤 배치
    // =====================================================

    // ★ GamePanel은 같은 패키지(com.balloon.ui.screens)에 있다고 가정.
    //   → import 필요 없음, 그냥 GamePanel.lastCompletedStage 사용.
    private void updateBackgroundByStage() {
        int stage = GamePanel.lastCompletedStage;   // GamePanel에 public static int lastCompletedStage; 가 있어야 함

        String imgName = switch (stage) {
            case 1 -> "bg_level1.png";
            case 2 -> "bg_level2.png";
            case 3 -> "bg_level3.png";
            default -> "bg_level3.png";
        };

        bgImage = ImageAssets.load(imgName);
        balloons.clear();
    }

    // 풍선 한 개 정보
    private static class BalloonIcon {
        BufferedImage img;
        int x, y, w, h;

        BalloonIcon(BufferedImage img, int x, int y, int w, int h) {
            this.img = img;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    // 현재 패널 크기에 맞춰 풍선들을 랜덤으로 배치 (작고 귀엽게)
    private void ensureBalloonsLayout(int width, int height) {
        if (!balloons.isEmpty() || width <= 0 || height <= 0) return;

        BufferedImage[] balloonImgs = new BufferedImage[]{
                ImageAssets.load("balloon_yellow.png"),
                ImageAssets.load("balloon_pink.png"),
                ImageAssets.load("balloon_purple.png"),
                ImageAssets.load("balloon_green.png"),
                ImageAssets.load("balloon_orange.png")
        };

        int count = 8;
        int minSize = 25;
        int maxSize = 34;

        for (int i = 0; i < count; i++) {
            BufferedImage img = balloonImgs[rnd.nextInt(balloonImgs.length)];
            if (img == null) continue;

            int size = minSize + rnd.nextInt(maxSize - minSize + 1);
            int x = rnd.nextInt(Math.max(1, width - size - 40)) + 20;
            int y = rnd.nextInt(Math.max(1, height - size - 220)) + 40;

            balloons.add(new BalloonIcon(img, x, y, size, size));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // 배경 이미지
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, w, h, this);
        } else {
            g2.setColor(new Color(10, 20, 60));
            g2.fillRect(0, 0, w, h);
        }

        // 풍선 그리기
        ensureBalloonsLayout(w, h);
        for (BalloonIcon b : balloons) {
            if (b.img != null) {
                g2.drawImage(b.img, b.x, b.y, b.w, b.h, this);
            }
        }

        g2.dispose();

        // 나머지 컴포넌트(라벨/버튼) 그리기
        super.paintComponent(g);
    }
}
