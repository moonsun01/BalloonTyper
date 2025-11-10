package com.balloon.game.model;

import java.awt.image.BufferedImage;

/** PNG 풍선 + 줄(실) 앵커 좌표 보유 */
public class BalloonSprite {
    public enum State { ALIVE, DEAD }

    public final String text;
    public final BufferedImage img;

    public int x, y;            // 풍선 중심 좌표
    public int w = 96, h = 78; // 그릴 크기(이미지 스케일) //풍선 크기
    public int anchorX, anchorY;    // 줄이 연결될 집 지붕 좌표
    public int tailOffset = 6;      // 이미지 아래쪽에서 줄이 붙는 오프셋(px)

    public State state = State.ALIVE;

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
