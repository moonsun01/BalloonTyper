package com.balloon.ui.theme;

import javax.swing.*;
import java.awt.*;

/**
 * Theme: 전역 '디자인 토큰(색상/폰트)'을 보관하고, UIManager에 주입하는 유틸 클래스.
 * - 장점: 화면 코드 수정 없이 여기 값만 바꿔도 전역으로 일괄 반영.
 * - 지금은 기본(임시) 다크톤/표준 폰트. 6일차에 실제 디자인 가이드 나오면 값만 교체하면 됨.
 */
public final class Theme {

    // 외부에서 new Theme() 못 하도록 막음 (유틸리티 전용)
    private Theme() { }

    // =========================
    // 1) 색상 팔레트 (임시 기본안)
    // =========================

    // 배경: 눈부심을 줄이는 다크 톤(컨테이너/패널)
    public static final Color BG_DARK  = new Color(18, 18, 22);  // 최하위 배경
    public static final Color BG_PANEL = new Color(28, 28, 34);  // 상위 패널/섹션

    // 기본 텍스트: Pure white(255,255,255)보다 눈 피로가 덜한 오프화이트
    public static final Color FG_TEXT  = new Color(235, 235, 245);

    // 게임 아이템 규칙과 연결:
    //  - RED   => time ± (시간 관련)
    //  - BLUE  => balloon ± (풍선 수 관련)
    //  - GREEN => 방해/디버프 계열
    public static final Color BAL_RED   = new Color(234, 84, 85);
    public static final Color BAL_BLUE  = new Color(80, 156, 245);
    public static final Color BAL_GREEN = new Color(75, 203, 148);

    // 풍선 테두리 및 그림자(입체감)
    public static final Color BAL_STROKE = new Color(0, 0, 0, 120);
    public static final Color BAL_SHADOW = new Color(0, 0, 0, 70);

    // HUD(타이머/스코어) 강조 컬러: 시인성 높은 노란 포인트
    public static final Color HUD_ACCENT = new Color(255, 214, 10);

    // =========================
    // 2) 폰트(시스템 기본 SansSerif 사용)
    // =========================
    //  - 커스텀 폰트 파일 의존 X → 배포/환경 차이 문제 최소화
    public static final Font FONT_UI   = new Font(Font.SANS_SERIF, Font.PLAIN, 14); // 기본 UI
    public static final Font FONT_HUD  = new Font(Font.SANS_SERIF, Font.BOLD, 18);  // HUD/타이틀
    public static final Font FONT_WORD = new Font(Font.SANS_SERIF, Font.BOLD, 16);  // 풍선 단어

    /**
     * 전역 UIManager에 색/폰트를 한번에 주입.
     * 주의: 반드시 프레임/컴포넌트 생성 '전에' 호출해야 전역 적용됨.
     * 보통 런처(Launcher.main)에서 LookAndFeel 설정 직후 호출.
     */
    public static void applyGlobalUI() {
        // 공통 컴포넌트 톤
        UIManager.put("Panel.background", BG_PANEL);
        UIManager.put("Label.foreground", FG_TEXT);

        // 다크 톤에 어울리는 입력/버튼 톤
        UIManager.put("Button.background", new Color(44, 44, 54));
        UIManager.put("Button.foreground", FG_TEXT);
        UIManager.put("TextField.background", new Color(38, 38, 48));
        UIManager.put("TextField.foreground", FG_TEXT);

        // 전역 폰트 교체(모든 기본 폰트 엔트리를 순회하며 교체)
        setUIFont(FONT_UI);
    }

    // UIManager의 Defaults 중 Font 타입 항목만 선별해 일괄 교체
    private static void setUIFont(Font f) {
        var keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object val = UIManager.get(key);
            if (val instanceof Font) {
                UIManager.put(key, f);
            }
        }
    }
}
