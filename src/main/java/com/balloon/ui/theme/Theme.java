package com.balloon.ui.theme; // UIManager 전역 스타일 주입

import javax.swing.*; // UIManager
import java.awt.*;    // Color, Font

public final class Theme { // 인스턴스화 방지
    private Theme() {}

    // 현재 모드(다크/라이트)에 맞춰 색 선택
    private static Color bg()    { return DesignSpec.DARK ? DesignSpec.DARK_BG    : DesignSpec.LIGHT_BG; }
    private static Color card()  { return DesignSpec.DARK ? DesignSpec.DARK_CARD  : DesignSpec.LIGHT_CARD; }
    private static Color fg()    { return DesignSpec.DARK ? DesignSpec.DARK_FG    : DesignSpec.LIGHT_FG; }
    private static Color mute()  { return DesignSpec.DARK ? DesignSpec.DARK_MUTE  : DesignSpec.LIGHT_MUTE; }
    private static Color input() { return DesignSpec.DARK ? DesignSpec.DARK_INPUT : DesignSpec.LIGHT_INPUT; }
    private static Color grid()  { return DesignSpec.DARK ? DesignSpec.DARK_GRID  : DesignSpec.LIGHT_GRID; }

    // Launcher에서 호출하는 전역 적용 메서드(네 파일에 맞춰 이름 통일)
    public static void applyGlobalUI() {
        // 라벨/텍스트
        UIManager.put("Label.foreground", fg());                        // 라벨 글자색
        UIManager.put("Label.font", DesignSpec.FONT_BODY);              // 라벨 폰트

        // 패널/배경
        UIManager.put("Panel.background", card());                      // 패널 배경
        UIManager.put("Panel.foreground", fg());                        // 패널 전경

        // 다이얼로그/옵션
        UIManager.put("OptionPane.background", card());                 // 옵션 배경
        UIManager.put("OptionPane.messageForeground", fg());            // 옵션 텍스트색
        UIManager.put("OptionPane.font", DesignSpec.FONT_BODY);         // 옵션 폰트

        // 버튼
        UIManager.put("Button.background", DesignSpec.ACCENT);          // 버튼 배경
        UIManager.put("Button.foreground", DesignSpec.ACCENT_TEXT_ON);  // 버튼 글자색
        UIManager.put("Button.font", DesignSpec.FONT_BODY);             // 버튼 폰트

        // 입력 필드
        UIManager.put("TextField.background", input());                 // 입력 배경
        UIManager.put("TextField.foreground", fg());                    // 입력 글자
        UIManager.put("TextField.caretForeground", fg());               // 캐럿 색

        // 스크롤/테이블
        UIManager.put("ScrollPane.background", card());                 // 스크롤 배경
        UIManager.put("Table.background", card());                      // 테이블 배경
        UIManager.put("Table.foreground", fg());                        // 테이블 글자
        UIManager.put("Table.font", DesignSpec.FONT_BODY);              // 테이블 폰트
        UIManager.put("Table.gridColor", grid());                       // 테이블 그리드
        UIManager.put("TableHeader.background",
                DesignSpec.DARK ? new Color(0x1D2230) : new Color(0xE9ECF4));     // 테이블 헤더 배경
        UIManager.put("TableHeader.foreground", fg());                  // 테이블 헤더 글자
        UIManager.put("TableHeader.font", DesignSpec.FONT_H2);          // 테이블 헤더 폰트
    }

    // 자주 쓰는 헬퍼(화면에서 바로 쓰기 좋게)
    public static Font h1()   { return DesignSpec.FONT_H1; }            // H1
    public static Font h2()   { return DesignSpec.FONT_H2; }            // H2
    public static Font body() { return DesignSpec.FONT_BODY; }          // 본문
    public static Color bgColor()   { return bg(); }                     // 전체 배경색
    public static Color cardColor() { return card(); }                   // 카드 배경색
    public static Color fgColor()   { return fg(); }                     // 텍스트색
    public static int  gapMD() { return DesignSpec.GAP_MD; }             // 여백 MD (필요시)

    // ========================
// 레거시 이름 호환 상수 (기존 코드 빌드용)
// - 기존 파일에서 쓰던 이름을 새 디자인 토큰으로 매핑합니다.
// - @Deprecated는 '나중에 교체하자'는 표시일 뿐 동작에는 영향이 없습니다.
// ========================
    @Deprecated public static final java.awt.Color BG_DARK    = DesignSpec.DARK_BG;   // 예전: 어두운 배경색 → 현재 토큰 매핑
    @Deprecated public static final java.awt.Font  FONT_HUD   = DesignSpec.FONT_MONO; // 예전: HUD용 고정폭 폰트 → 모노 폰트 매핑
    @Deprecated public static final java.awt.Color HUD_ACCENT = DesignSpec.ACCENT;    // 예전: HUD 포인트색 → 브랜드 포인트색 매핑

    // (선택) 다른 파일에서 쓸 수도 있는 일반 별칭들
    @Deprecated public static final java.awt.Color BG         = bg();                 // 배경색 별칭
    @Deprecated public static final java.awt.Color FG         = fg();                 // 전경(텍스트)색 별칭
    @Deprecated public static final java.awt.Color CARD_BG    = card();               // 카드 배경색 별칭
    @Deprecated public static final java.awt.Color ACCENT     = DesignSpec.ACCENT;    // 포인트색 별칭

    // ========================
// 레거시 이름 호환 상수 추가 (BalloonRenderer 등에서 참조)
// ========================

    // 풍선 그림자색(반투명 검정) – 기존 BAL_SHADOW 대체
    @Deprecated public static final java.awt.Color BAL_SHADOW =
            new java.awt.Color(0, 0, 0, 60);                 // 살짝 번지는 느낌의 그림자(알파 60)

    // 풍선 외곽선 색(약한 흰색) – 기존 BAL_STROKE 대체
    @Deprecated public static final java.awt.Color BAL_STROKE =
            new java.awt.Color(255, 255, 255, 110);          // 유리/풍선 하이라이트용 연한 외곽선

    // 풍선 안 “단어” 폰트 – 기존 FONT_WORD 대체
    @Deprecated public static final java.awt.Font FONT_WORD =
            new java.awt.Font(DesignSpec.FONT_STACK,          // 폰트 스택(한글 가독성)
                    java.awt.Font.BOLD,             // 굵게
                    20);                            // 크기(가독 좋은 기본값)

// ========================
// 레거시 이름 호환 상수 (패널/텍스트 계열)
// ========================

    // 예전: 패널 배경색
    @Deprecated public static final java.awt.Color BG_PANEL = card();
    // 예전: 전경(텍스트) 색
    @Deprecated public static final java.awt.Color FG_TEXT  = fg();

    // 선택: 비슷한 별칭들(혹시 다른 파일에서 쓸 수 있어 예방용)
    @Deprecated public static final java.awt.Color PANEL_BG = card();
    @Deprecated public static final java.awt.Color TEXT_FG  = fg();
    @Deprecated public static final java.awt.Color HUD_BG   = card();
    @Deprecated public static final java.awt.Color FG_HUD   = fg();

}
