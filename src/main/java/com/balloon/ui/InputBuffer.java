// [TEMP-A] Team A 임시 구현: 팀 B/C 코드 머지되면 교체 예정
// 사용 메서드: appendChar, backspace, clear, getText


package com.balloon.ui;

/** 키 입력을 누적/편집하는 간단한 버퍼 */
public class InputBuffer {
    private final StringBuilder sb = new StringBuilder();

    /** 출력 가능한 키만 GamePanel에서 appendChar로 넣어줌 */
    public void appendChar(char c) {
        sb.append(c);
    }

    /** 마지막 글자 삭제 (백스페이스) */
    public void backspace() {
        if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
    }

    /** 전체 지우기 (ESC) */
    public void clear() {
        sb.setLength(0);
    }

    /** 현재 입력 문자열 반환 */
    public String getText() {
        return sb.toString();
    }
}
