package com.balloon.game;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvWordLoader {

    public static List<String> loadWords(String resourcePath) {
        List<String> list = new ArrayList<>();

        try (InputStream is = CsvWordLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("❌ CSV 파일을 찾을 수 없습니다: " + resourcePath);
                return list;
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {

                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        list.add(line);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}