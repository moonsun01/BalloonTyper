package com.balloon.items;

//아이템 효과를 실제 게임 상태에 반영
public class ItemEffectApplier {

    //시간 조작 API
    public interface TimeApi {
        void addSeconds(int delta);     // 음수 허용
        int getTimeLeft();
    }

    //UI 표시 API
    public interface UiApi {
        void showToast(String message);
        void flashEffect(boolean positive);
    }

    //필드 조작 API
    public interface FieldApi {
        void addBalloons(int n);
        void removeBalloons(int n);
    }

    private final TimeApi timeApi;
    private final UiApi uiApi;
    private final FieldApi fieldApi;

    public ItemEffectApplier(TimeApi timeApi, UiApi uiApi, FieldApi fieldApi) {
        this.timeApi = timeApi;
        this.uiApi = uiApi;
        this.fieldApi = fieldApi;
    }

    //효과 적용 단일 진입점
    public void apply(Item item) {
        if (item == null) return;

        ItemKind kind = item.getKind();
        if (kind == null) return;

        switch (kind) {
            case TIME_PLUS_5 -> {
                if (timeApi != null) timeApi.addSeconds(5);
                if (uiApi != null) {
                    uiApi.showToast("+5초!");
                    uiApi.flashEffect(true);
                }
            }
            case TIME_MINUS_5 -> {
                if (timeApi != null) timeApi.addSeconds(-5);
                if (uiApi != null) {
                    uiApi.showToast("-5초...");
                    uiApi.flashEffect(false);
                }
            }
            case BALLOON_PLUS_2 -> {
                // 싱글/듀얼 공통: 풍선 +2 → 난이도 증가(부정적)
                if (fieldApi != null) fieldApi.addBalloons(2);
                if (uiApi != null) {
                    uiApi.showToast("풍선 +2!");
                    uiApi.flashEffect(false);
                }
            }
            case BALLOON_MINUS_2 -> {
                // 싱글/듀얼 공통: 풍선 -2 → 난이도 감소(긍정적)
                if (fieldApi != null) fieldApi.removeBalloons(2);
                if (uiApi != null) {
                    uiApi.showToast("풍선 -2!");
                    uiApi.flashEffect(true);
                }
            }

            default -> {
                // 아무것도 안 함
            }
        }
    }

}
