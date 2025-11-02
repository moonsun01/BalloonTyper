package com.balloon.core;

/**
 * ScreenId
 * - 각 화면(스크린)을 CardLayout에 등록/전환할 때 사용할 "문자열 키"를 모아둔 클래스
 * - 실수(오타) 방지를 위해 문자열을 하드코딩 하지 않고 상수로 관리
 * - final 클래스로 만들어서 상속을 막고, 생성자를 private으로 막아 인스턴스화를 방지
 */
public final class ScreenId {

    //생성자 private : new ScreenId() 같은 인스턴스 생성이 불가능하도록 막는다.
    private ScreenId(){}

    //시작 화면의 식별자 : 라우터.register(ScreenId.START, startPanel)처럼 사용
    public static final String START = "START";

    //가이드 화면의 식별자
    public static final String GUIDE = "GUIDE";

    //랭킹 화면의 식별자
    public static final String RANKING =  "RANKING";

    //실제 게임 화면의 식별자
    public static final String GAME = "GAME";
}
