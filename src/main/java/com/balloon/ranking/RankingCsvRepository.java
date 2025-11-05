package com.balloon.ranking;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * RankingCsvRepository
 * - CSV 파일(data/ranking.csv)을 읽어 RankingRecord 리스트로 변환한다.
 * - 파일이 없으면 디렉터리를 만들고 더미 CSV를 자동 생성한다.
 * - UI는 이 리포지토리의 loadAll()만 호출하면 데이터를 받을 수 있다.
 */
public class RankingCsvRepository {

    // 기본 CSV 위치: 프로젝트 루트 기준 data/ranking.csv
    private final Path csvPath = Paths.get("data", "ranking.csv");

    /**
     * CSV 전체를 읽어 List<RankingRecord>로 반환
     * - 첫 줄은 헤더로 간주하고 스킵
     * - 잘못된 행(컬럼 개수 부족 등)은 건너뜀
     */
    public List<RankingRecord> loadAll() {
        ensureExistsWithDummy(); // 없으면 자동 생성
        List<RankingRecord> list = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            boolean headerSkipped = false; // 첫 줄 헤더 스킵용 플래그

            while ((line = br.readLine()) != null) {
                if (!headerSkipped) {
                    // 첫 줄은 "name,score,accuracy,timeLeft,playedAt" 헤더라고 가정하고 넘긴다.
                    headerSkipped = true;
                    continue;
                }
                if (line.isBlank()) continue; // 빈 줄 무시

                // 간단 CSV 분리: 쉼표 기준, 좌우 공백 제거
                String[] t = splitCsv(line);

                // 컬럼이 부족하면 해당 행 무시
                if (t.length < 5) continue;

                // 각 필드 파싱(숫자형은 안전 파서로)
                String name = t[0].trim();
                int score = safeInt(t[1]);
                double accuracy = safeDouble(t[2]);
                int timeLeft = safeInt(t[3]);
                String playedAt = t[4].trim();

                // 한 행을 도메인 객체로 만들어 리스트에 추가
                list.add(new RankingRecord(name, score, accuracy, timeLeft, playedAt));
            }
        } catch (IOException e) {
            // 개발 단계에서는 콘솔로 에러 확인
            e.printStackTrace();
        }

        return list;
    }

    // --- 내부 유틸 메서드들 ---

    /**
     * 간단한 CSV 분리기
     * - 큰따옴표로 감싼 필드 처리 같은 복잡한 케이스는 고려하지 않는다.
     * - 현재 프로젝트 데이터는 단순 쉼표 구분으로 충분하다고 가정한다.
     */
    private String[] splitCsv(String line) {
        return line.split("\\s*,\\s*");
    }

    /** 정수 안전 파싱: 실패하면 0 반환 */
    private int safeInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /** 실수 안전 파싱: 실패하면 0.0 반환 */
    private double safeDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * CSV 파일이 없으면:
     * - data 디렉터리 생성
     * - 헤더 포함한 더미 데이터를 기록
     * 이렇게 해두면 UI는 파일 유무와 관계없이 바로 표를 띄워볼 수 있다.
     */
    private void ensureExistsWithDummy() {
        try {
            // 상위 디렉터리(data)가 없으면 생성
            if (csvPath.getParent() != null && Files.notExists(csvPath.getParent())) {
                Files.createDirectories(csvPath.getParent());
            }

            // CSV 파일이 없으면 신규로 생성 + 더미 데이터 작성
            if (Files.notExists(csvPath)) {
                List<String> lines = List.of(
                        // 헤더(순서 주의)
                        "name,score,accuracy,timeLeft,playedAt",
                        // 더미 행들(테스트용 데이터)
                        "jay,1510,89.0,5,2025-11-04 09:32:45",
                        "momo,1230,96.5,18,2025-11-03 22:11:03",
                        "suyeon,980,91.0,12,2025-11-03 21:55:10",
                        // 동점/정렬 테스트용(점수 동일, 정확도/시간 비교)
                        "rina,1510,92.0,3,2025-11-04 10:00:01"
                );
                Files.write(csvPath, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
