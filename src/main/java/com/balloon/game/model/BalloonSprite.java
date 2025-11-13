package com.balloon.game.model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import com.balloon.ui.skin.SecretItemSkin.ItemCategory;
import java.awt.image.BufferedImage;


/** PNG 풍선 + 줄(실) 앵커 좌표 보유 */
public class BalloonSprite {

    public enum State { ALIVE, DEAD }

    public final String text;
    public final BufferedImage img;

    public int x, y;            // 풍선 중심 좌표
    public int w = 96, h = 78; // 그릴 크기(이미지 스케일) //풍선 크기
    // 이미 있는 필드들 예: public String text; public BufferedImage img; ...
    public java.awt.Color textColor = null;   // ★ 기본 글자색(아이템이면 여기서 바뀜)

    public int anchorX, anchorY;    // 줄이 연결될 집 지붕 좌표
    public int tailOffset = 6;      // 이미지 아래쪽에서 줄이 붙는 오프셋(px)

    public State state = State.ALIVE;

    // [UI-only] 아이템 시각화를 위한 선택 필드(없으면 NORMAL/0으로 둔다)
    public ItemCategory category = ItemCategory.NONE;
    public int itemValue = 0;



    public BalloonSprite(String text, BufferedImage img, int x, int y,
                         int anchorX, int anchorY) {
        this.text = text;
        this.img = img;
        this.x = x; this.y = y;
        this.anchorX = anchorX; this.anchorY = anchorY;
    }

    public int attachX() { return x; }
    public int attachY() { return y + h/2 - tailOffset; } // 풍선 아랫부분 근처


}
