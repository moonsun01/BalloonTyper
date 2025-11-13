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

        //글씨 외곽선
        //g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(0, 0, 0, 0)); //그림자색
        //g2.draw(new QuadCurve2D.Float(ax, ay, cx, cy, bx, by));

        // 2) 풍선 PNG
        g2.drawImage(b.img, b.x - b.w/2, b.y - b.h/2, b.w, b.h, null);

        // 3) 텍스트
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font oldFont = g2.getFont();
        // 풍선 크기에 비례(가독 범위 제한)
        float px = Math.max(12f, Math.min((float)(b.w * 0.28), 28f));
        g2.setFont(oldFont.deriveFont(Font.PLAIN, px));

        String text = (b.text != null) ? b.text : "";
        FontMetrics fm = g2.getFontMetrics();
        int tx = b.x - fm.stringWidth(text)/2;
        int ty = b.y + fm.getAscent()/2 - 6;

        // ★ 본문 색: textColor 우선, 없으면 기본(검정)
        Color base = (b.textColor != null) ? b.textColor : Color.BLACK;
        g2.setColor(base);
        g2.drawString(text, tx, ty);

        // 복원
        g2.setFont(oldFont);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
    }

    // 풍선 안의 글자!!!!!!!!!!!!!!
    // 풍선 이미지 + 글자만 그리기 (아이템 정보 없이 가독성만 개선)
    public void renderBalloonOnly(Graphics2D g2, BalloonSprite b) {
        if (b.state == com.balloon.game.model.BalloonSprite.State.DEAD) return;

        // 1) 풍선 이미지
        if (b.img != null) {
            g2.drawImage(b.img, b.x - b.w / 2, b.y - b.h / 2, b.w, b.h, null);
        }

        // 2) 텍스트
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font oldFont = g2.getFont();
        float px = Math.max(12f, Math.min((float)(b.w * 0.30), 30f)); // 풍선 크기 비례
        g2.setFont(oldFont.deriveFont(Font.PLAIN, px));

        String text = (b.text != null) ? b.text : "";
        FontMetrics fm = g2.getFontMetrics();
        int tx = b.x - fm.stringWidth(text) / 2;
        int ty = b.y + fm.getAscent() / 2 - 6;

        // ★ 본문 색: textColor 우선
        Color base = (b.textColor != null) ? b.textColor : Color.BLACK;
        g2.setColor(base);
        g2.drawString(text, tx, ty);

        g2.setFont(oldFont);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
    }

}
