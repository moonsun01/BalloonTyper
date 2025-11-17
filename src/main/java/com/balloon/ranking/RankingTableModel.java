package com.balloon.ranking;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * RankingTableModel
 * - 화면에 보여줄 랭킹 테이블 모델.
 * - 컬럼: rank, NAME, SCORE, accuracy (4개만 사용)
 */
public class RankingTableModel extends AbstractTableModel {

    private final String[] columns = {"rank", "NAME", "SCORE", "accuracy"};
    private List<RankingRecord> rows = new ArrayList<>();

    public RankingTableModel(List<RankingRecord> initial) {
        setData(initial);
    }

    public void setData(List<RankingRecord> data) {
        rows = new ArrayList<>(data);

        // 정렬된 순서대로 1,2,3,... 순위 부여
        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setRank(i + 1);
        }

        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.length;  // 4
    }

    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> Integer.class; // rank
            case 1 -> String.class;  // name
            case 2 -> Integer.class; // score
            case 3 -> Double.class;  // accuracy
            default -> Object.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        RankingRecord r = rows.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> r.getRank();
            case 1 -> r.getName();
            case 2 -> r.getScore();
            case 3 -> r.getAccuracy(); // 렌더러에서 포맷팅
            default -> null;
        };
    }
}
