package com.balloon.items;

public class ItemsSmokeTest {
    public static void main(String[] args) {

        System.out.println("=== RED: TIME 아이템 테스트 ===");
        for (int i = 0; i < 5; i++) {
            Item item = ItemSpawner.roll(ItemKind.ColorKey.RED);
            System.out.println("RED → " + item.kind +
                    " (life=" + item.lifeSec + ", dur=" + item.durationSec + ")");
        }

        System.out.println("\n=== BLUE: 풍선 관련 아이템 테스트 ===");
        for (int i = 0; i < 5; i++) {
            Item item = ItemSpawner.roll(ItemKind.ColorKey.BLUE);
            System.out.println("BLUE → " + item.kind +
                    " (life=" + item.lifeSec + ", dur=" + item.durationSec + ")");
        }

        System.out.println("\n=== GREEN: BLIND/REVERSE 테스트 ===");
        for (int i = 0; i < 5; i++) {
            Item item = ItemSpawner.roll(ItemKind.ColorKey.GREEN);
            System.out.println("GREEN → " + item.kind +
                    " (life=" + item.lifeSec + ", dur=" + item.durationSec + ")");
        }
    }
}
