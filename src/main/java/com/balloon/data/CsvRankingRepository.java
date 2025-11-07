package com.balloon.data;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CsvRankingRepository implements RankingRepository {
    @Override
    public void save(ScoreEntry entry) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FilesConfig.RANKING_FILE, true))) {
            bw.write(entry.getName() + "," + entry.getScore());
            bw.newLine();   // 줄바꿈
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // CSV 한 줄을 ScoreEntry로 파싱 (깨진 라인은 null 반환)
    private ScoreEntry parseLine(String line){
        if (line == null || line.isBlank()) return null;

        String[] split = line.split(",");
        if (split.length != 2) return null;

        String name = split[0];
        try {
            int score = Integer.parseInt(split[1]);
            return new ScoreEntry(name, score);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // csv 파일 전체를 읽어서 ScoreEntry 리스트로 반환 (파일없음/에러 시 null 반환: 기존 정책 유지)
    @Override
    public List<ScoreEntry> loadAll() {
        List<ScoreEntry> entries = new ArrayList<>();

        File file = new File(FilesConfig.RANKING_FILE);
        if (!file.exists()) return null;

        try (BufferedReader br = new BufferedReader(new FileReader(FilesConfig.RANKING_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                ScoreEntry entry = parseLine(line);
                if (entry != null) entries.add(entry);
            }
        } catch (IOException e) {
            return null;
        }
        return entries;
    }

    // 점수 내림차순으로 정렬해 상위 n개 반환 (loadAll()이 null일 수 있어 NPE 방지)
    @Override
    public List<ScoreEntry> topN(int n) {
        if (n <= 0) return Collections.emptyList();

        List<ScoreEntry> all = loadAll();
        if (all == null || all.isEmpty()) return Collections.emptyList();

        all.sort(new Comparator<ScoreEntry>() {
            @Override
            public int compare(ScoreEntry a, ScoreEntry b) {
                return Integer.compare(b.getScore(), a.getScore()); // 내림차순
            }
        });

        if (n >= all.size()) return all;
        return new ArrayList<>(all.subList(0, n));
    }
}

