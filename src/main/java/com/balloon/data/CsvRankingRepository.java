package com.balloon.data;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CsvRankingRepository implements RankingRepository {

    @Override
    public void save(ScoreEntry entry) {
        // 필요 시 상위 디렉토리 생성
        File file = new File(FilesConfig.RANKING_FILE);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            // 실패해도 그냥 진행(앱 종료 안 되도록)
            try { parent.mkdirs(); } catch (Exception ignore) {}
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
            // 포맷: name,score (헤더 없음)
            bw.write(entry.getName() + "," + entry.getScore());
            bw.newLine();
        } catch (IOException e) {
            e.printStackTrace(); // 기존 정책 유지
        }
    }

    // "name,score" 한 줄 파싱(깨진 라인은 null)
    private ScoreEntry parseLine(String line) {
        if (line == null || line.isBlank()) return null;

        // 콤마가 여러 개여도 앞 2개만 사용
        String[] split = line.split(",", 3);
        if (split.length < 2) return null;

        String name = split[0];
        try {
            int score = Integer.parseInt(split[1].trim());
            return new ScoreEntry(name, score);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // 파일없음/에러 시 null 반환(기존 정책과 동일)
    @Override
    public List<ScoreEntry> loadAll() {
        File file = new File(FilesConfig.RANKING_FILE);
        if (!file.exists()) return null;

        List<ScoreEntry> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                ScoreEntry e = parseLine(line);
                if (e != null) entries.add(e);
            }
        } catch (IOException e) {
            return null; // 기존 정책
        }
        return entries;
    }

    // 점수 내림차순 상위 N
    @Override
    public List<ScoreEntry> topN(int n) {
        if (n <= 0) return Collections.emptyList();

        List<ScoreEntry> all = loadAll();
        if (all == null || all.isEmpty()) return Collections.emptyList();

        all.sort(new Comparator<ScoreEntry>() {
            @Override
            public int compare(ScoreEntry a, ScoreEntry b) {
                return Integer.compare(b.getScore(), a.getScore());
            }
        });

        return (n >= all.size()) ? all : new ArrayList<>(all.subList(0, n));
    }
}
