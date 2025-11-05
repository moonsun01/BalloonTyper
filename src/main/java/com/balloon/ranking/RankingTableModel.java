package com.balloon.ranking;

import javax.swing.table.AbstractTableModel;
import java.util.List;

/**
 * RankingTableModel
 * - JTable에 데이터를 공급하는 모델 클래스
 * - "어떤 컬럼 이름/타입을 가질지"와 "행/열의 실제 값"을 정의한다.
 */
public class RankingTableModel extends AbstractTableModel {

    // 테이블 컬럼 헤더 텍스트 (표 상단 제목들)
    private final String[] columns = {
            "순위",        // 0: 화면용 순위(데이터에 없음 → rowIndex+1로 계산)
            "이름",        // 1
            "점수",        // 2
            "정확도(%)",   // 3
            "남은시간(초)", // 4
            "플레이시각"   // 5
    };

    // 실제 데이터 소스: CSV -> List<RankingRecord>
    private List<RankingRecord> data;

    // 생성자: 외부에서 읽어온 리스트를 초기값으로 전달받는다.
    public RankingTableModel(List<RankingRecord> data) {
        this.data = data;
    }

    // 데이터 통째로 교체할 때 사용 (UI 새로고침 포함)
    public void setData(List<RankingRecord> data) {
        this.data = data;
        // 모델이 바뀌었음을 JTable에 알린다 → 화면 리프레시
        fireTableDataChanged();
    }

    // 행 개수: 데이터 리스트 크기
    @Override
    public int getRowCount() {
        return (data == null) ? 0 : data.size();
    }

    // 열 개수: columns 배열 길이
    @Override
    public int getColumnCount() {
        return columns.length;
    }

    // 각 컬럼의 화면 표시 이름
    @Override
    public String getColumnName(int column) {
        return columns[column];
    }

    // 각 컬럼의 "자료형"을 JTable에 알려준다 (정렬/렌더링 힌트)
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> Integer.class; // 순위(계산치)
            case 1 -> String.class;  // 이름
            case 2 -> Integer.class; // 점수
            case 3 -> Double.class;  // 정확도
            case 4 -> Integer.class; // 남은시간
            case 5 -> String.class;  // 플레이시각(문자열)
            default -> Object.class;
        };
    }

    // (row, col)에 해당하는 셀 값 반환
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        // 방어코드: 데이터가 없으면 null
        if (data == null || rowIndex < 0 || rowIndex >= data.size()) return null;

        RankingRecord r = data.get(rowIndex);

        return switch (columnIndex) {
            case 0 -> rowIndex + 1;        // 순위는 데이터가 아니라 "현재 정렬 후의 행번호 + 1"
            case 1 -> r.getName();
            case 2 -> r.getScore();
            case 3 -> r.getAccuracy();
            case 4 -> r.getTimeLeft();
            case 5 -> r.getPlayedAt();
            default -> null;
        };
    }

    // (옵션) 외부에서 특정 행의 원본 객체를 가져오고 싶을 때 사용
    public RankingRecord getAt(int row) {
        return (data == null || row < 0 || row >= data.size()) ? null : data.get(row);
    }
}
