package com.balloon.ui.theme; // 디자인 토큰(색/폰트/간격/라운드)을 한 곳에 모아 관리

import java.awt.*; // Color, Font

public final class DesignSpec { // 인스턴스 생성 방지용 유틸 클래스
    public static final boolean DARK = true;                         // 다크 모드 스위치 (라이트로 바꾸려면 false)

    // 브랜드/포인트 컬러
    public static final Color ACCENT = new Color(0x7C9CFF);          // 포인트(버튼 등)
    public static final Color ACCENT_TEXT_ON = Color.WHITE;          // 포인트 배경 위 텍스트색

    // 다크 팔레트
    public static final Color DARK_BG    = new Color(0x0F1115);      // 전체 배경
    public static final Color DARK_CARD  = new Color(0x171A21);      // 카드/패널 배경
    public static final Color DARK_FG    = new Color(0xE6E8EE);      // 기본 텍스트
    public static final Color DARK_MUTE  = new Color(0xA0A4AE);      // 보조 텍스트
    public static final Color DARK_INPUT = new Color(0x22252E);      // 입력 필드 배경
    public static final Color DARK_GRID  = new Color(0x303644);      // 테이블 그리드

    // 라이트 팔레트
    public static final Color LIGHT_BG    = Color.WHITE;             // 전체 배경
    public static final Color LIGHT_CARD  = new Color(0xF5F7FB);     // 카드/패널 배경
    public static final Color LIGHT_FG    = new Color(0x1F2430);     // 기본 텍스트
    public static final Color LIGHT_MUTE  = new Color(0x6B7280);     // 보조 텍스트
    public static final Color LIGHT_INPUT = Color.WHITE;             // 입력 필드 배경
    public static final Color LIGHT_GRID  = new Color(0xE5E7EB);     // 테이블 그리드

    // 라운드/간격
    public static final int RADIUS_BUTTON = 12;                      // 버튼 모서리
    public static final int RADIUS_CARD   = 16;                      // 카드 모서리
    public static final int GAP_XS = 6, GAP_SM = 12, GAP_MD = 16, GAP_LG = 24; // 여백 토큰

    // 폰트 스택
    public static final String FONT_STACK = "Pretendard, Apple SD Gothic Neo, Malgun Gothic, SansSerif"; // 한글 가독성
    public static final Font FONT_H1   = new Font(FONT_STACK, Font.BOLD, 28); // 큰 제목
    public static final Font FONT_H2   = new Font(FONT_STACK, Font.BOLD, 22); // 중간 제목
    public static final Font FONT_BODY = new Font(FONT_STACK, Font.PLAIN, 16);// 본문/버튼
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 14);// 숫자/테이블

    private DesignSpec() {} // 생성자 막기
}
