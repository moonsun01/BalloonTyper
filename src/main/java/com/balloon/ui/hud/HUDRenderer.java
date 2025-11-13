package com.balloon.ui.hud;

import com.balloon.core.GameContext;
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;

import java.awt.*;

/**
 * Day10: 우상단에 ‘현재 아이템’ 배지를 그려주는 유틸.
 * GamePanel의 PlayField.paintComponent() 마지막에서 호출.
 */
public final class HUDRenderer {

    private HUDRenderer() {}

    private static final int PAD = 10;
    private static final int RADIUS = 12;
    private static final float BG_ALPHA = 0.60f; // 배경 반투명

    public static void drawCurrentItemBadge(Graphics2D g2, int panelW, int panelH, GameContext ctx) {
        if (g2 == null || ctx == null) return;

        // 컨텍스트에서 현재 활성 아이템 상태 조회
        ItemCategory cat = ctx.getActiveItemCategory();
        String label     = ctx.getActiveItemLabel();
        long remainMs    = ctx.getActiveItemRemainingMs();

        // 없거나 만료되었으면 그리지 않음
        if (cat == null || label == null || remainMs <= 0) return;

        // 남은 시간(s)
        long remainSec = Math.max(0, (remainMs + 999) / 1000);

        // 타입별 색상
        Color dotColor;
        Color borderColor;
        switch (cat) {
            case TIME -> {
                dotColor    = new Color(230, 60, 60);    // 빨강
                borderColor = new Color(190, 70, 70);
            }
            case BALLOON -> {
                dotColor    = new Color(60, 120, 255);   // 파랑
                borderColor = new Color(70, 110, 200);
            }
            default -> {
                dotColor    = new Color(90, 90, 90);     // 회색(기타)
                borderColor = new Color(110, 110, 110);
            }
        }

        // 표시 문자열: "TIME +5s" / "BALLOON -1" (+ 남은초)
        String prefix = (cat == ItemCategory.TIME) ? "TIME " :
                (cat == ItemCategory.BALLOON) ? "BALLOON " : "";
        String text = prefix + label + "  •  " + remainSec + "s";

        // AA 켬
        Object aaOld = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 폰트
        Font oldFont = g2.getFont();
        if (oldFont == null) oldFont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        Font font = oldFont.deriveFont(Font.BOLD, 16f);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();

        // 배지 크기 계산
        int dotSize = 14;
        int textW   = fm.stringWidth(text);
        int textH   = fm.getAscent();

        int badgeW = PAD + dotSize + 8 + textW + PAD;
        int badgeH = PAD + Math.max(dotSize, textH) + PAD;

        int x = panelW - badgeW - 14;
        int y = 14;

        // 배지 배경(반투명)
        Composite compOld = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, BG_ALPHA));
        g2.setColor(Color.BLACK);
        g2.fillRoundRect(x, y, badgeW, badgeH, RADIUS, RADIUS);
        g2.setComposite(compOld);

        // 테두리
        g2.setColor(borderColor);
        g2.setStroke(new BasicStroke(1.6f));
        g2.drawRoundRect(x, y, badgeW, badgeH, RADIUS, RADIUS);

        // 좌측 컬러 점
        int dotX = x + PAD;
        int dotY = y + (badgeH - dotSize) / 2;
        g2.setColor(dotColor);
        g2.fillOval(dotX, dotY, dotSize, dotSize);

        // 텍스트
        g2.setColor(Color.WHITE);
        int textX = dotX + dotSize + 8;
        int textY = y + (badgeH + textH) / 2 - 2;
        g2.drawString(text, textX, textY);

        // 복구
        g2.setFont(oldFont);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aaOld);
    }
}
