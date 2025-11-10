package com.balloon.game.render;

import com.balloon.game.model.Balloon;
import com.balloon.ui.skin.SecretItemSkin;
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;

import java.awt.*;
import java.util.List;

/**
 * BalloonRenderer
 * - 풍선 텍스트를 카테고리 색으로 그린다.
 * - "줄(실)"은 여기서 그리지 않는다(줄은 GamePanel에서 모든 풍선의 줄을 먼저 그리고, 그 다음 풍선을 한꺼번에 그림).
 */
public class BalloonRenderer {

    // 텍스트 폰트(필요하면 프로젝트 폰트로 교체)
    private final Font textFont = new Font(Font.SANS_SERIF, Font.BOLD, 18);

    public void render(Graphics2D g2, List<Balloon> balloons) {
        if (balloons == null || balloons.isEmpty()) return;

        // 안티에일리어싱
        Object oldAA = g2.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font oldFont = g2.getFont();
        Color oldColor = g2.getColor();
        g2.setFont(textFont);

        for (Balloon b : balloons) {
            if (b == null) continue;

            // 1) 텍스트 색상 결정 (아이템이면 카테고리 색, 아니면 검정)
            ItemCategory cat = (b.getCategory() != null) ? b.getCategory() : ItemCategory.NONE;
            Color textColor = SecretItemSkin.textColorOf(cat);
            if (textColor == null) textColor = Color.BLACK;

            // 2) 문자열 그리기(중앙 정렬 보정)
            String t = (b.getText() != null) ? b.getText() : "";
            int tx = (int) b.getX();
            int ty = (int) b.getY();

            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(t);
            int h = fm.getAscent();

            g2.setColor(textColor);
            g2.drawString(t, tx - w / 2, ty + h / 2);
        }

        // 원복
        g2.setColor(oldColor);
        g2.setFont(oldFont);
        if (oldAA != null) g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, oldAA);
    }
}
