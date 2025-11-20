package com.balloon.game.model;

import com.balloon.ui.skin.SecretItemSkin.ItemCategory; // 글자색을 정할 카테고리(시간/풍선/트릭/일반)
import com.balloon.items.Item;

/**
 * Balloon
 * - 게임 내 풍선(단어) 한 개를 표현하는 모델.
 * - Day 9: 풍선 안 글씨 색상을 위해 ItemCategory를 보유하도록 확장.
 */
public class Balloon {

    // 풍선 종류(이미 있던 enum이라고 가정)
    public enum Kind { RED, GREEN, BLUE } // 실제 프로젝트 enum 그대로 두세요.

    // --- 기본 속성 ---
    private final String text;   // 풍선 안에 적힌 단어
    private float x;             // 화면상 X 좌표
    private float y;             // 화면상 Y 좌표
    private final Kind kind;     // 외형(렌더링에 쓰일 수도 있음)
    private Item attachedItem;  // 풍선에 붙은 아이템(없으면 null)


    private boolean active = true;
    // --- [Day 9] 글자색을 위한 카테고리 ---
    private ItemCategory category = ItemCategory.NONE; // 기본은 일반 단어

    // 기존에 사용 중인 생성자(호환 유지)
    public Balloon(String text, float x, float y, Kind kind) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.kind = kind;
    }

    // (선택) 카테고리까지 지정하는 보조 생성자
    public Balloon(String text, float x, float y, Kind kind, ItemCategory category) {
        this(text, x, y, kind);
        this.category = (category != null) ? category : ItemCategory.NONE;
    }

    // --- getters/setters ---
    public String getText() { return text; }

    public float getX() { return x; }
    public float getY() { return y; }

    public void setX(float x) { this.x = x; }
    public void setY(float y) { this.y = y; }

    public Kind getKind() { return kind; }

    //호환 메서드 추가 / GameJudge가 기대하는 api
    public String getWord() { return text; }

    //풍선이 아직 게임중인지 여부(active)
    public boolean isActive() { return active; }

    //풍선을 터뜨려 비활성화
    public void pop() {this.active = false;}


    // [Day 9] 카테고리 접근자
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) {
        this.category = (category != null) ? category : ItemCategory.NONE;
    }

    public Item getAttachedItem() {
        return attachedItem;
    }

    public void setAttachedItem(Item item) {
        this.attachedItem = item;
    }

    /** 아이템을 떼어내서 반환 (한 번만 적용되도록) */
    public Item detachAttachedItem() {
        Item tmp = attachedItem;
        attachedItem = null;
        return tmp;
    }


}
