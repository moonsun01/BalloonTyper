package com.balloon.ranking;                                     // CSV 입출력 유틸 패키지

import java.io.*;                                                // 파일 IO 전반
import java.nio.charset.StandardCharsets;                        // UTF-8 인코딩
import java.util.*;                                              // List, Comparator
// 정렬/파싱/보호용 유틸

public final class RankingCSV {                                  // 유틸 클래스로 선언(인스턴스 불가)
    private RankingCSV() {}                                      // 생성자 감추기

    private static final String DIR  = "data";                   // CSV 디렉터리(프로젝트 루트 하위)
    private static final String PATH = "data/ranking.csv";       // CSV 파일 경로
    private static final String HEADER =                          // CSV 헤더 라인
            "name,score,accuracy,timeLeft,epochMillis";          //  컬럼명들

    public static synchronized void append(RankingEntry e) {     // 레코드 추가(동기화=스레드 안전)
        try {                                                     // 예외 처리 시작
            File dir = new File(DIR);                             // 디렉터리 객체 생성
            if (!dir.exists()) dir.mkdirs();                      // 없으면 생성
            boolean needHeader = !new File(PATH).exists();        // 헤더 필요 여부(파일이 없으면 필요)
            try (PrintWriter pw = new PrintWriter(                // 출력 스트림 생성(append)
                    new OutputStreamWriter(
                            new FileOutputStream(PATH, true),     // append=true
                            StandardCharsets.UTF_8))) {           // UTF-8 인코딩
                if (needHeader) pw.println(HEADER);               // 새 파일이면 헤더 먼저 기록
                pw.printf("%s,%d,%.2f,%d,%d%n",                   // CSV 한 줄 출력 포맷
                        sanitize(e.name),                         //  콤마 제거 등 안전 처리된 이름
                        e.score,                                  //  점수
                        e.accuracy,                               //  정확도(소수 둘째 자리까지)
                        e.timeLeft,                               //  남은 시간
                        e.epochMillis);                           //  시각(ms)
            }                                                     // try-with-resources 자동 close
        } catch (IOException ex) {                                // 파일 예외 캐치
            ex.printStackTrace();                                 // 콘솔에 로그 출력
        }                                                         // 예외 처리 끝
    }                                                             // append 끝

    public static synchronized List<RankingEntry> readAllSorted() { // 전체 읽기 + 정렬
        List<RankingEntry> list = new ArrayList<>();              // 결과 리스트
        File f = new File(PATH);                                  // 파일 객체
        if (!f.exists()) return list;                             // 없으면 빈 리스트 반환

        try (BufferedReader br = new BufferedReader(              // 버퍼드 리더 생성
                new InputStreamReader(
                        new FileInputStream(f),                   // 파일 입력 스트림
                        StandardCharsets.UTF_8))) {               // UTF-8
            String line;                                          // 읽은 한 줄
            boolean first = true;                                 // 첫 줄(헤더) 여부
            while ((line = br.readLine()) != null) {              // EOF까지 반복
                if (first) { first = false; continue; }           // 헤더 스킵
                String[] a = line.split(",", -1);                 // 콤마 기준 분리(빈 칸 유지)
                if (a.length < 5) continue;                       // 컬럼 부족 방어
                String name = a[0];                               // 이름
                int score = parseInt(a[1]);                       // 점수 파싱
                double acc = parseDouble(a[2]);                   // 정확도 파싱
                int tl = parseInt(a[3]);                          // 남은 시간 파싱
                long ms = parseLong(a[4]);                        // 시각 파싱
                list.add(new RankingEntry(name, score, acc, tl, ms)); // 리스트에 추가
            }                                                     // while 끝
        } catch (IOException ex) {                                // 읽기 중 예외
            ex.printStackTrace();                                 // 로그 출력
        }                                                         // try-catch 끝

        list.sort(                                                // 정렬 기준 정의
                Comparator                                        // 1) 점수 내림차순
                        .comparingInt((RankingEntry r) -> r.score).reversed()
                        .thenComparingDouble(r -> r.accuracy).reversed() // 2) 정확도 내림차순
                        .thenComparingInt(r -> r.timeLeft).reversed()     // 3) 남은 시간 내림차순
                        .thenComparingLong(r -> r.epochMillis).reversed() // 4) 최신 기록 우선
        );                                                        // 정렬 수행
        return list;                                              // 정렬된 결과 반환
    }                                                             // readAllSorted 끝

    private static String sanitize(String s) {                    // CSV 안전 문자열 변환
        if (s == null) return "";                                 // null 방어
        return s.replace(",", " ");                               // 콤마 제거(필드 깨짐 방지)
    }                                                             // sanitize 끝

    private static int parseInt(String s) {                       // 안전 정수 파서
        try { return Integer.parseInt(s.trim()); }                //  정상 파싱 시 반환
        catch (Exception e) { return 0; }                         //  실패 시 0
    }                                                             // parseInt 끝

    private static double parseDouble(String s) {                 // 안전 실수 파서
        try { return Double.parseDouble(s.trim()); }              //  정상 파싱 시 반환
        catch (Exception e) { return 0.0; }                       //  실패 시 0.0
    }                                                             // parseDouble 끝

    private static long parseLong(String s) {                     // 안전 long 파서
        try { return Long.parseLong(s.trim()); }                  //  정상 파싱 시 반환
        catch (Exception e) { return 0L; }                        //  실패 시 0L
    }                                                             // parseLong 끝
}                                                                 // 클래스 끝
