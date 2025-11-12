package com.balloon.items;

import java.util.concurrent.ThreadLocalRandom;

public class ItemSpawner {
    private int dropPercent = 25;   //전체 드랍 확률

    //1p 가중치
    private int wTimePlus5 = 28, wTimeMinus3 = 18, wBalloonPlus2 = 24, wBalloonMinus2 = 20, wDud = 10;

    //2p 가중치
    private int wSelfPlus3 = 18, wSelfMinus2 = 14, wOppPlus3 = 24, wOppMinus2 = 24, wBlind = 15, wPvpDud = 5;

    public Item rollOnPop1P() {
        if (!roll(dropPercent)) return null;
        int sum = wTimePlus5 + wTimeMinus3 + wBalloonMinus2 + wBalloonPlus2 + wDud;
        int r = rnd(sum);
        if ((r-=wTimePlus5) < 0) return Item.timePlus5();
        if ((r-=wTimeMinus3) < 0) return Item.timeMinus3();
        if ((r-=wBalloonPlus2) < 0) return Item.balloonPlus2();
        if ((r-=wBalloonMinus2) < 0) return Item.balloonMinus2();
        return Item.dud();
    }

    public Item rollOnPop2P() {
        if (!roll(dropPercent)) return null;
        int sum = wSelfPlus3 + wSelfMinus2 + wOppPlus3 + wOppMinus2 + wBlind + wPvpDud;
        int r = rnd(sum);
        if ((r-=wSelfPlus3) < 0) return Item.selfPlus3();
        if ((r-=wSelfMinus2) < 0) return Item.selfMinus2();
        if ((r-=wOppPlus3) < 0) return Item.oppPlus3();
        if ((r-=wOppMinus2) < 0) return Item.oppMinus2();
        if ((r-=wBlind) < 0) return Item.trickBlind(2500);
        return Item.pvpDud();
    }

    private boolean roll(int percent) {return ThreadLocalRandom.current().nextInt(100) < percent;}
    private int rnd(int bound) {return ThreadLocalRandom.current().nextInt(bound);}

    public void setDropPercent(int p) {this.dropPercent = (Math.min(100, p));}
}
