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
        if (item == null || !item.isActive()) return;

        switch (item.getKind()) {
            case TIME_PLUS_5 -> {
                timeApi.addSeconds(+5);
                uiApi.showToast("+5초! (남은시간: " + timeApi.getTimeLeft() + "s)");
                uiApi.flashEffect(true);
            }
            case TIME_MINUS_5 -> {
                timeApi.addSeconds(-3);
                uiApi.showToast("-5초… (남은시간: " + timeApi.getTimeLeft() + "s)");
                uiApi.flashEffect(false);
            }
            case BALLOON_PLUS_2 -> {
                new javax.swing.Timer(400, e -> {
                    fieldApi.addBalloons(2);
                    uiApi.showToast("풍선+2!");
                    uiApi.flashEffect(true);
                }) {{ setRepeats(false); start(); }};
            }
            case BALLOON_MINUS_2 -> {
                new javax.swing.Timer(400, e -> {
                    fieldApi.removeBalloons(2);
                    uiApi.showToast("풍선-2!");
                    uiApi.flashEffect(false);
                }) {{ setRepeats(false); start(); }};
            }
        }

        item.deactivate();
    }
}
