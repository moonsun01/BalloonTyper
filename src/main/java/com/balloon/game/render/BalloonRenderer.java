package com.balloon.game.render;

import com.balloon.game.model.Balloon;
import com.balloon.ui.theme.Theme;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * BalloonRenderer:
 * - '그리기 전용' 클래스. 모델 데이터를 읽어서 화면에 예쁘게 그린다.
 * - 모델(Balloon)은 상태/이동만 담당, 렌더러는 시각화만 담당(역할 분리).
 */
public class BalloonRenderer {

    /**
     * 풍선 리스트를 화면에 그린다.
     * @param g2        Graphics2D (반드시 paintComponent에서 받은 g를 캐스팅해서 넘길 것)
     * @param balloons  그릴 풍선들
     */
    public void render(Graphics2D g2, List<Balloon> balloons) {
        // 계단현상 줄이기(부드러운 원, 텍스트)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        for (Balloon b : balloons) {
            float cx = b.getX();      // 중심 x
            float cy = b.getY();      // 중심 y
            float r  = b.getRadius(); // 반지름

            // 1) 그림자(아래쪽에 살짝)
            g2.setColor(Theme.BAL_SHADOW);
            g2.fill(new Ellipse2D.Float(cx - r + 3, cy - r + 6, r * 2, r * 2));

            // 2) 본체(원형)
            g2.setColor(b.getFillColor());
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // 3) 하이라이트(좌상단) - 그라디언트로 유리/풍선 느낌
            Paint old = g2.getPaint();
            GradientPaint highlight = new GradientPaint(
                    cx - r, cy - r, new Color(255, 255, 255, 110),
                    cx,     cy,     new Color(255, 255, 255,   0)
            );
            g2.setPaint(highlight);
            g2.fill(new Ellipse2D.Float(cx - r * 0.9f, cy - r * 0.9f, r * 1.2f, r * 1.2f));
            g2.setPaint(old);

            // 4) 테두리
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Theme.BAL_STROKE);
            g2.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));

            // 5) 끈(아래로 한 줄)
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(Math.round(cx), Math.round(cy + r - 4), Math.round(cx), Math.round(cy + r + 10));

            // 6) 텍스트(단어) - 중앙 정렬 + 외곽 그림자
            g2.setFont(Theme.FONT_WORD);
            String text = b.getWord();
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(text);
            int th = fm.getAscent();

            // 텍스트 외곽(그림자) 먼저
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawString(text, Math.round(cx - tw / 2f) + 1, Math.round(cy + th / 2f) + 1);

            // 본문(흰색)
            g2.setColor(Color.WHITE);
            g2.drawString(text, Math.round(cx - tw / 2f), Math.round(cy + th / 2f));
        }
    }
}
