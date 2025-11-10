package com.balloon.ui.render;

import com.balloon.game.model.BalloonSprite;

import java.awt.*;
import java.awt.geom.QuadCurve2D;

public class BalloonSpriteRenderer {

    public void render(Graphics2D g2, BalloonSprite b) {
        if (b.state == BalloonSprite.State.DEAD) return;

        // 1) 줄(실) : 집(anchor) → 풍선 아랫부분(attach)
        int ax = b.anchorX, ay = b.anchorY;
        int bx = b.attachX(), by = b.attachY();
        int cx = (ax + bx) / 2;               // 곡선 제어점(가운데)
        int cy = Math.min(ay, by) - 40;       // 살짝 위로 올려 휘는 느낌

        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255, 255, 255, 220));
        g2.draw(new QuadCurve2D.Float(ax, ay, cx, cy, bx, by));

        // 2) 풍선 PNG
        g2.drawImage(b.img, b.x - b.w/2, b.y - b.h/2, b.w, b.h, null);

        // 3) 텍스트
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();
        int tx = b.x - fm.stringWidth(b.text)/2;
        int ty = b.y + fm.getAscent()/2 - 6;
        g2.drawString(b.text, tx, ty);
    }
    // 줄(실)만 그리기
    public void renderLineOnly(Graphics2D g2, com.balloon.game.model.BalloonSprite b) {
        if (b.state == com.balloon.game.model.BalloonSprite.State.DEAD) return;
        int ax=b.anchorX, ay=b.anchorY, bx=b.attachX(), by=b.attachY();
        int cx=(ax+bx)/2, cy=Math.min(ay,by)-40; // 살짝 휘게
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(255,255,255,220));
        g2.draw(new java.awt.geom.QuadCurve2D.Float(ax, ay, cx, cy, bx, by));
    }

    // 풍선 이미지 + 글자만 그리기
    public void renderBalloonOnly(Graphics2D g2, com.balloon.game.model.BalloonSprite b) {
        if (b.state == com.balloon.game.model.BalloonSprite.State.DEAD) return;
        g2.drawImage(b.img, b.x - b.w/2, b.y - b.h/2, b.w, b.h, null);

        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();
        int tx = b.x - fm.stringWidth(b.text)/2;
        int ty = b.y + fm.getAscent()/2 - 6;
        g2.drawString(b.text, tx, ty);
    }

}
