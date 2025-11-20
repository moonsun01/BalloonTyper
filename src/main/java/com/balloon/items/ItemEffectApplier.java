package com.balloon.items;

// 아이템 효과를 실제 게임 상태에 반영
public class ItemEffectApplier {

    // 시간 조작 API
    public interface TimeApi {
        void addSeconds(int delta);     // 음수 허용
        int getTimeLeft();
    }

    // UI 표시 API
    public interface UiApi {
        void showToast(String message);
        void flashEffect(boolean positive);
    }

    // 필드 조작 API
    public interface FieldApi {
        void addBalloons(int n);
        void removeBalloons(int n);

        // ★ 추가: 상대에게 reverse 상태를 거는 기능
        default void activateReverseOnOpponent(long durationMillis) {
            // 기본 구현: 아무 것도 안 함 (싱글 모드에서는 필요 없음)
        }
    }

    // 듀얼 모드용 BLIND 콜백
    public interface BlindApi {
        // 아이템 쓴 사람 기준으로 상대방 화면을 BLIND 상태로 만들어라
        void applyBlindToOpponent();
    }

    private final TimeApi timeApi;
    private final UiApi uiApi;
    private final FieldApi fieldApi;
    private final BlindApi blindApi;

    // 기존 생성자 (싱글 모드)
    public ItemEffectApplier(TimeApi timeApi, UiApi uiApi, FieldApi fieldApi) {
        this(timeApi, uiApi, fieldApi, null);
    }

    // BLIND까지 사용하는 생성자 (듀얼 모드)
    public ItemEffectApplier(TimeApi timeApi, UiApi uiApi,
                             FieldApi fieldApi, BlindApi blindApi) {
        this.timeApi = timeApi;
        this.uiApi = uiApi;
        this.fieldApi = fieldApi;
        this.blindApi = blindApi;
    }

    // 효과 적용 단일 진입점
    public void apply(Item item) {
        if (item == null) return;

        ItemKind kind = item.getKind();
        if (kind == null) return;

        switch (kind) {
            case TIME_PLUS_5:
                if (timeApi != null) timeApi.addSeconds(5);
                if (uiApi != null) {
                    uiApi.showToast("+5초!");
                    uiApi.flashEffect(true);
                }
                break;

            case TIME_MINUS_5:
                if (timeApi != null) timeApi.addSeconds(-5);
                if (uiApi != null) {
                    uiApi.showToast("-5초...");
                    uiApi.flashEffect(false);
                }
                break;

            case BALLOON_PLUS_2:
                // 싱글/듀얼 공통: 풍선 +2 → 난이도 증가(부정적)
                if (fieldApi != null) fieldApi.addBalloons(2);
                if (uiApi != null) {
                    uiApi.showToast("풍선 +2!");
                    uiApi.flashEffect(false);
                }
                break;

            case BALLOON_MINUS_2:
                // 싱글/듀얼 공통: 풍선 -2 → 난이도 감소(긍정적)
                if (fieldApi != null) fieldApi.removeBalloons(2);
                if (uiApi != null) {
                    uiApi.showToast("풍선 -2!");
                    uiApi.flashEffect(true);
                }
                break;

            case BLIND:
                // 듀얼 BLIND 아이템
                if (blindApi != null) {
                    blindApi.applyBlindToOpponent();   // 상대 화면 BLIND 3초
                }
                if (uiApi != null) {
                    uiApi.showToast("BLIND! 상대 화면이 가려졌어요");
                    uiApi.flashEffect(true); // 나에겐 좋은 공격 아이템이니까 긍정 연출
                }
                break;

            case REVERSE_5S:
                // 상대에게 일정 시간 reverse 상태 적용
                if (fieldApi != null) {
                    fieldApi.activateReverseOnOpponent(10000); // 10초
                }
                if (uiApi != null) {
                    uiApi.showToast("상대가 10초간 거꾸로 타이핑!");
                    uiApi.flashEffect(true);
                }
                break;

            default:
                // 아무것도 안 함
                break;
        }
    }
}
