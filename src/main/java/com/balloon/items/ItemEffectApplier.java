package com.balloon.items;

import com.balloon.game.GameState;
import com.balloon.game.model.Balloon;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemEffectApplier {

    public interface TimeApi { void onTimeChanged(); }
    public interface UiApi {
        void toast(String text);
        void blindScreen(int durationMs);   //2P
    }
    public interface FieldApi {
        List<Balloon> getActiveBalloons();
        void addRandomBalloons(int n);
        void removeRandom(int n, int minKeep);
        int minKeep();
    }

    public void apply1P(Item item, GameState state, TimeApi time, UiApi ui, FieldApi field) {
        if (item == null) return;

        switch (item.getKind()) {
            case TIME_PLUS_5 -> {
                state.addTime(5);
                if (time != null) time.onTimeChanged();
                if (ui != null) ui.toast("+5초!");
            }
            case TIME_MINUS_3 -> {
                state.addTime(-3);
                if (time != null) time.onTimeChanged();
                if (ui != null) ui.toast("-3초!");
            }
            case BALLOON_PLUS_2 -> {
                field.addRandomBalloons(2);
                if (ui != null) ui.toast("풍선 +2");
            }
            case BALLOON_MINUS_2 -> {
                field.removeRandom(2, field.minKeep());
                if (ui != null) ui.toast("풍선 -2");
            }
            case DUD -> {
                if (ui != null) ui.toast("광!");
            }
            default -> {/* 2p전용 */}
        }
    }

    //2P 적용
    public void apply2P(Item item,
                        GameState myState, GameState oppState,
                        TimeApi myTime, UiApi myUi, FieldApi myField,
                        UiApi oppUi, FieldApi oppField) {
        if (item == null) return;

        switch (item.getKind()) {
            case SELF_FIELD_PLUS_3 -> {
                myField.addRandomBalloons(3);
                if (myUi != null) myUi.toast("내 필드 +3");
            }
            case SELF_FIELD_MINUS_2 -> {
                myField.removeRandom(2, myField.minKeep());
                if (myUi != null) myUi.toast("내 필드 -2");
            }
            case OPP_FIELD_PLUS_3 -> {
                oppField.addRandomBalloons(3);
                if (oppUi != null) oppUi.toast("상대 필드 +3");
            }
            case OPP_FIELD_MINUS_2 -> {
                oppField.removeRandom(2, oppField.minKeep());
                if (myUi != null) myUi.toast("상대 필드 -2");
                if (oppUi != null) oppUi.toast("필드 -2..");
            }
            case TRICK_BLIND -> {
                if (oppUi != null) oppUi.blindScreen(Math.max(800, item.getDurationMs()));
                if (myUi != null) myUi.toast("상대 시야 방해!");
            }
            case PVP_DUD -> {
                if (myUi != null) myUi.toast("꽝");
            }
            default -> {/*1P전용은 무시*/}
        }
    }

    static int rnd(int bound) {return ThreadLocalRandom.current().nextInt(bound);}
}
