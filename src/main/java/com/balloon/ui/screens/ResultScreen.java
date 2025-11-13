package com.balloon.ui.screens;

import com.balloon.core.ScreenId;
import com.balloon.core.ScreenRouter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * ResultScreen
 * - 점수 브레이크다운(단어/시간/정확도/아이템/총점) 테이블 표시
 * - GamePanel.showResult()에서 setResult()/setBreakdown()으로 값이 주입됨
 */
public class ResultScreen extends JPanel {

    private final ScreenRouter router;

    // 점수 항목
    private int wordScore;      // 단어 정답으로 얻은 점수
    private int timeScore;      // 남은 시간 점수(= timeBonus)
    private int accuracyScore;  // 정확도 환산 점수(없으면 0 유지)
    private int itemBonus;      // 아이템 보너스 합
    private int totalScore;     // 총점

    // 별도 표시용(정확도 %)
    private double accuracyPct; // 0~100 (%)

    // UI
    private JTable table;
    private DefaultTableModel model;
    private JLabel accLabel;

    // 기본 폰트 유틸(Theme 없이 동작)
    private static Font fontM() {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(14f);
    }
    private static Font fontXL() {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font("Dialog", Font.PLAIN, 12);
        return base.deriveFont(Font.BOLD, 26f);
    }

    public ResultScreen(ScreenRouter router) {
        this.router = router;

        // 초기값(0)이라도 화면은 뜨도록
        readFromContextSafely();

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // 상단 타이틀
        JLabel title = new JLabel("RESULT", SwingConstants.CENTER);
        title.setFont(fontXL());
        title.setBorder(BorderFactory.createEmptyBorder(24, 0, 12, 0));
        add(title, BorderLayout.NORTH);

        // 중앙: 정확도% 라벨 + 테이블
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);

        accLabel = new JLabel("", SwingConstants.RIGHT);
        accLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 8, 12));
        accLabel.setFont(fontM());
        center.add(accLabel, BorderLayout.NORTH);

        table = buildBreakdownTable();
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
        center.add(sp, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        // 하단 버튼
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 16));
        south.setOpaque(false);

        JButton toRanking = new JButton("랭킹 보기");
        JButton retry     = new JButton("다시 하기");
        JButton toMain    = new JButton("메인으로");

        toRanking.addActionListener(e -> router.show(ScreenId.RANKING));
        retry.addActionListener(e -> router.show(ScreenId.GAME));
        toMain.addActionListener(e -> router.show(ScreenId.START));

        south.add(toRanking);
        south.add(retry);
        south.add(toMain);

        add(south, BorderLayout.SOUTH);

        // 처음 표시 동기화
        refreshUI();
    }

    // ---------------------------
    // GamePanel에서 호출하는 메서드
    // ---------------------------

    /** 총점/정확도%/남은시간(=timeScore) 주입 */
    public void setResult(int totalScore, double accuracyRatio, int timeLeft) {
        this.totalScore  = totalScore;
        this.accuracyPct = accuracyRatio * 100.0; // 0~1 → %
        this.timeScore   = Math.max(0, timeLeft); // 남은 시간 점수

        refreshUI();
    }

    /** 브레이크다운 주입: 단어점수, 남은시간점수, 정확도점수, 아이템보너스 */
    public void setBreakdown(int wordScore, int timeBonus, int accuracyScore, int itemBonus) {
        this.wordScore     = wordScore;
        this.timeScore     = timeBonus;     // timeScore는 timeBonus와 동일 의미로 사용
        this.accuracyScore = accuracyScore; // 정확도 점수(없으면 0)
        this.itemBonus     = itemBonus;

        // 총점 재계산(안전)
        int sum = wordScore + timeScore + accuracyScore + itemBonus;
        if (sum > 0) this.totalScore = sum;

        refreshUI();
    }

    // ---------------------------

    private JTable buildBreakdownTable() {
        String[] cols = {"항목", "점수"};
        Object[][] rows = {
                {"단어 점수",      0},
                {"남은 시간 점수", 0},
                {"정확도 점수",    0},
                {"아이템 보너스",  0},
                {"총점",           0}
        };

        model = new DefaultTableModel(rows, cols) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 1 ? Integer.class : String.class;
            }
        };

        JTable t = new JTable(model);
        t.setRowHeight(28);
        t.setFont(fontM());
        t.getTableHeader().setFont(fontM().deriveFont(Font.BOLD));
        t.setFillsViewportHeight(true);

        // 총점 행 굵게
        t.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object v, boolean sel, boolean foc, int row, int col) {
                Component c = super.getTableCellRendererComponent(tbl, v, sel, foc, row, col);
                c.setFont(fontM());
                if (row == tbl.getRowCount() - 1) {
                    c.setFont(c.getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        return t;
    }

    /** 현재 필드값으로 라벨/테이블 갱신 */
    private void refreshUI() {
        if (accLabel != null) {
            if (accuracyPct > 0) accLabel.setText(String.format("정확도: %.1f%%", accuracyPct));
            else accLabel.setText("");
        }
        if (model != null) {
            model.setValueAt(wordScore,     0, 1);
            model.setValueAt(timeScore,     1, 1);
            model.setValueAt(accuracyScore, 2, 1);
            model.setValueAt(itemBonus,     3, 1);

            int sum = wordScore + timeScore + accuracyScore + itemBonus;
            if (sum == 0) sum = totalScore; // 총점만 먼저 들어온 경우 보정
            model.setValueAt(sum, 4, 1);
            totalScore = sum;
        }
    }

    /**
     * System property에서 값 읽기(없으면 0).
     * 나중에 GameContext/결과집계 객체로 교체 가능.
     */
    private void readFromContextSafely() {
        try {
            wordScore     = safeRead("WORD_SCORE");
            timeScore     = safeRead("TIME_SCORE");
            accuracyScore = safeRead("ACCURACY_SCORE");
            itemBonus     = safeRead("ITEM_BONUS");
            totalScore    = safeRead("TOTAL_SCORE");
            accuracyPct   = safeReadDouble("ACCURACY_PCT"); // 이미 %로 들어있으면 그대로
        } catch (Throwable ignore) {}

        if (totalScore == 0) {
            totalScore = wordScore + timeScore + accuracyScore + itemBonus;
        }
    }

    private int safeRead(String key) {
        try {
            String v = System.getProperty(key);
            if (v == null) return 0;
            return Integer.parseInt(v.trim());
        } catch (Throwable ignore) { return 0; }
    }

    private double safeReadDouble(String key) {
        try {
            String v = System.getProperty(key);
            if (v == null) return 0;
            return Double.parseDouble(v.trim());
        } catch (Throwable ignore) { return 0; }
    }
}
