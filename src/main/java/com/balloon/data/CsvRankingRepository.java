package com.balloon.data;

import com.balloon.core.GameMode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CSV 파일에 랭킹을 저장/로드하는 구현체.
 * - 컬럼: name,score,mode
 * - 예전 파일(name,score)도 자동으로 SINGLE 모드로 처리
 * - 헤더가 없어도 읽기 가능
 */
public class CsvRankingRepository implements RankingRepository {

    // CSV 헤더 한 줄
    private static final String HEADER = "name,score,mode";

    // ---------------- 저장 ----------------
    @Override
    public void save(ScoreEntry entry) {
        File file = new File(FilesConfig.RANKING_FILE);

        boolean needHeader = !file.exists() || file.length() == 0;

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            if (needHeader) {
                bw.write(HEADER);
                bw.newLine();
            }

            // name,score,mode 한 줄로 기록
            String line = entry.getName()
                    + "," + entry.getScore()
                    + "," + entry.getMode().name();
            bw.write(line);
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ResultScreen에서 사용하는 헬퍼: "한 줄 추가" 의미
    // 내부적으로는 기존 save(...) 를 그대로 재사용한다.
    public void append(ScoreEntry entry) {
        save(entry);
    }

    // ---------------- 전체 로드 ----------------
    @Override
    public List<ScoreEntry> loadAll() {
        File file = new File(FilesConfig.RANKING_FILE);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        List<ScoreEntry> result = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            if (line == null) {
                return result;
            }

            boolean hasHeader = line.toLowerCase().startsWith("name");

            // 첫 줄이 헤더가 아니면 데이터로 처리
            if (!hasHeader) {
                parseLine(line, result);
            }

            while ((line = br.readLine()) != null) {
                parseLine(line, result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 점수 내림차순 정렬
        result.sort(Comparator.comparingInt(ScoreEntry::getScore).reversed());
        return result;
    }

    // 한 줄 파싱해서 리스트에 추가
    private void parseLine(String line, List<ScoreEntry> out) {
        if (line == null || line.isBlank()) return;

        String[] cols = line.split(",");
        if (cols.length < 2) return; // name, score 없으면 버림

        String name = cols[0].trim();

        int score;
        try {
            score = Integer.parseInt(cols[1].trim());
        } catch (NumberFormatException e) {
            return;
        }

        // 기본값: 예전 CSV(name,score) 파일은 SINGLE
        GameMode mode = GameMode.SINGLE;
        if (cols.length >= 3 && !cols[2].isBlank()) {
            try {
                mode = GameMode.valueOf(cols[2].trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 이상한 값이면 그대로 SINGLE 유지
            }
        }

        out.add(new ScoreEntry(name, score, mode));
    }

    // ---------------- 상위 N개 ----------------
    @Override
    public List<ScoreEntry> topN(int n) {
        if (n <= 0) {
            return Collections.emptyList();
        }

        List<ScoreEntry> all = loadAll();
        if (all.size() <= n) {
            return all;
        }

        return new ArrayList<>(all.subList(0, n));
    }
}
