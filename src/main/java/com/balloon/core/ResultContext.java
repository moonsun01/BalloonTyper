// [L1]  패키지 선언
package com.balloon.core;                             // [L1]

// [L2]  Router 전환 직전에 GamePanel이 set() 해두고,
// [L3]  ResultScreen.onShown()에서 get()으로 읽는 단순 버스
public final class ResultContext {                    // [L2]
    // [L4]  마지막 결과 저장소(스레드 1개인 Swing EDT 기준, 동기화 불필요)
    private static ResultData lastResult;            // [L4]

    // [L5]  외부 생성 금지 (유틸 클래스)
    private ResultContext() {}                       // [L5]

    // [L6]  결과 저장
    public static void set(ResultData data) {        // [L6]
        lastResult = data;                           // [L7]
    }                                                // [L8]

    // [L9]  결과 조회(읽은 뒤에도 남겨둠)
    public static ResultData get() {                 // [L9]
        return lastResult;                           // [L10]
    }                                                // [L11]

    // [L12] 결과 비우기(재시작 등에서 사용)
    public static void clear() {                     // [L12]
        lastResult = null;                           // [L13]
    }                                                // [L14]
}                                                    // [L15]
